package com.cloud.bridge.auth.ec2;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.bridge.persist.dao.CloudStackUserDao;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;
import com.cloud.bridge.service.exception.EC2ServiceException.ServerError;


/**
 * An authentication handler for EC2 Query API requests.
 *
 * Specifications:
 * 	- AWS4: http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
 *
 */
public class QueryAPIAuthHandler {
	public static final Logger logger = Logger.getLogger(QueryAPIAuthHandler.class);

	protected String apiKey;
	protected String secretKey;
	protected String amzDateTime;
	protected String scopeDate;
	protected String scopeRegion;
	protected String scopeService;
	protected String signature;
	protected String requestBody;
	protected String[] signedHeaders;
	protected HttpServletRequest request;
	protected SupportedAuthSchemes authScheme;

	protected CloudStackUserDao userDao;

	/**
	 * The authentication schemes/versions supported by this handler
	 */
	public enum SupportedAuthSchemes {
		AWS4("AWS4-HMAC-SHA256", "HmacSHA256", "aws4_request");

		private String name;
		private String algorithm;
		private String requestConstant;

		private SupportedAuthSchemes(String name, String algorithm, String reqConst) {
			this.name = name;
			this.algorithm = algorithm;
			this.requestConstant = reqConst;
		}

		public String getName() { return name; }
		public String getRequestConstant() { return requestConstant; }
		public String getAuthAlgorithmName() { return algorithm; }
		public Mac getAuthAlgorithm() {
			try {
				return Mac.getInstance(algorithm);
			} catch (NoSuchAlgorithmException e) {
				logger.error(String.format("Unable to get MAC instance for %", this.name));
				throw new EC2ServiceException(ServerError.InternalError, "Unable to verify request");
			}
		}
	}

	public QueryAPIAuthHandler(HttpServletRequest request, CloudStackUserDao userDao) {
		this.request = request;
		this.userDao = userDao;
	}

	/**
	 * The main authentication method. Call this to authenticate the request. The method
	 * will verify signature version compatibility, verify the sanity of all auth parameters,
	 * the existence and validity of the user with the corresponding API key, the
	 * time scope, and finally verify the signature.
	 *
	 * @return true upon successful authentication, false on failure. Will also throw
	 * an {@link EC2ServiceException} with the appropriate errors messages if invalid
	 * or malformed values are encountered.
	 */
	public boolean authenticate() {
		if (!verifyAuthScheme())
			throw new EC2ServiceException(ClientError.Unsupported, "Unsupported authentication scheme");

		if (!verifyAuthParams())
			throw new EC2ServiceException(ClientError.AuthFailure, "Invalid authentication parameters");

		if (!verifyTimeScope())
			throw new EC2ServiceException(ClientError.AuthFailure, "Scope has expired");

		if (!verifyApiKey())
			throw new EC2ServiceException(ClientError.AuthFailure, "Unable to verify API key");

		if (!verifySignature())
			throw new EC2ServiceException(ClientError.SignatureDoesNotMatch, "Unable to verify signature");

		return true;
	}

	/**
	 * Verify we support the proposed authentication scheme.
	 */
	protected boolean verifyAuthScheme() {
		reconstructPayload();
		String authHeader = request.getHeader("Authorization");

		for (SupportedAuthSchemes supportedAuthScheme : SupportedAuthSchemes.values()) {
			if (authHeader.startsWith(supportedAuthScheme.getName())) {
				this.authScheme = supportedAuthScheme;
				return true;
			}
		}

		logger.debug(String.format("Unable to find supported auth scheme for Authorization header, %s", authHeader));
		return false;
	}

	/**
	 * Verify all provided authentication parameters.
	 *
	 * In AWS Signature Version 4, these are:
	 *
	 *  Credentials: A slash separated string on the form <APIkey>/<ScopeDate>/<Region>/<Service>/aws4_request
	 *  SignedHeaders: An ordered, lower-case, semicolon separated list of request headers used for signing
	 *  Signature: A lower-case hex encoded HmacSHA256 signature.
	 *
	 */
	protected boolean verifyAuthParams() {
		switch (authScheme) {
		case AWS4:
			String credentials = getAuthHeaderComponent("Credential");
			String credentialsPattern = "(.*)/(\\d{8})/(.*)/(.*)/(.*)";
			Matcher matcher = Pattern.compile(credentialsPattern).matcher(credentials);

			if (!matcher.find())
				return false;

			// Note that even if "X-Amz-Date" is not listed as one of the signed headers we will still need to 
			// privide a timestamp for the request string to be signed.
			amzDateTime = getAmzDate();

			apiKey = matcher.group(1);
			scopeDate = matcher.group(2);
			scopeRegion = matcher.group(3);
			scopeService = matcher.group(4);

			if (apiKey.isEmpty() || amzDateTime.isEmpty() || scopeDate.isEmpty() ||
					scopeRegion.isEmpty() || scopeService.isEmpty() ||
					!matcher.group(5).equals(authScheme.getRequestConstant())) {

				logger.debug(String.format("Malformed auth header credential component: %s", credentials));
				return false;
					}

			signature = getAuthHeaderComponent("Signature");
			if (signature.isEmpty()) {
				logger.debug("Malformed or missing auth header signature component");
				return false;
			}

			signedHeaders = getAuthHeaderComponent("SignedHeaders").split(";");
			if (signedHeaders.length == 0) {
				logger.debug("Malformed or missing auth header signed headers component");
				return false;
			}

			return true;
		}
		return false;
	}

	/**
	 * Verify the scope is no more than 7 days old.
	 */
	protected boolean verifyTimeScope() {
		int scopeYear = Integer.valueOf(scopeDate.substring(0,4));
		int scopeMonth = Integer.valueOf(scopeDate.substring(4,6)) - 1;
		int scopeDay = Integer.valueOf(scopeDate.substring(6,8));

		Calendar now = Calendar.getInstance();
		Calendar scope = Calendar.getInstance();
		scope.set(scopeYear, scopeMonth, scopeDay);

		long difference = now.getTimeInMillis() - scope.getTimeInMillis();
		boolean result = difference <= TimeUnit.DAYS.toMillis(7);

		if (!result)
			logger.debug("Request scope has expired");

		return result;
	}

	/**
	 * Verify the provided API key has a corresponding secret key in the database.
	 */
	protected boolean verifyApiKey() {
		secretKey = userDao.getSecretKeyByAccessKey(apiKey);

		if (secretKey == null)
			logger.debug(String.format("Unable to find API key: %s", apiKey));

		return secretKey != null;
	}

	/**
	 * Helper method to convert bytes to hex.
	 *
	 * @param bytes The byte array to be converted
	 * @return The byte array as lower case hex string.
	 */
	private String byteArrayToHex(byte[] bytes) {
		StringBuilder buffer = new StringBuilder(bytes.length * 2);
		for(byte item: bytes) {
			buffer.append(String.format("%02x", item & 0xff));
		}
		return buffer.toString();
	}

	/**
	 * Verify the provided signature.
	 */
	protected boolean verifySignature() {
		byte[] signatureBytes = hmac(getSigningKey(), getStringToSign());
        String calculatedSignature = byteArrayToHex(signatureBytes);

		boolean signatureMatch = calculatedSignature.equals(signature);

		if (!signatureMatch)
			logger.debug(String.format("Signatures do not match; local: %s, remote: %s", calculatedSignature, signature));

		return signatureMatch;
	}

	/**
	 * Constructs a signing key.
	 *
	 * For AWS Signature Version 4, this amounts to:
	 *
	 *  DateKey              = HMAC-SHA256("AWS4"+"<SecretAccessKey>", "<yyyymmdd>")
	 *  DateRegionKey        = HMAC-SHA256(<DateKey>, "<aws-region>")
	 *  DateRegionServiceKey = HMAC-SHA256(<DateRegionKey>, "<aws-service>")
	 *  SigningKey           = HMAC-SHA256(<DateRegionServiceKey>, "aws4_request")
	 *
	 * @return A byte array containing the signing key
	 */
	protected byte[] getSigningKey() {
		switch (authScheme) {
		case AWS4:
			try {
				byte[] scrtKeyArr = String.format("AWS4%s", secretKey).getBytes("UTF-8");
				byte[] dateKey    = hmac(scrtKeyArr, scopeDate);
				byte[] regionKey  = hmac(dateKey, scopeRegion);
				byte[] serviceKey = hmac(regionKey, scopeService);
				byte[] signingKey = hmac(serviceKey, authScheme.getRequestConstant());

				return signingKey;
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				break;
			}
		}

		return null;
	}
	
	/**
	 * Constructs a string for signing.
	 *
	 * For AWS Signature Version 4, this follows the pattern:
	 *
	 *  "AWS4-HMAC-SHA256" + "\n" +
	 *  TimeStampInISO8601Format + "\n" +
	 *  <Scope> + "\n" +
	 *  Hex(SHA256Hash(<CanonicalRequest>))
	 *
	 * @return
	 */
	protected String getStringToSign() {
		StringBuilder str = new StringBuilder(512);

		switch (authScheme) {
		case AWS4:
			str.append(authScheme.getName());						// "AWS4-HMAC-SHA256"
			str.append("\n");
			str.append(amzDateTime);								// TimeStampInISO8601Format
			str.append("\n");
			str.append(getScope());									// Scope
			str.append("\n");
			str.append(Hex.encodeHex(hash(getCanonicalRequest())));	// Hex(SHA256Hash(<CanonicalRequest>))
			break;
		}

		return str.toString();
	}

	/**
	 * Constructs a canonical request.
	 *
	 * For AWS Signature Version 4, use the following pattern:
	 *
	 *  <HTTPMethod>\n
	 *  <CanonicalURI>\n
	 *  <CanonicalQueryString>\n
	 *  <CanonicalHeaders>\n
	 *  <SignedHeaders>\n
	 *  <HashedPayload>
	 *
	 * @return The canonical request string
	 */
	protected String getCanonicalRequest() {
		StringBuilder req = new StringBuilder(512);

		switch (authScheme) {
		case AWS4:
			req.append(request.getMethod().toUpperCase());		// HTTPMethod
			req.append("\n");
			req.append(request.getPathInfo());					// CanonicalURI
			req.append("\n");
			req.append(getCanonicalQueryString());				// CanonicalQueryString
			req.append("\n");
			req.append(getCanonicalHeaders());					// CanonicalHeaders
			req.append("\n");
			req.append(StringUtils.join(signedHeaders, ";"));	// SignedHeaders
			req.append("\n");
			req.append(Hex.encodeHex(hash(reconstructPayload())));	// HashedPayload
			break;
		}

		return req.toString();
	}

	/**
	 * Constructs a canonical, sorted query string for the request, in the following pattern:
	 *
	 *  URI-encode("marker")+"="+URI-encode("someMarker")+"&"+
	 *  URI-encode("max-keys")+"="+URI-encode("20") + "&" +
	 *  URI-encode("prefix")+"="+URI-encode("somePrefix")
	 *
	 * @return The canonical query string
	 */
	protected String getCanonicalQueryString() {
		String query = request.getQueryString();
		SortedMap<String, String> queryParams = new TreeMap<String, String>();

		if (StringUtils.isEmpty(query)) return "";
		if (query.startsWith("?")) query = query.substring(1);

		for(String pair : query.split("&")) {
			String[] kv = pair.split("=");
			if (kv.length > 0) {
				queryParams.put(kv[0], kv.length == 1 ? "" : kv[1]);
			}
		}

		StringBuilder canonicalQueryStr = new StringBuilder(query.length());
		for (String key : queryParams.keySet()) {
			canonicalQueryStr.append(urlEncode(key));
			canonicalQueryStr.append("=");
			canonicalQueryStr.append(urlEncode(queryParams.get(key)));
			canonicalQueryStr.append("&");
		}
		canonicalQueryStr.deleteCharAt(canonicalQueryStr.length() -1);

		return canonicalQueryStr.toString();
	}

	/**
	 * Constructs a canonical, sorted header string.
	 *
	 * For AWS Signature Version 4, the string is comprised of headers defined in the
	 * SignedHeaders Authorization header component, according to the following pattern:
	 *
	 *  Lowercase(<HeaderName1>)+":"+Trim(<value>)+"\n"
	 *  Lowercase(<HeaderName2>)+":"+Trim(<value>)+"\n"
	 *  ...
	 *  Lowercase(<HeaderNameN>)+":"+Trim(<value>)+"\n"
	 *
	 * @return The canonical header string
	 */
	protected String getCanonicalHeaders() {
		StringBuilder headers = new StringBuilder(256);

		switch (authScheme) {
		case AWS4:
			for (String headerName : signedHeaders) {
				String headerVal = request.getHeader(headerName);
				headers.append(headerName.toLowerCase());
				headers.append(":");
				headers.append(headerVal.trim());
				headers.append("\n");
			}
			break;
		}

		return headers.toString();
	}

	/**
	 * EC2 tools calculate the signature based on the exact payload. However the request has
	 * already been parsed when it gets the EC2RestServlet. Hence we must count on being able
	 * to reconstruct it from the parameter strings.
	 * 
	 * NOTE: The order is critical, so we are dependent on the parameter map not being permuted!
	 *
	 * @return The reconstructed payload.
	 */
	public String reconstructPayload() {
		if (request.getMethod().equalsIgnoreCase("GET"))
			return "";

		StringBuilder buffer = new StringBuilder();
		Map<String, String[]> paramMap = request.getParameterMap();
		for (String paramName : paramMap.keySet()) {
			buffer.append(paramName);
			buffer.append("=");
			for (String paramValue : paramMap.get(paramName)) {
				buffer.append(paramValue);
			}
			buffer.append("&");
		}

		if (buffer.length() > 0) {
			buffer.setLength(buffer.length() - 1);
		}
		requestBody = buffer.toString();
		return requestBody;
	}

	/**
	 * The scope the authentication is bound to.
	 *
	 * @return The scope of the request
	 */
	protected String getScope() {
		return StringUtils.join(new String[] { scopeDate, scopeRegion, scopeService, authScheme.getRequestConstant() }, "/");
	}

	/**
	 * Returns the the request date in ISO8601 format, i.e. YYYYMMDD'T'HHMMSS'Z'
	 */
	protected String getAmzDate() {
		String dateHeader = request.getHeader("x-amz-date");
		if (StringUtils.isEmpty(dateHeader )) {
			dateHeader = request.getHeader("Date");
		}
		return dateHeader;
	}

	/**
	 * Returns the component in the Authorization request header with name @param componentName.
	 */
	protected String getAuthHeaderComponent(String componentName) {
		String header = request.getHeader("Authorization").substring(authScheme.getName().length() + 1);
		String[] headerParts = header.split(",");

		for (String part : headerParts) {
			part = part.trim();

			if (part.startsWith(componentName)) {
				String[] componentParts = part.split("=");
				return componentParts.length > 0 ? componentParts[1] : "";
			}
		}

		return "";
	}

	/**
	 * Returns a keyed-hash message authentication code for @param payload with @param key.
	 *
	 * For AWS Signature Version 4, the algorithm used is HmacSHA256.
	 */
	protected byte[] hmac(byte[] key, String payload) {
		switch (authScheme) {
		case AWS4:
			try {
				Mac hmac = authScheme.getAuthAlgorithm();
				SecretKeySpec keySpec = new SecretKeySpec(key, authScheme.getAuthAlgorithmName());
				hmac.init(keySpec);
				return hmac.doFinal(payload.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				break;
			} catch (InvalidKeyException e) {
				logger.debug("Invalid key specified", e);
				break;
			}
		}

		return null;
	}

	/**
	 * Returns a hash for the provided @param payload.
	 *
	 * For AWS Signature Version 4, the algorithm used is SHA-256.
	 */
	protected byte[] hash(String payload) {
		switch (authScheme) {
		case AWS4:
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				return md.digest(payload.getBytes("UTF-8"));
			} catch (NoSuchAlgorithmException e) {
				logger.error(e);
				break;
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				break;
			}
		}

		return null;
	}

	/**
	 * AWS acceptable version of URL encoding.
	 *
	 * @return A URL encoded version of @param payload
	 */
	private String urlEncode(String payload) {
		try {
			return URLEncoder.encode(payload, "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			logger.error(e);
			return null;
		}
	}

	/**
	 * Since it is necessary to read the body of the request to authenticate a POST
	 * request, we return a wrapper object to enable subsequent calls to getParameter*()
	 * methods.
	 *
	 * If the HTTP method is GET we return the original request.
	 *
	 * @return The authenticated request
	 */
	public HttpServletRequest getRequest() {
		if (request.getMethod().equalsIgnoreCase("GET"))
			return request;

		final Map<String, String[]> parameterMap = new HashMap<String, String[]>();

		for (String pair : requestBody.split("&")) {
			String[] kv = pair.split("=");
			if (kv.length > 0) {
				String[] values = parameterMap.get(kv[1]);
				if (values == null) {
					parameterMap.put(kv[0], new String[] { kv.length == 1 ? "" : kv[1] });
				} else {
					List<String> list = new ArrayList<String>(Arrays.asList(values));
					list.add(kv.length == 1 ? "" : kv[1]);
					parameterMap.put(kv[0], list.toArray(new String[list.size()]));
				}
			}
		}

		return new HttpServletRequestWrapper(request) {
			@Override
			public Map<String, String[]> getParameterMap() {
				return parameterMap;
			}

			@Override
			public String getParameter(String name) {
				String[] values = parameterMap.get(name);
				return values != null && values.length > 0 ? values[0] : null;
			}

			@Override
			public String[] getParameterValues(String name) {
				return parameterMap.get(name);
			}

			@Override
			public Enumeration<String> getParameterNames() {
				return Collections.enumeration(parameterMap.keySet());
			}
		};
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getSecretKey() {
		return secretKey;
	}
}

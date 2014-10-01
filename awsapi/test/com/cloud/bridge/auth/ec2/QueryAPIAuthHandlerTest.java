package com.cloud.bridge.auth.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.cloud.bridge.persist.dao.CloudStackUserDao;

public class QueryAPIAuthHandlerTest {

	String validAuthHeader = "AWS4-HMAC-SHA256 Credential=ASDF1234ASDF1234ASDF/20140910/is-1a/ec2/aws4_request,SignedHeaders=date;host;x-amz-date,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
	String validAuthHeader2 = "AWS4-HMAC-SHA256 Credential=ASDF1234/AS12123412F/20140910/is-1a/ec2/aws4_request,SignedHeaders=date;host;x-amz-date,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
	String invalidAuthHeader = "AWS3-HMAC-SHA256 Credential=ASDF1234ASDF1234ASDF/20140910/is-1a/ec2/aws4_request,SignedHeaders=date;host;x-amz-date,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
	String validAuthHeaderV4 = "AWS4-HMAC-SHA256 Credential=V0FWSHRFeTRmWk00NEpvNzl1a3dReExQMjJ5azdn/20140925/local/ec2/aws4_request, SignedHeaders=host;user-agent;x-amz-date, Signature=9ecab7185cc03c772972cd28ef8c3b43f56e306cd879252089fe2bbdb3383958";
	String validHostV4      = "c.local";
	String validAmzDateV4   = "20140925T151852Z";
	String validUserAgentV4 = "ec2-api-tools 1.7.1.0, aws-sdk-java/unknown-version Mac_OS_X/10.9.4 Java_HotSpot(TM)_64-Bit_Server_VM/20.65-b04-462";
	String validSecretKeyV4 = "YUhxWDJ5eXV1UGFidDdqMDB4UjBldGYwSFJEeHl4";

	@Test
	public void testVerifyAuthScheme() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));

		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
		when(request.getMethod()).thenReturn("POST");
		when(request.getParameterMap()).thenReturn(getStringMapFromString("&", "=", ",", ""));
		assertTrue(authHandler.verifyAuthScheme());

		when(request.getHeader("Authorization")).thenReturn(invalidAuthHeader);
		assertFalse(authHandler.verifyAuthScheme());
	}

	@Test
	public void testVerifyAuthParams() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
		when(request.getHeader("x-amz-date")).thenReturn(validAmzDateV4);
		when(request.getMethod()).thenReturn("POST");
		when(request.getParameterMap()).thenReturn(getStringMapFromString("&", "=", ",", ""));
		
		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		assertEquals(authHandler.apiKey, "ASDF1234ASDF1234ASDF");
		assertEquals(authHandler.amzDateTime, "20140925T151852Z");
		assertEquals(authHandler.scopeDate, "20140910");
		assertEquals(authHandler.scopeRegion, "is-1a");
		assertEquals(authHandler.scopeService, "ec2");
		assertEquals(authHandler.signature, "98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd");

		when(request.getHeader("Authorization")).thenReturn(validAuthHeader2);

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());
		assertEquals(authHandler.apiKey, "ASDF1234/AS12123412F");
	}

	@Test
	public void testGetSigningKey() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
		when(request.getHeader("x-amz-date")).thenReturn(validAmzDateV4);
		when(request.getMethod()).thenReturn("POST");

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		authHandler.secretKey = "1234ASDF1234ASDF1234";

		String expected = "09f67ab714788fbb3c0a9932a0f9c9233f894b5744606133c1becfc2c31a9f50";
		String provided = new String(Hex.encodeHex(authHandler.getSigningKey()));

		assertEquals(expected, provided);
	}

	@Test
	public void testHmac() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
		when(request.getHeader("X-Amz-Date")).thenReturn(validAmzDateV4);
		when(request.getMethod()).thenReturn("POST");

		assertTrue(authHandler.verifyAuthScheme());

		String expected = "42274f72808392e58b57d728d7e3c0e5163eaf07d165dc03105f62abbe1a7d11";
		String provided = new String(Hex.encodeHex(authHandler.hmac("key".getBytes(), "Hello World")));

		assertEquals(expected, provided);
	}

	@Test
	public void testHash() {
		HttpServletRequest request = setupRequest("", validAuthHeader, "", "", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, null);

		assertTrue(authHandler.verifyAuthScheme());

		String expected = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";
		String provided = new String(Hex.encodeHex(authHandler.hash("Hello World")));

		assertEquals(expected, provided);
	}

	@Test
	public void testGetCanonicalQueryString() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));
		when(request.getQueryString()).thenReturn("?Version=123&SomeParam=value");

		String expected = "SomeParam=value&Version=123";
		String provided = authHandler.getCanonicalQueryString();

		assertEquals(expected, provided);
	}

	@Test
	public void testGetCanoncialHeaders() {
		HttpServletRequest request = setupRequest("", validAuthHeader, "", "header-value", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));
		when(request.getHeader("date")).thenReturn("header-value");
		
		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		String expected = "date:header-value\nhost:header-value\nx-amz-date:" + authHandler.amzDateTime + "\n";
		String provided = authHandler.getCanonicalHeaders();

		assertEquals(expected, provided);
	}

	@Test
	public void testreconstructPayload() {
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";
		HttpServletRequest request = setupRequest(payload, validAuthHeader, "", "", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));

		assertEquals(authHandler.reconstructPayload(), payload);
	}

	@Test
	public void testGetReader() {
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";
		HttpServletRequest request = setupRequest(payload, validAuthHeader, "", "", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));

		assertNotNull(authHandler.reconstructPayload());
		assertNotNull(authHandler.getRequest().getParameter("Action"));
		assertNotNull(authHandler.getRequest().getParameterMap());
		assertTrue(authHandler.getRequest().getParameterMap().size() == 7);
		assertNotNull(authHandler.getRequest().getParameterValues("Timestamp"));
		assertTrue(authHandler.getRequest().getParameterValues("Timestamp").length == 1);
	}

	@Test
	public void testGetCanonicalRequest() {
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";
		HttpServletRequest request = setupRequest(payload, validAuthHeader, "?Version=123&SomeParam=value", "header-value", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));
		when(request.getPathInfo()).thenReturn("/");


		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		authHandler.secretKey = "1234ASDF1234ASDF1234";

		String expected = StringUtils.join(new String[] {
			"POST",
			   "/",
			   "SomeParam=value&Version=123",
			   "date:header-value",
			   "host:header-value",
			   "x-amz-date:" + authHandler.amzDateTime + "\n",
			   "date;host;x-amz-date",
			   "6900a0715dc1ed4773bcce334638416cf1d3e596297292cc505bbdbc70b91a37"
		}, "\n");
		String provided = authHandler.getCanonicalRequest();

		assertEquals(expected, provided);
	}

	private HttpServletRequest setupRequest(String payload, String header, String queryString, String host, String timestamp, String userAgent, String contentType)
	{
	
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn(header);
		when(request.getQueryString()).thenReturn(queryString);
		when(request.getHeader("host")).thenReturn(host);
		when(request.getHeader("x-amz-date")).thenReturn(timestamp);
		when(request.getHeader("user-agent")).thenReturn(userAgent);
		when(request.getHeader("content-type")).thenReturn(contentType);
		when(request.getMethod()).thenReturn("POST");
		when(request.getPathInfo()).thenReturn("/");
		when(request.getParameterMap()).thenReturn(getStringMapFromString("&", "=", ",", payload));
		
		// Default
		when(request.getHeader("date")).thenReturn("header-value");
		
		return request;
	}
	
	private Map<String, String[]> getStringMapFromString(String itemSep, String keyValueSep, String valuesSep, String inString) {
		Map<String, String[]> mapFromString = new LinkedHashMap<String, String[]>();
		String[] items = inString.split(itemSep);
		for (String item : items) {
			String[] itemStrings = item.split(keyValueSep);
			if (itemStrings.length == 2) {
				String[] valuesStrings = itemStrings[1].split(valuesSep);
				mapFromString.put(itemStrings[0], valuesStrings);
			}
		}
		return mapFromString;
	}

	@Test
	public void testVerifySignature() {
		// Test 1
		String payload = "Action=DescribeImages&Version=2014-06-15&Owner.1=self";
		HttpServletRequest request1 = setupRequest(payload, validAuthHeaderV4, "", validHostV4, validAmzDateV4, validUserAgentV4, "");
		QueryAPIAuthHandler authHandler1 = new QueryAPIAuthHandler(request1, mock(CloudStackUserDao.class));

		assertTrue(authHandler1.verifyAuthScheme());
		assertTrue(authHandler1.verifyAuthParams());
		authHandler1.secretKey = validSecretKeyV4;
		assertTrue(authHandler1.verifySignature());

		
		// Test 2, different signed headers
		String validHeader2 = "AWS4-HMAC-SHA256 Credential=V0FWSHRFeTRmWk00NEpvNzl1a3dReExQMjJ5azdn/20110909/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c";		
		String contentType = "application/x-www-form-urlencoded; charset=utf-8\n";
		payload = "Action=ListUsers&Version=2010-05-08";
		HttpServletRequest request2 = setupRequest(payload, validHeader2, "", "iam.amazonaws.com", "20110909T233600Z", "", contentType);
		QueryAPIAuthHandler authHandler2 = new QueryAPIAuthHandler(request2, mock(CloudStackUserDao.class));

		assertTrue(authHandler2.verifyAuthScheme());
		assertTrue(authHandler2.verifyAuthParams());
		authHandler2.secretKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
		assertTrue(authHandler2.verifySignature());
	}	
	
	@Test
	public void testGetStringToSign() {
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";
		HttpServletRequest request = setupRequest(payload, validAuthHeader, "?Version=123&SomeParam=value", "header-value", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		authHandler.secretKey = "1234ASDF1234ASDF1234";

		String expected = StringUtils.join(new String[] {
			"AWS4-HMAC-SHA256",
			   authHandler.amzDateTime,
			   "20140910/is-1a/ec2/aws4_request",
			   "aa3ed4d026410d1fb029fe2b0f33f94764ce1f25e6dbd85f59ce0d750ad0823d"
		}, "\n");
		String provided = authHandler.getStringToSign();

		assertEquals(expected, provided);
	}

	@Test
	public void testVerifyScopeDate() {
		HttpServletRequest request = setupRequest("", validAuthHeader, "", "header-value", validAmzDateV4, "", "");
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request, mock(CloudStackUserDao.class));

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());
		assertFalse(authHandler.verifyTimeScope());

		Calendar now = Calendar.getInstance();
		String dateStr = String.format("%4d%02d%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader.replace("20140910", dateStr));

		assertTrue(authHandler.verifyAuthParams());
		assertTrue(authHandler.verifyTimeScope());
	}
}

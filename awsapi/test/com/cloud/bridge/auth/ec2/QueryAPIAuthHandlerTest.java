package com.cloud.bridge.auth.ec2;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class QueryAPIAuthHandlerTest {

	String validAuthHeader = "AWS4-HMAC-SHA256 Credential=ASDF1234ASDF1234ASDF/20140910/is-1a/ec2/aws4_request,SignedHeaders=date;host;x-amz-date,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
	String validAuthHeader2 = "AWS4-HMAC-SHA256 Credential=ASDF1234/AS12123412F/20140910/is-1a/ec2/aws4_request,SignedHeaders=date;host;x-amz-date,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
	String invalidAuthHeader = "AWS3-HMAC-SHA256 Credential=ASDF1234ASDF1234ASDF/20140910/is-1a/ec2/aws4_request,SignedHeaders=date;host;x-amz-date,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";

	@Test
	public void testVerifyAuthScheme() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);

		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
		assertTrue(authHandler.verifyAuthScheme());

		when(request.getHeader("Authorization")).thenReturn(invalidAuthHeader);
		assertFalse(authHandler.verifyAuthScheme());
	}

	@Test
	public void testVerifyAuthParams() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		assertEquals(authHandler.apiKey, "ASDF1234ASDF1234ASDF");
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
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);

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
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);

		assertTrue(authHandler.verifyAuthScheme());

		String expected = "42274f72808392e58b57d728d7e3c0e5163eaf07d165dc03105f62abbe1a7d11";
		String provided = new String(Hex.encodeHex(authHandler.hmac("key".getBytes(), "Hello World")));

		assertEquals(expected, provided);
	}

	@Test
	public void testHash() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);

		assertTrue(authHandler.verifyAuthScheme());

		String expected = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";
		String provided = new String(Hex.encodeHex(authHandler.hash("Hello World")));

		assertEquals(expected, provided);
	}

	@Test
	public void testGetCanonicalQueryString() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getQueryString()).thenReturn("?Version=123&SomeParam=value");

		String expected = "SomeParam=value&Version=123";
		String provided = authHandler.getCanonicalQueryString();

		assertEquals(expected, provided);
	}

	@Test
	public void testGetCanoncialHeaders() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
		when(request.getHeader("date")).thenReturn("header-value");
		when(request.getHeader("host")).thenReturn("header-value");
		when(request.getHeader("x-amz-date")).thenReturn("header-value");

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		String expected = "date:header-value\nhost:header-value\nx-amz-date:header-value";
		String provided = authHandler.getCanonicalHeaders();

		assertEquals(expected, provided);
	}

	@Test
	public void testGetRequestBody() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";

		try {
			when(request.getMethod()).thenReturn("POST");
			when(request.getReader()).thenReturn(new BufferedReader(new StringReader(payload)));
		} catch (IOException e) { fail(); }

		assertEquals(authHandler.getRequestBody(), payload);
	}

	@Test
	public void testGetReader() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";

		try {
			when(request.getMethod()).thenReturn("POST");
			when(request.getReader()).thenReturn(new BufferedReader(new StringReader(payload)));
		} catch (IOException e) { fail(); }

		assertNotNull(authHandler.getRequestBody());
		assertNotNull(authHandler.getRequest().getParameter("Action"));
		assertNotNull(authHandler.getRequest().getParameterMap());
		assertTrue(authHandler.getRequest().getParameterMap().size() == 7);
		assertNotNull(authHandler.getRequest().getParameterValues("Timestamp"));
		assertTrue(authHandler.getRequest().getParameterValues("Timestamp").length == 1);
	}

	@Test
	public void testGetCanonicalRequest() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";

		try {
			when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
			when(request.getQueryString()).thenReturn("?Version=123&SomeParam=value");
			when(request.getHeader("date")).thenReturn("header-value");
			when(request.getHeader("host")).thenReturn("header-value");
			when(request.getHeader("x-amz-date")).thenReturn("header-value");
			when(request.getMethod()).thenReturn("POST");
			when(request.getPathInfo()).thenReturn("/");
			when(request.getReader()).thenReturn(new BufferedReader(new StringReader(payload)));
		} catch (IOException e) {
			fail();
		}

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		authHandler.secretKey = "1234ASDF1234ASDF1234";

		String expected = StringUtils.join(new String[] {
			"POST",
			   "/",
			   "SomeParam=value&Version=123",
			   "date:header-value",
			   "host:header-value",
			   "x-amz-date:header-value",
			   "date;host;x-amz-date",
			   "6900a0715dc1ed4773bcce334638416cf1d3e596297292cc505bbdbc70b91a37"
		}, "\n");
		String provided = authHandler.getCanonicalRequest();

		assertEquals(expected, provided);
	}

	@Test
	public void testGetStringToSign() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		String payload = "Action=DescribeInstances&SignatureMethod=HmacSHA256&AWSAccessKeyId=T0lpem5EZnNtY05MbkNqZ2tmSmNhYTlONThVQW9Q&SignatureVersion=2&Version=2012-08-15&Signature=51g5Fvodli5fRT294iQ3rR7tw6OvYGHuLB5KROJbqYg%3D&Timestamp=2014-09-11T11%3A43%3A28.911Z";

		try {
			when(request.getHeader("Authorization")).thenReturn(validAuthHeader);
			when(request.getQueryString()).thenReturn("?Version=123&SomeParam=value");
			when(request.getHeader("date")).thenReturn("header-value");
			when(request.getHeader("host")).thenReturn("header-value");
			when(request.getHeader("x-amz-date")).thenReturn("header-value");
			when(request.getMethod()).thenReturn("POST");
			when(request.getPathInfo()).thenReturn("/");
			when(request.getReader()).thenReturn(new BufferedReader(new StringReader(payload)));
		} catch (IOException e) {
			fail();
		}

		assertTrue(authHandler.verifyAuthScheme());
		assertTrue(authHandler.verifyAuthParams());

		authHandler.secretKey = "1234ASDF1234ASDF1234";

		String expected = StringUtils.join(new String[] {
			"AWS4-HMAC-SHA256",
			   "20140910",
			   "20140910/is-1a/ec2/aws4_request",
			   "1884477893465ab54dc52a136fd99502e0b43fab9f0f1dcb64baa561e58d3933"
		}, "\n");
		String provided = authHandler.getStringToSign();

		assertEquals(expected, provided);
	}

	@Test
	public void testVerifyScopeDate() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		QueryAPIAuthHandler authHandler = new QueryAPIAuthHandler(request);
		when(request.getHeader("Authorization")).thenReturn(validAuthHeader.replace("20140910", "20140808"));

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

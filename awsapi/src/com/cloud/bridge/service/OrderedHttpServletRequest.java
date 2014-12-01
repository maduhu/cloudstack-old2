package com.cloud.bridge.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wrapper for HttpServletRequst which reads the input stream into a cache and parses it into an ordered parameter map.
 * If the input stream has been read, an attempt is made to reconstruct the request payload from the parameter map and
 * parse it into an ordered parameter map.
 * 
 * WARNING: This class is only intended to exist until the AWS SOAP api has been dropped. This kind of low level code 
 *          should not be maintained by GreenQloud.
 *
 */

public class OrderedHttpServletRequest extends HttpServletRequestWrapper {
	private byte[] requestPayload = null;
	private final LinkedHashMap<String, String[]> parameterMap = new LinkedHashMap<String, String[]>();
	
	public class ServletInputStreamWrapper extends ServletInputStream {	
		private byte[] data;
	    private int pos = 0;
	    ServletInputStreamWrapper(byte[] data) {
	        if(data == null) {
	            data = new byte[0];
	        }
	        this.data = data;
	    }
	    @Override
	    public int read() throws IOException {
	        if(pos == data.length)
	            return -1;
	        return data[pos++] & 0xFF;
	    }
	}
	
	public OrderedHttpServletRequest(HttpServletRequest request) throws IOException {
		super(request);
		parseRequest();
	}	
	
	public void parseRequest() throws IOException {
	    if(requestPayload != null) {
	        return; //already parsed
	    }

	    getRequestContentBytes();
	    String enc = getRequest().getCharacterEncoding();
	    if(enc == null) {
	        enc = "UTF-8";
	    }
	    String[] queries = (new String(requestPayload, enc)).split("&");
	    boolean decode = getRequest().getContentType() != null && getRequest().getContentType().equals("application/x-www-form-urlencoded");
	    LinkedHashMap<String, LinkedList<String>> bufferMap = new LinkedHashMap<String, LinkedList<String>>();
	    LinkedList<String> valuesList;		    
	    for (String query : queries) {
	        String[] queryParts = query.split("=");	        
			if (queryParts.length > 1) {
				String name = urlDecode(queryParts[0], decode, "UTF-8");
				String value = urlDecode(queryParts[1], decode, "UTF-8");
				valuesList = bufferMap.get(name);
	            if (valuesList == null) {
	            	valuesList = new LinkedList<String>();
	            	bufferMap.put(name, valuesList);
	            }
	            valuesList.add(value);
			}
	    }
	    for(String key : bufferMap.keySet()) {   	
	        parameterMap.put(key, bufferMap.get(key).toArray(new String[bufferMap.get(key).size()]));
	    }
	}	

	// Do our best to url decode.
	private String urlDecode(String item, boolean decode, String method) {
		if (decode) {
            try {
            	item = URLDecoder.decode(item, method);
            } catch(Exception e) {}
		}
		return item;
	}

	public void getRequestContentBytes() throws IOException {
		if (requestPayload == null) {
		    if (! readInputStream()) {
			    reconstructPayload();
		    }
		}
	}

	// Create request body from the parameter map.
	public void reconstructPayload() {
		StringBuilder buffer = new StringBuilder();
		String querySeparator = "";
		for (String paramName : getRequest().getParameterMap().keySet()) {
			buffer.append(querySeparator);
			buffer.append(paramName);
			buffer.append("=");
			String valuePrefix = "";
			for (String paramValue : getRequest().getParameterMap().get(paramName)) {
				buffer.append(valuePrefix);
				buffer.append(paramValue);
				valuePrefix = "&" + paramName + "=";
			}
			querySeparator = "&";
		}
		requestPayload = buffer.toString().getBytes().clone();
	}

	// Return true if the entire request stream is successfully read, OW false, e.g. if the input stream has already been read.
	public boolean readInputStream() throws IOException {
		if (getRequest().getContentLength() > 0) {
		    InputStream ios = getRequest().getInputStream();
		    requestPayload = new byte[getRequest().getContentLength()];
		    int bytesRead = 0, pos = 0;
		    while ((bytesRead = ios.read(requestPayload, pos, requestPayload.length - bytesRead)) > 0) {
		        pos += bytesRead;
		    }
		} 
		return requestPayload != null && getRequest().getContentLength() == requestPayload.length;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
	    return new ServletInputStreamWrapper(requestPayload);
	}	
	
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
}

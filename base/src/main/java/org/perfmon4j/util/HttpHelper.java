package org.perfmon4j.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpHelper {
	private String userAgent = "PerfMon4j";
	private int connectTimeoutMillis = 2500;
	private int readTimeoutMillis = 2500;
	
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	public int getReadTimeoutMillis() {
		return readTimeoutMillis;
	}

	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
	}
	
	public Response doPost(String urlparam, String body) throws IOException {
		return doPost(urlparam, body, null);
	}

	public Response doPost(String urlparam, String body, Map<String, String>requestHeaders) throws IOException {
		URL url = new URL(urlparam);
		int responseCode;
		String responseMessage = null;
		String responseBody = null;
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		try {
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(connectTimeoutMillis);
			conn.setReadTimeout(readTimeoutMillis);
			conn.setRequestProperty("User-Agent", userAgent);
			
			byte[] outBytes = body.getBytes("UTF-8");
			conn.setDoOutput(true);
			conn.setFixedLengthStreamingMode(outBytes.length);
			if (requestHeaders != null) {
				for(Map.Entry<String, String> entry : requestHeaders.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			OutputStream out = conn.getOutputStream();
			try {
				out.write(outBytes);
				out.flush();
			} finally {
				out.close();
			}
			responseCode = conn.getResponseCode();
			responseMessage = conn.getResponseMessage();
			InputStream bodyStream = null;
			if (responseCode == HttpURLConnection.HTTP_OK) {
				bodyStream = conn.getInputStream();
			} else if (responseCode >= 400) {
				bodyStream = conn.getErrorStream();
			}
			if (bodyStream != null) {
				StringBuilder builder = null;
				BufferedReader in = new BufferedReader(new InputStreamReader(bodyStream, "UTF-8"));
				try {
					String line;
					while ((line = in.readLine()) != null) {
						if (builder == null) {
							builder = new StringBuilder();
						} else {
							builder.append("\r\n");
						}
						builder.append(line);
					}
					if (builder != null) {
						responseBody = builder.toString();
					}
				} finally {
					in.close();
				}
			}
		} finally {
			conn.disconnect();
		}
		
		return new Response(responseCode, responseMessage, responseBody);
	}
	
	public static class Response {
		private final int responseCode;
		private final String responseMessage;
		private final String responseBody;
		
		public Response(int responseCode, String responseMessage, String responseBody) {
			this.responseCode = responseCode;
			this.responseMessage = responseMessage;
			this.responseBody = responseBody;
		}

		public boolean isSuccess() {
			return (responseCode / 100) == 2;
		}
		
		public int getResponseCode() {
			return responseCode;
		}
		
		public String getResponseMessage() {
			return responseMessage;
		}
		
		public String getResponseBody() {
			return responseBody;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			
			result.append("Response [responseCode=");
			result.append(responseCode);
			if (responseMessage != null) {
				result.append(", responseMessage=\"")
					.append(responseMessage)
					.append("\"");
				
			}
			if (responseBody != null) {
				result.append(", responseBody=\"")
					.append(responseBody)
					.append("\"");
			}
			result.append("]");
			
			return  result.toString();
		}
	}
	
//	public static void main(String args[]) throws Exception {
//		HttpHelper client = new HttpHelper();
//		
//		client.doPost("http://192.168.56.1:9099/write?db=perfmon4j", "SnapShot.FromVM,system=FromVM,group=PROD.perfmon4j throughput=25,duration=10\r\n"
//				+ "SnapShot.FromVM2,system=FromVM2,group=PROD.perfmon4j throughput=25,duration=10");
//	}
}

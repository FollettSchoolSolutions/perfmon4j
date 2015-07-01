package web.org.perfmon4j.restdatasource.oauth2;

import java.net.URLEncoder;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class OauthTokenHelper {
	private final String oauthKey;
	private final String oauthSecret;
	private final int basicTokenLifetimeSeconds;
	private final int methodTokenLifetimeSeconds;
	private final int systemClockGraceSeconds;
	
	public static final String BASIC_TOKEN_PREFIX = "BASIC";
	public static final String METHOD_TOKEN_PREFIX = "METHOD";
	
	
	public OauthTokenHelper(String oauthKey, String oauthSecret) {
		this(oauthKey, oauthSecret, 30 * 60, 5 * 60, 30);
	}
	
	public OauthTokenHelper(String oauthKey, String oauthSecret,
			int basicTokenLifetimeSeconds, int methodTokenLifetimeSeconds,
			int systemClockGraceSeconds) {
		this.oauthKey = oauthKey;
		this.oauthSecret = oauthSecret;
		this.basicTokenLifetimeSeconds = basicTokenLifetimeSeconds;
		this.methodTokenLifetimeSeconds = methodTokenLifetimeSeconds;
		this.systemClockGraceSeconds = systemClockGraceSeconds;
	}

	public String buildBasicToken() throws Exception {
		return buildBasicToken(System.currentTimeMillis());
	}

	// Package level for testing.
	String buildBasicToken(long currentTime) throws Exception {
		String prefix = "BASIC~" + oauthKey + "~" + (currentTime/1000) + "~";
		return prefix + buildSignature(prefix);
	}
	
	
	public String buildMethodToken(String httpMethod, String partialURLPath, String signedParameters[]) throws Exception {
		return buildMethodToken(System.currentTimeMillis(), httpMethod, partialURLPath, signedParameters);
	}

	// Package level for testing
	String buildMethodToken(long currentTime, String httpMethod, String partialURLPath, String signedParameters[]) throws Exception {
		String prefix = "METHOD~" + oauthKey + "~" + (currentTime/1000) + "~";
		return prefix + buildSignature(prefix + buildMethodSignatureString(httpMethod, partialURLPath, signedParameters));
	}
	
	
	static String buildMethodSignatureString(String httpMethod, String partialURLPath, String signedParameters[]) {
		StringBuilder result = new StringBuilder();
		
		result.append(httpMethod)
			.append("&")
			.append(partialURLPath);
		
		if (signedParameters != null) {
			Arrays.sort(signedParameters);
			
			for (String p : signedParameters) {
				if (p != null) {
					result.append("&")
						.append(p);
				}
			}
		}
		
		return result.toString();
	}
	
	public boolean validateBasicToken(String token) throws RuntimeException {
		return validateBasicToken(System.currentTimeMillis(), token);
	}

	// Package level for testing.
	boolean validateBasicToken(long currentTime, String tokenString) throws RuntimeException {
		return validateToken(currentTime, tokenString, "", basicTokenLifetimeSeconds);
	}

	public boolean validateMethodToken(String token, String httpMethod, String partialURLPath, String signedParameters[]) throws RuntimeException {
		return validateMethodToken(System.currentTimeMillis(), token, httpMethod, partialURLPath, signedParameters);
	}

	// Package level for testing.
	boolean validateMethodToken(long currentTime, String tokenString, String httpMethod, String partialURLPath, String signedParameters[]) throws RuntimeException {
		return validateToken(currentTime, tokenString, buildMethodSignatureString(httpMethod, partialURLPath, signedParameters), methodTokenLifetimeSeconds);
	}

	private boolean validateToken(long currentTime, String tokenString, String methodSignature, int tokenLifetime) throws RuntimeException {
		boolean result = false;
	
		// First validate the signature...
		Token token = parseToken(tokenString);
		if (token != null && oauthKey.equals(token.getOauthKey())) {
			String prefix = token.getType() + "~" + token.getOauthKey() + "~" + token.getTimestamp() + "~";
			result = token.getSignature().equals(buildSignature(prefix + methodSignature));
			if (result) {
				// Check to see if the token is expired.
				result = currentTime < ((token.getTimestamp() + tokenLifetime) * 1000);
			}
			if (result) {
				// Make sure token create time is within the acceptable window.
				result = (token.getTimestamp() * 1000) < (currentTime + (systemClockGraceSeconds * 1000));
			}
		}
		return result;
	}
	
	
	public String getOauthKey() {
		return oauthKey;
	}

	public int getBasicTokenLifetimeSeconds() {
		return basicTokenLifetimeSeconds;
	}

	public int getMethodTokenLifetimeSeconds() {
		return methodTokenLifetimeSeconds;
	}

	public int getSystemClockGraceSeconds() {
		return systemClockGraceSeconds;
	}

	private String buildSignature(String prefix) throws RuntimeException {
		try {
			SecretKeySpec secretKey = new SecretKeySpec(oauthSecret.getBytes("UTF-8"), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(secretKey);
			
			byte[]  signatureBytes = mac.doFinal(prefix.getBytes("UTF-8"));
			
			return Base64.encodeBase64String(signatureBytes);
		} catch (Exception ex) {
			throw new RuntimeException("Error building oauth token", ex);
		}
	}

	public static String getOauthKey(String tokenString) {
		String result = null;
		
		Token token = parseToken(tokenString);
		if (token != null) {
			result = token.getOauthKey();
		}
		
		return result;
	}
	
	
	private static boolean anyEmpty(String[] array) {
		boolean foundEmpty = false;
		
		for (String s : array) {
			if ("".equals(s)) {
				foundEmpty = true;
				break;
			}
		}
		
		return foundEmpty;
	}
	
	static Token parseToken(String token) {
		Token result = null;
		if (token != null) {
			String[] parts = token.split("~");
			if (parts.length == 4 && (BASIC_TOKEN_PREFIX.equals(parts[0]) || METHOD_TOKEN_PREFIX.equals(parts[0])) && !anyEmpty(parts)) {
				try {
					result = new Token(parts[0], parts[1], Long.parseLong(parts[2]), parts[3]);
				} catch (NumberFormatException nfe) {
					// Just return null..  Unable to parse token.
				}
			}
		}
		return result;
	}
	
	static final class Token {
		private final String type;
		private final String oauthKey;
		private final long timestamp; 
		private final String signature;
		
		public Token(String type, String oauthKey, long timestamp, 	String signature) {
			this.type = type;
			this.oauthKey = oauthKey;
			this.timestamp = timestamp;
			this.signature = signature;
		}

		public String getType() {
			return type;
		}

		public String getOauthKey() {
			return oauthKey;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getSignature() {
			return signature;
		}
	}
	
	public static void main(String args[]) throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("GXMS-KNGD", "ZRCX-PDSP-CLDD-YFLM");
		
		System.out.println("Basic Bearer Token: " + URLEncoder.encode(helper.buildBasicToken()));
		
		System.out.println("Method Bearer Token: " + URLEncoder.encode(helper.buildMethodToken("GET", "/datasource/databases/default/systems", new String[]{})));
	}
}

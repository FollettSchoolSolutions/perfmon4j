/*
 *	Copyright 2015 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package web.org.perfmon4j.restdatasource.oauth2;

import junit.framework.TestCase;

public class OauthTokenHelperTest extends TestCase {
	public OauthTokenHelperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testBuildBasicToken() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		
		String token = helper.buildBasicToken(5000);
		assertEquals("BASIC~ABCD-EFGH~5~PNXHP8YoT+MpAQ1TIR/CclzBM64=", token);
	}

	public void testBuildMethodToken() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		
		String token = helper.buildMethodToken(5000, "GET", "/databases", new String[]{"x=value1", "a=value2"});
		assertEquals("METHOD~ABCD-EFGH~5~HP9KE2QIUu/QmpzFK1YsflBtvA8=", token);
	}
	
	public void testSimpleBasicTokenValidate() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		
		String token = helper.buildBasicToken();
		assertTrue("Should validate", helper.validateBasicToken(token));
		
		// Alter the token...
		token = token.replaceFirst("ABCD", "XBCD");
		assertFalse("Token has been tampered with... Should not validate", helper.validateBasicToken(token));
	}

	public void testSimpleMethodTokenValidate() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		
		String token = helper.buildMethodToken("GET", "/databases", new String[]{"a=value1", "z=value2", "q=value3"});
		
		assertTrue("Should validate", helper.validateMethodToken(token, "GET", "/databases", new String[]{"a=value1", "z=value2", "q=value3"}));
		assertTrue("Order of parameters should not be significant", 
				helper.validateMethodToken(token, "GET", "/databases", new String[]{"z=value2", "q=value3", "a=value1"}));
	}
	

	public void testMethodTokenValidateExpiration() throws Exception {
		// Method token's expire in 5 minutes by default.
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");

		long fourMinutesAgo = System.currentTimeMillis() - (4 * 60 * 1000); 
		long sixMinutesAgo = System.currentTimeMillis() - (6 * 60 * 1000); 
		
		String tokenGood = helper.buildMethodToken(fourMinutesAgo, "GET", "/databases", new String[]{"a=value1", "z=value2", "q=value3"});
		String tokenExpired = helper.buildMethodToken(sixMinutesAgo, "GET", "/databases", new String[]{"a=value1", "z=value2", "q=value3"});
	
		assertTrue("Should still be good", helper.validateMethodToken(tokenGood,"GET", "/databases", new String[]{"a=value1", "z=value2", "q=value3"}));
		assertFalse("Should be expired", helper.validateMethodToken(tokenExpired,"GET", "/databases", new String[]{"a=value1", "z=value2", "q=value3"}));
	}
	
	public void testBasicTokenValidateIncorrectKey() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		
		String token = helper.buildBasicToken();

		// Replace helper with same secret, but different key.
		helper = new OauthTokenHelper("ZBCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		assertFalse("Should NOT validate, oauth key does not match", helper.validateBasicToken(token));
	}
	
	public void testBasicTokenValidateIncorrectSecret() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		
		String token = helper.buildBasicToken();

		// Replace helper with same key, but different secret.
		helper = new OauthTokenHelper("ABCD-EFGH", "ZJKL-MNOP-QRST-UVWX");
		assertFalse("Should NOT validate, secret used to build key does not match", helper.validateBasicToken(token));
	}
	
	public void testBasicTokenValidateExpired() throws Exception {
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		long twentyNineMinutesAgo = System.currentTimeMillis() - (29 * 60 * 1000); 
		long thirtyOneMinutesAgo = System.currentTimeMillis() - (31 * 60 * 1000); 
		
		String tokenGood = helper.buildBasicToken(twentyNineMinutesAgo);
		String tokenExpired = helper.buildBasicToken(thirtyOneMinutesAgo);
	
		assertTrue("Should still be good", helper.validateBasicToken(tokenGood));
		assertFalse("Should be expired", helper.validateBasicToken(tokenExpired));
	}

	
	public void testBasicTokenValidateCreateTimestampNotInFuture() throws Exception {
		// This test ensures that the system clock on the client must be no more
		// than 30 seconds off (ahead of) the system clock of the data source sever.
		OauthTokenHelper helper = new OauthTokenHelper("ABCD-EFGH", "IJKL-MNOP-QRST-UVWX");
		long thirtySecondsFromNow = System.currentTimeMillis() + (30 * 1000); 
		long thirtyFiveSecondsFromNow = System.currentTimeMillis() + (35 * 1000); 
		
		String tokenGood = helper.buildBasicToken(thirtySecondsFromNow);
		String tokenBad = helper.buildBasicToken(thirtyFiveSecondsFromNow);
	
		assertTrue("Should still be good", helper.validateBasicToken(tokenGood));
		assertFalse("Client system clock outside of acceptable range", helper.validateBasicToken(tokenBad));
	}
	
	public void testParseToken() throws Exception {
		String validBasicToken = "BASIC~A~1~SIGNATURE"; 
		String validMethodToken = "METHOD~A~1~SIGNATURE"; 
			
		OauthTokenHelper.Token token = OauthTokenHelper.parseToken(validBasicToken);
		assertNotNull("Has the correct parts", token);
		assertEquals("BASIC", token.getType());
		assertEquals("A", token.getOauthKey());
		assertEquals(1, token.getTimestamp());
		assertEquals("SIGNATURE",token.getSignature());
		
		token = OauthTokenHelper.parseToken(validMethodToken);
		assertNotNull("Has the correct parts", token);
		assertEquals("METHOD", token.getType());

		assertNull("Uknown type", OauthTokenHelper.parseToken(validBasicToken.replaceFirst("BASIC", "OTHER")));
		assertNull("Missing Signature", OauthTokenHelper.parseToken(validBasicToken.replaceFirst("SIGNATURE", "")));
		assertNull("Too few elements", OauthTokenHelper.parseToken(validBasicToken.replaceFirst("~1", "")));
		assertNull("Too many elements", OauthTokenHelper.parseToken(validBasicToken + "~x"));
		assertNull("Timestamp is not a number", OauthTokenHelper.parseToken(validBasicToken.replaceFirst("~1", "~@")));
	}
	
	public void testBuildMethodSignatureString() {
		String expected = "GET&/datasource/databases/default/observations&a=a&a=b&x=param1Value";
		
		assertEquals(expected, OauthTokenHelper.buildMethodSignatureString("GET", "/datasource/databases/default/observations",
			new String[]{"a=b", "x=param1Value", "a=a"}));
	}

	public void testBuildMethodSignatureStringNoParams() {
		String expected = "GET&/datasource/databases/default/observations";
		
		assertEquals(expected, OauthTokenHelper.buildMethodSignatureString("GET", "/datasource/databases/default/observations",
			null));
	}
}

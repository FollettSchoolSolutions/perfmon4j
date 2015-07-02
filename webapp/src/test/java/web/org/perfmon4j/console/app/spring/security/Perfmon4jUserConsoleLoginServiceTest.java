/*
 *	Copyright 2015 Follett School Solutions 
 *
 *	Thisimport junit.framework.TestCase;
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
package web.org.perfmon4j.console.app.spring.security;

import junit.framework.TestCase;

public class Perfmon4jUserConsoleLoginServiceTest extends TestCase {

	public Perfmon4jUserConsoleLoginServiceTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testGenerateMD5Hash() {
		final String expected = "81dc9bdb52d04dc20036dbd8313ed055";  // MD5 Hash for "1234"
		final String result = Perfmon4jUserConsoleLoginService.generateMD5Hash("1234");
			
		assertEquals("Should have generated correct hash", expected, result);
	}

}

/*
 *	Copyright 2019 Follett Software Company 
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
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.util;

import junit.framework.TestCase;

public class WildcardPatternHelperTest extends TestCase {

	public WildcardPatternHelperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
    public void testPatternForChild() throws Exception {
    	assertEquals("Literal characters should be ignored", "abcd", WildcardPatternHelper.massagePattern("./abcd").getRegEx());
    	assertEquals("# character represents a word character", "a\\wc\\w", WildcardPatternHelper.massagePattern("./a#c#").getRegEx());
    	assertEquals("#* represents one or more word characters", "a\\wc\\w+", WildcardPatternHelper.massagePattern("./a#c#*").getRegEx());


    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Period in prefix is optional", "abcd", WildcardPatternHelper.massagePattern("/abcd").getRegEx());
    }

   
    public void testEmptyRemainder() throws Exception {
    	assertEquals("Should be no remainder", "", WildcardPatternHelper.massagePattern("./abcd").getRemainder());
    	assertEquals("Should be no remainder", "", WildcardPatternHelper.massagePattern("./a#c#").getRemainder());
    	assertEquals("Should be no remainder", "", WildcardPatternHelper.massagePattern("./a#c#*").getRemainder());

    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Should be no remainder", "", WildcardPatternHelper.massagePattern("/abcd").getRemainder());
    }
    

    public void testPatternForDescendent() throws Exception {
    	assertEquals("Literal characters should be ignored", "abcd", WildcardPatternHelper.massagePattern("./abcd.a#.#").getRegEx());
    	assertEquals("# character represents a word character", "a\\wc\\w", WildcardPatternHelper.massagePattern("./a#c#.a#.a#.#").getRegEx());
    	assertEquals("#* represents one or more word characters", "a\\wc\\w+", WildcardPatternHelper.massagePattern("./a#c#*.a#.#").getRegEx());


    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Period in prefix is optional", "abcd", WildcardPatternHelper.massagePattern("/abcd.a#.#").getRegEx());
    }

    public void testRemainderForDescendent() throws Exception {
    	assertEquals("Every thing after first period is the remainder", "a#.#", WildcardPatternHelper.massagePattern("./abcd.a#.#").getRemainder());
    	assertEquals("Every thing after first period is the remainder", "a#.#", WildcardPatternHelper.massagePattern("./a#c#.a#.#").getRemainder());
    	assertEquals("Every thing after first period is the remainder", "a#.#", WildcardPatternHelper.massagePattern("./a#c#*.a#.#").getRemainder());


    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Every thing after first period is the remainder", "a#.#", WildcardPatternHelper.massagePattern("/abcd.a#.#").getRemainder());
    }
}

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

import org.perfmon4j.PerfMon;

import junit.framework.TestCase;

public class EnhancedAppenderPatternHelperTest extends TestCase {

	public EnhancedAppenderPatternHelperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
    public void testPatternForChild() throws Exception {
    	assertEquals("Literal characters should be ignored", "abcd", EnhancedAppenderPatternHelper.massagePattern("./abcd").getRegEx());
    	assertEquals("# character represents a word character", "a\\wc\\w", EnhancedAppenderPatternHelper.massagePattern("./a#c#").getRegEx());
    	assertEquals("#* represents one or more word characters", "a\\wc\\w+", EnhancedAppenderPatternHelper.massagePattern("./a#c#*").getRegEx());


    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Period in prefix is optional", "abcd", EnhancedAppenderPatternHelper.massagePattern("/abcd").getRegEx());
    }

   
    public void testEmptyRemainder() throws Exception {
    	assertEquals("Should be no remainder", "", EnhancedAppenderPatternHelper.massagePattern("./abcd").getRemainder());
    	assertEquals("Should be no remainder", "", EnhancedAppenderPatternHelper.massagePattern("./a#c#").getRemainder());
    	assertEquals("Should be no remainder", "", EnhancedAppenderPatternHelper.massagePattern("./a#c#*").getRemainder());

    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Should be no remainder", "", EnhancedAppenderPatternHelper.massagePattern("/abcd").getRemainder());
    }
    

    public void testPatternForDescendent() throws Exception {
    	assertEquals("Literal characters should be ignored", "abcd", EnhancedAppenderPatternHelper.massagePattern("./abcd.a#.#").getRegEx());
    	assertEquals("# character represents a word character", "a\\wc\\w", EnhancedAppenderPatternHelper.massagePattern("./a#c#.a#.a#.#").getRegEx());
    	assertEquals("#* represents one or more word characters", "a\\wc\\w+", EnhancedAppenderPatternHelper.massagePattern("./a#c#*.a#.#").getRegEx());


    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Period in prefix is optional", "abcd", EnhancedAppenderPatternHelper.massagePattern("/abcd.a#.#").getRegEx());
    }

    public void testRemainderForDescendent() throws Exception {
    	assertEquals("Every thing after first period is the remainder", "a#.#", EnhancedAppenderPatternHelper.massagePattern("./abcd.a#.#").getRemainder());
    	assertEquals("Every thing after first period is the remainder", "a#.#", EnhancedAppenderPatternHelper.massagePattern("./a#c#.a#.#").getRemainder());
    	assertEquals("Every thing after first period is the remainder", "a#.#", EnhancedAppenderPatternHelper.massagePattern("./a#c#*.a#.#").getRemainder());


    	// Should also work if prefix just starts with a '/' 
    	assertEquals("Every thing after first period is the remainder", "a#.#", EnhancedAppenderPatternHelper.massagePattern("/abcd.a#.#").getRemainder());
    }

    public void testAlternateForwardSlashAsPackage() throws Exception {
    	assertEquals("Can separate packages with '.' or '/'", "ab", EnhancedAppenderPatternHelper.massagePattern("./ab/cd").getRegEx());
    	assertEquals("Can separate packages with '.' or '/'", "cd", EnhancedAppenderPatternHelper.massagePattern("./ab/cd").getRemainder());
    }
    
    public void testValidateEnhancedPattern() {
    	assertTrue("Valid starts with '/' prefix", EnhancedAppenderPatternHelper.validateAppenderPattern("/abc#*"));
    	assertTrue("Valid starts with './' prefix", EnhancedAppenderPatternHelper.validateAppenderPattern("./abc#*"));
    	assertFalse("Not valid must start with a valid prefix", EnhancedAppenderPatternHelper.validateAppenderPattern("noprefix"));

    	assertFalse("Must have at least 1 package", EnhancedAppenderPatternHelper.validateAppenderPattern("/"));
    	assertFalse("Must have at least 1 package", EnhancedAppenderPatternHelper.validateAppenderPattern("./"));
    	
    	// Packages can be separated by a '.' or a '/'
    	assertFalse("Must not have an empty package (Separated by /)", EnhancedAppenderPatternHelper.validateAppenderPattern("//abc#*"));
    	assertFalse("Must not have an empty package (Separated by .)", EnhancedAppenderPatternHelper.validateAppenderPattern("/.abc#*"));
    	
    	assertFalse("Whitespace not allowed", EnhancedAppenderPatternHelper.validateAppenderPattern("/a b"));
    }

    
    /**
     * Checks to see if the pattern matches the traditional/legacy hard coded patterns
     */
    public void testIsTraditionalPattern() {
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("."));
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("./"));
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("/*"));
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("./*"));
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("/**"));
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("./**"));
    	assertTrue("traditional pattern", EnhancedAppenderPatternHelper.isTraditionalPattern(""));
    	
    	assertFalse("This is an enhanced pattern", EnhancedAppenderPatternHelper.isTraditionalPattern("/#*"));
    }

    public void testBuildPattern() {
    	// Pattern should start with a  "./";
    	validatePattern("\\Qcom.acme.myMonitor\\E($|\\.a\\w\\w\\.xy\\w+)", "com.acme.myMonitor", "./a##/xy#*");

    	// We will also accept a prefix of  "/";
    	validatePattern("\\Qcom.acme.myMonitor\\E\\.a\\w\\w\\.xy\\w+", "com.acme.myMonitor", "/a##/xy#*");

    
    	// Package structure after first character can be separated with a '/' or '.';
    	validatePattern("\\Qcom.acme.myMonitor\\E\\.a\\w\\w\\.xy\\w+", "com.acme.myMonitor", "/a##.xy#*");
    }

    public void testBuildWithBadPattern() {
    	validatePattern(null, "com.acme.myMonitor", "this is bad");
    }

    public void testBuildWithLegacyPatterns() {
    	validatePattern(null, "com.acme.myMonitor", PerfMon.APPENDER_PATTERN_NA);
    	validatePattern(null, "com.acme.myMonitor", PerfMon.APPENDER_PATTERN_PARENT_ONLY);
    	validatePattern(null, "com.acme.myMonitor", PerfMon.APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY);
    	validatePattern(null, "com.acme.myMonitor", PerfMon.APPENDER_PATTERN_CHILDREN_ONLY);
    	validatePattern(null, "com.acme.myMonitor", PerfMon.APPENDER_PATTERN_ALL_DESCENDENTS);
    	validatePattern(null, "com.acme.myMonitor", PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);

    	// Synonym PerfMon.APPENDER_PATTERN_PARENT_ONLY 
    	validatePattern(null, "com.acme.myMonitor", ".");
    }
    
    
    private void validatePattern(String expectedRegEx, String monitorName, String pattern) {
    	if (expectedRegEx == null) {
	    	assertNull("Expected NULL regEx " 
	    			+ " from monitorName: '" + monitorName
	    			+ "' from pattern: '" + pattern + "'"
	    			, EnhancedAppenderPatternHelper.buildPattern(monitorName, pattern));
    	} else {
	    	assertEquals("Expected regEx: '" + expectedRegEx 
	    			+ "' from monitorName: '" + monitorName
	    			+ "' from pattern: '" + pattern + "'"
	    			, expectedRegEx, EnhancedAppenderPatternHelper.buildPattern(monitorName, pattern));
    	}
    }
    
    public void testCouldApplyToDescendents() throws Exception {
    	assertTrue("Because there is a package separator, descendents could match", 
    			EnhancedAppenderPatternHelper.massagePattern("./abcd/#").couldApplyToDescendants());
    	assertFalse("Since there is no package separator descendents can't possibly match ", 
    			EnhancedAppenderPatternHelper.massagePattern("./abcd").couldApplyToDescendants());
    }

    public void testCouldApplyToCurrent() throws Exception {
    	assertFalse("Since there is a path sparator only descendents of the current monitor could match", 
    			EnhancedAppenderPatternHelper.massagePattern("./abcd/#").couldApplyToCurrent());
    	assertTrue("Since there is no package the current monitor could match", 
    			EnhancedAppenderPatternHelper.massagePattern("./abcd").couldApplyToCurrent());
    }
    
}

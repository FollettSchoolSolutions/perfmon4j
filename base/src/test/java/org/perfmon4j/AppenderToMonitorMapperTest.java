/*
 *	Copyright 2022 Follett School Solutions, LLC 
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
 *  Follett School Solutions, LLC
 *  1340 Ridgeview Drive
 *  McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.AppenderToMonitorMapper.Builder;
import org.perfmon4j.AppenderToMonitorMapper.HashableRegEx;

import junit.framework.TestCase;

public class AppenderToMonitorMapperTest extends TestCase {
	
	public AppenderToMonitorMapperTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testBuildRegExParentOnly() {
		String monitorName = "com.acme.myMonitor";
		String pattern = ".";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.MYMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a.b");
		
		// Also should allow "./" as a pattern indicating parent
		
		pattern = "./";
		regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.MYMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a.b");
	}
	
	public void testBuildRegExParentAndChild() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "./*";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.MYMonitor");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a.b");
	}

	public void testBuildRegExChildOnly() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "/*";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a.b");
	}
	
	public void testBuildRegExParentAndDescendents() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "./**";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.MYMonitor");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a.b");
	}

	public void testBuildRegExAllDescendentsOnly() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "/**";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a.b");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.a.b.c.d");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a.b.c.d.");
	}

	public void testBuildEnhancedPattern() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "/abc/xyz";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.a");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.abc");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.abc.x");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.abc.xyz");
	}
	
	public void testBuildPatternMatcherWithOptions() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "/(dog|cat|bird)";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);

		// Base does not match
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor");
		
		// base + one of the three options (dog, cat or bird) should match. 
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.cat");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird");
		
		// turtle is not in the list of options
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.turtle");
	}
	
	public void testAppendClassicChildPatternToRegex() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "/(dog|cat|bird)/*";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);

		// By if you suffix a regex with the /* any child monitor, whose parent matches
		// the regex will also match.
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog.anychild");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird.anychild");

		// Grand child will NOT match
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.dog.anychild.anygrandchild");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.bird.anychild.anygrandchild");

		// Direct regex matches must still work.
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird");
		
		// Must direct match must still match regex
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.turtle");
		// Child must still match the regex
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.turtle.anychild");
	}	
	
	
	/**
	 * Usage of the all descendants pattern (/**)in general should be highly
	 * discouraged.  Still we will include it here for completeness
	 */
	public void testAppendClassicAllDescendantsPatternToRegex() {
		String monitorName = "com.acme.myMonitor";
		String pattern = "/(dog|cat|bird)/**";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);

		// By if you suffix a regex with the /* any child monitor, whose parent matches
		// the regex will also match.
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog.anychild");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird.anychild");

		// Grand child must match
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog.anychild.anygrandchild");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird.anychild.anygrandchild");
		
		// All descendants must match
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog.anychild.anygrandchild.anygreat");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird.anychild.anygrandchild.anygreat.anygreatgreat");
		
		// Direct regex matches must still work.
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.dog");
		validatePatternMatches(monitorName, pattern, regEx, "com.acme.myMonitor.bird");
		
		// Must still match regex
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.turtle");
		// Child must still match the regex
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.turtle.anychild");
		// Grand child must still match the regex
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme.myMonitor.turtle.anychild.anygrandchild");
	}	
	

	public void testAddToRootMonitor() {
		String monitorName = PerfMon.ROOT_MONITOR_NAME;
		String pattern = "/*";
		
		HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
		
		validatePatternMatches(monitorName, pattern, regEx, "com");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "com.acme");
		validatePatternMatches(monitorName, pattern, regEx, "org");
		validatePatternDoesNotMatch(monitorName, pattern, regEx, "org.acme");
	}
	
	
	public void testBuilder() {
		Builder builder = new Builder();
		
		builder.add("com.acme.myMonitor", "/*", AppenderID.getAppenderID(TextAppender.class.getName()));
		
		AppenderToMonitorMapper mapper = builder.build();
		
		assertEquals("root monitor should not be mapped", 0, 
				mapper.getAppendersForMonitor("com.acme.myMonitor").length);
		assertEquals("child monitor should  be mapped", 1, 
				mapper.getAppendersForMonitor("com.acme.myMonitor.a").length);
		assertEquals("grandchild monitor should not be mapped", 0, 
				mapper.getAppendersForMonitor("com.acme.myMonitor.a.b").length);
	}
	
	
	private void validatePatternMatches(String monitorName, String pattern, HashableRegEx regEx, String test) {
		assertTrue("Expected: '" + test + "' to match monitorName: '" + monitorName 
				+ "' pattern: '" + pattern + "'", regEx.matches(test));
	}
	
	private void validatePatternDoesNotMatch(String monitorName, String pattern, HashableRegEx regEx, String test) {
		assertFalse("'" + test + "' should NOT match monitorName: '" + monitorName 
				+ "' pattern: '" + pattern + "'", regEx.matches(test));
	}
}

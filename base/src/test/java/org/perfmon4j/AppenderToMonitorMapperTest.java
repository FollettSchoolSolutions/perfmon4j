package org.perfmon4j;

import junit.framework.TestCase;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.AppenderToMonitorMapper.Builder;
import org.perfmon4j.AppenderToMonitorMapper.HashableRegEx;

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

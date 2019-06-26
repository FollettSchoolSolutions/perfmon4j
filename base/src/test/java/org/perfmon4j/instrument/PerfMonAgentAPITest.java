/*
 *	Copyright 2008-2011 Follett Software Company 
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
package org.perfmon4j.instrument;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.perfmon4j.Appender;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.instrument.PerfMonTimerTransformerTest.SQLStatementTester.BogusAppender;
import org.perfmon4j.util.MiscHelper;

import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class PerfMonAgentAPITest extends PerfMonTestCase {
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

	private File perfmon4jJar = null;
	private File javassistJar = null;
	
/*----------------------------------------------------------------------------*/
    public PerfMonAgentAPITest(String name) {
        super(name);
    }
    
/*----------------------------------------------------------------------------*/
    public void setUp() throws Exception {
        super.setUp();
        
        perfmon4jJar = File.createTempFile("perfmon4j", "tmpdir");
        perfmon4jJar.delete(); // Just wanted the unique temporary file name.
        perfmon4jJar.mkdir();
        perfmon4jJar = new File(perfmon4jJar, "perfmon4j.jar");
        
        Properties props = new Properties();	
		props.setProperty("Premain-Class", "org.perfmon4j.instrument.PerfMonTimerTransformer");
		props.setProperty("Can-Redefine-Classes", "true");
		
		File classesFolder = new File("./target/classes");
		if (!classesFolder.exists()) {
			classesFolder = new File("./base/target/classes");
		}
		
		assertTrue("Could not find classes folder in: "  + classesFolder.getCanonicalPath(), classesFolder.exists());
		
        MiscHelper.createJarFile(perfmon4jJar.getAbsolutePath(), props, new File[]{classesFolder});
    }
    
/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	File folder = perfmon4jJar.getParentFile();
        perfmon4jJar.delete();
        if (javassistJar != null) {
        	javassistJar.delete();
        }
        folder.delete();
        
        perfmon4jJar = null;
        javassistJar = null;
        
    	super.tearDown();
    }

	public static class AgentAPIUsageTest implements Runnable {
		public void run() {
			if (org.perfmon4j.agent.api.PerfMon.isAttachedToAgent()) {
				System.out.println("Agent API for PerfMon class has been instrumented");
			} else {
				System.out.println("Agent API for PerfMon class has NOT been instrumented");
			}
			if (org.perfmon4j.agent.api.PerfMonTimer.isAttachedToAgent()) {
				System.out.println("Agent API for PerfMonTimer class has been instrumented");
			} else {
				System.out.println("Agent API for PerfMonTimer class has NOT been instrumented");
			}
			if (org.perfmon4j.agent.api.SQLTime.isAttachedToAgent()) {
				System.out.println("Agent API for SQLTime class has been instrumented");
			} else {
				System.out.println("Agent API for SQLTime class has NOT been instrumented");
			}
		}
	}

    public void testObjectsAreAttached() throws Exception {
    	String output = LaunchRunnableInVM.run(AgentAPIUsageTest.class, "-dFALSE", "", perfmon4jJar);
//System.out.println(output);   	
    	
    	assertTrue("PerfMon API class was not attached to agent", output.contains("Agent API for PerfMon class has been instrumented"));
    	assertTrue("PerfMonTimer API class was not attached to agent", output.contains("Agent API for PerfMonTimer class has been instrumented"));
    	assertTrue("SQLTime API class was not attached to agent", output.contains("Agent API for SQLTime class has been instrumented"));
    }
	
	
	public static class AgentAPIPerfMonInstTest implements Runnable {
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test.category";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "./*");
				PerfMon.configure(config);
				
				org.perfmon4j.agent.api.PerfMon apiPerfMon = org.perfmon4j.agent.api.PerfMon.getMonitor("not.active");
				if (apiPerfMon.isActive()) {
					System.out.println("**FAIL: 'not.active' is NOT configured to be monitored/active");
				}
				
				if (!"not.active".equals(apiPerfMon.getName())) {
					System.out.println("**FAIL: Incorrect monitor name, should have been 'not.active'");
				}

				apiPerfMon = org.perfmon4j.agent.api.PerfMon.getMonitor("test.category");
				if (!apiPerfMon.isActive()) {
					System.out.println("**FAIL: 'test.category' is configured to be monitored/active");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
    public void testAttachedPerfMonAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(AgentAPIPerfMonInstTest.class, "", "", perfmon4jJar);
//System.out.println(output);    	
    	String failures = extractFailures(output);
    	
    	if (!failures.isEmpty()) {
    		fail("One or more failures: " + failures);
    	}
    }

	public static class AgentAPIPerfMonTimerInstTest implements Runnable {
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test123.category";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "./*");
				PerfMon.configure(config);
				
				
				/* Test start with passing in an agent and abort */
				org.perfmon4j.agent.api.PerfMon apiPerfMon = org.perfmon4j.agent.api.PerfMon.getMonitor(monitorName);
				org.perfmon4j.agent.api.PerfMonTimer apiTimer = org.perfmon4j.agent.api.PerfMonTimer.start(apiPerfMon);
				org.perfmon4j.agent.api.PerfMonTimer.abort(apiTimer);
				
				PerfMon nativePerfMon = ((PerfMonAgentApiWrapper)apiPerfMon).getNativeObject();
				if (nativePerfMon.getTotalHits() != 1) {
					System.out.println("**FAIL: expected 1 hit");
				}
				if (nativePerfMon.getTotalCompletions() != 0) {
					System.out.println("**FAIL: still should have 0 completions, because we aborted");
				}
				
				
				/* Test start with passing in an string and stop */
				apiTimer = org.perfmon4j.agent.api.PerfMonTimer.start(monitorName);
				org.perfmon4j.agent.api.PerfMonTimer.stop(apiTimer);
				if (nativePerfMon.getTotalHits() != 2) {
					System.out.println("**FAIL: expected 2 hits");
				}
				if (nativePerfMon.getTotalCompletions() != 1) {
					System.out.println("**FAIL: still should have 1 completion, we aborted first time but passed the second");
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAttachedPerfMonTimerAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(AgentAPIPerfMonTimerInstTest.class, "", "", perfmon4jJar);
//System.out.println(output);    	
    	String failures = extractFailures(output);
    	
    	if (!failures.isEmpty()) {
    		fail("One or more failures: " + failures);
    	}
    }
	
    
	public static class AgentAPISQLTimeInstTest implements Runnable {
		public void run() {
			try {
				org.perfmon4j.SQLTime.setEnabled(true);
				
				// Call the API code.
				boolean isEnabled = org.perfmon4j.agent.api.SQLTime.isEnabled();
				if (!isEnabled) {
					System.out.println("**FAIL: API should indicate sqlTime is enabled");
				}
				
				org.perfmon4j.SQLTime.startTimerForThread();
				Thread.sleep(11);
				org.perfmon4j.SQLTime.stopTimerForThread();
				
				long sqlDuration = org.perfmon4j.agent.api.SQLTime.getSQLTime();
				System.out.println("SQLDuration: " + sqlDuration);
				
				if (sqlDuration < 10) {
					System.out.println("**FAIL: Should have at least 10 millis SQLTime on this thread");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAttachedSQLTimeAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(AgentAPISQLTimeInstTest.class, "", "", perfmon4jJar);
//System.out.println(output);    	
    	String failures = extractFailures(output);
    	
    	if (!failures.isEmpty()) {
    		fail("One or more failures: " + failures);
    	}
    }

	public static class AgentDeclarePerfMonTimerInstTest implements Runnable {
		
		@org.perfmon4j.agent.api.instrument.DeclarePerfMonTimer("test345.category")
		private void doSomething() {
			
		}
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test345.category";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "./*");
				PerfMon.configure(config);
				
				// Invoke the annotated method
				doSomething();
				
				PerfMon nativePerfMon = PerfMon.getMonitor(monitorName);
				if (nativePerfMon.getTotalCompletions() != 1) {
					System.out.println("**FAIL: expected 1 hit from the annotation");
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
    
	
    public void testAttachedDeclarePerfmonTimerAPI() throws Exception { 
    	String output = LaunchRunnableInVM.run(AgentDeclarePerfMonTimerInstTest.class, "-a" + AgentDeclarePerfMonTimerInstTest.class.getName(), "", perfmon4jJar); 
//System.out.println(output);    	 
    	String failures = extractFailures(output); 
    	 
    	if (!failures.isEmpty()) { 
    		fail("One or more failures: " + failures); 
    	} 
    } 
     	
    
    
    private String extractFailures(String output) {
    	StringBuilder failures = new StringBuilder();
    	
    	for (String line : output.split(System.lineSeparator())) {
    		if (line.contains("**FAIL:")) {
    			failures.append(System.lineSeparator())
    				.append(line);
    		}
    	}
    	return failures.toString();
    }
    

	public static class SnapShotMonitorWithAPIAnnotationTest implements Runnable {
		
		@org.perfmon4j.agent.api.instrument.SnapShotProvider
		public static class MySnapShotClass {
			private int counterValue = 0;

			@org.perfmon4j.agent.api.instrument.SnapShotCounter
			public int getCounter() {
				return counterValue++;
			}
			
			@org.perfmon4j.agent.api.instrument.SnapShotGauge
			public int getGauge() {
				return 1;
			}
			
			@org.perfmon4j.agent.api.instrument.SnapShotString
			public String getString() {
				return "MyString";
			}
		}
		
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String simpleSnapShotName = "Simple";
				final String appenderName = "bogus";
				
				config.defineSnapShotMonitor(simpleSnapShotName, MySnapShotClass.class.getName());
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(simpleSnapShotName, appenderName);
				PerfMon.configure(config);
				
				Thread.sleep(2000);
				Appender.flushAllAppenders();
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAPISnapShotAnnotations() throws Exception {
    	String output = LaunchRunnableInVM.run(SnapShotMonitorWithAPIAnnotationTest.class, "", "", perfmon4jJar);
//System.out.println(output);    	
    	
    	assertTrue("Should have the counter value in text output", output.contains("counter.................. 1/per duration"));
    	assertTrue("Should have the gauge value in text output", output.contains("gauge.................... 1"));
    	assertTrue("Should have the string value in text output", output.contains("string................... MyString"));
    }
    
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        System.setProperty("Perfmon4j.debugEnabled", "true");
		System.setProperty("JAVASSIST_JAR",  "G:\\projects\\perfmon4j\\.repository\\javassist\\javassist\\3.20.0-GA\\javassist-3.20.0-GA.jar");
		
        org.apache.log4j.Logger.getLogger(PerfMonAgentAPITest.class.getPackage().getName()).setLevel(Level.INFO);
        String[] testCaseName = {PerfMonAgentAPITest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
    	String testType = System.getProperty("UNIT");
//    	if (testType == null) {
//    		System.setProperty("Perfmon4j.debugEnabled", "true");
//    		System.setProperty("JAVASSIST_JAR",  "G:\\projects\\perfmon4j\\.repository\\javassist\\javassist\\3.10.0.GA\\javassist-3.10.0.GA.jar");
//    	}
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//		newSuite.addTest(new PerfMonTimerTransformerTest("testForceNoWrapperMethod"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testValidateNoWrapperMethodInstrumentationHandlesException"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testValidateNoWrapperMethodAnnotation"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testThreadTraceWithSQLTimeWithNoWrapperMethod"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testComparePerformanceWrapperVsNoWrapper"));
        
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonAgentAPITest.class);
        }

        return( newSuite);
    }
}

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
import org.perfmon4j.PerfMonTestCase;
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
		
//		File testClassesFolder = new File("./target/test-classes");
//		if (!testClassesFolder.exists()) {
//			testClassesFolder = new File("./base/target/test-classes");
//		}
		
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

	public static class AgentAPIUsageTester implements Runnable {
		public void run() {
			if (org.perfmon4j.agent.api.PerfMon.hasBeenInstrumented()) {
				System.out.println("Agent API for PerfMon class has been instrumented");
			} else {
				System.out.println("Agent API for PerfMon class has NOT been instrumented");
			}
		}
	}
    
    public void testObjectsAreOperating() throws Exception {
    	String output = LaunchRunnableInVM.run(AgentAPIUsageTester.class, "-dTRUE", "", perfmon4jJar);
//    	String output = LaunchRunnableInVM.runWithoutPerfmon4jJavaAgent(AgentAPIUsageTester.class, perfmon4jJar);
System.out.println(output);   	
    }
    
    
//	public static class SQLStatementTester implements Runnable {
//		
//		public static final class BogusAppender extends Appender {
//			public BogusAppender(AppenderID id) {
//				super(id);
//			}
//			
//			@Override
//			public void outputData(PerfMonData data) {
//				if (data instanceof IntervalData) {
//					IntervalData d = (IntervalData)data;
//					System.out.println("Monitor: " + d.getOwner().getName() + " Completions:" + d.getTotalCompletions());
//				} else {
//					System.out.println(data.toAppenderString());
//				}
//			}
//		}
//		
//	    final String DERBY_CREATE_1 = "CREATE TABLE Bogus(Name VARCHAR(200) NOT NULL)";
//	    
//		public void run() {
//			try {
//				Connection conn = null;
//				
//				Statement s = null;
//				
//				try {
//					PerfMonConfiguration config = new PerfMonConfiguration();
//					final String monitorName = "SQL.executeQuery";
//					final String appenderName = "bogus";
//					
//					config.defineMonitor(monitorName);
//					config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
//					config.attachAppenderToMonitor(monitorName, appenderName, ".");
//					
//					ThreadTraceConfig tcConfig = new ThreadTraceConfig();
//					tcConfig.addAppender(config.getAppenderForName(appenderName));
//					config.addThreadTraceConfig("MyManualTimer", tcConfig);
//					
//					PerfMon.configure(config);
//
//					PerfMonTimer timer = null;
//					try {
//						Driver driver = (Driver)Class.forName("org.apache.derby.jdbc.EmbeddedDriver", true, PerfMon.getClassLoader()).newInstance();
//						conn = driver.connect("jdbc:derby:memory:derbyDB;create=true", new Properties());
//						s = conn.createStatement();
//						s.execute(DERBY_CREATE_1);						
//						timer = PerfMonTimer.start("MyManualTimer");
//						s.executeQuery("SELECT * FROM BOGUS");
//						Thread.sleep(2000);
//					} finally {
//						PerfMonTimer.stop(timer);	
//					}
//					Appender.flushAllAppenders();
//				} finally {
//					if (conn != null) {
//						conn.close();
//					}
//				}
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//	}
//	
//    public void XtestInstrumentSQLStatement() throws Exception {
//    	String output = LaunchRunnableInVM.run(SQLStatementTester.class, "-dtrue,-eSQL(DERBY)", "", perfmon4jJar);
//    	System.out.println(output);   	
//    	assertTrue("Should have 1 completion for SQL.executeQuery", output.contains("SQL.executeQuery Completions:1"));
//    }
//
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

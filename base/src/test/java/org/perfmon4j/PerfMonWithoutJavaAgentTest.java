/*
 *	Copyright 2017 Follett Software Company 
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
package org.perfmon4j;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.perfmon4j.instrument.LaunchRunnableInVM;
import org.perfmon4j.util.MiscHelper;


/**
 * These test validate that Perfmon4j can perform in a Java VM that does not load 
 * perfmon4j as a java agent (Of course without the JavaAgent classes can not be instrumented)
 */
public class PerfMonWithoutJavaAgentTest extends TestCase {
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

	private File perfmon4jJar = null;
	
/*----------------------------------------------------------------------------*/
    public PerfMonWithoutJavaAgentTest(String name) {
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
		
		File testClassesFolder = new File("./target/test-classes");
		if (!testClassesFolder.exists()) {
			testClassesFolder = new File("./base/target/test-classes");
		}
		
		assertTrue("Could not find classes folder in: "  + classesFolder.getCanonicalPath(), classesFolder.exists());
		
        MiscHelper.createJarFile(perfmon4jJar.getAbsolutePath(), props, new File[]{classesFolder, testClassesFolder});
        
        System.out.println("perfmon4j jar file: " + perfmon4jJar.getCanonicalPath());
    }
    
    

/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	File folder = perfmon4jJar.getParentFile();
        perfmon4jJar.delete();
        folder.delete();
        
        perfmon4jJar = null;
        
    	super.tearDown();
    }
    

	public static class StartStopTimerPerfmonNotInitialize implements Runnable{
		
		public void run() {
			// This will be "no-op" since perfmon4j was not initialized..
			PerfMonTimer timer = PerfMonTimer.start("simple");
			PerfMonTimer.stop(timer);
		}
	}

	/**
	 * 5/23/2017 - This test tries to replicate a defect found when Perfmon4j was running within
	 * the Armstrong client.  The call to PerfMonTimer.start() was throwing an exception for being unable
	 * to load other Perfmon4j classes.   
	 * 
	 * The exception was properly caught within the call to PerfMonTimer.start() however the overhead
	 * of writing the exception to stdout was slowing down the Armstrong client.
	 * 
	 * The assumption was this was caused by the Armstrong client not being loaded with the perfmon4j
	 * java agent.  This test attempts the same thing and verifies no exceptions are thrown.
	 *  
	 * @throws Exception
	 */
	public void testSimpleTimerStopStartWithNoAgent() throws Exception {
    	String output = LaunchRunnableInVM.runWithoutPerfmon4jJavaAgent(StartStopTimerPerfmonNotInitialize.class, perfmon4jJar);
    	System.out.println(output);
    	
    	assertFalse("Should not have thrown an Error", output.toLowerCase().contains("error"));
    	assertFalse("Should not have thrown an Exception", output.toLowerCase().contains("exception"));
    }
	
    
/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
    	String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//		newSuite.addTest(new PerfMonTimerTransformerTest("testForceNoWrapperMethod"));
        
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonWithoutJavaAgentTest.class);
        }

        return( newSuite);
    }
}

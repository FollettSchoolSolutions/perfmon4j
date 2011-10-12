/*
 *	Copyright 2011 Follett Software Company 
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

package org.perfmon4j.remotemanagement;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.PerfMonTimer;


public class ExternalAppenderTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    
/*----------------------------------------------------------------------------*/
    public ExternalAppenderTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testBuildIntervalMonitorKey() throws Exception {
		String monitorKey = ExternalAppender.buildIntervalMonitorKey("com.follett.fsc");
        assertEquals("Need to ensure we can determine monitor type", 
        		"INTERVAL:com.follett.fsc", monitorKey);
    }	
    
    public void testGetIntervalMonitor() throws Exception {
		String monitorKey = ExternalAppender.buildIntervalMonitorKey("com.follett.fsc");
		
		assertEquals("com.follett.fsc", ExternalAppender.getIntervalMonitorName(monitorKey));
		assertNull("If string does not start with INTERVAL_PREFIX return null", 
				ExternalAppender.getIntervalMonitorName("xyz"));
		assertNull("null should return null", 
				ExternalAppender.getIntervalMonitorName(null));
		assertNull("Prefix alone should return null", 
				ExternalAppender.getIntervalMonitorName(ExternalAppender.INTERVAL_PREFIX));
    }	
    
    /*----------------------------------------------------------------------------*/    
    public void testExternalMonitor() throws Exception {
        final String MONITOR = "aa.b.c";
        
        String sessionID = ExternalAppender.connect();
		try {
			String monitorKey = ExternalAppender.buildIntervalMonitorKey(MONITOR);
			ExternalAppender.subscribe(sessionID, monitorKey);
			
	        for (int i = 0; i < 10; i++) {
	        	PerfMonTimer t = null;
	        	try {
	        		t = PerfMonTimer.start(MONITOR);
	        		Thread.sleep(50);
	        	} finally {
	        		PerfMonTimer.stop(t);
	        	}
	        }
	        PerfMonData d = (PerfMonData)ExternalAppender.takeSnapShot(sessionID, monitorKey);
	        assertNotNull("takeSnapShot should not return null", d);
	        assertTrue("Should be  interval data", d instanceof IntervalData);

	        IntervalData i = (IntervalData)d;
	        assertEquals("Total completions", 10, i.getTotalCompletions());
	        
	        assertTrue("Should have a stop time", i.getTimeStop() != PerfMon.NOT_SET);
		} finally {
			ExternalAppender.disconnect(sessionID);
		}
    }

/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
//        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {ExternalAppenderTest.class.getName()};

        TestRunner.main(testCaseName);
    }
    
/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new ExternalAppenderTest("testExternalMonitor"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ExternalAppenderTest.class);
        }

        return( newSuite);
    }
}

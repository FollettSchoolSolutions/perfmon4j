/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;

public class PerfMonTimerTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public PerfMonTimerTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        PerfMon.configure();
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInit();
        super.tearDown();
    }
    
/*----------------------------------------------------------------------------*/
    public void testInactiveMonitorsByPassed() throws Exception {
        PerfMon.getRootMonitor().addAppender(TestAppender.getAppenderID());
        
        PerfMonTimer timer = PerfMonTimer.start("a.b");
        assertEquals("Should have timer", "a.b", timer.perfMon.getName());
        PerfMonTimer.stop(timer);
        
        // Remove appender from "a.b"
        PerfMon.getMonitor("a.b").removeAppender(TestAppender.getAppenderID());
        
        timer = PerfMonTimer.start("a.b");
        assertEquals("Should bypass inactive timer a.b", "a", timer.perfMon.getName());
        PerfMonTimer.stop(timer);
        
        // If the only active timer is the root monitor, we should just
        // get the null timer...
        PerfMon.getMonitor("a").removeAppender(TestAppender.getAppenderID());
        timer = PerfMonTimer.start("a.b");
        assertNull("Should have null timer", timer.perfMon);
        PerfMonTimer.stop(timer);
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testTimerByPassUpdatedWhenAncestorGetsAnAppender() throws Exception {
        PerfMonTimer timer = PerfMonTimer.start("a.b.c");
        assertNull("Should be returning null timer", timer.perfMon);
        PerfMonTimer.stop(timer);
        
        // Add an appender to "a.b.c"'s grandparent
        PerfMon.getMonitor("a").addAppender(TestAppender.getAppenderID(), 
            PerfMon.APPENDER_PATTERN_PARENT_ONLY);
        
        timer = PerfMonTimer.start("a.b.c");
        assertEquals("Should now get my grandparent's timer", "a", timer.perfMon.getName());
        PerfMonTimer.stop(timer);
    }    
    
    
/*----------------------------------------------------------------------------*/
    private static class TestAppender extends Appender {
    	
        public static AppenderID getAppenderID() {
            return AppenderID.getAppenderID(TestAppender.class.getName());
        }
        
        public TestAppender(AppenderID id) {
            super(id);
        }
        
        public void outputData(@SuppressWarnings("unused") PerfMonData data) {
        }
    }
    

    
   
    

/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        String[] testCaseName = {PerfMonTimerTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new PerfMonTimerTest("testAddAppenderToChildrenOnly"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonTimerTest.class);
        }

        return( newSuite);
    }
}

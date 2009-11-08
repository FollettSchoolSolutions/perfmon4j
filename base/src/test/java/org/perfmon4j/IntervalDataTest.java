/*
 *	Copyright 2008 Follett Software Company 
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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class IntervalDataTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public IntervalDataTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/
    public void testCalcThroughput() throws Exception {
        // Perfmon with 5 minute duration....
        long now = System.currentTimeMillis();
        long fiveMinutesAgo = now - (5 * 60 * 1000);
    
        IntervalData data = new IntervalData(null, fiveMinutesAgo, null, null, now);
        data.setTotalCompletions(5*60);
        
        assertEquals("throughPutPerMinute", new Double(60.00), new Double(data.getThroughputPerMinute()));
    }

    
     public void testToString() throws Exception {
        final long NOW = System.currentTimeMillis();
        final long FIVE_MINUTES = (5 * 60 * 1000);
        final long FIVE_MINUTES_AGO = NOW - FIVE_MINUTES;
        
        IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), FIVE_MINUTES_AGO, null, null, NOW);
        data.setTotalCompletions(5*60);
        data.stop(FIVE_MINUTES, FIVE_MINUTES * FIVE_MINUTES, NOW);
        
        System.err.println(data.toString());
    }
   
    

/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {IntervalDataTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new PerfMonDataTest("testToString"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(IntervalDataTest.class);
        }

        return( newSuite);
    }
}

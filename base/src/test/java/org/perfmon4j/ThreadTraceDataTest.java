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

import java.util.GregorianCalendar;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ThreadTraceDataTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    final long MIDNIGHT = (new GregorianCalendar(2007, 0, 0)).getTimeInMillis();
    final long SECOND = 1000;
    final long MINUTE = SECOND * 60;
    final long HOUR = MINUTE * 60;
    
/*----------------------------------------------------------------------------*/
    public ThreadTraceDataTest(String name) {
        super(name);
    }
    
/*----------------------------------------------------------------------------*/
    public void testSimpleToAppenderString() throws Exception {
        ThreadTraceData data = new ThreadTraceData("com.perfmon4j.Test.test", MIDNIGHT + HOUR + MINUTE + (SECOND) + 1);
        data.setEndTime(MIDNIGHT + HOUR + MINUTE + MINUTE);
    
        assertEquals("\r\n********************************************************************************\r\n" +
            "+-01:01:01:001 (58999) com.perfmon4j.Test.test\r\n" +
            "+-01:02:00:000 com.perfmon4j.Test.test\r\n" +
            "********************************************************************************", data.toAppenderString());
    }

/*----------------------------------------------------------------------------*/
    public void testNestedToAppenderString() throws Exception {
        ThreadTraceData data = new ThreadTraceData("com.perfmon4j.Test.test", MIDNIGHT + HOUR + MINUTE);
        data.setEndTime(MIDNIGHT + HOUR + MINUTE + MINUTE);
    
        ThreadTraceData child = new ThreadTraceData("com.perfmon4j.MiscHelper.formatString", data, data.getStartTime() + SECOND);
        child.setEndTime(data.getEndTime() - SECOND);
        
        assertEquals("\r\n********************************************************************************\r\n" +
            "+-01:01:00:000 (60000) com.perfmon4j.Test.test\r\n" +
            "|\t+-01:01:01:000 (58000) com.perfmon4j.MiscHelper.formatString\r\n" +
            "|\t+-01:01:59:000 com.perfmon4j.MiscHelper.formatString\r\n" +
            "+-01:02:00:000 com.perfmon4j.Test.test\r\n" +
            "********************************************************************************", data.toAppenderString());
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {ThreadTraceDataTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(ThreadTraceDataTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SnapShotManagerTest("testDefineMonitorWithAttributes"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ThreadTraceDataTest.class);
        }

        return(newSuite);
    }
}

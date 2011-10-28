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

package org.perfmon4j.remotemanagement.intf;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;


public class MonitorKeyTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";
    
/*----------------------------------------------------------------------------*/
    public MonitorKeyTest(String name) {
        super(name);
    }

    public void testToString() {
    	assertNotNull("Interval Monitor", MonitorKey.parseNoThrow("INTERVAL(name=org.perfmon4j)"));
    	assertNotNull("ThreadTrace Monitor", MonitorKey.parseNoThrow("THREADTRACE(name=org.perfmon4j)"));
    	assertNotNull("SnapShot Monitor", MonitorKey.parseNoThrow("SNAPSHOT(name=org.perfmon4j)"));
    }

    public void testToStringX() {
    	MonitorKey key = new MonitorKey(MonitorKey.SNAPSHOT_TYPE, "org.perfmon4j");
    	assertEquals("SNAPSHOT(name=org.perfmon4j)", key.toString());

    	key = new MonitorKey(MonitorKey.SNAPSHOT_TYPE, "org.perfmon4j", "http-80");
    	assertEquals("SNAPSHOT(name=org.perfmon4j;instance=http-80)", key.toString());
    }

    public void testFromStringNoInstance() {
    	MonitorKey key = new MonitorKey(MonitorKey.INTERVAL_TYPE, "org.perfmon4j");
    	
    	MonitorKey compare = MonitorKey.parseNoThrow(key.toString());
    	assertTrue(compare.equals(key));

    	compare = MonitorKey.parseNoThrow(key.toString() + "asdfasf");
    	assertTrue("Any trailing characters should be ignored", compare.equals(key));
    }
    
    public void testFromStringWithInstance() {
    	MonitorKey key = new MonitorKey(MonitorKey.SNAPSHOT_TYPE, "org.perfmon4j", "http-80");
    	
    	MonitorKey compare = MonitorKey.parseNoThrow(key.toString());
    	assertTrue(compare.equals(key));

    	compare = MonitorKey.parseNoThrow(key.toString() + "asdfasf");
    	assertTrue("Any trailing characters should be ignored", compare.equals(key));
    }
        
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
//        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {MonitorKeyTest.class.getName()};

        TestRunner.main(testCaseName);
    }
    
/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SessionManagerTest("testLifeCycleTimeout"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(MonitorKeyTest.class);
        }

        return( newSuite);
    }
}

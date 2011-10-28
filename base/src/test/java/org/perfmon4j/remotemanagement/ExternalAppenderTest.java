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

import java.util.ArrayList;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public class ExternalAppenderTest extends TestCase {
	private static final Logger logger = LoggerFactory.initLogger(ExternalAppenderTest.class);
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    private String sessionID = null;
    
/*----------------------------------------------------------------------------*/
    public ExternalAppenderTest(String name) {
        super(name);
    }

    /*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        sessionID = ExternalAppender.connect();
    }
    
    /*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
    	ExternalAppender.disconnect(sessionID);
    	sessionID = null;
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testExternalMonitor() throws Exception {
        final String MONITOR = "aa.b.c";

        MonitorKeyWithFields m = IntervalData.getFields(MONITOR);
        ExternalAppender.subscribe(sessionID, m);
		
        for (int i = 0; i < 10; i++) {
        	PerfMonTimer t = null;
        	try {
        		t = PerfMonTimer.start(MONITOR);
        		Thread.sleep(50);
        	} finally {
        		PerfMonTimer.stop(t);
        	}
        }
     
        Map<FieldKey, Object> d = ExternalAppender.takeSnapShot(sessionID, m);
        assertNotNull("takeSnapShot should not return null", d);
    
        FieldKey totalCompletions = FieldKey.getFieldByName(m.getFields(), "TotalCompletions");
        FieldKey timeStop = FieldKey.getFieldByName(m.getFields(), "TimeStop");
        
        assertEquals("Total completions", 10, ((Integer)d.get(totalCompletions)).intValue());
        assertTrue("Should have a stop time", ((Long)d.get(timeStop)).longValue() != PerfMon.NOT_SET);
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testGetSubscribedMonitors() throws Exception {
        final String MONITOR = "aa.b.c";
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MONITOR);
        
		assertEquals("None subscribed should return empty array", 0,
				ExternalAppender.getSubscribedMonitors(sessionID).length);

		ExternalAppender.subscribe(sessionID, monitorKey);
		
		MonitorKeyWithFields[] v = ExternalAppender.getSubscribedMonitors(sessionID);
		assertEquals("One subscribed should return value", 
				1, v.length);
		assertEquals("Should match monitor key", 
				monitorKey, v[0]);
		
		// Build a monitorkey with fields that does not exactly match on the fields...
		// It should still respect the unsubscribe event.
		MonitorKeyWithFields missingFields =
			new MonitorKeyWithFields(monitorKey.getMonitorKeyOnly(),
			new ArrayList<FieldKey>());
		
		ExternalAppender.unSubscribe(sessionID, missingFields);
		v = ExternalAppender.getSubscribedMonitors(sessionID);
		
		assertEquals("Should have unsubscribed", 0, v.length);
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testMonitorResetsWhenMadeInactive() throws Exception {
        final String MON_NAME = "aaa.b.ccc";
        
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MON_NAME);
        PerfMon mon = PerfMon.getMonitor(MON_NAME);
        
        assertFalse("Monitor is not active", mon.isActive());
        
        ExternalAppender.subscribe(sessionID, monitorKey);
        assertTrue("Monitor is active", mon.isActive());

        PerfMonTimer timer = PerfMonTimer.start(MON_NAME);
        Thread.sleep(100);
        PerfMonTimer.stop(timer);
        
        // Keep 1 active timer...  
        timer = PerfMonTimer.start(MON_NAME);
        Thread.sleep(100);
        
        // Make inactive and verify that the counters are reset...
        ExternalAppender.unSubscribe(sessionID, monitorKey);
        assertFalse("Monitor is no longer active", mon.isActive());
        
        assertEquals("MaxDuration", 0, mon.getMaxDuration());
        assertEquals("MinDuration", 0, mon.getMinDuration());
        assertEquals("totalHits", 0, mon.getTotalHits());
        assertEquals("totalCompletions", 0, mon.getTotalCompletions());
        assertEquals("totalDuration", 0, mon.getTotalDuration());
        assertEquals("maxActiveThreadCount", 0, mon.getMaxActiveThreadCount());
        
        // If a active thread count is reset we have the possiblilty of a 
        // activeThreadCount going negative!
        assertEquals("activeThreadCount MUST not be reset when monitor is made inactive"
            ,1, mon.getActiveThreadCount());
        
        // Add and appender and make the monitor active...
        ExternalAppender.subscribe(sessionID, monitorKey);
        
        // The outstanding timer should update the active count, but none
        // of the other timers since the timer was started before it was active
        PerfMonTimer.stop(timer);
        assertEquals("activeThreadCount will be updated when thread is returned"
            ,0, mon.getActiveThreadCount());
        
        assertEquals("totalCompletions", 0, mon.getTotalCompletions());
        assertEquals("totalDuration", 0, mon.getTotalDuration());
        assertEquals("maxActiveThreadCount", 0, mon.getMaxActiveThreadCount());
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        LoggerFactory.setDefaultDebugEnbled(true);
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
//        newSuite.addTest(new ExternalAppenderTest("testGetSubscribedMonitors"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ExternalAppenderTest.class);
        }

        return( newSuite);
    }
}

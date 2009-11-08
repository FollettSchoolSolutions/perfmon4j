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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.util.MiscHelper;


public class PerfMonTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public PerfMonTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        PerfMon.configure();
        BogusAppender.dataStopCount = 0;
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInit();
        super.tearDown();
    }
    
/*----------------------------------------------------------------------------*/
    public void testSimple() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String MONITOR_NAME = "testSimple";
        PerfMon perfMon = PerfMon.getMonitor(MONITOR_NAME);
        
        assertTrue("Successive calls to getMonitor should return same object", 
            perfMon == PerfMon.getMonitor(MONITOR_NAME));
        
        PerfMonTimer timer = PerfMonTimer.start(MONITOR_NAME);
        // It's considered a hit as soon as we start...
        assertEquals("totalHits", 1, PerfMon.getMonitor(MONITOR_NAME).getTotalHits());
        // It's not considerd complete until stop.
        assertEquals("totalCompletions", 0, PerfMon.getMonitor(MONITOR_NAME).getTotalCompletions());
        PerfMonTimer.stop(timer);
        assertEquals("totalHits", 1, PerfMon.getMonitor(MONITOR_NAME).getTotalHits());
        // It's not considerd complete until stop.
        assertEquals("totalCompletions", 1, PerfMon.getMonitor(MONITOR_NAME).getTotalCompletions());
    }
    
/*----------------------------------------------------------------------------*/
    public void testAbort() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String MONITOR_NAME = "testAbort";
        PerfMon perfMon = PerfMon.getMonitor(MONITOR_NAME);
        
        PerfMonTimer timer = PerfMonTimer.start(MONITOR_NAME);
        Thread.sleep(500);
        PerfMonTimer.abort(timer);

        assertEquals("Should NOT have passed abort on to our IntervalData for each appender", 0, BogusAppender.dataStopCount);
        
        assertEquals("totalHits should be incremented even though we aborted the timer",
            1, perfMon.getTotalHits());
        assertEquals("Should not have incremented complections with an abort",
            0, perfMon.getTotalCompletions());
        assertEquals("Should not have incremented duration with an abort",
            0, perfMon.getTotalDuration());
        assertEquals("Should have rolled back active thread count with an abort",
            0, perfMon.getActiveThreadCount());
    }
    
/*----------------------------------------------------------------------------*/
    public void testNestedAbort() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String MONITOR_NAME = "testNestedAbort";
        PerfMon perfMon = PerfMon.getMonitor(MONITOR_NAME);
        
        PerfMonTimer timerOuter = PerfMonTimer.start(MONITOR_NAME);
        
        // Abort on nested timer should be ignored....
        PerfMonTimer timerInner = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer.abort(timerInner);
        
        PerfMonTimer.stop(timerOuter);

        assertEquals("Should have passed stop onto our appender", 1, BogusAppender.dataStopCount);
        
        assertEquals("Should have incremented completions based on outer stop",
            1, perfMon.getTotalCompletions());
    }

/*----------------------------------------------------------------------------*/    
    public void testHirearchyAbort() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String PARENT_MONITOR_NAME = "testHirearchyAbort";
        final String CHILD_MONITOR_NAME = PARENT_MONITOR_NAME + ".a";
        
        PerfMonTimer timer = PerfMonTimer.start(CHILD_MONITOR_NAME);
        Thread.sleep(500);
        PerfMonTimer.abort(timer);

        PerfMon parent = PerfMon.getMonitor(PARENT_MONITOR_NAME);
        assertEquals("Parent totalHits should be incremented even though we aborted the timer",
            1, parent.getTotalHits());
        assertEquals("Parent Should not have incremented complections with an abort",
            0, parent.getTotalCompletions());
        assertEquals("Parent Should not have incremented duration with an abort",
            0, parent.getTotalDuration());
        assertEquals("Parent Should have rolled back active thread count with an abort",
            0, parent.getActiveThreadCount());    
    }
    
/*----------------------------------------------------------------------------*/    
    public void testNestedCallsAreIgnored() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String MONITOR_NAME = "testNestedCallsAreIgnored";
        
        PerfMonTimer timerA = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer timerB = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer.stop(timerB);
        PerfMonTimer.stop(timerA);
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_NAME);
        
        assertEquals("totalHits", 1, mon.getTotalHits());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testSimpleHirearchy() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String GRANDPARENT_MONITOR_NAME = "testSimpleHirearchy";
        final String PARENT_MONITOR_NAME = "testSimpleHirearchy.parent";
        final String CHILD_MONITOR_NAME = "testSimpleHirearchy.parent.child";
        
        PerfMonTimer timer = PerfMonTimer.start(CHILD_MONITOR_NAME);
        PerfMonTimer.stop(timer);

        assertEquals("Should promote to parent", 1, PerfMon.getMonitor(PARENT_MONITOR_NAME).getTotalHits());
        assertEquals("Should promote to grandparent", 1, PerfMon.getMonitor(GRANDPARENT_MONITOR_NAME).getTotalHits());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testParseMonitorHirearchy() throws Exception {
        assertEquals("Handle null input", 
            unpackArray(new String[]{}), 
            unpackArray(PerfMon.parseMonitorHirearchy(null)));
        assertEquals("Handle empty string input", 
            unpackArray(new String[]{}), 
            unpackArray(PerfMon.parseMonitorHirearchy("")));
        assertEquals("RootLevel", 
            unpackArray(new String[]{"a"}), 
            unpackArray(PerfMon.parseMonitorHirearchy("a")));
        assertEquals("Trailing period should be ignored", 
            unpackArray(new String[]{"a", "a.b"}), 
            unpackArray(PerfMon.parseMonitorHirearchy("a.b.")));
        assertEquals("Multiple trailing periods be should be ignored", 
            unpackArray(new String[]{"a", "a.b"}), 
            unpackArray(PerfMon.parseMonitorHirearchy("a.b......")));

        
        assertEquals("Simple hirearchy", 
            unpackArray(new String[]{"a", "a.b", "a.b.c"}), 
            unpackArray(PerfMon.parseMonitorHirearchy("a.b.c")));
        assertEquals("Whitespace is respected", 
            unpackArray(new String[]{"a", "a.this is a test", "a.this is a test.c"}), 
            unpackArray(PerfMon.parseMonitorHirearchy("a.this is a test.c")));
    }

/*----------------------------------------------------------------------------*/    
    public void testActiveCount() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String MONITOR_KEY = "testActiveCount";
        final int NUM_THREADS = 10;
        
        TestPerfMonThread.resetLatch();
        for (int i = 0; i < NUM_THREADS; i++) {
            new TestPerfMonThread(MONITOR_KEY).start();
        }
        Thread.sleep(500);
        
        assertEquals("active count", NUM_THREADS, PerfMon.getMonitor(MONITOR_KEY).getActiveThreadCount());
        TestPerfMonThread.releaseLatch();
    }

/*----------------------------------------------------------------------------*/  
    public void testDurationIsThreadSafe() throws Exception {
        final String MONITOR_KEY = "testDurationIsThreadSafe";
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        TestPerfMonThread.resetLatch();
        
        PerfMonTimer timer = PerfMonTimer.start(MONITOR_KEY);
        Thread.sleep(500);
        
        // Running the timer on a concurrent thread should not change our duration...
        new TestPerfMonThread(MONITOR_KEY).start();
        TestPerfMonThread.releaseLatch(); 
        
        Thread.sleep(500);
        PerfMonTimer.stop(timer);
        
        long maxDuration = PerfMon.getMonitor(MONITOR_KEY).getMaxDuration();
        // Componsate for granularity of Java Timer...
        assertTrue("Max duration should be around 1000ms but was: " + maxDuration, maxDuration >= 980 && maxDuration < 1050);
    }
    

/*----------------------------------------------------------------------------*/    
    public void testIntervalAppender() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        
        final String MONITOR_NAME = "testIntervalResults";
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        PerfMon perfMon = PerfMon.getMonitor(MONITOR_NAME);
        perfMon.addAppender(appenderID);
        Thread.sleep(100);
  
        // First Period... 1 hit AND 0 completions
        PerfMonTimer timer = PerfMonTimer.start(MONITOR_NAME);
        Thread.sleep(INTERVAL_MILLIS + 100);
        
        
        // Second Period... 2 hits AND 2 Completions
        PerfMonTimer.stop(timer);
        timer = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer.stop(timer);
        
        timer = PerfMonTimer.start(MONITOR_NAME);
        Thread.sleep(INTERVAL_MILLIS + 100);
       
        // Third Period... 3 hits AND 4 Completions
        PerfMonTimer.stop(timer);

        timer = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer.stop(timer);
        timer = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer.stop(timer);
        timer = PerfMonTimer.start(MONITOR_NAME);
        PerfMonTimer.stop(timer);
        Thread.sleep(INTERVAL_MILLIS+100);
        
        TestAppender appender = (TestAppender)Appender.getOrCreateAppender(appenderID);
        appender.flush();
        assertEquals("Number of log events appended", 3, appender.output.size());
        IntervalData data0 = (IntervalData)appender.output.get(0);
        IntervalData data1 = (IntervalData)appender.output.get(1);
        IntervalData data2 = (IntervalData)appender.output.get(2);
        
        assertEquals("expected hits", 1, data0.getTotalHits());
        assertEquals("expected hits", 2, data1.getTotalHits());
        assertEquals("expected hits", 3, data2.getTotalHits());

        assertEquals("expected completions", 0, data0.getTotalCompletions());
        assertEquals("expected completions", 2, data1.getTotalCompletions());
        assertEquals("expected completions", 4, data2.getTotalCompletions());

        perfMon.removeAppender(appenderID);
    }

    
    
    /**
     * This tests a defect where adding child monitors did not work
     */
    public void testAddAppenderToChildrenOnly() throws Exception {

        final String MONITOR_NAME = "testAddAppenderToChildrenOnly.a";
        final String MONITOR_NAME_CHILD = MONITOR_NAME + ".b";
        final String MONITOR_NAME_GRANDCHILD = MONITOR_NAME_CHILD + ".c";
        
        final long INTERVAL_MILLIS = 100;
        PerfMon perfMon = PerfMon.getMonitor(MONITOR_NAME);
        
        AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        perfMon.addAppender(appenderID, "/*");

        PerfMonTimer timer = PerfMonTimer.start(MONITOR_NAME_GRANDCHILD);
        Thread.sleep(INTERVAL_MILLIS + 50);
        PerfMonTimer.stop(timer);
        
        TestAppender appender = (TestAppender)Appender.getOrCreateAppender(appenderID);
        appender.flush();
        assertEquals("Number of log events appended", 1, appender.output.size());

        IntervalData data = (IntervalData)appender.output.get(0);
        assertEquals("Should be associated with the child", MONITOR_NAME_CHILD, data.getOwner().getName());
        
        perfMon.removeAppender(appenderID);
    }    
    
/*----------------------------------------------------------------------------*/    
    public void testAppenderIsAssignedToChildren() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String MONITOR_NAME = "testAppenderIsAssignedToChildren";
        PerfMon rootMon = PerfMon.getRootMonitor();

        PerfMon childMon = PerfMon.getMonitor(MONITOR_NAME);
        rootMon.addAppender(appenderID);
        assertEquals("Root monitor should have an appender", 1, rootMon.getNumAppenders());
        assertEquals("Root monitor should never have tasks associated with it", 0, rootMon.getNumPerfMonTasks());
         
//        assertEquals("Appender should be associated with child monitor", 1, childMon.getNumPerfMonDataElements());
        
        PerfMon grandChildMon = PerfMon.getMonitor(MONITOR_NAME + ".grandChild");
        assertEquals("Appender should be associated with grandchild monitor", 1, grandChildMon.getNumPerfMonTasks());
        
        rootMon.removeAppender(appenderID);
        
        assertEquals("Appender should be removed from child monitor", 0, childMon.getNumPerfMonTasks());
        assertEquals("Appender should be removed from grandchild monitor", 0, grandChildMon.getNumPerfMonTasks());
    }

    private boolean timersMatch(String timerKey1, String timerKey2) {
    	boolean result = false;
    	
    	PerfMonTimer timer1 = null;
    	PerfMonTimer timer2 = null;
    	try {
    		timer1 = PerfMonTimer.start(timerKey1);
    		timer2 = PerfMonTimer.start(timerKey2);
    		result = (timer1 == timer2);
    	} finally {
    		PerfMonTimer.stop(timer2);
    		PerfMonTimer.stop(timer1);
    	}
    	
    	return result;
    }

    
    /*----------------------------------------------------------------------------*/    
    private boolean isActiveTimer(String timerKey) {
    	boolean result = false;
    	
    	PerfMonTimer timer = null;
    	try {
    		timer = PerfMonTimer.start(timerKey);
    		result = (timer != PerfMonTimer.getNullTimer()) && timer.perfMon.isActive();
    	} finally {
    		PerfMonTimer.stop(timer);
    	}
    	
    	return result;
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersParentOnlyPattern() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_PARENT_ONLY = "./";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
        parent.addAppender(appenderID, PATTERN_PARENT_ONLY);
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer (Should actually be the same timer as the parents)", 
        		isActiveTimer(CHILD_KEY) && timersMatch(PARENT_KEY, CHILD_KEY));
        assertTrue("grandChild is active timer (Should actually be the same timer as the parents)", 
        		isActiveTimer(GRAND_CHILD_KEY) && timersMatch(PARENT_KEY, GRAND_CHILD_KEY));
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersChildOnlyPattern() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_CHILDREN_ONLY = "/*";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
        parent.addAppender(appenderID, PATTERN_CHILDREN_ONLY);
        assertFalse("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", 
        		isActiveTimer(CHILD_KEY));
        assertTrue("grandChild is active timer (Should actually be the same timer as its parents)", 
        		isActiveTimer(GRAND_CHILD_KEY) && timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersOnParentAndChildrenPattern() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_PARENT_AND_CHILDREN = "./*";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
        parent.addAppender(appenderID, PATTERN_PARENT_AND_CHILDREN);
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY) && !timersMatch(PARENT_KEY, CHILD_KEY));
        assertFalse("grandChild is active timer ", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
        
        // Try it again... This time go through the code with the monitor already in place.
        parent.removeAppender(appenderID);
        parent.addAppender(appenderID, PATTERN_PARENT_AND_CHILDREN);
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY) && !timersMatch(PARENT_KEY, CHILD_KEY));
        assertFalse("grandChild is active timer ", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersNoParentAllDescendents() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_DESCENDENTS = "/**";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
        parent.addAppender(appenderID, PATTERN_DESCENDENTS);
        assertFalse("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY));
        assertTrue("grandChild should be active timer", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersParentAndAllDescendents() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_ALL = "./**";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
        parent.addAppender(appenderID, PATTERN_ALL);
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY) && !timersMatch(PARENT_KEY, CHILD_KEY));
        assertTrue("grandChild should be active timer", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersForAllDescendents() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_ALL_DESCENDENTS = "/**";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
        parent.addAppender(appenderID, PATTERN_ALL_DESCENDENTS);
        assertFalse("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", 
        		isActiveTimer(CHILD_KEY));
        assertTrue("grandChild is active timer", 
        		isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(PARENT_KEY, GRAND_CHILD_KEY));
    }
    
    
/*----------------------------------------------------------------------------*/    
    public void testAppenderRespectsPattern() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_PARENT_ONLY = "./";
        final String PATTERN_CHILDREN_ONLY = "/*";
        final String PATTERN_ALL_DESCENDENTS = "/**";
        final String PATTERN_PARENT_AND_ALL_DESCENDENTS = "./**";
        
        final String PARENT_KEY = "a";
        final String CHILD_KEY = "a.b";
        final String GRAND_CHILD_KEY = "a.b.c";
        
        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        PerfMon child = PerfMon.getMonitor(CHILD_KEY);
        PerfMon grandChild = PerfMon.getMonitor(GRAND_CHILD_KEY);
        
        parent.addAppender(appenderID, PATTERN_PARENT_ONLY);
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertTrue("parent.isActive", parent.isActive());
        assertEquals("parent should have a task associated with the appender", 1, parent.getNumPerfMonTasks());

        assertEquals("child appender count", 0, child.getNumAppenders());
 
        assertEquals("grandChild appender count", 0, grandChild.getNumAppenders());
        
        assertEquals("new child appender count", 0,  PerfMon.getMonitor("a.1").getNumAppenders());
        assertEquals("grandChild appender count", 0, PerfMon.getMonitor("a.1.1").getNumAppenders());
        
        parent.addAppender(appenderID, PATTERN_CHILDREN_ONLY);
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertFalse("parent.isActive", parent.isActive());
        
        assertEquals("parent should not have a task associated with the appender", 0, parent.getNumPerfMonTasks());
        assertEquals("child appender count", 1, child.getNumAppenders());
        assertTrue("child.isActive", child.isActive());
        
        assertEquals("grandChild appender count", 0, grandChild.getNumAppenders());

        assertEquals("new child appender count", 1,  PerfMon.getMonitor("a.2").getNumAppenders());
        assertEquals("grandChild appender count", 0, PerfMon.getMonitor("a.2.2").getNumAppenders());
        
        parent.addAppender(appenderID, PATTERN_ALL_DESCENDENTS);
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertFalse("parent.isActive", parent.isActive());
        assertEquals("parent should not have a task associated with the appender", 0, parent.getNumPerfMonTasks());
        
        assertEquals("child appender count", 1, child.getNumAppenders());
        assertTrue("child.isActive", child.isActive());
        assertEquals("grandChild appender count", 1, grandChild.getNumAppenders());

        assertEquals("new child appender count", 1,  PerfMon.getMonitor("a.3").getNumAppenders());
        assertEquals("grandChild appender count", 1, PerfMon.getMonitor("a.3.3").getNumAppenders());
        assertTrue("grandChild.isActive", grandChild.isActive());
        
        parent.addAppender(appenderID, PATTERN_PARENT_AND_ALL_DESCENDENTS);
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertEquals("parent should have a task associated with the appender", 1, parent.getNumPerfMonTasks());
        assertTrue("parent.isActive", parent.isActive());
        
        assertEquals("child appender count", 1, child.getNumAppenders());
        assertTrue("child.isActive", child.isActive());
        assertEquals("grandChild appender count", 1, grandChild.getNumAppenders());

        assertEquals("new child appender count", 1,  PerfMon.getMonitor("a.4").getNumAppenders());
        assertEquals("grandChild appender count", 1, PerfMon.getMonitor("a.4.4").getNumAppenders());
        assertTrue("grandChild.isActive", grandChild.isActive());
    }


/*----------------------------------------------------------------------------*/    
    public void testRemoveAppenderRespectsPattern() throws Exception {
        final long INTERVAL_MILLIS = 1000;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_PARENT_ONLY = "./";
        final String PATTERN_CHILDREN_ONLY = "/*";
        final String PATTERN_ALL_DESCENDENTS = "/**";
        final String PATTERN_PARENT_AND_ALL_DESCENDENTS = "./**";
        
        PerfMon parent = PerfMon.getMonitor("a");
        PerfMon child = PerfMon.getMonitor("a.b");
        PerfMon grandChild = PerfMon.getMonitor("a.b.c");
        
        parent.addAppender(appenderID);
        parent.removeAppender(appenderID, PATTERN_PARENT_AND_ALL_DESCENDENTS);
        
        assertEquals("parent appender count", 0, parent.getNumAppenders());
        assertEquals("child appender count", 0, child.getNumAppenders());
        assertEquals("grandChild appender count", 0, grandChild.getNumAppenders());
        
        parent.addAppender(appenderID);
        parent.removeAppender(appenderID, PATTERN_ALL_DESCENDENTS);
        
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertEquals("child appender count", 0, child.getNumAppenders());
        assertEquals("grandChild appender count", 0, grandChild.getNumAppenders());

        parent.addAppender(appenderID);
        parent.removeAppender(appenderID, PATTERN_CHILDREN_ONLY);
        
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertEquals("child appender count", 0, child.getNumAppenders());
        assertEquals("grandChild appender count", 1, grandChild.getNumAppenders());

        parent.addAppender(appenderID);
        parent.removeAppender(appenderID, PATTERN_PARENT_ONLY);
        
        assertEquals("parent appender count", 0, parent.getNumAppenders());
        assertEquals("child appender count", 1, child.getNumAppenders());
        assertEquals("grandChild appender count", 1, grandChild.getNumAppenders());
        
    }
    
/*----------------------------------------------------------------------------*/    
    public void testAppenderPatternParentChildConversion() throws Exception {
        final String PATTERN_PARENT_ONLY = "./";
        final String PATTERN_CHILDREN_ONLY = "/*";
        final String PATTERN_ALL_DESCENDENTS = "/**";
        final String PATTERN_PARENT_AND_ALL_DESCENDENTS = "./**";
        
        assertEquals("Parent only to child", "", PerfMon.parentToChildConversion(PATTERN_PARENT_ONLY));
        assertEquals("children only to child", PATTERN_PARENT_ONLY, PerfMon.parentToChildConversion(PATTERN_CHILDREN_ONLY));
        assertEquals("all descendents to child", PATTERN_PARENT_AND_ALL_DESCENDENTS, PerfMon.parentToChildConversion(PATTERN_ALL_DESCENDENTS));
        assertEquals("parent and all descendents to child", PATTERN_PARENT_AND_ALL_DESCENDENTS, PerfMon.parentToChildConversion(PATTERN_PARENT_AND_ALL_DESCENDENTS));
    }

/*----------------------------------------------------------------------------*/    
    public void testMonitorResetsWhenMadeInactive() throws Exception {
        final String MON_NAME = "base";
        PerfMon mon = PerfMon.getMonitor(MON_NAME);
        
        assertFalse("Monitor is not active", mon.isActive());
        mon.addAppender(BogusAppender.getAppenderID());
        assertTrue("Monitor is active", mon.isActive());

        PerfMonTimer timer = PerfMonTimer.start(MON_NAME);
        Thread.sleep(100);
        PerfMonTimer.stop(timer);
        
        // Keep 1 active timer...  
        timer = PerfMonTimer.start(MON_NAME);
        Thread.sleep(100);
        
        
        // Make inactive and verify that the counters are reset...
        mon.removeAppender(BogusAppender.getAppenderID());
        assertFalse("Monitor is no longer active", mon.isActive());
        
        assertEquals("MaxDuration", 0, mon.getMaxDuration());
        assertEquals("MinDuration", PerfMon.NOT_SET, mon.minDuration);
        assertEquals("totalHits", 0, mon.getTotalHits());
        assertEquals("totalCompletions", 0, mon.getTotalCompletions());
        assertEquals("totalDuration", 0, mon.getTotalDuration());
        assertEquals("maxActiveThreadCount", 0, mon.getMaxActiveThreadCount());
        
        // If a active thread count is reset we have the possiblilty of a 
        // activeThreadCount going negative!
        assertEquals("activeThreadCount MUST not be reset when monitor is made inactive"
            ,1, mon.getActiveThreadCount());
        
        // Add and appender and make the monitor active...
        mon.addAppender(BogusAppender.getAppenderID());
        
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
    public void testRefreshConfigWithPaterns() throws Exception {
        final String SIMPLE_MONITOR = "simple";
        final String SIMPLE_CHILD = "simple.a";
        
        final String APPENDER_10_SEC = "10 Second Appender";
        final String APPENDER_20_SEC = "20 Second Appender";
        final String APPENDER_30_SEC = "30 Second Appender";
        
        final String BOGUS_APPENDER = BogusAppender.class.getName();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor(SIMPLE_MONITOR);
        config.defineAppender(APPENDER_10_SEC, BOGUS_APPENDER, "10 seconds");
        config.defineAppender(APPENDER_20_SEC, BOGUS_APPENDER, "20 seconds");

        // Apply 10 Second Appender to Parent only
        config.attachAppenderToMonitor(SIMPLE_MONITOR, APPENDER_10_SEC, PerfMon.APPENDER_PATTERN_PARENT_ONLY);
        
        // Apply 20 Second Appender to Chidren only
        config.attachAppenderToMonitor(SIMPLE_MONITOR, APPENDER_20_SEC, PerfMon.APPENDER_PATTERN_CHILDREN_ONLY);
        PerfMon.configure(config);
        
        assertTrue("Should have appender", PerfMon.getMonitor(SIMPLE_MONITOR).hasAppenderWithTask(BOGUS_APPENDER, 10000));
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_MONITOR).hasAppenderWithTask(BOGUS_APPENDER, 20000));
        
        assertTrue("Should have appender", PerfMon.getMonitor(SIMPLE_CHILD).hasAppenderWithTask(BOGUS_APPENDER, 20000));
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_CHILD).hasAppenderWithTask(BOGUS_APPENDER, 10000));
    
        // Now reconfigure..
        config = new PerfMonConfiguration();
        config.defineMonitor(SIMPLE_MONITOR);
        config.defineAppender(APPENDER_30_SEC, BOGUS_APPENDER, "30 seconds");
        
        // Apply 30 Second Appender to Chidren only
        config.attachAppenderToMonitor(SIMPLE_MONITOR, APPENDER_30_SEC, PerfMon.APPENDER_PATTERN_CHILDREN_ONLY);
        PerfMon.configure(config);

        // Parent should not have any active appenders....
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_MONITOR).hasAppenderWithTask(BOGUS_APPENDER, 10000));
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_MONITOR).hasAppenderWithTask(BOGUS_APPENDER, 20000));
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_MONITOR).hasAppenderWithTask(BOGUS_APPENDER, 30000));
    
        // Child should only have new appender...
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_CHILD).hasAppenderWithTask(BOGUS_APPENDER, 10000));
        assertFalse("Should not have appender", PerfMon.getMonitor(SIMPLE_CHILD).hasAppenderWithTask(BOGUS_APPENDER, 20000));
        assertTrue("Should have appender", PerfMon.getMonitor(SIMPLE_CHILD).hasAppenderWithTask(BOGUS_APPENDER, 30000));
    }    
    
/*----------------------------------------------------------------------------*/    
    public void testAddRemoveThreadTraceToMonitorWithConfigure() throws Exception {
        final String MONITOR_KEY = "testAddThreadTraceToMonitorWithConfigure";
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
       
        ThreadTraceConfig traceConfig = new ThreadTraceConfig();
        traceConfig.addAppender(config.getAppenderForName("APP1"));
        
        config.addThreadTraceConfig(MONITOR_KEY, traceConfig);
        
        PerfMon.configure(config);
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        assertTrue("Monitor should have threadTraceConfig", traceConfig == mon.getThreadTraceConfig());
        
        config = new PerfMonConfiguration();
        PerfMon.configure(config);
        
        mon = PerfMon.getMonitor(MONITOR_KEY);
        assertNull("threadTraceConfig should have been removed", mon.getThreadTraceConfig());
    }


    public void testConfigureWillSkipClassNotFoundAppender() throws Exception {
        PerfMon.deInit();
        
        // Define a single monitor on DESCENDENT_2.
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineSnapShotMonitor("bogus", SimpleSnapShot.class.getName());
        config.defineMonitor("abc");
        config.defineAppender("APP1", "ClassNameDoesNotExist", "1 second");
        config.attachAppenderToMonitor("abc", "APP1");
        config.attachAppenderToSnapShotMonitor("bogus", "APP1");

        PerfMon.configure(config);
        assertTrue("Should indicate partial load", config.isPartialLoad());
        assertEquals("Should indicate class could not be found", "Appender: ClassNameDoesNotExist", config.getClassNotFoundInfo().iterator().next());
    }

    
    public void testConfigureWillSkipClassNotFoundSnapShotMonitor() throws Exception {
        PerfMon.deInit();
        
        // Define a single monitor on DESCENDENT_2.
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineSnapShotMonitor("bogus", "ClassNameDoesNotExist");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToSnapShotMonitor("bogus", "APP1");

        PerfMon.configure(config);
        assertTrue("Should indicate partial load", config.isPartialLoad());
        assertEquals("Should indicate class could not be found", "SnapShotMonitor: ClassNameDoesNotExist", config.getClassNotFoundInfo().iterator().next());
    }
    
    public void testConfigureWillClearMonitorsNoLongerActive() throws Exception {
        final String DESCENDENT_1 = "aa.b.c";
        final String DESCENDENT_2 = "aa.b.c.d";
        
        // Add an appender to DESCENDENT_1 and DESCENDENT_2...
        PerfMon.getMonitor(DESCENDENT_1).addAppender(TestAppender.getAppenderID(1000),
            PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
        
        // Define a single monitor on DESCENDENT_2.
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor(DESCENDENT_2);
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor(DESCENDENT_2, "APP1");

        // Run Config... This should remove the monitor on 
        // DESCENDENT_1
        PerfMon.configure(config);
        
        assertEquals("Monitor should have been removed", 0, PerfMon.getMonitor(DESCENDENT_1).getNumAppenders());
        assertEquals("Monitor should remain on descenden", 1, PerfMon.getMonitor(DESCENDENT_2).getNumAppenders());
    }
    
    
    public void testClearAllConfigViaEmptyMonitor() throws Exception {
        final String MONITOR_ROOT = PerfMon.ROOT_MONITOR_NAME;
        final String DESCENDENT_1 = "aa.b.c";
        final String DESCENDENT_2 = "aa.b.c.d";
        
        // All appenders should now have monitor...
        PerfMon.getMonitor(DESCENDENT_1).addAppender(TestAppender.getAppenderID(1000));
        
        // Add second Appender to DESCENDENT_1
        PerfMon.getMonitor(DESCENDENT_1).addAppender(TestAppender.getAppenderID(5000));
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        // Root will define no monitors at all... Should clear any
        // that currently exists..
        config.defineMonitor(PerfMon.ROOT_MONITOR_NAME);
        
        // Will redefine 3 monitors
        // 1 added by root and the 1 added by it's parent and a new one
        config.defineMonitor(DESCENDENT_2);
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.defineAppender("APP2", TestAppender.class.getName(), "5 seconds");
        config.defineAppender("APP3", TestAppender.class.getName(), "15 seconds");
        
        config.attachAppenderToMonitor(DESCENDENT_2, "APP1");
        config.attachAppenderToMonitor(DESCENDENT_2, "APP2");
        config.attachAppenderToMonitor(DESCENDENT_2, "APP3");
        
        PerfMon.configure(config);
        
        assertEquals("Monitor should have been removed", 0, PerfMon.getMonitor(MONITOR_ROOT).getNumAppenders());
        assertEquals("Monitor should have been removed", 0, PerfMon.getMonitor(DESCENDENT_1).getNumAppenders());
        assertEquals("Monitor should remain on descendent and a new one added", 3, PerfMon.getMonitor(DESCENDENT_2).getNumAppenders());
        assertEquals("Should have a task for each appender", 3, PerfMon.getMonitor(DESCENDENT_2).getNumPerfMonTasks());
    }
        
    @SnapShotProvider
    public static class SimpleSnapShot {
    	private static int numTimesInvoked = 0;
    
    	@SnapShotGauge
    	public int getNumTimesInvoked() {
    		return numTimesInvoked++;
    	}
    }

    @SnapShotProvider
    public static class BogusSnapShot {
    	@SnapShotGauge
    	public int getBogusValue() {
    		return 1;
    	}
    }
    
    /*----------------------------------------------------------------------------*/
    /**
     * This test verifies that when a SnapShotMonitor is removed during a config
     * refresh it is no longer actively invoked.
     */
    public void testRemoveSnapShotConfigOnRefresh() throws Exception {
        final int INTERVAL_MILLIS = 250; 
    	
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("Appender", TestAppender.class.getName(), INTERVAL_MILLIS + " ms");
        config.defineSnapShotMonitor("SimpleSnapShot", SimpleSnapShot.class.getName());
        config.attachAppenderToSnapShotMonitor("SimpleSnapShot", "Appender");
        
        PerfMon.configure(config);
        
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        TestAppender appender = (TestAppender)Appender.getOrCreateAppender(appenderID);
        
        Thread.sleep(INTERVAL_MILLIS * 5);
        
        config = new PerfMonConfiguration();
        config.defineAppender("Appender", TestAppender.class.getName(), INTERVAL_MILLIS + " ms");
        config.defineSnapShotMonitor("BogusSnapShot", BogusSnapShot.class.getName());
        config.attachAppenderToSnapShotMonitor("BogusSnapShot", "Appender");
        
        PerfMon.configure(config);
        int timesInvoked = SimpleSnapShot.numTimesInvoked;
        
        Thread.sleep(INTERVAL_MILLIS * 5);
        
        assertEquals("Obsoleted SnapShotMonitor should have been removed", 
        		timesInvoked, SimpleSnapShot.numTimesInvoked);
    }
    
 
    
    public static class ThreadTraceTestRunnable implements Runnable {
    	public final static String CATEGORY = "ThreadTraceRunner.test";
    	
		public void run() {
			PerfMonTimer timer = PerfMonTimer.start(CATEGORY);
			
			PerfMonTimer.stop(timer);
		}
    	
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testActiveThreadTraceWillRetainAppender() throws Exception {
    	String appenderName = "THREAD_TRACE";
    	PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender(appenderName, TestAppender.class.getName(), "1 minute");
        config.defineMonitor(ThreadTraceTestRunnable.CATEGORY);
//        config.attachAppenderToMonitor(ThreadTraceTestRunnable.CATEGORY, appenderName);
        
        AppenderID appenderID = config.getAppenderForName(appenderName);
        
        ThreadTraceConfig threadTraceConfig = new ThreadTraceConfig();
        threadTraceConfig.addAppender(config.getAppenderForName(appenderName));
        
        config.addThreadTraceConfig(ThreadTraceTestRunnable.CATEGORY, threadTraceConfig);
    	PerfMon.configure(config);
    	
    	new ThreadTraceTestRunnable().run();
    	
        TestAppender appender = (TestAppender)Appender.getOrCreateAppender(appenderID);
        appender.flush();

        assertEquals("Should have written a thread trace", 1, appender.output.size());
        ThreadTraceData data = (ThreadTraceData)appender.output.get(0);
        System.out.println(data.toAppenderString());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testRefreshConfig() throws Exception {
        final String MONITOR_ROOT = PerfMon.ROOT_MONITOR_NAME;
        final String DESCENDENT_1 = "aa.b.c";
        final String DESCENDENT_2 = "aa.b.c.d";
        
        // All appenders should now have monitor...
        PerfMon.getMonitor(MONITOR_ROOT).addAppender(TestAppender.getAppenderID(1000));
        
        // Add second Appender to DESCENDENT_1
        PerfMon.getMonitor(DESCENDENT_1).addAppender(TestAppender.getAppenderID(5000));
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        // Root will define no monitors at all... Should clear any
        // that currently exists..
        config.defineMonitor(PerfMon.ROOT_MONITOR_NAME);
        
        // Will redefine 3 monitors
        // 1 added by root and the 1 added by it's parent and a new one
        config.defineMonitor(DESCENDENT_2);
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.defineAppender("APP2", TestAppender.class.getName(), "5 seconds");
        config.defineAppender("APP3", TestAppender.class.getName(), "15 seconds");
        
        config.attachAppenderToMonitor(DESCENDENT_2, "APP1");
        config.attachAppenderToMonitor(DESCENDENT_2, "APP2");
        config.attachAppenderToMonitor(DESCENDENT_2, "APP3");
        
        PerfMon.configure(config);
        
        assertEquals("Monitor should have been removed", 0, PerfMon.getMonitor(MONITOR_ROOT).getNumAppenders());
        assertEquals("Monitor should have been removed", 0, PerfMon.getMonitor(DESCENDENT_1).getNumAppenders());
        assertEquals("Monitor should remain on descendent and a new one added", 3, PerfMon.getMonitor(DESCENDENT_2).getNumAppenders());
        assertEquals("Should have a task for each appender", 3, PerfMon.getMonitor(DESCENDENT_2).getNumPerfMonTasks());
    }
    
    // This test validates that when an appender is removed
    // The associated data with that appender is removed
    // from the data array...
    public void testRemoveAppenderRemovesDataElement() throws Exception {
        final String MONITOR_NAME = "testRemoveAppenderRemovesDataElement";
        
        PerfMon perfMon = PerfMon.getMonitor(MONITOR_NAME);
        int start = perfMon.getNumPerfMonTasks();
        
        Appender.AppenderID id = TestAppender.getAppenderID();
        perfMon.addAppender(id);
        assertEquals("Number of perfmon data elements",start + 1, perfMon.getNumPerfMonTasks());
        
        perfMon.removeAppender(id);
        assertEquals("Number of perfmon data elements",start, perfMon.getNumPerfMonTasks());
    }
    
/*----------------------------------------------------------------------------*/
    private static class TestAppender extends Appender {
        private List<PerfMonData> output = new Vector<PerfMonData>();

        public static AppenderID getAppenderID() {
            return Appender.getAppenderID(TestAppender.class.getName());
        }
        
        public static AppenderID getAppenderID(long intervalMillis) {
            return Appender.getAppenderID(TestAppender.class.getName(), intervalMillis);
        }
        
        public TestAppender(AppenderID id) {
            super(id);
        }
        
        public void outputData(PerfMonData data) {
            output.add(data);
        }
    }
    
/*----------------------------------------------------------------------------*/
    private static class BogusAppender extends Appender {
        static int dataStopCount = 0;
        
        public static AppenderID getAppenderID() {
            return Appender.getAppenderID(BogusAppender.class.getName());
        }
        
        public BogusAppender(AppenderID id) {
            super(id);
        }
        
        
        
        public void outputData(@SuppressWarnings("unused") PerfMonData data) {
        }
        

        public IntervalData newIntervalData(PerfMon owner, long timeStart) {
            return new IData(owner, timeStart);
        }
        
        private static class IData extends IntervalData {
            private IData(PerfMon owner, long timeStart) {
                super(owner, timeStart);
            }
            
            void stop(long duration, long durationSquared, long systemTime) {
                BogusAppender.dataStopCount++;
                super.stop(duration, durationSquared, systemTime);
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public void testDuration() throws Exception {
        PerfMon.getRootMonitor().addAppender(BogusAppender.getAppenderID());
        
        final String GRANDPARENT_MONITOR_NAME = "testDuration";
        final String PARENT_MONITOR_NAME = "testDuration.parent";
        final String CHILD_MONITOR_NAME = "testDuration.parent.child";
        
        PerfMonTimer timer = PerfMonTimer.start(CHILD_MONITOR_NAME);
        Thread.sleep(500);
        PerfMonTimer.stop(timer);
        
        timer = PerfMonTimer.start(CHILD_MONITOR_NAME);
        Thread.sleep(1000);
        PerfMonTimer.stop(timer);
        
        assertTrue("All related monitors should have the same average duration",
            valuesMatch(
                PerfMon.getMonitor(GRANDPARENT_MONITOR_NAME).getAverageDuration(),
                PerfMon.getMonitor(PARENT_MONITOR_NAME).getAverageDuration(),
                PerfMon.getMonitor(CHILD_MONITOR_NAME).getAverageDuration()));
        assertTrue("All related monitors should have the same max duration",
            valuesMatch(
                PerfMon.getMonitor(GRANDPARENT_MONITOR_NAME).getMaxDuration(),
                PerfMon.getMonitor(PARENT_MONITOR_NAME).getMaxDuration(),
                PerfMon.getMonitor(CHILD_MONITOR_NAME).getMaxDuration()));
        assertTrue("All related monitors should have the same min duration",
            valuesMatch(
                PerfMon.getMonitor(GRANDPARENT_MONITOR_NAME).getMinDuration(),
                PerfMon.getMonitor(PARENT_MONITOR_NAME).getMinDuration(),
                PerfMon.getMonitor(CHILD_MONITOR_NAME).getMinDuration()));
        
        long avgDuration = PerfMon.getMonitor(CHILD_MONITOR_NAME).getAverageDuration();
        long maxDuration = PerfMon.getMonitor(CHILD_MONITOR_NAME).getMaxDuration();
        long minDuration = PerfMon.getMonitor(CHILD_MONITOR_NAME).getMinDuration();
        
        // Comparisons have to componsate for the granularity of the java timer...
        assertTrue("Average duration should be around 750ms but was: " + avgDuration, avgDuration > 730 && avgDuration < 770);
        assertTrue("Max duration should be around 1000ms but was: " + maxDuration, maxDuration > 980 && maxDuration < 1020);
        assertTrue("Min duration should be around 500ms but was: " + minDuration, minDuration > 480 && minDuration < 520);
    }
    
/*----------------------------------------------------------------------------*/    
    private static boolean valuesMatch(long a, long b, long c) {
        return (a == b && b == c);
    }
    
/*----------------------------------------------------------------------------*/    
    private static void doSomethingTrivial(boolean useMon, int x) throws Exception {
        PerfMonTimer timer = null;
        
        if (useMon && x == 0) {
            PerfMon mon = PerfMon.getMonitor("simple.test.a");
            mon.addAppender(TestAppender.getAppenderID());
        }
        
        if (useMon) {
            timer = PerfMonTimer.start("simple.test.a." + (x/10));
        }
        
        try {
            int val = (int)(Math.random() * 100) + 1;
            double result = Math.sqrt(val);
            System.err.println("The square root of " + val + "=" + result);
        } finally {
            if (useMon) {
                PerfMonTimer.stop(timer);
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public void testTimming() throws Exception {
        long tsNow = System.currentTimeMillis();
        for (int x = 0; x < 5000; x++) {
            doSomethingTrivial(false, x);
        }
        long durationWithout = System.currentTimeMillis() - tsNow;
        

        tsNow = System.currentTimeMillis();
        for (int x = 0; x < 5000; x++) {
            doSomethingTrivial(true, x);
        }
        long durationEnabled = System.currentTimeMillis() - tsNow;

        tsNow = System.currentTimeMillis();
        for (int x = 0; x < 5000; x++) {
            doSomethingTrivial(true, x);
        }
        long durationEnabledRun2 = System.currentTimeMillis() - tsNow;

        tsNow = System.currentTimeMillis();
        for (int x = 0; x < 5000; x++) {
            doSomethingTrivial(true, x);
        }
        long durationEnabledRun3 = System.currentTimeMillis() - tsNow;
        
        PerfMon.deInit();
        
        tsNow = System.currentTimeMillis();
        for (int x = 0; x < 5000; x++) {
            doSomethingTrivial(true, x);
        }
        long durationDisabled = System.currentTimeMillis() - tsNow;
        System.err.println("durationWithout=" + durationWithout 
            + " durationEnabled=" + durationEnabled 
            + " durationEnabledRun2=" + durationEnabledRun2 
            + " durationEnabledRun3=" + durationEnabledRun3 
            + " durationDisabled=" + durationDisabled);
    }
  
/*----------------------------------------------------------------------------*/
    private static class TestPerfMonThread extends Thread {
        final String monitorKey;
        private static CountDownLatch latch = new CountDownLatch(1);
        
        static void resetLatch() {
            latch = new CountDownLatch(1);
        }
        static void releaseLatch() {
            latch.countDown();
        }
        
        TestPerfMonThread(String monitorKey) {
            this.monitorKey = monitorKey;
        }
        
        public void run() {
            PerfMonTimer timer = PerfMonTimer.start(monitorKey);
            try {
                latch.await();
            } catch (InterruptedException ex) {
                // Nothing todo
            } finally {
                PerfMonTimer.stop(timer);
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    private String unpackArray(String [] str) {
        String result = "";
        for (int i = 0; i < str.length; i++) {
            if (i > 0) {
                result += " ,";
            }
            result += "\"" + str[i] + "\"";
        }
        
        return result;
    }

/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {PerfMonTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
        newSuite.addTest(new PerfMonTest("testConfigureWillSkipClassNotFoundAppender"));
//        newSuite.addTest(new PerfMonTest("testActiveTimersNoParentAllDescendents"));
//        newSuite.addTest(new PerfMonTest("testActiveTimersParentAndAllDescendents"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonTest.class);
        }

        return( newSuite);
    }
}

/*
 *	Copyright 2008, 2009, 2010, 2011 Follett Software Company 
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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;

import junit.framework.TestSuite;
import junit.textui.TestRunner;



public class PerfMonTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    
    public void testToAppenderStringWithSQLActive() throws Exception {
    	PerfMon mon = PerfMon.getMonitor("DAVE");
    	IntervalData id = new IntervalData(mon);
    	
    	assertTrue("Should include SQL time if SQLTime option was enabled", id.toAppenderString(true).contains("(SQL)Avg. Duration"));
    	assertFalse("Should NOT include SQL time if SQLTime option was enabled", id.toAppenderString(false).contains("(SQL)Avg. Duration"));
    
    	// Any SQL monitor should not include the SQL time.  It would be identical to the default sqlTime anyway.
    	mon = PerfMon.getMonitor("SQL");
    	id = new IntervalData(mon);
    	
    	assertFalse("Should NOT include SQL time for SQL category, even if SQL is enabled", id.toAppenderString(true).contains("(SQL)Avg. Duration"));
    	
    	mon = PerfMon.getMonitor("SQL.executeQuery");
    	id = new IntervalData(mon);
    	
    	assertFalse("Should NOT include SQL time for SQL child category, even if SQL is enabled", id.toAppenderString(true).contains("(SQL)Avg. Duration"));
    }
    
    
    
/*----------------------------------------------------------------------------*/
    public PerfMonTest(String name) {
        super(name);
    }

	private TestConfigBuilder configBuilder = null;

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        
        PerfMon.configure();
        BogusAppender.dataStopCount = 0;
        configBuilder = new TestConfigBuilder();
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInitAndCleanMonitors_TESTONLY();
        super.tearDown();
    }
    
    private static final AppenderID bogusAppenderID = AppenderID.getAppenderID(BogusAppender.class.getName());
    
    
/*----------------------------------------------------------------------------*/
    public void testSimple() throws Exception {
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
    	
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
    public void testGetDynamicOnChildOfRoot() throws Exception {
        TestConfigBuilder builder = new TestConfigBuilder();
        
        PerfMon.configure(builder
        	.defineRootMonitor(".")
        	.build(bogusAppenderID));
        
        
        PerfMon monitor = PerfMon.getMonitor("xyz", true);
        assertEquals("Should return the root monitor because their is no appender attachted to monitor",
        		"<ROOT>", monitor.getName());
        
        // Now put an appender on the root where we are monitoring
        // children...  In this case we should create and return a child
        PerfMon.configure(builder
            .defineRootMonitor("./*")
            .build(bogusAppenderID));
        
        monitor = PerfMon.getMonitor("xyz", true);
        assertEquals("Should return the root monitor because their is no appender attachted to monitor",
        		"xyz", monitor.getName());
        
        // Now remove the child based appender...  
        PerfMon.configure(builder
        		.defineRootMonitor(".")
                .build(bogusAppenderID));
        
        // We will still return any child monitor that a
        monitor = PerfMon.getMonitor("xyz", true);
        assertEquals("Should still return child that has already been created",
        		"xyz", monitor.getName());
        
        // We will still return any child monitor that a
        monitor = PerfMon.getMonitor("lmn", true);
        assertEquals("Will no longer create new children",
        		"<ROOT>", monitor.getName());
    }
    
    /*----------------------------------------------------------------------------*/
    public void testGetDynamicOnChildOfMonitor() throws Exception {
        TestConfigBuilder builder = new TestConfigBuilder();

        PerfMon.configure(builder
                .defineMonitor("xyz", ".")
                .build(bogusAppenderID));

        PerfMon monitor = PerfMon.getMonitor("xyz.childA", true);
        assertEquals("Should not have created child", "xyz", monitor.getName());
        
        // Now put an appender on the root where we are monitoring
        // children...  In this case we should create and return a child
        PerfMon.configure(builder
                .defineMonitor("xyz", "./*")
                .build(bogusAppenderID));
        
        monitor = PerfMon.getMonitor("xyz.childA", true);
        assertEquals("Child should have been created",
        		"xyz.childA", monitor.getName());
        
        // Now remove the child based appender...  
        PerfMon.configure(builder
                .defineMonitor("xyz", ".")
                .build(bogusAppenderID));
        
        // We will still return any child monitor that a
        monitor = PerfMon.getMonitor("xyz.childA", true);
        assertEquals("xyz.childA already exists",
        		"xyz.childA", monitor.getName());
        
        monitor = PerfMon.getMonitor("xyz.childB", true);
        assertEquals("Child should not have been created", "xyz", monitor.getName());
    }

    /*----------------------------------------------------------------------------*/
    public void testGetDynamicOnGRANDChildOfMonitor() throws Exception {
        TestConfigBuilder builder = new TestConfigBuilder();

        PerfMon.configure(builder
                .defineMonitor("xyz", "./*")
                .build(bogusAppenderID));

        PerfMon monitor = PerfMon.getMonitor("xyz.childA.grandchild", true);
        assertEquals("Should not have created child but NOT grandchild", "xyz.childA", monitor.getName());
        
        // Now put an appender on the root where we are monitoring
        // children...  In this case we should create and return a child
        PerfMon.configure(builder
                .defineMonitor("xyz", "./**")
                .build(bogusAppenderID));
        
        monitor = PerfMon.getMonitor("xyz.childA.grandchild", true);
        assertEquals("Grand Child should have been created",
        		"xyz.childA.grandchild", monitor.getName());
        
        // Now remove the child based appender...  
        PerfMon.configure(builder
                .defineMonitor("xyz", "./*")
                .build(bogusAppenderID));
        
        // We will still return any child monitor that a
        monitor = PerfMon.getMonitor("xyz.childA.grandchild", true);
        assertEquals("xyz.childA.grandchild already exists",
        		"xyz.childA.grandchild", monitor.getName());
        
        monitor = PerfMon.getMonitor("xyz.childB.grandchild", true);
        assertEquals("Child should not have been created but NOT grandchild", "xyz.childB", monitor.getName());
    }

    
    

    private static void validateMonitorExistsAndCompletion(String key) {
    	PerfMon mon = PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY(key);
    	
        assertNotNull(key + " expected to exist", mon);
        assertEquals(key + " should have 1 completion", 1, mon.getTotalCompletions());
    }
    
    
    /*----------------------------------------------------------------------------*/
    public void testLazyCreateDeepMonitor() throws Exception {
        TestConfigBuilder builder = new TestConfigBuilder();

        PerfMon.configure(builder
                .defineMonitor("xyz", "./**")
                .build(bogusAppenderID));
        
        PerfMonTimer timer = PerfMonTimer.start("xyz.1.2.3.4.5.6.7.8", true);
        PerfMonTimer.stop(timer);
     
        // Make sure we created all of the  child/grandchild/etc.. monitors.
        validateMonitorExistsAndCompletion("xyz.1.2.3.4.5.6.7.8");
        validateMonitorExistsAndCompletion("xyz.1.2.3.4.5.6.7");
        validateMonitorExistsAndCompletion("xyz.1.2.3.4.5.6");
        validateMonitorExistsAndCompletion("xyz.1.2.3.4.5");
        validateMonitorExistsAndCompletion("xyz.1.2.3.4");
        validateMonitorExistsAndCompletion("xyz.1.2.3");
        validateMonitorExistsAndCompletion("xyz.1.2");
        validateMonitorExistsAndCompletion("xyz.1");
    }
    
    
    
/*----------------------------------------------------------------------------*/
    public void testAbort() throws Exception {
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
        
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
        
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
    	
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
        
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./**")
    		.build(bogusAppenderID));
        
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
        
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineRootMonitor("./*")
    		.build(bogusAppenderID));
        
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
        final long INTERVAL_MILLIS = 999;
        
        final String MONITOR_NAME = "testIntervalResults";
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineMonitor(MONITOR_NAME, "./*")
    		.build(appenderID));
        
        
        
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
    }


    /*----------------------------------------------------------------------------*/
    /**
     * This test exposes a defect where when no activity is recorded in an interval (no completions or hits)
     * the max thread count remains at 0, even if there are threads currently active within the monitor.
     * @throws Exception
     */
    public void testIntervalAppenderCountsMaxThreadsWithNoThroughput() throws Exception {
        final long INTERVAL_MILLIS = 500;
        
        final String MONITOR_NAME = "testIntervalResults";
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineMonitor(MONITOR_NAME, "./*")
    		.build(appenderID));
        
        
        Thread.sleep(100);
  
        // First Period... 1 hit AND 0 completions
        PerfMonTimer timer = PerfMonTimer.start(MONITOR_NAME);
        Thread.sleep(INTERVAL_MILLIS + 100);
        
        // Second Period... 0 hits and 0 completions
        Thread.sleep(INTERVAL_MILLIS + 100);
       
        // Third Period... 0 hits AND 1 Completion
        PerfMonTimer.stop(timer);
        Thread.sleep(INTERVAL_MILLIS + 100);
        
        TestAppender appender = (TestAppender)Appender.getOrCreateAppender(appenderID);
        appender.flush();
        assertEquals("Number of log events appended", 3, appender.output.size());
        IntervalData firstPeriod = (IntervalData)appender.output.get(0);
        IntervalData secondPeriod = (IntervalData)appender.output.get(1);
        IntervalData thirdPeriod = (IntervalData)appender.output.get(2);
        
        assertEquals("Should have 1 active thread in first period", 1, firstPeriod.getMaxActiveThreadCount());
        assertEquals("Should have 1 hit in first period", 1, firstPeriod.getTotalHits());
        assertEquals("Should have 0 completions in first period", 0, firstPeriod.getTotalCompletions());
        
        assertEquals("Should have 1 active thread in second period", 1, secondPeriod.getMaxActiveThreadCount());
        assertEquals("Should have 0 hits in second period", 0, secondPeriod.getTotalHits());
        assertEquals("Should have 0 completions in second period", 0, secondPeriod.getTotalCompletions());
        
        assertEquals("Should have 1 active thread in third period", 1, thirdPeriod.getMaxActiveThreadCount());
        assertEquals("Should have 0 hits in second period", 0, thirdPeriod.getTotalHits());
        assertEquals("Should have 1 completions in second period", 1, thirdPeriod.getTotalCompletions());
    }
    
    
    
    
    /**
     * This tests a defect where adding child monitors did not work
     */
    public void testAddAppenderToChildrenOnly() throws Exception {

        final String MONITOR_NAME = "testAddAppenderToChildrenOnly.a";
        final String MONITOR_NAME_CHILD = MONITOR_NAME + ".b";
        final String MONITOR_NAME_GRANDCHILD = MONITOR_NAME_CHILD + ".c";
        
        final long INTERVAL_MILLIS = 100;
        AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
    	PerfMon.configure(builder
    		.defineMonitor(MONITOR_NAME, "/*")
    		.build(appenderID));

        PerfMonTimer timer = PerfMonTimer.start(MONITOR_NAME_GRANDCHILD);
        Thread.sleep(INTERVAL_MILLIS + 50);
        PerfMonTimer.stop(timer);
        
        TestAppender appender = (TestAppender)Appender.getOrCreateAppender(appenderID);
        appender.flush();
        assertEquals("Number of log events appended", 1, appender.output.size());

        IntervalData data = (IntervalData)appender.output.get(0);
        assertEquals("Should be associated with the child", MONITOR_NAME_CHILD, data.getOwner().getName());
    }    
    
/*----------------------------------------------------------------------------*/    
    public void testAppenderIsAssignedToChildren() throws Exception {
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String MONITOR_NAME = "testAppenderIsAssignedToChildren";
        PerfMon rootMon = PerfMon.getRootMonitor();

        PerfMon childMon = PerfMon.getMonitor(MONITOR_NAME);
    	PerfMon.configure(configBuilder
    		.defineRootMonitor("./**")
    		.build(appenderID));
        
        
        assertEquals("Appender is no longer assigned to root monitor", 0, rootMon.getNumAppenders());
        assertEquals("Root monitor should never have tasks associated with it", 0, rootMon.getNumPerfMonTasks());
         
        PerfMon grandChildMon = PerfMon.getMonitor(MONITOR_NAME + ".grandChild");
        assertEquals("Appender should be associated with child monitor", 1, childMon.getNumPerfMonTasks());
        assertEquals("Appender should be associated with grandchild monitor", 1, grandChildMon.getNumPerfMonTasks());

    	PerfMon.configure(configBuilder
        		.defineRootMonitor(".")
        		.build(appenderID));
        
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
    		result = (timer1.getEffectiveMonitorCategory() == timer2.getEffectiveMonitorCategory());
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_PARENT_ONLY = "./";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
//        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
    	
//        parent.addAppender(appenderID, PATTERN_PARENT_ONLY);
    	PerfMon.configure(builder
    		.defineMonitor(PARENT_KEY, PATTERN_PARENT_ONLY)
    		.build(appenderID));
        
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer (Should actually be the same timer as the parents)", 
        		isActiveTimer(CHILD_KEY) && timersMatch(PARENT_KEY, CHILD_KEY));
        assertTrue("grandChild is active timer (Should actually be the same timer as the parents)", 
        		isActiveTimer(GRAND_CHILD_KEY) && timersMatch(PARENT_KEY, GRAND_CHILD_KEY));
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersChildOnlyPattern() throws Exception {
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_CHILDREN_ONLY = "/*";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
//        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
//        parent.addAppender(appenderID, PATTERN_CHILDREN_ONLY);
        PerfMon.configure(
        	configBuilder
        		.defineMonitor(PARENT_KEY, PATTERN_CHILDREN_ONLY)
        		.build(appenderID)
        );
        
        assertFalse("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", 
        		isActiveTimer(CHILD_KEY));
        assertTrue("grandChild is active timer (Should actually be the same timer as its parents)", 
        		isActiveTimer(GRAND_CHILD_KEY) && timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersOnParentAndChildrenPattern() throws Exception {
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_PARENT_AND_CHILDREN = "./*";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_PARENT_AND_CHILDREN)
            		.build(appenderID)
        );
        
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY) && !timersMatch(PARENT_KEY, CHILD_KEY));
        assertFalse("grandChild is active timer ", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
        
        // Try it again... This time go through the code with the monitor already in place.
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_PARENT_AND_CHILDREN)
            		.build(appenderID)
        );
        
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY) && !timersMatch(PARENT_KEY, CHILD_KEY));
        assertFalse("grandChild is active timer ", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersNoParentAllDescendents() throws Exception {
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_DESCENDENTS = "/**";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
//        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
//        parent.addAppender(appenderID, PATTERN_DESCENDENTS);
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_DESCENDENTS)
            		.build(appenderID)
        );

        
        assertFalse("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY));
        assertTrue("grandChild should be active timer", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }

    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersParentAndAllDescendents() throws Exception {
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_ALL = "./**";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
//        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
//        
//        parent.addAppender(appenderID, PATTERN_ALL);
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_ALL)
            		.build(appenderID)
        );
        
        
        assertTrue("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", isActiveTimer(CHILD_KEY) && !timersMatch(PARENT_KEY, CHILD_KEY));
        assertTrue("grandChild should be active timer", isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(CHILD_KEY, GRAND_CHILD_KEY));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testActiveTimersForAllDescendents() throws Exception {
        final long INTERVAL_MILLIS = 999;
        Appender.AppenderID appenderID = TestAppender.getAppenderID(INTERVAL_MILLIS);
        
        final String PATTERN_ALL_DESCENDENTS = "/**";
        
        final String PARENT_KEY = "" + System.currentTimeMillis();
        final String CHILD_KEY = PARENT_KEY + ".b";
        final String GRAND_CHILD_KEY =  CHILD_KEY + ".c";
        
//        PerfMon parent = PerfMon.getMonitor(PARENT_KEY);
        
//        parent.addAppender(appenderID, PATTERN_ALL_DESCENDENTS);
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_ALL_DESCENDENTS)
            		.build(appenderID)
        );
        
        
        assertFalse("parent is active timer", isActiveTimer(PARENT_KEY));
        assertTrue("child is active timer", 
        		isActiveTimer(CHILD_KEY));
        assertTrue("grandChild is active timer", 
        		isActiveTimer(GRAND_CHILD_KEY) && !timersMatch(PARENT_KEY, GRAND_CHILD_KEY));
    }
    
    
/*----------------------------------------------------------------------------*/    
    public void testAppenderRespectsPattern() throws Exception {
        final long INTERVAL_MILLIS = 999;
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
        
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_PARENT_ONLY)
            		.build(appenderID)
        );
        
        assertEquals("parent appender count", 1, parent.getNumAppenders());
        assertTrue("parent.isActive", parent.isActive());
        assertEquals("parent should have a task associated with the appender", 1, parent.getNumPerfMonTasks());

        assertEquals("child appender count", 0, child.getNumAppenders());
 
        assertEquals("grandChild appender count", 0, grandChild.getNumAppenders());
        
        assertEquals("new child appender count", 0,  PerfMon.getMonitor("a.1").getNumAppenders());
        assertEquals("grandChild appender count", 0, PerfMon.getMonitor("a.1.1").getNumAppenders());
        
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_CHILDREN_ONLY)
            		.build(appenderID)
        );
        
        assertEquals("appender should have been removed from parent", 0, parent.getNumAppenders());
        assertFalse("parent.isActive", parent.isActive());
        
        assertEquals("parent should not have a task associated with the appender", 0, parent.getNumPerfMonTasks());
        assertEquals("child appender count", 1, child.getNumAppenders());
        assertTrue("child.isActive", child.isActive());
        
        assertEquals("grandChild appender count", 0, grandChild.getNumAppenders());

        assertEquals("new child appender count", 1,  PerfMon.getMonitor("a.2").getNumAppenders());
        assertEquals("grandChild appender count", 0, PerfMon.getMonitor("a.2.2").getNumAppenders());
        
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_ALL_DESCENDENTS)
            		.build(appenderID)
        );
        
        
        assertEquals("parent appender count", 0, parent.getNumAppenders());
        assertFalse("parent.isActive", parent.isActive());
        assertEquals("parent should not have a task associated with the appender", 0, parent.getNumPerfMonTasks());
        
        assertEquals("child appender count", 1, child.getNumAppenders());
        assertTrue("child.isActive", child.isActive());
        assertEquals("grandChild appender count", 1, grandChild.getNumAppenders());

        assertEquals("new child appender count", 1,  PerfMon.getMonitor("a.3").getNumAppenders());
        assertEquals("grandChild appender count", 1, PerfMon.getMonitor("a.3.3").getNumAppenders());
        assertTrue("grandChild.isActive", grandChild.isActive());
        
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(PARENT_KEY, PATTERN_PARENT_AND_ALL_DESCENDENTS)
            		.build(appenderID)
        );
        
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

    private PerfMon buildMockWithName(String name) {
    	PerfMon result = Mockito.mock(PerfMon.class);
    	Mockito.when(result.getName()).thenReturn(name);
    	
    	return result;
    }
    
/*----------------------------------------------------------------------------*/    
    public void testAppenderPatternParentChildConversion() throws Exception {
    	PerfMon parent = buildMockWithName("a");
    	PerfMon child = buildMockWithName("a.b");
    
    	
        final String PATTERN_PARENT_ONLY = "./";
        final String PATTERN_CHILDREN_ONLY = "/*";
        final String PATTERN_ALL_DESCENDENTS = "/**";
        final String PATTERN_PARENT_AND_ALL_DESCENDENTS = "./**";
        
        assertEquals("Parent only to child", "", PerfMon.parentToChildConversion(PATTERN_PARENT_ONLY, parent, child));
        assertEquals("children only to child", PATTERN_PARENT_ONLY, PerfMon.parentToChildConversion(PATTERN_CHILDREN_ONLY, parent, child));
        assertEquals("all descendents to child", PATTERN_PARENT_AND_ALL_DESCENDENTS, PerfMon.parentToChildConversion(PATTERN_ALL_DESCENDENTS, parent, child));
        assertEquals("parent and all descendents to child", PATTERN_PARENT_AND_ALL_DESCENDENTS, PerfMon.parentToChildConversion(PATTERN_PARENT_AND_ALL_DESCENDENTS, parent, child));
    }
    
/*----------------------------------------------------------------------------*/    
    public void testWildcardPatternSingleLevel() throws Exception {
        final String PARENT_PATTERN = "/abc#";
        
        PerfMon parent = buildMockWithName("a"); 
        PerfMon matchingChild = buildMockWithName("a.abcd"); 
        PerfMon nonMatchingChild = buildMockWithName("a.Abcd"); 

        assertEquals("Matching child",  PerfMon.APPENDER_PATTERN_PARENT_ONLY, PerfMon.parentToChildConversion(PARENT_PATTERN, parent, matchingChild));
        assertEquals("Non Matching child", PerfMon.APPENDER_PATTERN_NA, PerfMon.parentToChildConversion(PARENT_PATTERN, parent, nonMatchingChild));
    }


/*----------------------------------------------------------------------------*/    
    public void testWildcardPatternMultiLevel() throws Exception {
        final String PARENT_PATTERN = "/abc#.#.Xyz*";
        
        PerfMon parent = buildMockWithName("a"); 
        PerfMon matchingChild = buildMockWithName("a.abcd"); 

        assertEquals("Potentially matches grandchildren", "/#.Xyz*", 
        		PerfMon.parentToChildConversion(PARENT_PATTERN, parent, matchingChild));
    }
    
/*----------------------------------------------------------------------------*/    
    public void testMonitorResetsWhenMadeInactive() throws Exception {
        final String MON_NAME = "base";
        PerfMon mon = PerfMon.getMonitor(MON_NAME);
        
        assertFalse("Monitor is not active", mon.isActive());
//        mon.addAppender(bogusAppenderID);
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(MON_NAME, "./*")
            		.build(bogusAppenderID)
        );
        
        assertTrue("Monitor is active", mon.isActive());

        PerfMonTimer timer = PerfMonTimer.start(MON_NAME);
        Thread.sleep(100);
        PerfMonTimer.stop(timer);
        
        // Keep 1 active timer...  
        timer = PerfMonTimer.start(MON_NAME);
        Thread.sleep(100);
        
        
        // Make inactive and verify that the counters are reset...
//        mon.removeAppender(bogusAppenderID);
        PerfMon.configure(
            	configBuilder
            		.clearMonitors()
            		.build(bogusAppenderID)
        );
        
        
        
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
//        mon.addAppender(bogusAppenderID);
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(MON_NAME, "./*")
            		.build(bogusAppenderID)
        );
        
        
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
        assertTrue("Monitor should have threadTraceConfig", traceConfig == mon.getInternalThreadTraceConfig());
        
        config = new PerfMonConfiguration();
        PerfMon.configure(config);
        
        mon = PerfMon.getMonitor(MONITOR_KEY);
        assertNull("threadTraceConfig should have been removed", mon.getInternalThreadTraceConfig());
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

    public void testConfigureThresholdCalculatorOnMonitor() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("thresholdCalculator", "1 second, 2 seconds, 5 seconds");
        config.defineMonitor("efg");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");
        config.attachAppenderToMonitor("efg", "APP1");

        PerfMon.configure(config);
        
        assertNotNull("abc should have a threshold monitor", PerfMon.getMonitor("abc").getThresholdCalculator());
        assertNull("efg should NOT have a threshold monitor", PerfMon.getMonitor("efg").getThresholdCalculator());
    }

    public void testThresholdCalculatorCascadesToChildren() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("thresholdCalculator", "1 second, 2 seconds, 5 seconds");
        config.defineMonitor("abc.efg");
        config.defineMonitor("abc.efg.hjk");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        assertNotNull("abc should have a threshold monitor", PerfMon.getMonitor("abc").getThresholdCalculator());
        assertNotNull("Child abc.efg should also have a threshold monitor", PerfMon.getMonitor("abc.efg").getThresholdCalculator());
        assertNotNull("Grandchild abc.efg.hjk should also have a threshold monitor", PerfMon.getMonitor("abc.efg.hjk").getThresholdCalculator());
        
        // Now let's make sure an updated configuration will remove the threshold monitors added above.
        config = new PerfMonConfiguration();
        config.defineMonitor("abc"); // Removed threshold monitor from parent.
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");
    
        PerfMon.configure(config);
        
        assertNull("abc should NOT have a threshold monitor", PerfMon.getMonitor("abc").getThresholdCalculator());
        assertNull("Child abc.efg should also NOT have a threshold monitor", PerfMon.getMonitor("abc.efg").getThresholdCalculator());
        assertNull("Grandchild abc.efg.hjk should also NOT have a threshold monitor", PerfMon.getMonitor("abc.efg.hjk").getThresholdCalculator());
    }

    public void testThresholdCalculatorCascadesToDynamicChildren() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("thresholdCalculator", "1 second, 2 seconds, 5 seconds");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        // Create a new child monitor on the fly.
        PerfMon childMonitor = PerfMon.getMonitor("abc.trs");
        assertNotNull("Dynamically created child should also have a threshold monitor", childMonitor.getThresholdCalculator());
    }

    public void testThresholdCalculatorCascadesToDynamicGrandchildren() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("thresholdCalculator", "1 second, 2 seconds, 5 seconds");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        // Create a new child monitor on the fly.
        PerfMon grandchildMonitor = PerfMon.getMonitor("abc.qrs.mno");
        assertNotNull("Dynamically created grandchild should also have a threshold monitor", grandchildMonitor.getThresholdCalculator());
        
        // Check to see that it is removed from grandchild
        config = new PerfMonConfiguration();
        config.defineMonitor("abc"); // Removed threshold from monitor abc
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        assertNull("Dynamically created grandchild should also have a active thread monitor removed", grandchildMonitor.getThresholdCalculator());
    }

    public void testThresholdCalculatorIsRemovedIfMissing() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc"); // Removed threshold from monitor abc
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        assertNull("abc should NOT have a threshold monitor", PerfMon.getMonitor("abc").getThresholdCalculator());
        assertNull("Child abc.efg should NOT also have a threshold monitor", PerfMon.getMonitor("abc.efg").getThresholdCalculator());
        assertNull("Grandchild abc.efg.hjk NOT should also have a threshold monitor", PerfMon.getMonitor("abc.efg.hjk").getThresholdCalculator());
    }
    
    public void testActiveThreadMonitorOnMonitor() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("activeThreadMonitor", "10 minutes, 30 minutes, 1 hour");
        config.defineMonitor("efg");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");
        config.attachAppenderToMonitor("efg", "APP1");

        PerfMon.configure(config);
        
        assertNotNull("abc should have an active thread monitor", PerfMon.getMonitor("abc").getActiveThreadMonitor());
        assertNull("efg should NOT have a threshold monitor", PerfMon.getMonitor("efg").getActiveThreadMonitor());
    }

    public void testActiveThreadMonitorCascadesToChildren() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("activeThreadMonitor", "10 minutes, 30 minutes, 1 hour");
        config.defineMonitor("abc.efg");
        config.defineMonitor("abc.efg.hjk");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        assertNotNull("abc should have a threshold monitor", PerfMon.getMonitor("abc").getActiveThreadMonitor());
        assertNotNull("Child abc.efg should also have a threshold monitor", PerfMon.getMonitor("abc.efg").getActiveThreadMonitor());
        assertNotNull("Grandchild abc.efg.hjk should also have a threshold monitor", PerfMon.getMonitor("abc.efg.hjk").getActiveThreadMonitor());
        
        // Now let's make sure an updated configuration will remove the threshold monitors added above.
        config = new PerfMonConfiguration();
        config.defineMonitor("abc"); // Removed threshold monitor from parent.
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");
    
        PerfMon.configure(config);
        
        assertNull("abc should NOT have a threshold monitor", PerfMon.getMonitor("abc").getActiveThreadMonitor());
        assertNull("Child abc.efg should also NOT have a threshold monitor", PerfMon.getMonitor("abc.efg").getActiveThreadMonitor());
        assertNull("Grandchild abc.efg.hjk should also NOT have a threshold monitor", PerfMon.getMonitor("abc.efg.hjk").getActiveThreadMonitor());
    }

    public void testActiveThreadMonitorCascadesToDynamicChildren() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("activeThreadMonitor", "10 minutes, 30 minutes, 1 hour");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        // Create a new child monitor on the fly.
        PerfMon childMonitor = PerfMon.getMonitor("abc.trs");
        assertNotNull("Dynamically created child should also have a threshold monitor", childMonitor.getActiveThreadMonitor());
    }

    public void testActiveThreadMonitorCascadesToDynamicGrandchildren() throws Exception {
        PerfMon.deInit();
        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("abc").setProperty("activeThreadMonitor", "10 minutes, 30 minutes, 1 hour");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");

        PerfMon.configure(config);
        
        // Create a new child monitor on the fly.
        PerfMon grandchildMonitor = PerfMon.getMonitor("abc.qrs.mno");
        assertNotNull("Dynamically created grandchild should also have a threshold monitor", grandchildMonitor.getActiveThreadMonitor());
        
        // Now reconfigure without the active thread monitor and ensure it is removed from grand child
        config = new PerfMonConfiguration(); 
        config.defineMonitor("abc"); // Removed .setProperty("activeThreadMonitor", "10 minutes, 30 minutes, 1 hour");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("abc", "APP1");
        
        PerfMon.configure(config);
        assertNull("Dynamically created grandchild should also have a threshold monitor removed", grandchildMonitor.getActiveThreadMonitor());
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
//        PerfMon.getMonitor(DESCENDENT_1).addAppender(TestAppender.getAppenderID(1000),
//            PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(DESCENDENT_1, PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS)
            		.build(TestAppender.getAppenderID(1000))
        );

        
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
        
        PerfMon.configure(
            	configBuilder
            		.defineMonitor(DESCENDENT_1, "./*")
            		.build(TestAppender.getAppenderID(1000), TestAppender.getAppenderID(5000))
        );
        
        
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
//        System.out.println(data.toAppenderString());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testRefreshConfig() throws Exception {
        final String MONITOR_ROOT = PerfMon.ROOT_MONITOR_NAME;
        final String DESCENDENT_1 = "aa.b.c";
        final String DESCENDENT_2 = "aa.b.c.d";

        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("1000", TestAppender.getAppenderID(1000));
        config.defineAppender("5000", TestAppender.getAppenderID(5000));
        
        config.defineMonitor(MONITOR_ROOT);
        config.defineMonitor(DESCENDENT_1);
        config.attachAppenderToMonitor(MONITOR_ROOT, "1000", "./**");
        config.attachAppenderToMonitor(DESCENDENT_1, "5000", "./**");
               
        PerfMon.configure(config);
        
        assertEquals("Number of appenders", 2, PerfMon.getMonitor(DESCENDENT_1).getNumAppenders());
        
        config = new PerfMonConfiguration();
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
        
        Appender.AppenderID id = TestAppender.getAppenderID(1000);
//        perfMon.addAppender(id);
        PerfMon.configure(
        		configBuilder
        			.defineMonitor(MONITOR_NAME, "./**")
        			.build(id)
        			
        );
        
        
        assertEquals("Number of perfmon data elements",start + 1, perfMon.getNumPerfMonTasks());
        
//        perfMon.removeAppender(id);
        PerfMon.configure(
        		configBuilder
        			.clearMonitors()
        			.build()
        			
        );
        
        
        assertEquals("Number of perfmon data elements",start, perfMon.getNumPerfMonTasks());
    }
    
/*----------------------------------------------------------------------------*/
    static public class TestAppender extends Appender {
        private List<PerfMonData> output = new Vector<PerfMonData>();

        public static AppenderID getAppenderID(long intervalMillis) {
        	return AppenderID.getAppenderID(TestAppender.class.getName(), intervalMillis);
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
            
            void stop(long duration, long durationSquared, long systemTime, long sqlDuration, long sqlDurationSquared) {
                BogusAppender.dataStopCount++;
                super.stop(duration, durationSquared, systemTime, sqlDuration, sqlDurationSquared);
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public void testDuration() throws Exception {
        PerfMon.configure(
            	configBuilder
            		.defineRootMonitor("./**")
            		.build(bogusAppenderID)
        );
        
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
//            mon.addAppender(TestAppender.getAppenderID(1000));
            TestConfigBuilder builder = new TestConfigBuilder();
            
            
            PerfMon.configure(
                	builder
                		.defineMonitor("simple.test.a", "./*")
                		.build(TestAppender.getAppenderID(1000))
            );
        }
        
        if (useMon) {
            timer = PerfMonTimer.start("simple.test.a." + (x/10));
        }
        
        try {
            int val = (int)(Math.random() * 100) + 1;
            @SuppressWarnings("unused")
			double result = Math.sqrt(val);
//            System.err.println("The square root of " + val + "=" + result);
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
    
    public void testGetSimpleName() throws Exception {
    	PerfMon mon = PerfMon.getMonitor("this.is.the.SimpleName");
    	assertEquals("getSimpleName()", "SimpleName", mon.getSimpleName());
    	
    	mon = PerfMon.getRootMonitor();
    	assertEquals("rootMonitor.getSimpleName()", "<ROOT>", mon.getSimpleName());
    }

    
    public void testConfigureWithEnhancedAppenderPattern() throws Exception {
        final String MATCH = "aa.abcde.x";
        final String PARENT_OF_MATCH = "aa.abcde"; 
        final String NO_MATCH_A = "aa.abcde.y";
        final String NO_MATCH_B = "aa.Abcde.x";
        final String enhancedPattern = "/abc#*/x"; // Can use either '/' to separate patterns or '.' 

        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("aa");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("aa", "APP1", enhancedPattern);

        PerfMon.configure(config);
        
        assertEquals("should be attached to appender", 1, PerfMon.getMonitor(MATCH).getNumAppenders());
        assertEquals("The parent of the match should NOT be attached to appender", 0, PerfMon.getMonitor(PARENT_OF_MATCH).getNumAppenders());
        
        
        assertEquals("should NOT be attached to appender", 0, PerfMon.getMonitor(NO_MATCH_A).getNumAppenders());
        assertEquals("should NOT be attached to appender", 0, PerfMon.getMonitor(NO_MATCH_B).getNumAppenders());
    }    
    
    public void testEnhancedAppenderPatternWithDynamicCreate() throws Exception {
        final String MATCH = "aa.abcdefg.x.y.z";
        final String NOMATCH = "aa.abcdefg";
        
        final String enhancedPattern = "/abc#*/x/y/z"; // Can use either '/' to separate patterns or '.' 

        
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor("aa");
        config.defineAppender("APP1", TestAppender.class.getName(), "1 second");
        config.attachAppenderToMonitor("aa", "APP1", enhancedPattern);

        PerfMon.configure(config);
        
        PerfMon mon = PerfMon.getMonitor(MATCH);
        assertEquals("should have an appender", 1, mon.getNumAppenders());

        mon = PerfMon.getMonitor(NOMATCH);
        assertEquals("should have an appender", 0, mon.getNumAppenders());
    }
    
    
    public void testRoundInterval() {
    	// For intervals divisible by 1 minute we round to the nearest minute.
    	assertEquals("Our starting is 30 seconds with 1 minute interval, should round up "
    			+ "to 2 minutes", 
    			90000, PerfMon.roundInterval(30000, 60000));
    	
    	// For intervals divisible by 1 minute we round to the nearest minute.
    	assertEquals("Our starting is 29.999 seconds with 1 minute interval, should round down "
    			+ "to 1 minute", 
    			30001, PerfMon.roundInterval(29999, 60000));

    	assertEquals("For intervals evenly divisible by one second we round to nearest second", 
    			999, PerfMon.roundInterval(1, 1000));
    	
    	assertEquals("For intervals evenly divisible by two second we round to nearest second", 
    			1999, PerfMon.roundInterval(1, 2000));
    
    	assertEquals("For intervals not evenly divisible by one second we don't do any rounding", 
    			500, PerfMon.roundInterval(1, 500));
    	
    	assertEquals("For intervals not evenly divisible by one second we don't do any rounding", 
    			1500, PerfMon.roundInterval(1, 1500));
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
//        newSuite.addTest(new PerfMonTest("testMonitorResetsWhenMadeInactive"));
//        newSuite.addTest(new PerfMonTest("testGetDynamicOnChildOfMonitor"));
//        newSuite.addTest(new PerfMonTest("testGetDynamicOnGRANDChildOfMonitor"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonTest.class);
        }

        return( newSuite);
    }
}

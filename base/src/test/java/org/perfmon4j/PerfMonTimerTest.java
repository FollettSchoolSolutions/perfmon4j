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


import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.BasicConfigurator;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

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
        PerfMon.deInitAndCleanMonitors_TESTONLY();
        super.tearDown();
    }
    
/*----------------------------------------------------------------------------*/
    public void testInactiveMonitorsByPassed() throws Exception {
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor("a", "./*")
    		.build(TestAppender.getAppenderID()));

    	
        PerfMonTimer timer = PerfMonTimer.start("a.b");
        assertEquals("Should have timer", "a.b", timer.perfMon.getName());
        PerfMonTimer.stop(timer);
        
        // Remove appender from "a.b"
    	PerfMon.configure(builder.defineMonitor("a", "./")
        		.build(TestAppender.getAppenderID()));
        
        timer = PerfMonTimer.start("a.b");
        assertEquals("Should bypass inactive timer a.b", "a", timer.perfMon.getName());
        PerfMonTimer.stop(timer);
        
        // If the only active timer is the root monitor, we should just
        // get the null timer...
    	PerfMon.configure(builder
    			.clearMonitors()
    			.defineMonitor("d", ".")
        		.build(TestAppender.getAppenderID()));
      
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
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor("a", ".")
    		.build(TestAppender.getAppenderID()));
        
        timer = PerfMonTimer.start("a.b.c");
        assertEquals("Should now get my grandparent's timer", "a", timer.perfMon.getName());
        PerfMonTimer.stop(timer);
    }    
    
    /*----------------------------------------------------------------------------*/
    public void testLazyCreateOnDynamicTimer() throws Exception {
    	final String dynamicMonitorName = "a.testLazyCreateOnDynamicTimer.child.grandchild";

    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor("a", ".")
    		.build(TestAppender.getAppenderID()));
    	
    	int numAtStart = PerfMon.getMonitorKeys().size();
    	
    	PerfMonTimer timer = PerfMonTimer.start(dynamicMonitorName, true);
    	PerfMonTimer.stop(timer);
    	
    	assertEquals("Should not have created monitors", numAtStart, PerfMon.getMonitorKeys().size());
    }

    /*----------------------------------------------------------------------------*/
    public void testLazyCreateOnDynamicTimerOneLevel() throws Exception {
    	final String dynamicMonitorName = "a.testLazyCreateOnDynamicTimerOneLevel.child.grandchild";
    	
    	// The appender pattern "/*" indicates to monitor each child of
    	// the root monitor.  In this case it indicates we should monitor 
    	// testLazyCreateOnDynamicTimerOneLevel, but not its child or
    	// grandchild monitors.
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor("a", "/*")
    		.build(TestAppender.getAppenderID()));
   
    	int numAtStart = PerfMon.getMonitorKeys().size();
    	
    	PerfMonTimer timer = PerfMonTimer.start(dynamicMonitorName, true);
    	PerfMonTimer.stop(timer);
    	
    	assertEquals("One level should have been created", numAtStart + 1, PerfMon.getMonitorKeys().size());
    }

    /*----------------------------------------------------------------------------*/
    public void testLazyCreateOnDynamicTimerAllLevels() throws Exception {
    	final String dynamicMonitorName = "a.testLazyCreateOnDynamicTimerAllLevels.child.grandchild.greatgrandchild";

    	// The appender pattern "/**" indicates to monitor all descendents
    	// of the root monitor.  
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor("a", "/**")
    		.build(TestAppender.getAppenderID()));
   
    	int numAtStart = PerfMon.getMonitorKeys().size();
    	
    	PerfMonTimer timer = PerfMonTimer.start(dynamicMonitorName, true);
    	PerfMonTimer.stop(timer);
    	
    	assertEquals("One level should have been created", numAtStart + 4, PerfMon.getMonitorKeys().size());
    }
   
    /*----------------------------------------------------------------------------*/
    public void testEagerCreateOnDefaultTimer() throws Exception {
    	final String monitorName = "testEagerCreateOnDefaultTimer." + new Random().nextInt();
//    	PerfMon.getRootMonitor().addAppender(TestAppender.getAppenderID());
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineRootMonitor("/**")
    		.build(TestAppender.getAppenderID()));
    
    	int numAtStart = PerfMon.getMonitorKeys().size();
    	
    	PerfMonTimer timer = PerfMonTimer.start(monitorName, false);
    	PerfMonTimer.stop(timer);
    	
    	assertEquals("Monitors should have been created immediately", numAtStart + 2, PerfMon.getMonitorKeys().size());
    }

    
    public void testLazyCreateOnDynamicTimerWithEnhancedPattern() throws Exception {
    	final String dynamicMonitorName = "testLazyCreateOnDynamicTimerWithEnhancedPattern.b.c.d.e.f";
    	final String parentOfDynamicMonitorName = dynamicMonitorName.substring(0, dynamicMonitorName.length()-2);

    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor("testLazyCreateOnDynamicTimerWithEnhancedPattern", "/b.c.d.e.f")
    		.build(TestAppender.getAppenderID()));
   
    	int numAtStart = PerfMon.getMonitorKeys().size();

    	PerfMonTimer timer = PerfMonTimer.start(parentOfDynamicMonitorName, true);
    	PerfMonTimer.stop(timer);
    	
    	assertEquals("We didn't get a match, so no new monitors should have been added", 
    		numAtStart, PerfMon.getMonitorKeys().size());

    	
    	timer = PerfMonTimer.start(dynamicMonitorName, true);
    	PerfMonTimer.stop(timer);
    	
    	assertEquals("Since we found a match, all ancestors of the match should have been created", 
    		numAtStart + 5, PerfMon.getMonitorKeys().size());
    }
    
    public void testStartStopTimerAcrossThreads() throws Exception {
    	String monitorName = "testStartStopTimerAcrossThreads";
    	String childMonitorName = monitorName + ".child";
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor(monitorName, ".")
    		.build(TestAppender.getAppenderID()));

    	CountDownLatch latch = new CountDownLatch(1);
    	PerfMonTimer timer = PerfMonTimer.start(monitorName);

    	PerfMonTimer timerChild = PerfMonTimer.start(childMonitorName);
		PerfMonTimer.stop(timerChild);
    	
    	(new Thread(() -> {
    		PerfMonTimer.stop(timer);
    		latch.countDown();
    	})).start();
    	
    	// Wait for thread to run.
    	latch.await();
    	
    	PerfMon mon = PerfMon.getMonitor(monitorName);
    	assertEquals("Should have stopped monitor", 0, mon.getActiveThreadCount());
    	assertEquals("Should have 1 completion", 1, mon.getTotalCompletions());
    }

    public void testStartStopChildRespectsParentAcrossThreads() throws Exception {
    	String monitorName = "testStartStopChildRespectsParentAcrossThreads";
    	String childMonitorName = monitorName + ".child";
    	
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor(monitorName, "./*")
    		.build(TestAppender.getAppenderID()));

    	CountDownLatch latch = new CountDownLatch(1);

    	PerfMonTimer timerChild = PerfMonTimer.start(childMonitorName);
    	
    	(new Thread(() -> {
    		PerfMonTimer.stop(timerChild);
    		latch.countDown();
    	})).start();
    	
    	// Wait for thread to run.
    	latch.await();
    	
    	PerfMon mon = PerfMon.getMonitor(childMonitorName);
    	assertEquals("Should have 1 child hit", 1, mon.getTotalHits());
    	assertEquals("Should have 1 child completion", 1, mon.getTotalCompletions());
    	assertEquals("Should have stopped child monitor", 0, mon.getActiveThreadCount());

    	mon = PerfMon.getMonitor(monitorName);
    	assertEquals("Should have 1 parent hit", 1, mon.getTotalHits());
    	assertEquals("Should have 1 parent completion", 1, mon.getTotalCompletions());
    	assertEquals("Should have stopped parent monitor", 0, mon.getActiveThreadCount());
    }
    
    public void testMultipeStopsOnTimer() throws Exception {
    	String monitorName = "testMultipeStopsOnTimer";
    	TestConfigBuilder builder = new TestConfigBuilder();
    	PerfMon.configure(builder.defineMonitor(monitorName, ".")
    		.build(TestAppender.getAppenderID()));

    	PerfMonTimer timer = PerfMonTimer.start(monitorName);
    	
    	// Call Stop 2 times
    	PerfMonTimer.stop(timer);
    	PerfMonTimer.stop(timer);
    	
    	timer = PerfMonTimer.start(monitorName);
    	PerfMonTimer.stop(timer);

    	PerfMon mon = PerfMon.getMonitor(monitorName);
    	assertEquals("Expected number of completions", 2, mon.getTotalCompletions());
    }

    public void testMultipeStopsOnChildTimer() throws Exception {
    	String parentMonitorName = "testMultipeStopsOnChildTimer";
    	String childMonitorName = "testMultipeStopsOnChildTimer.child";
    	TestConfigBuilder builder = new TestConfigBuilder();

    	PerfMon.configure(builder.defineMonitor(parentMonitorName, "./*")
    		.build(TestAppender.getAppenderID()));

    	// Starting the child will also start the parent..
    	PerfMonTimer timer = PerfMonTimer.start(childMonitorName);
    	
    	// Stop the child twice. Make sure stop is only called once 
    	// on both the child and the parent.
    	PerfMonTimer.stop(timer);
    	PerfMonTimer.stop(timer);
    	
    	timer = PerfMonTimer.start(childMonitorName);
    	PerfMonTimer.stop(timer);

    	PerfMon mon = PerfMon.getMonitor(childMonitorName);
    	assertEquals("Expected number of completions on child", 2, mon.getTotalCompletions());
    	assertEquals("Expected number of activeThreads on child", 0, mon.getActiveThreadCount());
    	
    	mon = PerfMon.getMonitor(parentMonitorName);
    	assertEquals("Expected number of completions on parent", 2, mon.getTotalCompletions());
    	assertEquals("Expected number of activeThreads on parent", 0, mon.getActiveThreadCount());
    
    	// As one final check to make sure the parent's referenceCount did not get
    	// out of sync, we'll start and stop the parent directly.
    	
    	timer = PerfMonTimer.start(parentMonitorName);
    	PerfMonTimer.stop(timer);
    
    	mon = PerfMon.getMonitor(parentMonitorName);
    	assertEquals("Expected number of completions on parent", 3, mon.getTotalCompletions());
    	assertEquals("Expected number of activeThreads on parent", 0, mon.getActiveThreadCount());
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

    public void testStopAbortNullTimer() throws Exception {
    	try {
    		PerfMonTimer.stop(null);
    	} catch (NullPointerException npe) {
    		fail("Should be able to stop a null timer!");
    	}
    	try {
    		PerfMonTimer.abort(null);
    	} catch (NullPointerException npe) {
    		fail("Should be able to abort a null timer!");
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

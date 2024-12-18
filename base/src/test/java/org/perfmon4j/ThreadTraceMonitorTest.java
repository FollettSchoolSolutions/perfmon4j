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
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.reactive.ReactiveContextManager;
import org.perfmon4j.util.MiscHelper;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class ThreadTraceMonitorTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public ThreadTraceMonitorTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        PerfMon.configure();
        TestAppender.clearResult();
        TestAppender.getLastResult();
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInit();
        System.gc();
        super.tearDown();
    }


    private void doStartStopMonitor(PerfMon mon) {
        PerfMonTimer timer = null;
        try {
            timer = PerfMonTimer.start(mon);
        } finally {
            PerfMonTimer.stop(timer);
        }
    }

    private void doStartStopMonitorOnNewThread(PerfMon mon, String reactiveContextID) throws InterruptedException {
    	CountDownLatch latch = new CountDownLatch(1);
    	
    	(new Thread(() -> {
    		if (reactiveContextID != null) {
    			ReactiveContextManager.getContextManagerForThread().moveContext(reactiveContextID);
    		}
    		doStartStopMonitor(mon);
    		latch.countDown();
    	})).start();
    	
    	latch.await();
    	
		if (reactiveContextID != null) {
			ReactiveContextManager.getContextManagerForThread().moveContext(reactiveContextID);
		}
    }

    
    
    public void testSamplingBasedThreadName() throws Exception {
        final String MONITOR_KEY = "testThreadBasedTrigger";
        final Thread currentThread = Thread.currentThread();
        final String originalThreadName = currentThread.getName();
        final String TARGET_THREAD_NAME = originalThreadName + "X";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        ThreadTraceConfig.Trigger trigger = new ThreadTraceConfig.ThreadNameTrigger(TARGET_THREAD_NAME);
        config.setTriggers(new ThreadTraceConfig.Trigger[]{trigger});
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);
        try {
        	// First try where the thread name DOES not
        	// contain matching thread name... It should not 
        	// include a thread stack.
        	doStartStopMonitor(mon);
            int outputCount = TestAppender.getOutputCount();
            assertEquals("Thread name does NOT match should NOT do a trace", 
            		0, outputCount);
            
            // Now change our thread to match the monitor and retry
            currentThread.setName(TARGET_THREAD_NAME);
        	doStartStopMonitor(mon);
            outputCount = TestAppender.getOutputCount();
            assertEquals("Thread name does match should do a trace", 
            		1, outputCount);
        } finally {
        	currentThread.setName(originalThreadName);
        }
    }

    
    public void testSamplingBasedOnThreadProperty() throws Exception {
        final String MONITOR_KEY = "testThreadPropertyTrigger";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        ThreadTraceConfig.Trigger trigger = new ThreadTraceConfig.ThreadPropertytTrigger("jobID", "156");
        config.setTriggers(new ThreadTraceConfig.Trigger[]{trigger});
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);

    	// First try where the thread name DOES not
    	// contain matching thread property... It should not 
    	// include a thread stack.
    	doStartStopMonitor(mon);
        int outputCount = TestAppender.getOutputCount();
        assertEquals("Thread property does NOT exist should NOT do a trace", 
        		0, outputCount);
        try {
            // Now push on the thread property and retry....
        	ThreadTraceConfig.pushThreadProperty("jobID", "156");
        	
        	doStartStopMonitor(mon);
            outputCount = TestAppender.getOutputCount();
            assertEquals("Property matches should do a trace", 
            		1, outputCount);
        } finally {
        	ThreadTraceConfig.popThreadProperty();
        }
    }
    
    public void testSamplingBasedOnThreadProperty_OnReactiveContext() throws Exception {
        final String MONITOR_KEY = "testSamplingBasedOnThreadProperty_OnReactiveContext";
        final String OUTER_KEY = "OUTER" + MONITOR_KEY;

        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineMonitor(OUTER_KEY);
        config.defineAppender("appender", TestAppender.getAppenderID());
        config.attachAppenderToMonitor(OUTER_KEY, "appender");
        
        ThreadTraceConfig ttConfig = new ThreadTraceConfig();
        ttConfig.addAppender(TestAppender.getAppenderID());
        ThreadTraceConfig.Trigger trigger = new ThreadTraceConfig.ThreadPropertytTrigger("jobID", "156");
        ttConfig.setTriggers(new ThreadTraceConfig.Trigger[]{trigger});
        config.addThreadTraceConfig(MONITOR_KEY, ttConfig);
        
        PerfMon.configure(config);

        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        
        final String reactiveContextID = MiscHelper.generateOauthKey(); // Create a random context.
        PerfMonTimer outerWithReactiveContext = PerfMonTimer.start(OUTER_KEY, false, reactiveContextID);
        try {
        	// Push property on the current context.
        	ThreadTraceConfig.pushThreadProperty("jobID", "156", reactiveContextID);
        	
        	// Run once on this thread... it should count.
        	doStartStopMonitor(mon);
            int outputCount = TestAppender.getOutputCount();
            assertEquals("Thread is associated with reactiveContextID - should trigger a trace", 
            		1, outputCount);
            TestAppender.clearResult();
        	
        	// Run monitor once on a thread that has NOT been assigned to the reactive context.
        	// This should not trigger a thread trace.
        	doStartStopMonitorOnNewThread(mon, null);
            outputCount = TestAppender.getOutputCount();
            assertEquals("Thread is not associated with reactiveContextID - should not trigger a trace", 
            		0, outputCount);
            TestAppender.clearResult();
            
            // Now run monitor on a thread that HAS been assigned to the ractive context. 
        	// This should not trigger a thread.
        	doStartStopMonitorOnNewThread(mon, reactiveContextID);
            outputCount = TestAppender.getOutputCount();
            assertEquals("Thread IS associated with reactiveContextID - should trigger a trace", 
            		1, outputCount);
            TestAppender.clearResult();
        } finally {
        	ThreadTraceConfig.popThreadProperty(reactiveContextID);
        	PerfMonTimer.stop(outerWithReactiveContext);
        }
    }
    
    public void testRandomSampling() throws Exception {
        final String MONITOR_KEY = "testRandomSampling";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        config.setRandomSamplingFactor(10);  // Random sample at an approximate rate of 1 out of 10...
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);
        
        for (int x = 0; x < 500; x++) {
            PerfMonTimer timer = null;
            try {
                timer = PerfMonTimer.start(mon);
                
                PerfMonTimer inner = PerfMonTimer.start("SimpleTest");
                PerfMonTimer.stop(inner);
            } finally {
                PerfMonTimer.stop(timer);
            }
        }
        
        // This is random....  We should come close to sampling 1 out of 10 and in this case 50.
        // We will consider it close enough if we get between 25 and 75..
        int outputCount = TestAppender.getOutputCount();
System.out.println("outputCount: " + outputCount);        
        
        String message = "Expected between 20 and 80 samples but was: " + outputCount;
        assertTrue(message, outputCount >= 20 && outputCount <= 80);
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testSimple() throws Exception {
        final String MONITOR_KEY = "testSimple";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);
        
        PerfMonTimer timer = null;
        try {
            timer = PerfMonTimer.start(mon);
            
            PerfMonTimer inner = PerfMonTimer.start("SimpleTest");
            PerfMonTimer.stop(inner);
        } finally {
            PerfMonTimer.stop(timer);
        }

        ThreadTraceData trace = TestAppender.getLastResult();
        assertNotNull("Should have a tracing", trace);
        assertEquals("Root trace name", MONITOR_KEY, trace.getName());
        assertNotNull("Should have a child array", trace.getChildren());
        assertEquals("Should have 1 child", 1, trace.getChildren().length);
        assertEquals("Child should have the same name as the timer", "SimpleTest", trace.getChildren()[0].getName());
    }
    
/*----------------------------------------------------------------------------*/
    private void assertTimesClose(String info, long expectedTime, long actualTime) {
        long expectedHigh = expectedTime + 20;
        
        assertTrue(info + ": expected time between " + expectedTime + " and " + expectedHigh + " but was " + actualTime,
            actualTime >= expectedTime && actualTime <= expectedHigh);
    }
    
/*----------------------------------------------------------------------------*/
    public void testStartEndTimeOnTraceStartStop() throws Exception {
        final String MONITOR_KEY = "testStartEndTimeOnTraceStartStop";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);

        long startTime = 0;
        long endTime = 0;
        PerfMonTimer timer = null;
        try {
            startTime = System.currentTimeMillis();
            timer = PerfMonTimer.start(mon);
            
            Thread.currentThread().sleep(100);
        } finally {
            endTime = System.currentTimeMillis();
            PerfMonTimer.stop(timer);
        }
        ThreadTraceData trace = TestAppender.getLastResult();
    
        assertNotNull(trace);
        assertTimesClose("startTime", startTime, trace.getStartTime());
        assertTimesClose("endTime", endTime, trace.getEndTime());
    }
    
/*----------------------------------------------------------------------------*/
    public void testNestedMonitorStartIsIgnored() throws Exception {
        final String MONITOR_KEY = "testNestedMonitorStartIsIgnored";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);
        
        long startTime = 0;
        long endTime = 0;
        PerfMonTimer timer = null;
        try {
            startTime = System.currentTimeMillis();
            timer = PerfMonTimer.start(mon);
            Thread.sleep(100);
            PerfMonTimer nested = null;
            try {
                nested = PerfMonTimer.start(MONITOR_KEY);
                Thread.sleep(100);
            } finally {
                PerfMonTimer.stop(nested);
            }
            Thread.sleep(100);
        } finally {
            endTime = System.currentTimeMillis();
            PerfMonTimer.stop(timer);
        }
        
        
        // Nested Attach/Detach
        ThreadTraceData trace = TestAppender.getLastResult();
        assertNotNull(trace);
        assertTimesClose("startTime", startTime, trace.getStartTime());
        assertTimesClose("endTime", endTime, trace.getEndTime());    
    }
    
/*----------------------------------------------------------------------------*/
    private int countDescendents(ThreadTraceData data) {
        int total = 0;
        
        if (data != null) {
            ThreadTraceData[] children = data.getChildren();
            total += children.length;
            for (int i = 0; i < children.length; i++) {
                total += countDescendents(children[i]);
            }
        }
        return total;
    }

    
    public void testAttachThreadTraceToMonitor() throws Exception {
        final String MONITOR_KEY = "testStartEndTimeForCheckPoint";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);
        
        PerfMonTimer timer = null;
        try {
            timer = PerfMonTimer.start(MONITOR_KEY + ".child", true);
            Thread.sleep(30);
        } finally {
            PerfMonTimer.stop(timer);
        }
        assertNotNull("Should have written output to appender", TestAppender.getLastResult());
        System.out.println(TestAppender.getLastResult().toAppenderString());
        
        assertEquals("CheckPoint name", MONITOR_KEY, TestAppender.getLastResult().getName());
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testStartEndTimeForTimer() throws Exception {
        final String MONITOR_KEY = "testStartEndTimeForTimer";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        long startTimeOuterMethod = 0;
        long startTimeInnerMethod = 0;
        long endTimeInnerMethod = 0;
        long endTimeOuterMethod = 0;

        PerfMonTimer traceTimer = null;
        PerfMonTimer outerTimer = null;
        PerfMonTimer innerTimer = null;
        try {
            traceTimer = PerfMonTimer.start(MONITOR_KEY);
            
            try {
                startTimeOuterMethod = System.currentTimeMillis();
                outerTimer = PerfMonTimer.start(MONITOR_KEY + ".outer");
                Thread.sleep(50);
                
                try {
                    startTimeInnerMethod = System.currentTimeMillis();
                    innerTimer = PerfMonTimer.start(MONITOR_KEY + ".inner");
                    Thread.sleep(50);
                    
                } finally {
                    endTimeInnerMethod = System.currentTimeMillis();
                    PerfMonTimer.stop(innerTimer);
                }
                Thread.sleep(50);
            } finally {
                endTimeOuterMethod = System.currentTimeMillis();
                PerfMonTimer.stop(outerTimer);
            }
        } finally {
            PerfMonTimer.stop(traceTimer);
        }
        
        
        ThreadTraceData trace = TestAppender.getLastResult();
    
        assertNotNull(trace);
        assertEquals("Number of children", 1, trace.getChildren().length);
        
        ThreadTraceData childTrace = trace.getChildren()[0];
        
        assertEquals("CheckPoint name", MONITOR_KEY + ".outer", childTrace.getName());
        assertTimesClose("childTrace startTime", startTimeOuterMethod, childTrace.getStartTime());
        assertTimesClose("childTrace endTime", endTimeOuterMethod, childTrace.getEndTime());

        assertEquals("Number of grand children", 1, childTrace.getChildren().length);
        ThreadTraceData grandChild = childTrace.getChildren()[0];

        assertEquals("grandChild name", MONITOR_KEY + ".inner", grandChild.getName());
        assertTimesClose("startTime", startTimeInnerMethod, grandChild.getStartTime());
        assertTimesClose("endTime", endTimeInnerMethod, grandChild.getEndTime());
    }
    
/*----------------------------------------------------------------------------*/
    public void testMaxStackDepth() throws Exception {
        final String MONITOR_KEY = "testMaxStackDepth";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.setMaxDepth(1);
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;
        PerfMonTimer outerTimer = null;
        PerfMonTimer innerTimer = null;
        try {
            traceTimer = PerfMonTimer.start(MONITOR_KEY);
            try {
                outerTimer = PerfMonTimer.start(MONITOR_KEY + ".outer");
                try {
                    innerTimer = PerfMonTimer.start(MONITOR_KEY + ".inner");
                } finally {
                    PerfMonTimer.stop(innerTimer);
                }
            } finally {
                PerfMonTimer.stop(outerTimer);
            }
        } finally {
            PerfMonTimer.stop(traceTimer);
        }
        
        ThreadTraceData trace = TestAppender.getLastResult();
        assertEquals("Total descendents", 1, countDescendents(trace));
    }


    
    /*----------------------------------------------------------------------------*/
    public void testHandleMonitorsStartedBeforeTheThreadTraceStarted() throws Exception {
        final String MONITOR_KEY = "testHandleMonitorsStartedBeforeTheThreadTraceStarted";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        // This timer will start outside of the thread Trace...
        // However it will be stopped inside the thread trace,
        // For all practical purposes this will be totally ignored...
        PerfMonTimer traceTimerOutside = null;
        
        PerfMonTimer traceTimer = null;
        long traceTimerStart = 0;
        long traceTimerEnd = 0;
        
        PerfMonTimer traceTimerInner = null;
        long traceTimerInnerStart = 0;
        long traceTimerInnerEnd = 0;
        
        traceTimerOutside = PerfMonTimer.start("outside-" + MONITOR_KEY);
        Thread.sleep(50);

        traceTimerStart = System.currentTimeMillis();
        traceTimer = PerfMonTimer.start(MONITOR_KEY );
        Thread.sleep(50);
        
        traceTimerInnerStart = System.currentTimeMillis();
        traceTimerInner = PerfMonTimer.start("inside-" + MONITOR_KEY);
        Thread.sleep(50);

        PerfMonTimer.stop(traceTimerOutside);
        Thread.sleep(50);
        
        traceTimerInnerEnd = System.currentTimeMillis();
        PerfMonTimer.stop(traceTimerInner);
        Thread.sleep(50);
        
        traceTimerEnd = System.currentTimeMillis();
        PerfMonTimer.stop(traceTimer);
        
        ThreadTraceData trace = TestAppender.getLastResult();
//System.out.println(trace.toAppenderString());      

        assertTimesClose("traceTimer start", traceTimerStart, trace.getStartTime());
        assertTimesClose("traceTimer end", traceTimerEnd, trace.getEndTime());
        
        ThreadTraceData traceInner = trace.getChildren()[0];
        assertTimesClose("traceInner start", traceTimerInnerStart, traceInner.getStartTime());
        assertTimesClose("traceInner end", traceTimerInnerEnd, traceInner.getEndTime());
    }

    /**
     * Thread traces are displayed like a call stack.
     * When using extreme method level, or method annotation monitors you are assured that
     * a proper call stack pattern will be in place.  That is monitors will be started and 
     * stopped in a predictable order. i.e.: 
     * 			Monitor1 - Start
     * 				Monitor2 - Start
     * 				Monitor2 - End
     * 			Monitor1 - End
     * 
     * However with manual timers it is possible to circumvent this order. i.e.:
     * 			Monitor1 - Start
     * 				Monitor2 - Start
     * 			Monitor1 - End
     * 				Monitor2 - End
     * 		
     * Since this is not a correct call stack, perfmon4j will discard the offending
     * monitor (based on the above example -- Monitor1).  
     * 
     * @throws Exception
     */
    public void testShouldIgnoreMissNestedMonitor() throws Exception {
        final String MONITOR_KEY = "testSimpleDiscovery";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;
        PerfMonTimer traceMissNested = null;
        PerfMonTimer startedWhileInMissNested = null;
        
        // Starting threadtrace.
        traceTimer = PerfMonTimer.start(MONITOR_KEY);
        
        // Start a timer that will be miss nested.  That means it will stop before it's child.
        traceMissNested = PerfMonTimer.start("missNested");
        PerfMonTimer.stop(PerfMonTimer.start("Checkpoint-A"));

        
        // So the top of the stack is now the missNested timer... 
        startedWhileInMissNested = PerfMonTimer.start("startedInMissNested");
        PerfMonTimer.stop(PerfMonTimer.start("Checkpoint-B"));
        
        
        PerfMonTimer.stop(traceMissNested);
        PerfMonTimer.stop(PerfMonTimer.start("Checkpoint-C"));
        	
        
        PerfMonTimer.stop(startedWhileInMissNested);
        PerfMonTimer.stop(PerfMonTimer.start("Checkpoint-D"));
        	
    	PerfMonTimer.stop(traceTimer);
    
        ThreadTraceData trace = TestAppender.getLastResult();
        String appenderString = trace.toAppenderString();
        
//System.out.println(appenderString);
		
		// Remove times/durations and line feeds for easy compare.
		appenderString = normalizeTraceOutput(appenderString);
		
		final String expectedOutput = "********************************************************************************" +
				"+-X:X:X:X (X) testSimpleDiscovery" +
				"|	+-X:X:X:X (X) Checkpoint-A" +
				"|	+-X:X:X:X Checkpoint-A" +
				"|	+-X:X:X:X (X) startedInMissNested" +
				"|	|	+-X:X:X:X (X) Checkpoint-B" +
				"|	|	+-X:X:X:X Checkpoint-B" +
				"|	|	+-X:X:X:X (X) Checkpoint-C" +
				"|	|	+-X:X:X:X Checkpoint-C" +
				"|	+-X:X:X:X startedInMissNested" +
				"|	+-X:X:X:X (X) Checkpoint-D" +
				"|	+-X:X:X:X Checkpoint-D" +
				"+-X:X:X:X testSimpleDiscovery" +
				"********************************************************************************";
		assertEquals("Expected output should ignore the monitor 'missNested'", expectedOutput, appenderString);
    }


    
    public void testThreadDepthCleanedUpBasedOnMissNestedMonitor() throws Exception {
        final String MONITOR_KEY = "testThreadDepthCleanedUpBasedOnMissNestedMonitor";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        config.setMaxDepth(3);
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;
        PerfMonTimer traceMissNested = null;
        PerfMonTimer level1 = null;
        PerfMonTimer level2 = null;
        
        // Starting threadtrace.
        traceTimer = PerfMonTimer.start(MONITOR_KEY);
        
        // Start a timer that will be miss nested.  That means it will stop before it's child.
        traceMissNested = PerfMonTimer.start("missNested");
        level1 = PerfMonTimer.start("level1");
        PerfMonTimer.stop(traceMissNested);
        
        level2 = PerfMonTimer.start("Level2");
        PerfMonTimer.stop(PerfMonTimer.start("Level3"));
        
        PerfMonTimer.stop(level2);
        PerfMonTimer.stop(level1);
    	PerfMonTimer.stop(traceTimer);
    
        ThreadTraceData trace = TestAppender.getLastResult();
        String appenderString = trace.toAppenderString();
        
//System.out.println(appenderString);
		assertTrue("Level3 should have been included in output", appenderString.contains("Level3"));
    }
    
    /*----------------------------------------------------------------------------*/
    public void testHandleNestedTimerStartAndStops() throws Exception {
        final String MONITOR_KEY = "testHandleNestedStarts";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;
        PerfMonTimer traceTimerNested1 = null;
        PerfMonTimer traceTimerNested2 = null;
        PerfMonTimer traceTimerNested3 = null;
        
        traceTimer = PerfMonTimer.start(MONITOR_KEY);
        
        traceTimerNested1 = PerfMonTimer.start("nested-" + MONITOR_KEY);
        traceTimerNested2 = PerfMonTimer.start("nested-" + MONITOR_KEY);
        traceTimerNested3 = PerfMonTimer.start("nested-" + MONITOR_KEY);

        PerfMonTimer.stop(traceTimerNested3);
        PerfMonTimer.stop(traceTimerNested2);
        PerfMonTimer.stop(traceTimerNested1);
        
        PerfMonTimer.stop(traceTimer);
        
        
        ThreadTraceData trace = TestAppender.getLastResult();
        assertEquals("countDescendents", 3, countDescendents(trace));
    }
    

    /*----------------------------------------------------------------------------*/
    public void testThreadTraceDisplaysFullMonitorNameOnStart() throws Exception {
        final String MONITOR_KEY = "testThreadTraceDisplaysFullMonitorNameOnStart";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;

        String fullyQualifiedName = MONITOR_KEY + ".SubMonitor";
        
        traceTimer = PerfMonTimer.start(fullyQualifiedName);
        PerfMonTimer.stop(traceTimer);
        
        
        ThreadTraceData trace = TestAppender.getLastResult();
        String appenderString = trace.toAppenderString();
System.out.println(appenderString);    
        
        assertTrue("Fully qualified name should appear in the thread trace.", appenderString.contains(fullyQualifiedName));
    }
    

    public void testDynamicThreadTraceDoesNOTDisplaysFullMonitorNameOnStart() throws Exception {
        final String MONITOR_KEY = "testThreadTraceDisplaysFullMonitorNameOnStart";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;

        String fullyQualifiedName = MONITOR_KEY + ".SubMonitor";
        
        traceTimer = PerfMonTimer.start(fullyQualifiedName, true);  // Flagged as dynamic monitor.
        PerfMonTimer.stop(traceTimer);
        
        
        ThreadTraceData trace = TestAppender.getLastResult();
        String appenderString = trace.toAppenderString();
System.out.println(appenderString);    
        
        assertFalse("For a dynamic monitor we MUST not add overhead of logging each category", 
        		appenderString.contains(fullyQualifiedName));
    }
    
    
    
/*----------------------------------------------------------------------------*/
    public void testMinDurationToCapture() throws Exception {
        final String MONITOR_KEY = "testMinDurationToCapture";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.setMinDurationToCapture(30);
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        PerfMonTimer traceTimer = null;
        try {
            traceTimer = PerfMonTimer.start(MONITOR_KEY);

            PerfMonTimer belowThreshold = null;
            try {
                belowThreshold = PerfMonTimer.start(MONITOR_KEY + ".belowThreshold");
                Thread.sleep(10);
            } finally {
                PerfMonTimer.stop(belowThreshold);
            }

            PerfMonTimer overThreshold = null;
            try {
                overThreshold = PerfMonTimer.start(MONITOR_KEY + ".overThreshold");
                Thread.sleep(50);
            } finally {
                PerfMonTimer.stop(overThreshold);
            }
        } finally {
            PerfMonTimer.stop(traceTimer);
        }
        
        ThreadTraceData trace = TestAppender.getLastResult();
//System.out.println(trace.toAppenderString());        
        assertEquals("Total descendents", 1, countDescendents(trace));
    }

    
/*----------------------------------------------------------------------------*/
    public void testMultiThreaded() throws Exception {
        final String MONITOR_KEY = "testMultiThreaded";
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        for (int x = 0; x < 5; x++) {
            new Thread(new ThreadTraceRunnable(MONITOR_KEY)).start();
        }
        Thread.sleep(50);
        
        while (ThreadTraceRunnable.runningCount > 0) {
            Thread.sleep(50);
        }
        Thread.sleep(50);
        
        int count = TestAppender.getOutputCount();
        assertTrue("Total descendents expected at least 950 but was: " + count,  count >= 450);
    }
    
    private static class ThreadTraceRunnable implements Runnable {
        private final String monitorName;
        static int runningCount = 0;
        
        ThreadTraceRunnable(String monitorName) {
            this.monitorName = monitorName;
        }

        public void run() {
            runningCount++;
            
            int countDown = 100;
            
            while (countDown-- > 0) {
                PerfMonTimer traceTimer = null;
                try {
                    traceTimer = PerfMonTimer.start(monitorName);
        
                    PerfMonTimer inner = null;
                    try {
                        inner = PerfMonTimer.start(monitorName + ".inner");
                    } finally {
                        PerfMonTimer.stop(inner);
                    }
                } finally {
                    PerfMonTimer.stop(traceTimer);
                }
            }
            runningCount--;
        }
    }
    
    
/*----------------------------------------------------------------------------*/    
    private static class TestAppender extends Appender {
        static private ThreadTraceData lastResult = null;
        static private int outputCount = 0;
        static private Long outputDelayMillis = null;
        
        public static ThreadTraceData getLastResult() throws Exception {
            Appender.getOrCreateAppender(getAppenderID()).flush();
            return lastResult;
        }

        public static int getOutputCount() throws Exception {
            Appender.getOrCreateAppender(getAppenderID()).flush();
            return outputCount;
        }

        
        public static void clearResult() {
        	outputDelayMillis = null;
        	outputCount = 0;
            lastResult = null;
        }
        
        public static AppenderID getAppenderID() {
            return AppenderID.getAppenderID(TestAppender.class.getName());
        }
        
        public TestAppender(AppenderID id) {
            super(id);
        }
        
        public void outputData(PerfMonData data) {
        	if (outputDelayMillis != null) {
        		long millis = outputDelayMillis.longValue();
        		try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
					// Ignore...
				}
        		
        	}
        	if (data instanceof ThreadTraceData) {
        		outputCount++;
        		lastResult = (ThreadTraceData)data;
        	}
        }
    }    

    public void testThreadTraceAppenderIsAsync() throws Exception {
        final String MONITOR_KEY = "testThreadTraceAppenderIsAsync";
        final long sleepDurationMillis = 1000;
        
        TestAppender.outputDelayMillis = Long.valueOf(sleepDurationMillis);
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon mon = PerfMon.getMonitor(MONITOR_KEY);
        mon.setInternalThreadTraceConfig(config);
        
        PerfMonTimer timer = null;
        long now = System.currentTimeMillis();
        try {
            timer = PerfMonTimer.start(MONITOR_KEY);
        } finally {
            PerfMonTimer.stop(timer);
        }
        long duration = System.currentTimeMillis() - now;
        assertTrue("Should not have waited for appender to write the output", duration < sleepDurationMillis);

        assertNotNull("Output should have been written by asynch thread", TestAppender.getOutputCount());
    }
    
    public void testTraceOnReactiveMonitor() throws Exception {
    	final String REACTIVE_CONTEXT = "ctx-ReactiveTrace";
        final String MONITOR_KEY = "ReactiveTrace";

        // Main Thread.
        
        ThreadTraceConfig config = new ThreadTraceConfig();
        config.addAppender(TestAppender.getAppenderID());
        
        PerfMon traceMonitor = PerfMon.getMonitor(MONITOR_KEY);
        traceMonitor.setInternalThreadTraceConfig(config);
        
        
        final PerfMonTimer reactiveTimer = PerfMonTimer.start(MONITOR_KEY + ".SubCategory", false, REACTIVE_CONTEXT );
        PerfMonTimer traceNestedOnStartedThread = PerfMonTimer.start("traceNestedOnStartedThread");
        
        new Thread(() -> {
        	// 2nd Thread
        	ReactiveContextManager.getContextManagerForThread().moveContext(REACTIVE_CONTEXT);
        	PerfMonTimer traceNestedOnStoppingThread = PerfMonTimer.start("traceNestedOnSecondThread");
            PerfMonTimer.stop(traceNestedOnStoppingThread);
        	ReactiveContextManager.getContextManagerForThread().dissociateContextFromThread(REACTIVE_CONTEXT);
        }).start();

        Thread.sleep(250); // Give thread time to complete.

        // Since the context has not been restored to this thread, this timer
        // must not be part of this trace.
        PerfMonTimer timerNotInReactiveContext = PerfMonTimer.start("timerNotInReactiveContext");
        PerfMonTimer.stop(timerNotInReactiveContext);

        // Move the context back to the main thread
    	ReactiveContextManager.getContextManagerForThread().moveContext(REACTIVE_CONTEXT);
        
        PerfMonTimer.stop(traceNestedOnStartedThread);
        
        // Finally stop the timer asssociated with the reactive Context on a 3rd thread
        
        new Thread(() -> {
        	// 3nd Thread
        	// This time we'll cheat and not explicitly move the reactiveContext
        	// to the thread.  We can get away with that here because we are
        	// stopping the reactive timer and passing in the matching 
        	// reactive context.
        	PerfMonTimer.stop(reactiveTimer);
        }).start();

        Thread.sleep(250); // Give thread time to complete.
    	
        ThreadTraceData trace = TestAppender.getLastResult();
        String appenderString = trace.toAppenderString();
System.out.println(appenderString);
        
		
		appenderString = normalizeTraceOutput(appenderString);
//System.out.println(appenderString);
		
		final String expectedOutput = 
				"********************************************************************************"
				+ "+-X:X:X:X (X) ReactiveTrace.SubCategory"
				+ "|	+-X:X:X:X (X) traceNestedOnStartedThread"
				+ "|	|	+-X:X:X:X (X) traceNestedOnSecondThread"
				+ "|	|	+-X:X:X:X traceNestedOnSecondThread"
				+ "|	+-X:X:X:X traceNestedOnStartedThread"
				+ "+-X:X:X:X ReactiveTrace.SubCategory"
				+ "********************************************************************************";
		assertEquals("Expected output should contain timers across threads within the same context"
			, expectedOutput, appenderString);
    }
    
    
	// Remove times/durations and line feeds from thread trace appender output for easy compare.
    private String normalizeTraceOutput(String appenderString) {
		appenderString = appenderString.replaceAll("\\d+", "X");
		appenderString = appenderString.replaceAll("\r", "");
		appenderString = appenderString.replaceAll("\n", "");

		return appenderString;
    }
    
    
    public static class SimpleListAppender extends Appender {
    	private static final List<String> output = new ArrayList<String>(); 
	 
		public SimpleListAppender(AppenderID id) { 
			super(id); 
		} 
	 
		@Override 
		public void outputData(PerfMonData data) { 
			synchronized (output) { 
				output.add(data.toAppenderString()); 
			} 
		} 
	 
		public static String extractOutput() { 
			StringBuilder result = new StringBuilder(); 
			 
			synchronized (output) { 
				for (String value : output) { 
					result.append(value) 
						.append(System.lineSeparator()); 
				} 
				output.clear(); 
			} 
			return result.toString(); 
		} 
	} 

    public static class NoOpAppender extends Appender {
		public NoOpAppender(AppenderID id) { 
			super(id); 
		} 
	 
		@Override 
		public void outputData(PerfMonData data) { 
			// Do nothing.
		} 
	} 
    
    private void runThroughTimers(String category) {
    	PerfMonTimer.stop(PerfMonTimer.start(category, true));
    }
 
	final private static String XML_ON_MyBase = 
	        "<Perfmon4JConfig enabled='true'>" + 
	        "   <appender name='noOp' className='" + NoOpAppender.class.getName() + "' interval='500 millis'/>" + 
	        "   <appender name='inMemory' className='" + SimpleListAppender.class.getName() + "' interval='500 millis'/>" + 
	        "	<monitor name='WebRequest'>" +  
	        "    	<appender name='noOp' pattern='.'/>" + 
	        "       <attribute name='activeThreadMonitor'>5 minutes, 30 minutes, 1 hour</attribute>" +
	    	"	</monitor>" +                		 
	        "	<monitor name='WebRequest.circulation'>" +  
	        "    	<appender name='noOp' pattern='.'/>" + 
	    	"	</monitor>" +                		 
	        "</Perfmon4JConfig>";     
	final private static String XML_ON_MyBase_WithThreadTrace = 
	        "<Perfmon4JConfig enabled='true'>" + 
	        "   <appender name='noOp' className='" + NoOpAppender.class.getName() + "' interval='500 millis'/>" + 
	        "   <appender name='inMemory' className='" + SimpleListAppender.class.getName() + "' interval='500 millis'/>" + 
	        "	<monitor name='WebRequest'>" +  
	        "    	<appender name='noOp' pattern='.'/>" + 
	        "       <attribute name='activeThreadMonitor'>5 minutes, 30 minutes, 1 hour</attribute>" +
	    	"	</monitor>" +                		 
	        "	<monitor name='WebRequest.circulation'>" +  
	        "    	<appender name='noOp' pattern='.'/>" + 
	    	"	</monitor>" +                		 
	        "	<threadTrace monitorName='WebRequest.api.v1.rest.self'>" + 
	        "    	<appender name='inMemory'/>" + 
	    	"	</threadTrace>" +                		 
	        "</Perfmon4JConfig>";     


	public void testThreadTraceOnMyBase() throws Exception {
		final String expectedThreadTraceOutput = "********************************************************************************"
				+ "+-X:X:X:X (X) WebRequest.api.vX.rest.self"
				+ "+-X:X:X:X WebRequest.api.vX.rest.self"
				+ "********************************************************************************";
		
		PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_ON_MyBase));
		PerfMon.configure(config);
	    try { 
	    	runThroughTimers("WebRequest.api.v1.rest.self");
	    	Thread.sleep(50);
	    	Appender.flushAllAppenders(); 
	    	 
	    	String output = SimpleListAppender.extractOutput(); 
//System.out.println(output);	    	
	    	assertFalse("Thread Trace was not enabled. Did not expect ouput", normalizeTraceOutput(output).contains(expectedThreadTraceOutput));
	    	
			config = XMLConfigurationParser.parseXML(new StringReader(XML_ON_MyBase_WithThreadTrace));
			PerfMon.configure(config);

	    	runThroughTimers("WebRequest.api.v1.rest.self");
	    	Thread.sleep(50);
	    	Appender.flushAllAppenders(); 
	    	 
	    	output = SimpleListAppender.extractOutput(); 
//System.out.println(output);
	    	assertTrue("We now should see the thread trace output", normalizeTraceOutput(output).contains(expectedThreadTraceOutput));
    } finally { 
	    	PerfMon.deInit(); 
	    } 
	} 
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {ThreadTraceMonitorTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(ThreadTraceMonitorTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new ThreadTraceMonitorTest("testMultiThreaded"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ThreadTraceMonitorTest.class);
        }

        return(newSuite);
    }
}

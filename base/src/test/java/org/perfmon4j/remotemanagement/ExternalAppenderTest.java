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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public class ExternalAppenderTest extends PerfMonTestCase {
	private static final Logger logger = LoggerFactory.initLogger(ExternalAppenderTest.class);
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    private String sessionID = null;
    private boolean originalExternalAppenderEnabled = false;
    
/*----------------------------------------------------------------------------*/
    public ExternalAppenderTest(String name) {
        super(name);
    }

    /*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        originalExternalAppenderEnabled = ExternalAppender.isEnabled();
        ExternalAppender.setEnabled(true);
        sessionID = ExternalAppender.connect();
    }
    
    /*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
    	ExternalAppender.disconnect(sessionID);
    	ExternalAppender.setEnabled(originalExternalAppenderEnabled);
    	sessionID = null;
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testExternalMonitor() throws Exception {
        final String MONITOR = "aa.b.c";

        MonitorKeyWithFields m = IntervalData.getFields(MonitorKey.newIntervalKey(MONITOR));
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
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(MONITOR));
        
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
        
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(MON_NAME));
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
    public void testScheduleThreadTrace() throws Exception {
        final String MON_NAME = "aaa.b.ccc";
        
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(MON_NAME));
        FieldKey threadTraceKey = FieldKey.buildThreadTraceKeyFromInterval(monitorKey);
        
        ExternalAppender.scheduleThreadTrace(sessionID, threadTraceKey);

        PerfMonTimer t1 = PerfMonTimer.start(MON_NAME);
        for(int i = 0; i < PerfMon.MAX_ALLOWED_EXTERNAL_THREAD_TRACE_ELEMENTS; i++) {
        	PerfMonTimer t2 = PerfMonTimer.start("a");
        		{
	        	PerfMonTimer t3 = PerfMonTimer.start("b");
	        		{
		        	PerfMonTimer t4 = PerfMonTimer.start("c");
		        	PerfMonTimer.stop(t4);
	        		}
	        	PerfMonTimer.stop(t3);
        		}
        	PerfMonTimer.stop(t2);
        }
        PerfMonTimer.stop(t1);
        
        Map<FieldKey, Object> map = ExternalAppender.getThreadTraceData(sessionID);
        assertNotNull("map", map);
        
        assertEquals("map.size()", 1, map.size());
        String result = (String)map.get(threadTraceKey);
        assertTrue("Thread trace should have been truncated...", result.contains("Thread Trace Limit Exceeded -- Data truncated"));
        
//        System.out.println(result);
    }

    /*----------------------------------------------------------------------------*/    
    public void testScheduleThreadTraceWithMinDurationToCapture() throws Exception {
        final String MON_NAME = "aaa.b.ccc";
        
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(MON_NAME));
        Map<String, String> params = new HashMap<String, String>();
        params.put(FieldKey.THREAD_TRACE_MIN_DURATION_ARG, "10");

        FieldKey threadTraceKey = 
        	FieldKey.buildThreadTraceKeyFromInterval(monitorKey, params);
        
        ExternalAppender.scheduleThreadTrace(sessionID, threadTraceKey);
        PerfMonTimer t1 = PerfMonTimer.start(MON_NAME);
        	{
        	PerfMonTimer t2 = PerfMonTimer.start("a");
        		Thread.sleep(100);
        	PerfMonTimer.stop(t2);
        	PerfMonTimer t3 = PerfMonTimer.start("shouldbefiltered");
    		PerfMonTimer.stop(t3);
        	}
        PerfMonTimer.stop(t1);
        
        Map<FieldKey, Object> map = ExternalAppender.getThreadTraceData(sessionID);
        assertNotNull("map", map);
        
        assertEquals("map.size()", 1, map.size());
        String result = (String)map.get(threadTraceKey);
        
        assertTrue("Thread trace should have filtered element with short duration...", 
        		!result.contains("shouldbefiltered"));
//        System.out.println(result);
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testScheduleThreadTraceWithMaxDepth() throws Exception {
        final String MON_NAME = "aaa.b.ccc";
        
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(MON_NAME));
        Map<String, String> params = new HashMap<String, String>();
        params.put(FieldKey.THREAD_TRACE_MAX_DEPTH_ARG, "2");

        FieldKey threadTraceKey = 
        	FieldKey.buildThreadTraceKeyFromInterval(monitorKey, params);
        
        ExternalAppender.scheduleThreadTrace(sessionID, threadTraceKey);
        PerfMonTimer t1 = PerfMonTimer.start(MON_NAME);
        	{
        	PerfMonTimer t2 = PerfMonTimer.start("a");
	        	PerfMonTimer t3 = PerfMonTimer.start("b");
		        	PerfMonTimer t4 = PerfMonTimer.start("shouldbefiltered");
		    		PerfMonTimer.stop(t4);
	        	PerfMonTimer.stop(t3);
        	PerfMonTimer.stop(t2);
        	}
        PerfMonTimer.stop(t1);
        
        Map<FieldKey, Object> map = ExternalAppender.getThreadTraceData(sessionID);
        assertNotNull("map", map);
        
        assertEquals("map.size()", 1, map.size());
        String result = (String)map.get(threadTraceKey);
//        System.out.println(result);
        
        assertTrue("Thread should filter max depth", 
        		!result.contains("shouldbefiltered"));
    }


    /*----------------------------------------------------------------------------*/
    public void testScheduleThreadTraceWithTriggerOnlyFiresOnMatch() throws Exception {
        final String MON_NAME = "aaa.b.ccc";

        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(MON_NAME));
        Map<String, String> params = new HashMap<String, String>();
        // Name/value deliberately contain ',' and '=' to prove the Base64 URL-safe
        // encoding survives the naive CSV/'='-split tokenizer used to carry extraParams
        // (MiscHelper.tokenizeCSVString + String.split("=")) untouched.
        params.put(FieldKey.THREAD_TRACE_TRIGGER_ARG,
        		FieldKey.encodeTriggerArg("HTTP_COOKIE", "cookieName", "a,b=c"));

        FieldKey threadTraceKey = FieldKey.buildThreadTraceKeyFromInterval(monitorKey, params);
        ExternalAppender.scheduleThreadTrace(sessionID, threadTraceKey);

        // First invocation: no matching trigger validator pushed on this thread -- the
        // scheduled trace must stay pending rather than firing for an unrelated request.
        PerfMonTimer t = PerfMonTimer.start(MON_NAME);
        PerfMonTimer.stop(t);

        Map<FieldKey, Object> map = ExternalAppender.getThreadTraceData(sessionID);
        assertEquals("map.size()", 1, map.size());
        assertEquals("Trace should still be pending, trigger did not match",
        		FieldKey.THREAD_TRACE_PENDING, map.get(threadTraceKey));

        // Second invocation: push a validator matching the same HTTP_COOKIE trigger --
        // the trace should now fire.
        final ThreadTraceConfig.Trigger expectedTrigger = new ThreadTraceConfig.HTTPCookieTrigger("cookieName", "a,b=c");
        ThreadTraceConfig.pushValidator(new ThreadTraceConfig.TriggerValidator() {
        	public boolean isValid(ThreadTraceConfig.Trigger trigger) {
        		return expectedTrigger.getTriggerString().equals(trigger.getTriggerString());
        	}
        });
        try {
        	t = PerfMonTimer.start(MON_NAME);
        	PerfMonTimer.stop(t);
        } finally {
        	ThreadTraceConfig.popValidator();
        }

        map = ExternalAppender.getThreadTraceData(sessionID);
        assertEquals("map.size()", 1, map.size());
        String result = (String)map.get(threadTraceKey);
        assertNotNull("Trace should have captured now that the trigger matched", result);
        assertFalse("Should no longer be pending", FieldKey.THREAD_TRACE_PENDING.equals(result));
    }

    private static FieldKey buildCookieTriggerKey(String monName, String cookieName, String cookieValue) {
        MonitorKeyWithFields monitorKey = IntervalData.getFields(MonitorKey.newIntervalKey(monName));
        Map<String, String> params = new HashMap<String, String>();
        params.put(FieldKey.THREAD_TRACE_TRIGGER_ARG, FieldKey.encodeTriggerArg("HTTP_COOKIE", cookieName, cookieValue));
        return FieldKey.buildThreadTraceKeyFromInterval(monitorKey, params);
    }

    private static void fireWithMatchingCookie(String monName, String cookieName, String cookieValue) {
        final ThreadTraceConfig.Trigger expectedTrigger = new ThreadTraceConfig.HTTPCookieTrigger(cookieName, cookieValue);
        ThreadTraceConfig.pushValidator(new ThreadTraceConfig.TriggerValidator() {
            public boolean isValid(ThreadTraceConfig.Trigger trigger) {
                return expectedTrigger.getTriggerString().equals(trigger.getTriggerString());
            }
        });
        try {
            PerfMonTimer t = PerfMonTimer.start(monName);
            PerfMonTimer.stop(t);
        } finally {
            ThreadTraceConfig.popValidator();
        }
    }

    /*----------------------------------------------------------------------------*/
    public void testExternalCookieTriggerGateTracksSchedulingAndExplicitUnschedule() throws Exception {
        final String MON_NAME = "cookieGate.a";
        assertFalse("Gate should be false before anything is scheduled",
                PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        FieldKey key = buildCookieTriggerKey(MON_NAME, "c1", "v1");
        ExternalAppender.scheduleThreadTrace(sessionID, key);
        assertTrue("Gate should be true once a cookie-triggered trace is scheduled",
                PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        ExternalAppender.unScheduleThreadTrace(sessionID, key);
        assertFalse("Gate should be false again after explicit unschedule",
                PerfMon.hasHttpCookieBasedThreadTraceTriggers());
    }

    /*----------------------------------------------------------------------------*/
    public void testExternalCookieTriggerGateResetsWhenTraceFiresWithNoExplicitUnschedule() throws Exception {
        final String MON_NAME = "cookieGate.b";

        FieldKey key = buildCookieTriggerKey(MON_NAME, "c2", "v2");
        ExternalAppender.scheduleThreadTrace(sessionID, key);
        assertTrue(PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        fireWithMatchingCookie(MON_NAME, "c2", "v2");

        assertFalse("Gate should drop as soon as the trace fires, before any poll/unschedule",
                PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        // Draining the (now completed) result should not change the already-correct gate.
        Map<FieldKey, Object> map = ExternalAppender.getThreadTraceData(sessionID);
        assertNotNull(map.get(key));
        assertFalse(PerfMon.hasHttpCookieBasedThreadTraceTriggers());
    }

    /*----------------------------------------------------------------------------*/
    public void testExternalCookieTriggerGateNoDoubleDecrementWhenUnscheduledAfterFiring() throws Exception {
        final String MON_A = "cookieGate.c1";
        final String MON_B = "cookieGate.c2";

        FieldKey keyA = buildCookieTriggerKey(MON_A, "ca", "va");
        FieldKey keyB = buildCookieTriggerKey(MON_B, "cb", "vb");
        ExternalAppender.scheduleThreadTrace(sessionID, keyA);
        ExternalAppender.scheduleThreadTrace(sessionID, keyB);
        assertTrue(PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        // Fire A; it is now off PerfMon's internal queue (consumed) but is still sitting
        // in ExternalAppender's scheduledThreadTraces map, since nobody has polled it yet.
        fireWithMatchingCookie(MON_A, "ca", "va");
        assertTrue("B is still pending, so the gate must stay true", PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        // Unscheduling A now (after it already fired) must be a no-op for the counter --
        // it must NOT double-decrement past what firing already did.
        ExternalAppender.unScheduleThreadTrace(sessionID, keyA);
        assertTrue("Gate must still be true -- B alone should still hold it open",
                PerfMon.hasHttpCookieBasedThreadTraceTriggers());

        ExternalAppender.unScheduleThreadTrace(sessionID, keyB);
        assertFalse("Gate should now be false -- nothing left pending", PerfMon.hasHttpCookieBasedThreadTraceTriggers());
    }

    /*----------------------------------------------------------------------------*/
    public void testExternalCookieTriggerGateResetsOnSessionDisconnect() throws Exception {
        final String MON_NAME = "cookieGate.d";
        String otherSessionID = ExternalAppender.connect();
        try {
            FieldKey key = buildCookieTriggerKey(MON_NAME, "cd", "vd");
            ExternalAppender.scheduleThreadTrace(otherSessionID, key);
            assertTrue(PerfMon.hasHttpCookieBasedThreadTraceTriggers());
        } finally {
            ExternalAppender.disconnect(otherSessionID);
        }

        assertFalse("Disconnecting the session that scheduled the only pending trigger "
                + "must release the gate, not leak it forever", PerfMon.hasHttpCookieBasedThreadTraceTriggers());
    }

    @SnapShotProvider(type=SnapShotProvider.Type.INSTANCE_PER_MONITOR)
    public static final class SimpleInstancePerMonitorSnapShot {
    	@SnapShotCounter()
    	public long getCurrentMillis() {
    		return System.currentTimeMillis();
    	}
    }

    /*----------------------------------------------------------------------------*/    
    public void testRegisterInstancePerMonitorSnapShot() throws Exception {
    	final MonitorKey expectedMonitorKey = MonitorKey.newSnapShotKey(SimpleInstancePerMonitorSnapShot.class.getName());
    	final FieldKey expectedFieldKey = new FieldKey(expectedMonitorKey, "currentMillisPerSecond", FieldKey.DOUBLE_TYPE);
    	
    	ExternalAppender.registerSnapShotClass(SimpleInstancePerMonitorSnapShot.class.getName());
    	
    	MonitorKey monitors[] = ExternalAppender.getSnapShotMonitorKeys();
    	assertTrue("Should have returned the monitor key", Arrays.asList(monitors).contains(expectedMonitorKey));
    	
    	FieldKey fieldKey[] = ExternalAppender.getFieldsForSnapShotMonitor(expectedMonitorKey);
    	assertNotNull("fieldKey[]", fieldKey);
    	assertEquals("fieldKey[].length", 1, fieldKey.length);
    	assertTrue("Should match expected field key", expectedFieldKey.equals(fieldKey[0]));
    }
    

    /*----------------------------------------------------------------------------*/    
    public void testSubscribeToSnapShot() throws Exception {
    	final MonitorKey expectedMonitorKey = MonitorKey.newSnapShotKey(SimpleInstancePerMonitorSnapShot.class.getName());
    	ExternalAppender.registerSnapShotClass(SimpleInstancePerMonitorSnapShot.class.getName());
    	
    	
    	FieldKey fieldKey[] = ExternalAppender.getFieldsForSnapShotMonitor(expectedMonitorKey);
    	MonitorKeyWithFields monitorKey[] = MonitorKeyWithFields.groupFields(fieldKey);
    	
    	FieldKey millisPerSecond = FieldKey.getFieldByName(fieldKey, "currentMillisPerSecond");
    	ExternalAppender.subscribe(sessionID, monitorKey[0]);

    	Thread.sleep(1000);
    	
    	Map<FieldKey, Object> data = ExternalAppender.takeSnapShot(sessionID, monitorKey[0]);
    	assertNotNull(data);
    	
    	assertNotNull("Should have millisPerSecond field", data.get(millisPerSecond));
    	
//    	System.out.println(data.get(millisPerSecond));
    }
    
    
    /*----------------------------------------------------------------------------*/    
    public void testForceDynamicChildCreation() throws Exception {
    	final String baseMonitor = "BASE";
    	final String child1 = baseMonitor + ".1";
    	final String child2 = baseMonitor + ".2";
    	
    	MonitorKey baseKey = MonitorKey.newIntervalKey(baseMonitor);
    	PerfMon.getMonitor(baseMonitor);
    	
    	PerfMon mon = PerfMon.getMonitor(child1, true);
    	assertEquals("Should not create child since dynamicPath = true",
    			baseMonitor, mon.getName());
    	
    	ExternalAppender.forceDynamicChildCreation(sessionID, baseKey);
    	
    	mon = PerfMon.getMonitor(child1, true);
    	assertEquals("Should create child since monitor specified base should create children",
    			child1, mon.getName());
    	
    	ExternalAppender.unForceDynamicChildCreation(sessionID, baseKey);
    	
    	mon = PerfMon.getMonitor(child2, true);
    	assertEquals("Should no longer create child since dynamicPath = true",
    			baseMonitor, mon.getName());
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

package org.perfmon4j;

import org.mockito.Mockito;
import org.perfmon4j.MonitorThreadTracker.Tracker;

/*
 *	Copyright 2021 Follett School Solutions, LLC 
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


import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class MonitorThreadTrackerTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

    private MonitorThreadTracker trackerList = null;
    
/*----------------------------------------------------------------------------*/
    public MonitorThreadTrackerTest(String name) {
        super(name);
    }
    
/*----------------------------------------------------------------------------*/
    public void setUp() throws Exception {
    	super.setUp();
    	trackerList = new MonitorThreadTracker(Mockito.mock(PerfMon.class));
    }
    
/*----------------------------------------------------------------------------*/
    private MonitorThreadTracker.Tracker newTracker(String threadName) {
    	return new DummyTracker(new Thread(threadName));
    }
    
/*----------------------------------------------------------------------------*/
    public void testAddAndRemoveOne() throws Exception {
    	Tracker one = newTracker("one");
   	
    	assertEquals("Should start out empty", 0, trackerList.getAllRunning().length);
    	trackerList.addTracker(one);
    	
    	assertEquals("Should have 1 element", 1, trackerList.getAllRunning().length);
    	
    	// Remove the single element,  we should now be empty again.
    	trackerList.removeTracker(one);
    	assertEquals("Should be back to empty", 0, trackerList.getAllRunning().length);
    }

    public void testRemoveHead() throws Exception {
    	Tracker one = newTracker("one");
    	Tracker two = newTracker("two");
   	
    	trackerList.addTracker(one);
    	trackerList.addTracker(two);
    	
    	assertEquals("Should have 2 elements", 2, trackerList.getAllRunning().length);
    	assertTrue("one should point forward to two", one.getNext() == two);
    	assertTrue("two should point backwards to one", two.getPrevious() == one);
    	
    	// Remove the head we should now be back to one element.
    	trackerList.removeTracker(one);
    	assertEquals("Should be back to one element", 1, trackerList.getAllRunning().length);
    	
    	assertEquals("Should have thread two left", "two", trackerList.getAllRunning()[0].getThreadName());
    	assertNull("two should not point forwards", two.getNext());
    	assertNull("two should not point backwards", two.getPrevious());
    }

    public void testRemoveTail() throws Exception {
    	Tracker one = newTracker("one");
    	Tracker two = newTracker("two");
   	
    	trackerList.addTracker(one);
    	trackerList.addTracker(two);
    	
    	assertEquals("Should have 2 elements", 2, trackerList.getAllRunning().length);
    	
    	// Remove the head we should now be back to one element.
    	trackerList.removeTracker(two);
    	assertEquals("Should be back to one", 1, trackerList.getAllRunning().length);
    	
    	assertEquals("Should have thread one left", "one", trackerList.getAllRunning()[0].getThreadName());
    	assertNull("one should not point forwards", one.getNext());
    	assertNull("one should not point backwards", one.getPrevious());
    }
    
    public void testRemoveMiddle() throws Exception {
    	Tracker one = newTracker("one");
    	Tracker two = newTracker("two");
    	Tracker three = newTracker("three");
   	
    	trackerList.addTracker(one);
    	trackerList.addTracker(two);
    	trackerList.addTracker(three);
    	
    	assertEquals("Should have 3 elements", 3, trackerList.getAllRunning().length);
    	
    	// Remove the middle.
    	trackerList.removeTracker(two);
    	assertEquals("Should be back to two elements", 2, trackerList.getAllRunning().length);
    	
    	assertEquals("Element one should be left", "one", trackerList.getAllRunning()[0].getThreadName());
    	assertEquals("Followed by element three", "three", trackerList.getAllRunning()[1].getThreadName());
    }

    /**
     * Perfmon4j has long had an issue where if a monitor was enabled, after threads were already
     * in flight, the number of active threads could go negative.  
     * This should not happen anymore when Monitor Thread tracking is enabled.
     * @throws Exception
     */
    public void testUnbalancedRemove() throws Exception {

    	// For this test we will assume that thread one, was started 
    	// before the monitor was enabled.
    	Tracker before = newTracker("addedBEFOREMonitorWasActive");
    	
    	
    	Tracker after = newTracker("addedAFTERMonitorWasActive");
    	trackerList.addTracker(after);
    	
    	assertEquals("We have two threads, but we are only tracking one", 1, trackerList.getAllRunning().length);
   	
    	// Now remove the thread that started before we started tracking
    	assertEquals("We weren't tracking this thread, so it should not impact number running", 1
    		, trackerList.removeTracker(before));
    	
    	assertEquals("Remove the thread that we just added after monitor was active", 0
        		, trackerList.removeTracker(after));
    }
    
    
    public void testDisableThreadTrackers() {
    	Tracker one = newTracker("one");
    	Tracker two = newTracker("two");
    	
		MonitorThreadTracker disabledTrackerList = new MonitorThreadTracker(Mockito.mock(PerfMon.class), true);

		assertEquals("trackerList must maintain the number of active threads (length) even when disabled",
			0, disabledTrackerList.getLength());
		
		// Add Trackers 
		assertEquals("trackerList must maintain the number of active threads (length) even when disabled",
    		1, disabledTrackerList.addTracker(one));
		assertEquals("trackerList must maintain the number of active threads (length) even when disabled",
        	2, disabledTrackerList.addTracker(two));
		
		
		// When disabled we will not return active threads
		assertNull("Not tracking so we don't know what the longest running thread is", disabledTrackerList.getLongestRunning());
		assertEquals("Not tracking so thread list will be empty", 0, disabledTrackerList.getAllRunning().length);
		

		assertEquals("trackerList must maintain the number of active threads (length) even when disabled",
    			2, disabledTrackerList.getLength());
		
		// Remove trackers
		assertEquals("trackerList must maintain the number of active threads (length) even when disabled",
        		1, disabledTrackerList.removeTracker(two));
		assertEquals("trackerList must maintain the number of active threads (length) even when disabled",
        		0, disabledTrackerList.removeTracker(one));
    }
    
    
    /*----------------------------------------------------------------------------*/
    public void testGetLongestRunning() throws Exception {
    	Tracker one = newTracker("one");
   	
    	assertNull("Should not have an oldest", trackerList.getLongestRunning());
    	
    	trackerList.addTracker(one);
    	assertEquals("Should return the head element, that will be the oldest", "one", trackerList.getLongestRunning().getThreadName());
    }
    
    private static class DummyTracker implements MonitorThreadTracker.Tracker {
    	private Tracker previous = null;
    	private Tracker next = null;
    	private Thread thread = null;
    	private long startTime;
    	
    	private DummyTracker(Thread thread) {
    		this.thread = thread;
    		startTime = System.currentTimeMillis();
    	}
    	
		@Override
		public Thread getThread() {
			return thread;
		}

		@Override
		public void setPrevious(Tracker previous) {
			this.previous = previous;
		}

		@Override
		public Tracker getPrevious() {
			return previous;
		}

		@Override
		public void setNext(Tracker next) {
			this.next = next;
		}

		@Override
		public Tracker getNext() {
			return next;
		}

		@Override
		public long getStartTime() {
			return startTime;
		}
    }
    	
    	

/*---------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {MonitorThreadTrackerTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new MonitorThreadTrackerTest("testToString"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(MonitorThreadTrackerTest.class);
        }

        return( newSuite);
    }
}


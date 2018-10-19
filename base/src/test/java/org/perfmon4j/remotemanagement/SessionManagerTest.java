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

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.PerfMonTestCase;

public class SessionManagerTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";
    private SessionManager<TestSessionData> sessionManager = null;
    
/*----------------------------------------------------------------------------*/
    public SessionManagerTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        sessionManager = new SessionManager<TestSessionData>(100);
    }
    
    public void tearDown() throws Exception {
    	sessionManager.deInit();
        super.tearDown();
    }
    
    public void testLifeCycle() {
    	String controlID = null;
    	TestSessionData control = new TestSessionData();
    	
    	String testID = null;
    	TestSessionData test = new TestSessionData();
    	
    	controlID = sessionManager.addSession(control, SessionManager.NO_TIMEOUT);
    	testID = sessionManager.addSession(test, SessionManager.NO_TIMEOUT);
    	
    	assertEquals("Should be able to retrieve session by ID", 
    			test, sessionManager.getSession(testID));
    	
    	sessionManager.disposeSession(testID);
    	assertNull("Should return null after my session is disposed", 
    			sessionManager.getSession(testID));
    	
    	assertEquals("Destroy should have been called", 1, test.destroyCount);
    	
    	// Validate "control" session was not touched...
    	assertEquals("Control should still exist", 
    			control, sessionManager.getSession(controlID));
    	assertEquals("Destroy should have been called on Control", 0, control.destroyCount);
    }

    
    public void testLifeCycleTimeout() throws Exception {
    	String controlID = null;
    	TestSessionData control = new TestSessionData();
    	
    	String testID = null;
    	TestSessionData test = new TestSessionData();
    	
    	controlID = sessionManager.addSession(control, 500);
    	testID = sessionManager.addSession(test, 1);
    	
    	// Wait for the timer task to run...
    	Thread.sleep(250);
    	
    	assertNull("Should null after my session is timed out", 
    			sessionManager.getSession(testID));
    	assertEquals("Destroy should have been called", 1, test.destroyCount);
    	
    	// Validate "control" session was not touched...
    	assertEquals("Control should still exist", 
    			control, sessionManager.getSession(controlID));
    	assertEquals("Destroy should have been called on Control", 0, control.destroyCount);

    	// Wait for the timer task to run...
    	Thread.sleep(800);

    	assertNull("Control should now be null", 
    			sessionManager.getSession(controlID));
    	assertEquals("Destroy should have been called", 1, control.destroyCount);
    }
    
    
    static class TestSessionData implements SessionManager.SessionData {
    	int destroyCount = 0;
    	
		public void destroy() {
			destroyCount++;
		}

		public Object getSessionValue(String key) {
			return null;
		}

		public void putSessionValue(String key, Object value) {
		}

		public Object removeSessionValue(String key) {
			return null;
		}
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
//        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {SessionManagerTest.class.getName()};

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
            newSuite = new TestSuite(SessionManagerTest.class);
        }

        return( newSuite);
    }
}

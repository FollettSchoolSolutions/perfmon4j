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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.SnapShotMonitor.SnapShotMonitorID;

public class SnapShotManagerTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public SnapShotManagerTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        TestSnapShotMonitor.constructCount = 0;
        TestAppender.dataList.clear();
        PerfMon.configure();
        TestSnapShotMonitor.finalizeCount= 0;        
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInit();
        System.gc();
        super.tearDown();
    }
    
/*----------------------------------------------------------------------------*/
    public void testAddAppenderToSnapShotManager() throws Exception {
        SnapShotMonitorID monitorID = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1");
        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getOrCreateMonitor(monitorID);
        
        AppenderID appenderID = AppenderID.getAppenderID(TestAppender.class.getName(), 100);
        monitor.addAppender(appenderID);
        
        Thread.sleep(350);
        Appender.getOrCreateAppender(appenderID).flush();
        
        assertEquals("dataList.size()", 3, TestAppender.dataList.size());

        // When our appender is deInitialized our timer will stop.
        Appender.getOrCreateAppender(appenderID).deInit();
        Thread.sleep(100);

        int currentSnapShotCount = monitor.snapShotCount;
        
        Thread.sleep(400);
        
        assertEquals("Our timer task should have stopped", currentSnapShotCount, monitor.snapShotCount);
    }

    
/*----------------------------------------------------------------------------*/
    public void testAddMultipleAppendersToMonitor() throws Exception {
        SnapShotMonitorID monitorID = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1");
        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getOrCreateMonitor(monitorID);
        
        AppenderID appenderID1 = AppenderID.getAppenderID(TestAppender.class.getName(), 100);
        AppenderID appenderID2 = AppenderID.getAppenderID(TestAppender.class.getName(), 200);
        
        monitor.addAppender(appenderID1);
        monitor.addAppender(appenderID2);
        
        Thread.sleep(475);
        Appender.getOrCreateAppender(appenderID1).flush();
        Appender.getOrCreateAppender(appenderID2).flush();
        
        assertEquals("Both appenders dataList.size()", 6, TestAppender.dataList.size());

        // When our appender is deInitialized our timer will stop.
        Appender.getOrCreateAppender(appenderID2).deInit();

        Thread.sleep(300);
        Appender.getOrCreateAppender(appenderID1).flush();
        Appender.getOrCreateAppender(appenderID2).flush();
        assertEquals("Only 1 active appender should have 3 more snap shots", 9, TestAppender.dataList.size());
        
        // When our appender is deInitialized our timer will stop.
        Appender.getOrCreateAppender(appenderID1).deInit();
        
        Thread.sleep(300);
        Appender.getOrCreateAppender(appenderID1).flush();
        assertEquals("All appenders gone - no more snapShots", 
            9, TestAppender.dataList.size());
    }
    

/*----------------------------------------------------------------------------*/
    public void testInitSnapShot() throws Exception {
        SnapShotMonitorID monitorID = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1");
        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getOrCreateMonitor(monitorID);
        
        AppenderID appenderID1 = AppenderID.getAppenderID(TestAppender.class.getName(), 100);
        monitor.addAppender(appenderID1);
        
        Thread.sleep(475);
        
        assertEquals("initSnapShotCount", 5, monitor.initSnapShotCount);
        
        Appender.getAppender(appenderID1).deInit();
        Thread.sleep(100);
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testPerfMonDeintClearSnapShots() throws Exception {
        SnapShotMonitorID monitorID = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1");
        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getOrCreateMonitor(monitorID);
        
        AppenderID appenderID1 = AppenderID.getAppenderID(TestAppender.class.getName(), 100);
        monitor.addAppender(appenderID1);
        
        Thread.sleep(150);
        
        assertEquals("snapShotCount", 1, monitor.snapShotCount);
        
        TestSnapShotMonitor.finalizeCount = 0;
        PerfMon.deInit();
        
        Thread.sleep(200);
        assertEquals("snapShotCount should have stopped", 1, monitor.snapShotCount);
        monitor = null;
        
        System.gc();
        Thread.sleep(200);
        System.gc();
        
        assertEquals("Monitor should have been finalized", 1, TestSnapShotMonitor.finalizeCount);
        assertEquals("All monitors should have been removed", 0, SnapShotManager.getMonitorCount());
    }

    
/*----------------------------------------------------------------------------*/
    public void testApplyPerfMonConfig() throws Exception {
        PerfMonConfiguration config = new PerfMonConfiguration();
        
        SnapShotMonitorID id = config.defineSnapShotMonitor("bob", TestSnapShotMonitor.class.getName());
        config.defineAppender("test", TestAppender.class.getName(), "100 millis", null);
        config.attachAppenderToSnapShotMonitor("bob", "test");

        SnapShotManager.applyConfig(config);

        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getMonitor(id);
        assertNotNull("Monitor should have been created when configuration was applied", monitor);
        
        Thread.sleep(150);
        
        assertEquals("snapShotCount", 1, monitor.snapShotCount);
    }
    

/*----------------------------------------------------------------------------*/
    public void testApplyPerfMonConfigWillReleaseUnusedMonitors() throws Exception {
        SnapShotMonitorID monitorID1 = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1");
        SnapShotMonitorID monitorID2 = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "2");
        SnapShotMonitorID monitorID3 = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "3");
        
        // Create all 3 monitors...
        SnapShotManager.getOrCreateMonitor(monitorID1);
        SnapShotManager.getOrCreateMonitor(monitorID2);
        SnapShotManager.getOrCreateMonitor(monitorID3);
        
        // Apply configuration with only middle monitor defined
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineSnapShotMonitor("2", TestSnapShotMonitor.class.getName());
        
        SnapShotManager.applyConfig(config);
        
        assertNull("Obsolete monitor1 should have been removed", SnapShotManager.getMonitor(monitorID1));
        assertNull("Obsolete monitor3 should have been removed", SnapShotManager.getMonitor(monitorID3));
    
        assertNotNull("Monitor2 defined by config should remain", 
            SnapShotManager.getMonitor(monitorID2));
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testPerfMonConfigWillReleaseUnusedAppenders() throws Exception {
        SnapShotMonitorID monitorID = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1");
        
        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getOrCreateMonitor(monitorID);
        AppenderID appenderID1 = AppenderID.getAppenderID(TestAppender.class.getName(), 100);
        monitor.addAppender(appenderID1);

        // We should be running now
        Thread.sleep(150);
        Appender myAppender = Appender.getAppender(appenderID1);
        myAppender.flush();
        
        assertEquals("Should have 1 event", 1, TestAppender.dataList.size());

        // Now config... Apply the SnapShotMonitor, but not the appender
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineSnapShotMonitor("1", TestSnapShotMonitor.class.getName());
        PerfMon.configure(config);
        
        Thread.sleep(200);
        myAppender.flush();
        assertEquals("Should no longer be appending events", 1, TestAppender.dataList.size());
    }    
    
    
    
/*----------------------------------------------------------------------------*/
    public static class TestSnapShotMonitor extends SnapShotMonitor {
        public static int constructCount = 0;
        public static int finalizeCount = 0;
        public int initSnapShotCount = 0;
        public int snapShotCount = 0;
        private String strVal = null;
        
        public TestSnapShotMonitor(String name) {
            super(name);
            constructCount++;
        }
        
        public SnapShotData initSnapShot(long currentTimeMillis) {
            initSnapShotCount++;
            return null;
        }
        
        public void finalize() {
            finalizeCount++;
        }
        
        public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
            return new TestSnapShotData(snapShotCount++);
        }
        
        public String getStrVal() {
            return strVal;
        }

        public void setStrVal(String strVal) {
            this.strVal = strVal;
        }

    }

/*----------------------------------------------------------------------------*/
    public void testDefineMonitorWithAttributes() throws Exception {
        Properties props = new Properties();
        props.setProperty("strVal", "bob");
        
        SnapShotMonitorID monitorID = SnapShotMonitor.getSnapShotMonitorID(TestSnapShotMonitor.class.getName(), "1", props);
        TestSnapShotMonitor monitor = (TestSnapShotMonitor)SnapShotManager.getOrCreateMonitor(monitorID);
    
        assertEquals("bob", monitor.getStrVal());
    }
    
    
    /*----------------------------------------------------------------------------*/
    public static class TestSnapShotData extends SnapShotData  {
        final int count;
        
        TestSnapShotData(int count) {
            this.count = count;
        }
        
        public int getCount() {
            return count;
        }

        public String toAppenderString() {
            return "count=" + count;
        }
    }
    
 /*----------------------------------------------------------------------------*/
     public static class TestAppender extends Appender {
         static List<SnapShotData> dataList = new ArrayList();
         
         public TestAppender(AppenderID appenderID) {
             super(appenderID);
         }
         
         public void outputData(PerfMonData data) {
             dataList.add((SnapShotData)data);
         }
     }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {SnapShotManagerTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(SnapShotManagerTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SnapShotManagerTest("testSimpleGetMonitorKeyWithFieldsFromMonitorWithInstances"));
//        newSuite.addTest(new SnapShotManagerTest("testSimpleGetMonitorKeyWithStringField"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(SnapShotManagerTest.class);
        }

        return(newSuite);
    }
}

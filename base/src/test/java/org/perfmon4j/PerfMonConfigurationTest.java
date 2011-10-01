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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PerfMonConfigurationTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public PerfMonConfigurationTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/
    public void testGetMonitorArray() throws Exception {
        PerfMonConfiguration config = new PerfMonConfiguration();

        config.defineMonitor("a.b.c");
        config.defineMonitor("aa.bb");
        config.defineMonitor("a");
        config.defineMonitor("b");
        config.defineMonitor("a.bb");
        config.defineMonitor("a.b");
        config.defineMonitor("<ROOT>");

        String[] monitorArray = config.getMonitorArray();
        assertEquals("monitorArray.length", 7, monitorArray.length);
        
        // Monitors should be sorted in alphabetical order.. With root 
        // if it exists at the element 0.
        
        assertEquals("<ROOT>", monitorArray[0]);
        assertEquals("a", monitorArray[1]);
        assertEquals("a.b", monitorArray[2]);
        assertEquals("a.b.c", monitorArray[3]);
        assertEquals("a.bb", monitorArray[4]);
        assertEquals("aa.bb", monitorArray[5]);
        assertEquals("b", monitorArray[6]);
    }
    
/*----------------------------------------------------------------------------*/
    public void testGetAppendersForMonitor() throws Exception {
        PerfMonConfiguration config = new PerfMonConfiguration();
        final String MONITOR_1 = "a.b.c";
        final String MONITOR_2 = "a";
        final String MONITOR_3 = PerfMon.ROOT_MONITOR_NAME;
        
        final String APPENDER_CLASS_NAME = BogusAppender.class.getName();
        final String APPENDER_NAME = "5_MINUTE_LOG";
        
        config.defineAppender(APPENDER_NAME, APPENDER_CLASS_NAME, "5 minutes");
        
        config.defineMonitor(MONITOR_1);
        config.defineMonitor(MONITOR_2);
        config.defineMonitor(MONITOR_3);
        
        config.attachAppenderToMonitor(MONITOR_1, APPENDER_NAME);
        config.attachAppenderToMonitor(MONITOR_2, APPENDER_NAME);
        config.attachAppenderToMonitor(MONITOR_3, APPENDER_NAME);
        
        PerfMonConfiguration.AppenderAndPattern appenders1[] = config.getAppendersForMonitor(MONITOR_1);
        PerfMonConfiguration.AppenderAndPattern appenders2[] = config.getAppendersForMonitor(MONITOR_2);
        PerfMonConfiguration.AppenderAndPattern appenders3[] = config.getAppendersForMonitor(MONITOR_3);
        
        assertEquals("Appender[] length", 1, appenders1.length);
        assertEquals("Appender[] length", 1, appenders2.length);
        assertEquals("Appender[] length", 1, appenders3.length);
        
        // Since they are all attached to the same appender they should
        // reutrn the same java object
        assertTrue("All monitors should point to the same appender",
            appenders1[0].getAppender() == appenders2[0].getAppender() 
            && appenders2[0].getAppender() == appenders3[0].getAppender());

        // Attaching the same appender multiple times should
        // do nothing...
        config.attachAppenderToMonitor(MONITOR_3, APPENDER_NAME);
        appenders3 = config.getAppendersForMonitor(MONITOR_3);
        assertEquals("Appender[] length", 1, appenders3.length);

        final String APPENDER_NAME_B = "10_MINUTE_LOG";
        config.defineAppender(APPENDER_NAME_B, APPENDER_CLASS_NAME, "10 minutes");
        config.attachAppenderToMonitor(MONITOR_3, APPENDER_NAME_B);

        appenders3 = config.getAppendersForMonitor(MONITOR_3);
        assertEquals("Appender[] length", 2, appenders3.length);
    }

/*----------------------------------------------------------------------------*/    
    public void testDefineSnapShotMonitorArray() throws Exception {
        PerfMonConfiguration config = new PerfMonConfiguration();
        
        config.defineSnapShotMonitor("a", BogusSnapShotMonitor.class.getName());
        config.defineSnapShotMonitor("b", BogusSnapShotMonitor.class.getName());
        config.defineSnapShotMonitor("c", BogusSnapShotMonitor.class.getName());

        assertEquals("snapShotMonitorArray length", 3, config.getSnapShotMonitorArray().length);
    }

    
///*----------------------------------------------------------------------------*/    
//    public void testDuplicateSnapShotNameNotAllowed() throws Exception {
//        PerfMonConfiguration config = new PerfMonConfiguration();
//        
//        config.defineSnapShotMonitor("a", BogusSnapShotMonitor.class.getName());
//        try {
//            config.defineSnapShotMonitor("a", BogusSnapShotMonitor2.class.getName());
//            fail("Should not allow duplicate snap shot monitor name");
//        } catch (InvalidConfigException iec) {
//            // Expected...
//        }
//    }
    
/*----------------------------------------------------------------------------*/    
    public void testAttachAppenderToSnapShotMonitor() throws Exception {
        PerfMonConfiguration config = new PerfMonConfiguration();
        
        config.defineSnapShotMonitor("a", BogusSnapShotMonitor.class.getName());
        config.defineAppender("bob", BogusAppender.class.getName(), "5 min");
    
        config.attachAppenderToSnapShotMonitor("a", "bob");
        
        PerfMonConfiguration.SnapShotMonitorConfig monitorConfig = config.getSnapShotMonitorArray()[0];
        
        assertEquals("a", monitorConfig.getMonitorID().getName());
    
        assertEquals(BogusAppender.class.getName(), monitorConfig.getAppenders()[0].getClassName());
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static class BogusSnapShotMonitor extends SnapShotMonitor {
        public BogusSnapShotMonitor(String name) {
            super(name);
        }
        
        public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
            return null;
        }
    }

/*----------------------------------------------------------------------------*/    
    public static class BogusSnapShotMonitor2 extends SnapShotMonitor {
        public BogusSnapShotMonitor2(String name) {
            super(name);
        }
        
        public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
            return null;
        }
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static class BogusAppender extends Appender {
        public BogusAppender(AppenderID id) {
            super(id);
        }
        
        public void outputData(@SuppressWarnings("unused") PerfMonData data) {
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(PerfMonConfigurationTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {PerfMonConfigurationTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new PerfMonConfigurationTest("testUndefinedAppenderToSnapShotMonitor"));
//        newSuite.addTest(new PerfMonConfigurationTest("testDuplicateSnapShotNameNotAllowed"));
//        newSuite.addTest(new PerfMonConfigurationTest("testAttachAppenderToSnapShotMonitor"));

        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonConfigurationTest.class);
        }

        return( newSuite);
    }
}

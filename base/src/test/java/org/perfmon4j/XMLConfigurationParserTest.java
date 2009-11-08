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

import java.io.StringReader;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.ThresholdCalculator;

public class XMLConfigurationParserTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public XMLConfigurationParserTest(String name) {
        super(name);
    }

    
/*----------------------------------------------------------------------------*/
    public void testParseEnabledAttribute() throws Exception {
        final String XML_ENABLED =
            "<Perfmon4JConfig enabled='true'>" +
            "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
            "   <monitor name='KeywordBean.performSearch'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        final String XML_DISABLED =
            "<Perfmon4JConfig enabled='false'>" +
            "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
            "   <monitor name='KeywordBean.performSearch'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";

        final String XML_GARBAGE =
            "<Perfmon4JConfig enabled='YES'>" +
            "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
            "   <monitor name='KeywordBean.performSearch'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        
        final String XML_DEFAULT =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
            "   <monitor name='KeywordBean.performSearch'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        
        assertTrue("Should default to enabled", XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT)).isEnabled());
        assertTrue("Should be able to explicitly enable", XMLConfigurationParser.parseXML(new StringReader(XML_ENABLED)).isEnabled());
        assertFalse("Should be able to explicitly disable", XMLConfigurationParser.parseXML(new StringReader(XML_DISABLED)).isEnabled());
        assertFalse("If you set the value to anything other than 'true' we will default to disabled", 
            XMLConfigurationParser.parseXML(new StringReader(XML_GARBAGE)).isEnabled());
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testParseWithAddedAttribute() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "       <attribute name='extraString'>MyExtraString</attribute>" +
            "       <attribute name='extraInt'>10</attribute>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
        
        assertEquals("Appender class", MyAppender.class, appender.getClass());
        assertEquals("extraString", "MyExtraString", ((MyAppender)appender).extraString);
        assertEquals("extraInt", 10, ((MyAppender)appender).extraInt);
    }
    

/*----------------------------------------------------------------------------*/
    public void testParseWithAttributeFromSystemProperty() throws Exception {
        System.setProperty("testParse", "x");
        try {
            final String XML =
                "<Perfmon4JConfig>" +
                "   <appender name='5 minute' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='5 min'>" +
                "       <attribute name='extraString'>testParse=${testParse}</attribute>" +
                "   </appender>" +
                "   <monitor name='mon'>" +
                "       <appender name='5 minute'/>" +
                "   </monitor>" +
                "</Perfmon4JConfig>";
            
            PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
            Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
            
            assertEquals("Appender class", MyAppender.class, appender.getClass());
            assertEquals("extraString", "testParse=x", ((MyAppender)appender).extraString);
            
        } finally {
            System.getProperties().remove("testParse");
        }
    }


/*----------------------------------------------------------------------------*/
    public void testParseWithMedianCalculatorAttribute() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "       <attribute name='medianCalculator'>maxElements=50 factor=1000</attribute>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
        
        assertEquals("Appender class", MyAppender.class, appender.getClass());
        MedianCalculator medianCalculator = appender.getMedianCalculator();
        assertNotNull(medianCalculator);
        
        assertEquals(50, medianCalculator.getMaxElements());
        assertEquals(1000, medianCalculator.getFactor());
    }
    

    public void testParseWithThresholdCalculatorAttribute() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "       <attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
        
        assertEquals("Appender class", MyAppender.class, appender.getClass());
        ThresholdCalculator thresholdCalculator = appender.getThresholdCalculator();
        assertNotNull(thresholdCalculator);
        
        assertEquals(3,thresholdCalculator.getThresholdMillis().length);
    }
    
    public void testParseThreadTraceConfigWithDefaultOptions() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='base'" +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "       <attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>" +
            "   </appender>" +
            "   <threadTrace monitorName='WebRequest'>" +
            "       <appender name='5 minute'/>" +
            "   </threadTrace>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertEquals("config should have 1 threadTrace defined", 1, config.getThreadTraceConfigMap().size());
        
        ThreadTraceConfig webRequestThreadTrace = config.getThreadTraceConfigMap().get("WebRequest");
        assertNotNull("Should have WebRequest Thread Trace", webRequestThreadTrace);
        
        assertEquals("Appender count", 1, webRequestThreadTrace.getAppenders().length);
        
        assertEquals("maxDepth", 0, webRequestThreadTrace.getMaxDepth());
        assertEquals("minDurationToCapture", 0, webRequestThreadTrace.getMinDurationToCapture());
        assertEquals("randomSamplingFactor", 0, webRequestThreadTrace.getRandomSamplingFactor());
    }

    public void testParseThreadTraceConfigWithAllAttributes() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <threadTrace monitorName='WebRequest'" +
            "       maxDepth='10'" +
            "       minDurationToCapture='1 second'" +
            "       randomSamplingFactor='500'>" +
            "   </threadTrace>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertEquals("config should have 1 threadTrace defined", 1, config.getThreadTraceConfigMap().size());
        
        ThreadTraceConfig webRequestThreadTrace = config.getThreadTraceConfigMap().get("WebRequest");
        assertNotNull("Should have WebRequest Thread Trace", webRequestThreadTrace);
        
        assertEquals("maxDepth", 10, webRequestThreadTrace.getMaxDepth());
        assertEquals("minDurationToCapture", 1000, webRequestThreadTrace.getMinDurationToCapture());
        assertEquals("randomSamplingFactor",500, webRequestThreadTrace.getRandomSamplingFactor());
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testInvalidAttributeNameDoesNotStopParse() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "       <attribute name='doesNotExist'>bogus</attribute>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        try {
            PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
            config.getAppendersForMonitor("mon")[0].getAppender();
        } catch (Exception ex) {
            fail("Should just ignore missing attributes");
        }
    }
    
    
    public static class MyAppender extends TextAppender {
        private String extraString;
        private int extraInt;
        
        public MyAppender(AppenderID appenderID) {
            super(appenderID);
        }
        
        public void setExtraString(String str) {
            extraString = str;
        }

        public void setExtraInt(int i) {
            extraInt = i;
        }
    
    }

/*----------------------------------------------------------------------------*/
    public void testParseSnapShotManager() throws Exception {
        final String XML_DEFAULT =
            "<Perfmon4JConfig enabled='true'>" +
            "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
            "   <snapShotMonitor name='SystemMemory' className='perfmon.SystemMemory'>" +
            "       <attribute name='param1'>value1</attribute>" +
            "       <appender name='5 minute'/>" +
            "   </snapShotMonitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        PerfMonConfiguration.SnapShotMonitorConfig monitorIDs[] = config.getSnapShotMonitorArray();
        assertEquals("Have 1 snap shot monitor", 1, monitorIDs.length);
        
        assertEquals("SystemMemory", monitorIDs[0].getMonitorID().getName());
        assertEquals("perfmon.SystemMemory", monitorIDs[0].getMonitorID().getClassName());
        assertEquals("Should have attributes", "value1", monitorIDs[0].getMonitorID().getAttributes().get("param1"));
        assertEquals("Number of appenders", 1, monitorIDs[0].getAppenders().length);
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(XMLConfigurationParserTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {XMLConfigurationParserTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new XMLConfigurationParserTest("testParseThreadTraceConfigWithAllAttributes"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(XMLConfigurationParserTest.class);
        }

        return( newSuite);
    }
}

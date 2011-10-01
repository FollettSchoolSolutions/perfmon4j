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
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonConfiguration.AppenderAndPattern;
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

    public void testThreadTraceMinDurationToCaptureDefaultsToMillis() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='base'" +
            "       className='org.perfmon4j.TextAppender' " +
            "       interval='15 min'>" +
           "   </appender>" +
            "   <threadTrace monitorName='WebRequest'" +
            "		minDurationToCapture='5'>" +
            "       <appender name='base'/>" +
            "   </threadTrace>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertEquals("minDurationToCapture", 5, config.getThreadTraceConfigMap().get("WebRequest").getMinDurationToCapture());
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

    
    

    public void testParseThreadTraceConfigWithRequestParameterToken() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <threadTrace monitorName='WebRequest'>" +
            "		<Triggers>" +
            "       	<HTTPRequestTrigger name='BibID' value='100'/>" +
            "       	<HTTPSessionTrigger attributeName='UserID' attributeValue='200'/>" +
            "       	<ThreadNameTrigger threadName='Processor-http:localhost:8080'/>" +
            "       	<ThreadPropertyTrigger name='jobID' value='300'/>" +
            "       	<HTTPCookieTrigger name='JSESSIONID' value='400'/>" +
            "		</Triggers>" +
            "   </threadTrace>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        ThreadTraceConfig webRequestThreadTrace = config.getThreadTraceConfigMap().get("WebRequest");
        assertNotNull("Should have WebRequest Thread Trace", webRequestThreadTrace);
        
        assertNotNull("Should have triggers", webRequestThreadTrace.getTriggers());
        assertEquals("Should have 5 triggers", 5, webRequestThreadTrace.getTriggers().length);
       
   
        // Trigger 1
        ThreadTraceConfig.Trigger trigger  = webRequestThreadTrace.getTriggers()[0];
        assertTrue("Should be an HTTPRequestTrigger", trigger instanceof ThreadTraceConfig.HTTPRequestTrigger);
        
        ThreadTraceConfig.HTTPRequestTrigger httpTrigger = (ThreadTraceConfig.HTTPRequestTrigger)trigger;
        
        assertEquals("BibID", httpTrigger.getName());
        assertEquals("100", httpTrigger.getValue());

    
        // Trigger 2
        trigger  = webRequestThreadTrace.getTriggers()[1];
        assertTrue("Should be an HTTPSessionTrigger", trigger instanceof ThreadTraceConfig.HTTPSessionTrigger);
        
        ThreadTraceConfig.HTTPSessionTrigger sessionTrigger = (ThreadTraceConfig.HTTPSessionTrigger)trigger;
        
        assertEquals("UserID", sessionTrigger.getName());
        assertEquals("200", sessionTrigger.getValue());
        
        // Trigger 3
        trigger  = webRequestThreadTrace.getTriggers()[2];
        assertTrue("Should be an ThreadNameTrigger", trigger instanceof ThreadTraceConfig.ThreadNameTrigger);
        
        ThreadTraceConfig.ThreadNameTrigger threadNameTrigger = (ThreadTraceConfig.ThreadNameTrigger)trigger;
        assertEquals("Processor-http:localhost:8080", threadNameTrigger.getThreadName());
        
        // Trigger 4
        trigger  = webRequestThreadTrace.getTriggers()[3];
        assertTrue("Should be an ThreadPropertyTrigger", trigger instanceof ThreadTraceConfig.ThreadPropertytTrigger);
        
        ThreadTraceConfig.ThreadPropertytTrigger threadProp = (ThreadTraceConfig.ThreadPropertytTrigger)trigger;
        
        assertEquals("jobID", threadProp.getName());
        assertEquals("300", threadProp.getValue());

        // Trigger 5
        trigger  = webRequestThreadTrace.getTriggers()[4];
        assertTrue("Should be an HttpCookieTrigger", trigger instanceof ThreadTraceConfig.HTTPCookieTrigger);
        
        ThreadTraceConfig.HTTPCookieTrigger cookieTrigger = (ThreadTraceConfig.HTTPCookieTrigger)trigger;
        
        assertEquals("JSESSIONID", cookieTrigger.getName());
        assertEquals("400", cookieTrigger.getValue());
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
    public void testDefaultConfiguration() throws Exception {
        final String INVALID_XML = "GARBAGE";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(INVALID_XML));
 
        AppenderAndPattern p[] = config.getAppendersForMonitor(PerfMon.ROOT_MONITOR_NAME);
        assertEquals(1, p.length);
        assertEquals("Default appender should have 5 minute duration", 60 * 5 * 1000, p[0].getAppender().getIntervalMillis());
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), p[0].getAppender().getClass().getName());
        assertEquals("Should monitor all children of root appender by default", 
        		PerfMon.APPENDER_PATTERN_CHILDREN_ONLY, p[0].getAppenderPattern());
    }

    public void testDefaultAppenderCreatedWhenMissing_intervalMonitor() throws Exception {
        final String XML_WITH_MISSING_APPENDER =
            "<Perfmon4JConfig>" +
            "   <monitor name='mon'>" +
            "       <appender name='missing'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";    
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_WITH_MISSING_APPENDER));
 
        AppenderAndPattern p[] = config.getAppendersForMonitor("mon");
        assertEquals(1, p.length);
        
        assertEquals("Default appender should have 1 minute duration", 60 * 1 * 1000, p[0].getAppender().getIntervalMillis());
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), p[0].getAppender().getClass().getName());
    }
    
    
    /*----------------------------------------------------------------------------*/
    public void testDefaultAppenderCreatedWhenMissing_snapShotMonitor() throws Exception {
        final String XML_DEFAULT =
            "<Perfmon4JConfig enabled='true'>" +
            "   <snapShotMonitor name='SystemMemory' className='perfmon.SystemMemory'>" +
            "       <appender name='missing'/>" +
            "   </snapShotMonitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        PerfMonConfiguration.SnapShotMonitorConfig monitorIDs[] = config.getSnapShotMonitorArray();
        assertEquals("Should have 1 appender", 1, monitorIDs[0].getAppenders().length);
        
        AppenderID appenderID = monitorIDs[0].getAppenders()[0];
        assertEquals("Default appender should have 1 minute duration", 60 * 1 * 1000, appenderID.getIntervalMillis());
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), appenderID.getClassName());
    }    

    
    /**
     * If an monitor is created with out defining any appenders, it will automatically 
     * be added to the default appender
     * @throws Exception
     */
    public void testDefaultAppenderCreated_intervalMonitor() throws Exception {
        final String XML_WITH_MISSING_APPENDER =
            "<Perfmon4JConfig>" +
            "   <monitor name='mon'/>" +
            "</Perfmon4JConfig>";    
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_WITH_MISSING_APPENDER));
 
        AppenderAndPattern p[] = config.getAppendersForMonitor("mon");
        assertEquals(1, p.length);
        
        assertEquals("Default appender should have 1 minute duration", 60 * 1 * 1000, p[0].getAppender().getIntervalMillis());
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), p[0].getAppender().getClass().getName());
        assertEquals("By default the appender will be applied to the parent monitor only!", 
        		PerfMon.APPENDER_PATTERN_PARENT_ONLY, p[0].getAppenderPattern());
    }
    
    
    /*----------------------------------------------------------------------------*/
    public void testDefaultAppenderCreated_snapShotMonitor() throws Exception {
        final String XML_DEFAULT =
            "<Perfmon4JConfig enabled='true'>" +
            "   <snapShotMonitor name='SystemMemory' className='perfmon.SystemMemory'/>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        PerfMonConfiguration.SnapShotMonitorConfig monitorIDs[] = config.getSnapShotMonitorArray();
        assertEquals("Should have 1 appender", 1, monitorIDs[0].getAppenders().length);
        
        AppenderID appenderID = monitorIDs[0].getAppenders()[0];
        assertEquals("Default appender should have 1 minute duration", 60 * 1 * 1000, appenderID.getIntervalMillis());
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), appenderID.getClassName());
    }    
    

    /*----------------------------------------------------------------------------*/
    public void testDefaultAppenderCreatedWhenMissing_threadTraceMonitor() throws Exception {
        final String XML_DEFAULT =
            "<Perfmon4JConfig>" +
            "   <threadTrace monitorName='WebRequest'>" +
            "       <appender name='missing'/>" +
            "   </threadTrace>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        Set<Entry<String, ThreadTraceConfig>> entries = config.getThreadTraceConfigMap().entrySet();
        assertEquals("Should have 1 threadTrace configuration", 1, entries.size());
        
        ThreadTraceConfig ttConfig = entries.iterator().next().getValue();
        
        assertEquals("Should have 1 appender", 1, ttConfig.getAppenders().length);
        
        AppenderID appenderID = ttConfig.getAppenders()[0];
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), appenderID.getClassName());
    }    

    
    /*----------------------------------------------------------------------------*/
    public void testDefaultAppenderCreated_threadTraceMonitor() throws Exception {
        final String XML_DEFAULT =
            "<Perfmon4JConfig>" +
            "   <threadTrace monitorName='WebRequest'/>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        Set<Entry<String, ThreadTraceConfig>> entries = config.getThreadTraceConfigMap().entrySet();
        assertEquals("Should have 1 threadTrace configuration", 1, entries.size());
        
        ThreadTraceConfig ttConfig = entries.iterator().next().getValue();
        
        assertEquals("Should have 1 appender", 1, ttConfig.getAppenders().length);
        
        AppenderID appenderID = ttConfig.getAppenders()[0];
        assertEquals("Default should be a TextAppender", TextAppender.class.getName(), appenderID.getClassName());
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
        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreatedWhenMissing_intervalMonitor"));
        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreatedWhenMissing_snapShotMonitor"));
        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreated_intervalMonitor"));
        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreated_snapShotMonitor"));
        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreatedWhenMissing_threadTraceMonitor"));
        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreated_threadTraceMonitor"));
        
        
//        
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(XMLConfigurationParserTest.class);
        }

        return( newSuite);
    }
}

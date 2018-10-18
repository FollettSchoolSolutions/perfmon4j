/*
 *	Copyright 2014 Follett Software Company 
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

package org.perfmon4j.config.xml;

import java.io.StringReader;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMonTestCase;

public class XMLConfigurationParser2Test extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public XMLConfigurationParser2Test(String name) {
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
        
        
        assertTrue("Should default to enabled", XMLConfigurationParser2.parseXML(new StringReader(XML_DEFAULT)).isEnabled());
        assertTrue("Should be able to explicitly enable", XMLConfigurationParser2.parseXML(new StringReader(XML_ENABLED)).isEnabled());
        assertFalse("Should be able to explicitly disable", XMLConfigurationParser2.parseXML(new StringReader(XML_DISABLED)).isEnabled());
        assertFalse("If you set the value to anything other than 'true' we will default to disabled", 
            XMLConfigurationParser2.parseXML(new StringReader(XML_GARBAGE)).isEnabled());
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testParseAppenderWithAddedAttribute() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='my.appender.name' " +
            "       className='my.class.name' " +
            "       interval='5 min'>" +
            "       <attribute name='extraString'>MyExtraString</attribute>" +
            "       <attribute name='extraInt'>10</attribute>" +
            "   </appender>" +
            "</Perfmon4JConfig>";
        
        ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
        
        assertEquals("Should have 1 appender", 1, config.getAppenders().size());
        AppenderConfigElement appenderConfig = config.getAppenders().get(0);
        
        assertEquals("my.appender.name", appenderConfig.getName());
        assertEquals("my.class.name", appenderConfig.getClassName());
        assertEquals("5 min", appenderConfig.getInterval());
        assertEquals(2,  appenderConfig.getAttributes().size());

        assertEquals("MyExtraString",  appenderConfig.getAttributes().get("extraString"));
        assertEquals("10",  appenderConfig.getAttributes().get("extraInt"));
    }

  ///*----------------------------------------------------------------------------*/
    public void testDisableAppender() throws Exception {
    	final String XML =
        "<Perfmon4JConfig>" +
        "   <appender name='disabledAppender' " +
        "     className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
        "     interval='5 min'" +
        "		enabled='false'/>" +
        "   <appender name='enabledAppender' " +
        "     className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
        "     interval='5 min'" +
        "		enabled='true'/>" +
        "</Perfmon4JConfig>";

    	ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
    	assertFalse(config.getAppender("disabledAppender").isEnabled());
    	assertTrue(config.getAppender("enabledAppender").isEnabled());
	}

  ///*----------------------------------------------------------------------------*/
    public void testDisableMonitor() throws Exception {
      	final String XML =
          "<Perfmon4JConfig>" +
          "   <monitor name='disabledMonitor' enabled='false'/>" +
          "   <monitor name='enabledMonitor' enabled='true'/>" +
          "</Perfmon4JConfig>";

      	ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
      	assertTrue(config.getMonitor("enabledMonitor").isEnabled());
      	assertFalse(config.getMonitor("disabledMonitor").isEnabled());
  	}

    ///*----------------------------------------------------------------------------*/
    public void testDisableSnapShot() throws Exception {
      	final String XML =
          "<Perfmon4JConfig>" +
          "   <snapShotMonitor name='disabledSnapShot' className='class1' enabled='false'/>" +
          "   <snapShotMonitor name='enabledSnapShot' className='class2' enabled='true'/>" +
          "</Perfmon4JConfig>";

      	ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
      	assertTrue(config.getSnapShot("enabledSnapShot").isEnabled());
      	assertFalse(config.getSnapShot("disabledSnapShot").isEnabled());
  	}

    ///*----------------------------------------------------------------------------*/
    public void testDisableThreadTrace() throws Exception {
    	final String XML =
          "<Perfmon4JConfig>" +
          "   <threadTrace monitorName='disabledThreadTrace' enabled='false'/>" +
          "   <threadTrace monitorName='enabledThreadTrace' enabled='true'/>" +
          "</Perfmon4JConfig>";

      	ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
      	assertTrue(config.getThreadTrace("enabledThreadTrace").isEnabled());
      	assertFalse(config.getThreadTrace("disabledThreadTrace").isEnabled());
  	}
  	
///*----------------------------------------------------------------------------*/
//    public void testParseSystemAttributeOnAppender() throws Exception {
//        System.setProperty("testParse", "x");
//        try {
//            final String XML =
//                "<Perfmon4JConfig>" +
//                "   <appender name='5 minute' " +
//                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
//                "       interval='5 min'" +
//                "		ifSystemProperty='MY_WAR_IS_INSTALLED'>" +
//                "       <attribute name='extraString'>testParse=${testParse}</attribute>" +
//                "   </appender>" +
//                "   <monitor name='mon'>" +
//                "       <appender name='5 minute'/>" +
//                "   </monitor>" +
//                "</Perfmon4JConfig>";
//            
//            PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
//            assertNull("System property is NOT set, should not have loaded the 5 minute appender",
//            		config.getAppenderForName("5 minute"));
//            
//            assertEquals("System property is NOT set, monitor should NOT be loaded.",
//            		0, config.getMonitorArray().length);
//            
//            assertEquals("Should have captured the system property state", Boolean.FALSE, 
//            		config.getSystemPropertyMap().get("MY_WAR_IS_INSTALLED")); 
//        } finally {
//            System.getProperties().remove("testParse");
//        }
//    }


    
    public void testParseThreadTraceConfigWithDefaultOptions() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <threadTrace monitorName='WebRequest'>" +
            "       <appender name='5 minute'/>" +
            "   </threadTrace>" +
            "</Perfmon4JConfig>";
        
        ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
        assertEquals(1, config.getThreadTraces().size());
        
        ThreadTraceConfigElement ttConfig = config.getThreadTraces().get(0);
        assertEquals("maxDepth", "0", ttConfig.getMaxDepth());
        assertEquals("minDurationToCapture", "0", ttConfig.getMinDurationToCapture());
        assertEquals("randomSamplingFactor", "0", ttConfig.getRandomSamplingFactor());
        
        assertEquals(1, ttConfig.getAppenders().size());
        AppenderMappingElement mapping = ttConfig.getAppenders().get(0);
        
        assertEquals("5 minute", mapping.getName());
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
        
        ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
        assertEquals(1, config.getThreadTraces().size());
        
        ThreadTraceConfigElement ttConfig = config.getThreadTraces().get(0);
        assertEquals("maxDepth", "10", ttConfig.getMaxDepth());
        assertEquals("minDurationToCapture", "1 second", ttConfig.getMinDurationToCapture());
        assertEquals("randomSamplingFactor", "500", ttConfig.getRandomSamplingFactor());
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
        
        ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML));
        assertEquals(1, config.getThreadTraces().size());
        
        ThreadTraceConfigElement ttConfig = config.getThreadTraces().get(0);
        assertEquals("Should have 5 triggers", 5, ttConfig.getTriggers().size());
   
        // Trigger 1
        TriggerConfigElement trigger = ttConfig.getTriggers().get(0);
        
        assertEquals(TriggerConfigElement.Type.REQUEST_TRIGGER, trigger.getType());
        assertEquals("BibID", trigger.getName());
        assertEquals("100", trigger.getValue());
    
        // Trigger 2
        trigger = ttConfig.getTriggers().get(1);
        
        assertEquals(TriggerConfigElement.Type.SESSION_TRIGGER, trigger.getType());
        assertEquals("UserID", trigger.getName());
        assertEquals("200", trigger.getValue());
        
        // Trigger 3
        trigger = ttConfig.getTriggers().get(2);
        
        assertEquals(TriggerConfigElement.Type.THREAD_TRIGGER, trigger.getType());
        assertEquals("Processor-http:localhost:8080", trigger.getName());
        assertNull(trigger.getValue());
        
        // Trigger 4
        trigger = ttConfig.getTriggers().get(3);
        
        assertEquals(TriggerConfigElement.Type.THREAD_PROPERTY_TRIGGER, trigger.getType());
        assertEquals("jobID", trigger.getName());
        assertEquals("300", trigger.getValue());
        
        // Trigger 5
        trigger = ttConfig.getTriggers().get(4);
        
        assertEquals(TriggerConfigElement.Type.COOKIE_TRIGGER, trigger.getType());
        assertEquals("JSESSIONID", trigger.getName());
        assertEquals("400", trigger.getValue());
    }
    
/*----------------------------------------------------------------------------*/
    public void testParseSnapShotManager() throws Exception {
        final String XML_DEFAULT =
            "<Perfmon4JConfig enabled='true'>" +
            "   <snapShotMonitor name='SystemMemory' className='perfmon.SystemMemory'>" +
            "       <attribute name='param1'>value1</attribute>" +
            "       <appender name='5 minute'/>" +
            "   </snapShotMonitor>" +
            "</Perfmon4JConfig>";
        ConfigElement config = XMLConfigurationParser2.parseXML(new StringReader(XML_DEFAULT));
        assertEquals(1, config.getSnapShots().size());

        SnapShotConfigElement ssConfig = config.getSnapShots().get(0);
        
        assertEquals("SystemMemory", ssConfig.getName());
        assertEquals("perfmon.SystemMemory", ssConfig.getClassName());
        
        assertEquals(1, ssConfig.getAppenders().size());
        AppenderMappingElement mapping = ssConfig.getAppenders().get(0);
        
        assertEquals("5 minute", mapping.getName());
        assertNull(mapping.getPattern());

        assertEquals(1, ssConfig.getAttributes().size());
        assertEquals("value1", ssConfig.getAttributes().get("param1"));
    }

    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(XMLConfigurationParser2Test.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {XMLConfigurationParser2Test.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(XMLConfigurationParser2Test.class);
        }

        return( newSuite);
    }
}

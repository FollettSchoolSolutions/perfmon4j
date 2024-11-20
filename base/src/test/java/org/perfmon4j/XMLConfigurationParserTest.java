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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonConfiguration.AppenderAndPattern;
import org.perfmon4j.PerfMonConfiguration.MonitorConfig;
import org.perfmon4j.PerfMonConfiguration.SnapShotMonitorConfig;
import org.perfmon4j.util.ConfigurationProperties;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.ThresholdCalculator;
import org.perfmon4j.util.mbean.MBeanInstance;
import org.perfmon4j.util.mbean.MBeanQuery;
import org.perfmon4j.util.mbean.MBeanQueryBuilder.NamedRegExFilter;
import org.perfmon4j.util.mbean.MBeanQueryBuilder.RegExFilter;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class XMLConfigurationParserTest extends PerfMonTestCase {
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
    public void testParseTagValueFromSystemProperty() throws Exception {
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
    public void testProvideDefaultValueForSystemProperty() throws Exception {
        try {
            final String XML =
                "<Perfmon4JConfig>" +
                "   <appender name='5 minute' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='5 min'>" +
                "       <attribute name='extraString'>testParse=${testParse:this is the default value}</attribute>" +
                "   </appender>" +
                "   <monitor name='mon'>" +
                "       <appender name='5 minute'/>" +
                "   </monitor>" +
                "</Perfmon4JConfig>";
            
            PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
            Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
            
            assertEquals("Appender class", MyAppender.class, appender.getClass());
            assertEquals("extraString property was not set, should have used the default value", 
            		"testParse=this is the default value", ((MyAppender)appender).extraString);
            
        } finally {
            System.getProperties().remove("testParse");
        }
    }    
    
    /*----------------------------------------------------------------------------*/
    public void testSystemPropertySubstitionShouldBeAllowedInAttributes() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='${textAppender.defaultInterval:5 min}'>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
        
        assertEquals("Appender class", MyAppender.class, appender.getClass());
        assertEquals("Shouild have used the default value from system property for the interval",
        		TimeUnit.MINUTES.toMillis(5), appender.getIntervalMillis());
    }
    
    public void testDefineConfigProperty() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<property name='prop1'>test1</property>" +
                "		<property name='prop2'>test2</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
           
        ConfigurationProperties properties = new ConfigurationProperties(new Properties(), new Properties());
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        
        assertEquals("Should have defined prop1", "test1", properties.getProperty("prop1"));
        assertEquals("Should have defined prop2", "test2", properties.getProperty("prop2"));
    }

    public void testMultipePropertiesElements() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<property name='prop1'>test1</property>" +
                "	</properties>" +
                "	<properties>" +
                "		<property name='prop2'>test2</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
           
        ConfigurationProperties properties = new ConfigurationProperties(new Properties(), new Properties());
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        
        assertEquals("Should have defined prop1", "test1", properties.getProperty("prop1"));
        assertEquals("Should have defined prop2", "test2", properties.getProperty("prop2"));
    }

    public void testDefinedPropertyBeingAppliedToSubsequentValues() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<property name='prop1'>test1</property>" +
                "	</properties>" +
                "	<properties>" +
                "		<property name='prop2-${prop1}'>test2-${prop1}</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
           
        ConfigurationProperties properties = new ConfigurationProperties(new Properties(), new Properties());
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        
        assertEquals("Key and value for the second property should have been altered by the prop1", "test2-test1", properties.getProperty("prop2-test1"));
    }    

    public void testActivateBasedOnPropertyExists() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<activation>" +
                "			<property name='verbosePerfmonOutput'/>" + // Just require property to exist
                "		</activation>" +
                "		<property name='prop1'>activated</property>" +
                "	</properties>" +
                "	<properties>" +
                "		<property name='prop2-${prop1:notActivated}'>test2-${prop1:notActivated}</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
           
        Properties envProps = new Properties();

        // First try without verbosePerfmonOutput property being defined.
        ConfigurationProperties properties = new ConfigurationProperties(envProps, new Properties());
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        assertEquals("prop1 was not activated and should not have been set", 
        	"test2-notActivated", properties.getProperty("prop2-notActivated"));
        
        // Now try with verbosePerfmonOutput property being defined.
        envProps.setProperty("verbosePerfmonOutput", ""); 
        properties = new ConfigurationProperties(envProps, new Properties());
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        assertEquals("prop1 was not activated and should have been set", 
        	"test2-activated", properties.getProperty("prop2-activated"));
    }    

    public void testActivateBasedOnPropertyMatch() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<activation>" +
                "			<property name='perfmonOutputLevel'>verbose</property>" + 
                "		</activation>" +
                "		<property name='prop1'>verbose</property>" +
                "	</properties>" +
                "	<properties>" +
                "		<property name='prop2-${prop1:notVerbose}'>test2-${prop1:notVerbose}</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
           
        Properties envProps = new Properties();

        // First try with non-matching perfmonOutputLevel
        ConfigurationProperties properties = new ConfigurationProperties(envProps, new Properties());
        envProps.setProperty("perfmonOutputLevel", "default"); 
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        assertEquals("prop1 was not activated and should not have been set", 
        	"test2-notVerbose", properties.getProperty("prop2-notVerbose"));
        
        // Now try with verbosePerfmonOutput property being defined.
        envProps.setProperty("perfmonOutputLevel", "verbose"); 
        properties = new ConfigurationProperties(envProps, new Properties());
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        assertEquals("prop1 was not activated and should have been set", 
            	"test2-verbose", properties.getProperty("prop2-verbose"));
    }    
    
    public void testMultipleActivationPropertiesMustAllMatch() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<activation>" +
                "			<property name='influxGroups'/>" + 
                "			<property name='influxPassword'/>" + 
                "		</activation>" +
                "		<property name='influxEnabled'>true</property>" +
                "	</properties>" +
                "	<properties>" +
                "		<property name='isInfluxEnabled'>${influxEnabled:false}</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
           
        Properties envProps = new Properties();

        // First try with only 1 or two required properties set.
        ConfigurationProperties properties = new ConfigurationProperties(envProps, new Properties());
        envProps.setProperty("influxGroups", "dly"); 
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        assertEquals("Influx enabled requres BOTH groups and password to be set", 
        	"false", properties.getProperty("isInfluxEnabled"));
        
        // Now try with both set
        envProps.setProperty("influxPassword", "secretPassword"); 
        XMLConfigurationParser.parseXML(new StringReader(XML), properties);
        assertEquals("Influx should be enabled", 
        	"true", properties.getProperty("isInfluxEnabled"));
    }    
    
    public void testMultipePropertyActivationsNotAllowed() throws Exception {
        final String XML =
                "<Perfmon4JConfig>" +
                "	<properties>" +
                "		<activation>" +
                "			<property name='influxGroups'/>" + 
                "		</activation>" +
                "		<activation>" +
                "			<property name='influxPassword'/>" + 
                "		</activation>" +
                "		<property name='influxEnabled'>true</property>" +
                "	</properties>" +
                "</Perfmon4JConfig>";
    	
        
        PerfMonConfiguration result = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertNotNull("Expected default configuration to be returned", result.getAppenderForName("DEFAULT_APPENDER"));
    }

    public void testDisableAppenderDisablesEverythingSoleyDependentOnIt() throws Exception { 
    	final String XML =
                "<Perfmon4JConfig>" +
                "   <appender enabled='false' " + 
                "		name='myAppender' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='1 min'/>" +
                "   <monitor name='myMon'>" +
                "       <appender name='myAppender'/>" +
                "   </monitor>" +                
                "   <snapShotMonitor name='mySnapShot' className='perfmon.SystemMemory'>" +
                "       <appender name='myAppender'/>" +
                "   </snapShotMonitor>" +
                "   <emitterMonitor name='myEmitter' className='org.perfmon4j.emitter.DemoEmitter'>" +
                "       <appender name='myAppender'/>" +
                "   </emitterMonitor>" +
                "   <threadTrace monitorName='com.follett.fsc.DoSomething'>" +
                "       <appender name='myAppender'/>" +
                "   </threadTrace>" +
                "</Perfmon4JConfig>";
        
        PerfMonConfiguration result = XMLConfigurationParser.parseXML(new StringReader(XML));
        
        assertNull("Text appender is disabled so it should not be included", 
        	result.getAppenderForName("myAppender"));
        assertFalse("Since the only appender on Monitor myMon is disabled, "
        		+ "the monitor should not be configured", hasMonitor(result, "myMon"));
        assertFalse("Since the only appender on SnapShotMonitor mySnapShot is disabled, "
        		+ "the SnapShot should not be configured", hasSnapShot(result, "mySnapShot"));
        assertFalse("Since the only appender on EmitterMonitor myEmitter is disabled, "
        		+ "the Emitter (under the covers an Emitter is the same as a SnapShotConfig)"
        		+ " should not be configured", hasSnapShot(result, "myEmitter"));
        assertFalse("Since the only appender on ThreadTrace com.follett.fsc.DoSomething is disabled, "
        		+ "the ThreadTrace should not be configured", hasThreadTrace(result, "com.follett.fsc.DoSomething"));
    }

    public void testDisableMonitor() throws Exception { 
    	final String XML =
                "<Perfmon4JConfig>" +
                "   <appender" + 
                "		name='myAppender' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='1 min'/>" +
                "   <monitor enabled='false' name='myMon'>" +
                "       <appender name='myAppender'/>" +
                "   </monitor>" +                
                "</Perfmon4JConfig>";
        
        PerfMonConfiguration result = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertFalse("Monitor should be ignored since it is disabled", hasMonitor(result, "myMon"));
    }

    public void testDisableSnapShot() throws Exception { 
    	final String XML =
                "<Perfmon4JConfig>" +
                "   <appender" + 
                "		name='myAppender' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='1 min'/>" +
                "   <snapShotMonitor enabled='false' name='mySnapShot' className='perfmon.SystemMemory'>" +
                "       <appender name='myAppender'/>" +
                "   </snapShotMonitor>" +
                "</Perfmon4JConfig>";
        
        PerfMonConfiguration result = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertFalse("SnapShot should be ignored since it is disabled", hasSnapShot(result, "mySnapShot"));
    }
    
    public void testDisableEmitter() throws Exception { 
    	final String XML =
                "<Perfmon4JConfig>" +
                "   <appender" + 
                "		name='myAppender' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='1 min'/>" +
                "   <emitterMonitor enabled='false' name='myEmitter' className='org.perfmon4j.emitter.DemoEmitter'>" +
                "       <appender name='myAppender'/>" +
                "   </emitterMonitor>" +
                "</Perfmon4JConfig>";
        
        PerfMonConfiguration result = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertFalse("Emitter should be ignored since it is disabled"
        		+ " should not be configured", hasSnapShot(result, "myEmitter"));
    }

    public void testDisableThreadTrace() throws Exception { 
    	final String XML =
                "<Perfmon4JConfig>" +
                "   <appender" + 
                "		name='myAppender' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='1 min'/>" +
                "   <threadTrace enabled='false' monitorName='com.follett.fsc.DoSomething'>" +
                "       <appender name='myAppender'/>" +
                "   </threadTrace>" +
                "</Perfmon4JConfig>";
        
        PerfMonConfiguration result = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertFalse("ThreadTrace should be ignored since it is disabled", 
        	hasThreadTrace(result, "com.follett.fsc.DoSomething"));
    }
    
    /*----------------------------------------------------------------------------*/
    public void testParseBodyValueFromEnvironmentVariable() throws Exception {
    	Map.Entry<String, String> entry = getEnvironmentVariableForUser();
    	
    	assertNull("Can not have a system property set with the same value", System.getProperty(entry.getValue()));
    	
    	String token = "${" + entry.getKey() + "}";
    	
    	assertEquals(entry.getValue(), substituteTokenInXMLBody(token));
    }

    
    /*----------------------------------------------------------------------------*/
    public void testParseBodyValueFromEnvironmentVariableSystemPropIsPreferred() throws Exception {
    	Map.Entry<String, String> entry = getEnvironmentVariableForUser();
    	String tokenPrefersSystemProperty = "${" + entry.getKey() + "}";
    	String tokenPrefersEnvVariable = "${env." + entry.getKey() + "}";

    	final String resetSystemProperty = System.getProperty(entry.getKey());
    	final String systemPropertyValue = entry.getValue() + "ABC";
    	System.setProperty(entry.getKey(), systemPropertyValue);
    	try {
        	assertEquals(entry.getValue(), substituteTokenInXMLBody(tokenPrefersEnvVariable));
        	assertEquals(systemPropertyValue, substituteTokenInXMLBody(tokenPrefersSystemProperty));
        	
    	} finally {
    		if (resetSystemProperty != null) {
    			System.setProperty(entry.getKey(), resetSystemProperty);
    		} else {
    			System.getProperties().remove(entry.getKey());
    		}
    	}
    }
    
    /*----------------------------------------------------------------------------*/
    public void testParseWithAttributeFromSystemProperty() throws Exception {
        System.setProperty("monitorName", "setByProperty");
        try {
            final String XML =
                "<Perfmon4JConfig>" +
                "   <appender name='5 minute' " +
                "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
                "       interval='5 min'>" +
                "   </appender>" +
                "   <monitor name='${monitorName}'>" +
                "       <appender name='5 minute'/>" +
                "   </monitor>" +
                "</Perfmon4JConfig>";
            
            PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
            assertEquals("Monitor name should be value of system property", "setByProperty", config.getMonitorArray()[0]);
            
        } finally {
            System.getProperties().remove("monitorName");
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
    
    public void testParseWithThresholdOnMonitor() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <attribute name='thresholdCalculator'>2 seconds, 5 seconds, 10 seconds</attribute>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertEquals("Should have one monitor defined", 1, config.getMonitorConfigArray().length);
        
        MonitorConfig monitorConfig = config.getMonitorConfigArray()[0];
        assertEquals("Expected threshold calculator", monitorConfig.getProperty("thresholdCalculator"), "2 seconds, 5 seconds, 10 seconds");
    }

    public void testParseActiveThreadMonitorOnMonitor() throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <attribute name='activeThreadMonitor'>1 minute, 30 minutes, 1 hour</attribute>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        assertEquals("Should have one monitor defined", 1, config.getMonitorConfigArray().length);
        
        MonitorConfig monitorConfig = config.getMonitorConfigArray()[0];
        assertEquals("Expected activeThreadMonitor", monitorConfig.getProperty("activeThreadMonitor"), "1 minute, 30 minutes, 1 hour");
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
    

    
    /**
     * If you don't supply an appender pattern for a monitor, or if you define a bad appender pattern, the pattern should default to 
     * the parent only appender pattern.
     * 
     * @throws Exception
     */
    public void testDefaultAppenderPattern() throws Exception {
        final String XML_DEFAULT =
                "<Perfmon4JConfig enabled='true'>" +
                "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
                "   <monitor name='mon'>" +
                "    	<appender name='5 minute'/>" +
                "	</monitor>" +
                "   <monitor name='mon2'>" +
                "    	<appender name='5 minute' pattern='garbage'/>" +
                "	</monitor>" +
                "</Perfmon4JConfig>";        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
 
        AppenderAndPattern p[] = config.getAppendersForMonitor("mon");
        assertEquals(1, p.length);
        assertEquals("If you dont provide a pattern it should default to parent only", 
        		PerfMon.APPENDER_PATTERN_PARENT_ONLY, p[0].getAppenderPattern());
        
        p = config.getAppendersForMonitor("mon2");
        assertEquals(1, p.length);
        assertEquals("If you provide an unrecognized pattern it will default to parent only", 
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
    
    public void testIgnoreBootSection() throws Exception {
        final String XML_WITH_BOOT =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve outputRequestAndDuration='true'/>" +
            "	</boot>" +
            "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
            "   <monitor name='KeywordBean.performSearch'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";

    	XMLPerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_WITH_BOOT));
    	assertNotNull("Should not have recieved default configuration",config.getAppenderForName("5 minute"));
    }

    private Map.Entry<String, String> getEnvironmentVariableForUser() {
    	Map<String, String> envProperties = System.getenv();
    	for (Map.Entry<String, String> envProp : envProperties.entrySet()) {
    		final String key = (String)envProp.getKey();
    		final String value = (String)envProp.getValue();
    		if (key.equalsIgnoreCase("USER") || key.equalsIgnoreCase("USERNAME") ) {
		        return new Map.Entry<String, String>() {
		
		    			public String getKey() {
		    				return key;
		    			}
		
		    			public String getValue() {
		    				return value;
		    			}
		
		    			public String setValue(String value) {
		    				return null;
		    			}
		    		};
    		}
    	}
    	
    	fail("Could not find environment variable for User or UserName");
    	return null;
 	}

    /**
     * This helper method will build an XML configuration containing the
     * specified tokenName within a body tag.
     * The configuration will be processed and the substituted value
     * of the token will be returned.
     * 
     *  
     * @param token - Examples ${USER}, ${env.USER}
     * @return
     * @throws Exception
     */
    private String substituteTokenInXMLBody(String token) throws Exception {
        final String XML =
            "<Perfmon4JConfig>" +
            "   <appender name='5 minute' " +
            "       className='org.perfmon4j.XMLConfigurationParserTest$MyAppender' " +
            "       interval='5 min'>" +
            "       <attribute name='extraString'>" + token + "</attribute>" +
            "   </appender>" +
            "   <monitor name='mon'>" +
            "       <appender name='5 minute'/>" +
            "   </monitor>" +
            "</Perfmon4JConfig>";
        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML));
        Appender appender = config.getAppendersForMonitor("mon")[0].getAppender();
        return ((MyAppender)appender).extraString;
    }

    public void testExtendedPattern() throws Exception {
        final String XML_DEFAULT =
                "<Perfmon4JConfig enabled='true'>" +
                "   <appender name='5 minute' className='org.perfmon4j.TextAppender' interval='5 min'/>" +
                "   <monitor name='mon'>" +
                "    	<appender name='5 minute' pattern='/##.MyPackage#*'/>" +
                "	</monitor>" +
                "</Perfmon4JConfig>";        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        AppenderAndPattern[] appenders = config.getAppendersForMonitor("mon");
        assertEquals("Should have one appender", 1, appenders.length);		
        assertEquals("Expected pattern", "/##.MyPackage#*", appenders[0].getAppenderPattern());		
    }

    public void testParseMBeanSnapshot() throws Exception {
        final String XML_DEFAULT =
                "<Perfmon4JConfig enabled='true'>" +
                "   <appender name='5 minute' className='org.SpecialAppender' interval='5 min'/>" +
                "	<mBeanSnapshotMonitor name='WildflyThreadPool'" + 
            	"		domain='jboss'" + // domain is optional and typically not needed.
            	"		jmxName='jboss:threads:type=thread-pool'" + 
            	"		instanceKey='name'" +
            	"		instanceValueFilter='Standard|Priority'" +  // Regular Expression matching the ObjectName property specified by 'instanceKey'
            	"		attributeValueFilter='type=Regular.*'" +  // <mBeanAttributName>=<regular expression matching attributeValue>
            	"		gauges='poolSize'" + 
            	"		counters='completedTaskCount'>" +
                "    	<appender name='5 minute'/>" +
            	"	</mBeanSnapshotMonitor>" +                		
                "</Perfmon4JConfig>";        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        /**
         * First check that we created a the MBeanQuery.
         */
        MBeanQuery querys[] = config.getMBeanQueryArray().toArray(new MBeanQuery[] {}); 
        assertEquals("Expected mBeanQuery count", 1, querys.length);
        
        MBeanQuery query = querys[0];
        assertEquals("mBeanQuery display name", "WildflyThreadPool", query.getDisplayName());
        assertEquals("mBeanQuery domain", "jboss", query.getDomain());
        assertEquals("mBeanQuery jmxName", "jboss:threads:type=thread-pool", query.getBaseJMXName());
        assertEquals("mBeanQuery instanceKey", "name", query.getInstanceKey());
        assertEquals("mBeanQuery gauges", "poolSize", query.getGauges()[0]);
        assertEquals("mBeanQuery counters", "completedTaskCount", query.getCounters()[0]);
        
        RegExFilter filter = query.getInstanceValueFilter();
        assertNotNull("Should have instanceValueFilter", filter);
        assertTrue("instanceValueFilter regEx should match value of 'Standard'", filter.matches("Standard"));
        
        NamedRegExFilter namedFilter = query.getAttributeValueFilter();
        assertNotNull("Should have attributeValueFilter", namedFilter);
        assertEquals("Expected attributeValueFilter attibute name", "type", namedFilter.getName());
        assertTrue("attributeValueFilter regEx should match value of 'Regular Type", namedFilter.matches("Regular type"));
        
        
        /**
         * Now check to make sure we created a SnapShot Monitor that will be 'attached'
         * to the MBeanInstance created by the MBeanQuery
         */
        SnapShotMonitorConfig[] snapShotMonitors = config.getSnapShotMonitorArray();
        assertEquals("Expected 'implicit' snapShotMonitor to be created", 1, snapShotMonitors.length);
        
        SnapShotMonitorConfig monitorConfig = snapShotMonitors[0];
        assertEquals("Expected name of SnapShotMonitor", query.getDisplayName(), monitorConfig.getMonitorID().getName());
        assertEquals("Expected 'classname' of the SnapShotMonitor", MBeanInstance.buildEffectiveClassName(query), monitorConfig.getMonitorID().getClassName());
        
        assertEquals("expected number of appenders", 1, monitorConfig.getAppenders().length);
        assertEquals("expected appender", "org.SpecialAppender", monitorConfig.getAppenders()[0].getClassName());
    }

    public void testParseMultipleMBeanSnapshots() throws Exception {
        final String XML_DEFAULT =
                "<Perfmon4JConfig enabled='true'>" +
                "   <appender name='5 minute' className='org.SpecialAppender' interval='5 min'/>" +
                "	<mBeanSnapshotMonitor name='OldGenGC'" + 
            	"		jmxName='java.lang:name=G1 Old Generation,type=GarbageCollector'" + 
            	"		counters='collectionTime,collectionCount'>" +
                "    	<appender name='5 minute'/>" +
            	"	</mBeanSnapshotMonitor>" +                		
                "	<mBeanSnapshotMonitor name='OldGenMemoryPool'" + 
            	"		jmxName='java.lang:name=G1 Old Gen,type=MemoryPool'" + 
            	"		counters='usage.committed(displayName=\"committed\"),usage.max(displayName=\"max\"),usage.used(displayName=\"used\")'>" +
                "    	<appender name='5 minute'/>" +
            	"	</mBeanSnapshotMonitor>" +                		
                "</Perfmon4JConfig>";        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        /**
         * First check that we created a the MBeanQuery.
         */
        MBeanQuery querys[] = config.getMBeanQueryArray().toArray(new MBeanQuery[] {}); 
        assertEquals("Expected mBeanQuery count", 2, querys.length);
        
        /**
         * Now check to make sure we created a SnapShot Monitor that will be 'attached'
         * to the MBeanInstance created by the MBeanQuery
         */
        SnapShotMonitorConfig[] snapShotMonitors = config.getSnapShotMonitorArray();
        assertEquals("Expected 'implicit' snapShotMonitor count", 2, snapShotMonitors.length);
    }
    
    
    public void testParseMBeanSnapshotWithRatio() throws Exception {
        final String XML_DEFAULT =
                "<Perfmon4JConfig enabled='true'>" +
                "   <appender name='5 minute' className='org.SpecialAppender' interval='5 min'/>" +
                "	<mBeanSnapshotMonitor name='WildflyThreadPool'" + 
            	"		jmxName='jboss:threads:type=thread-pool'" + 
            	"		ratios='inUsePercent=inUse/poolSize(formatAsPercent=\"true\")'>" +
                "    	<appender name='5 minute'/>" +
            	"	</mBeanSnapshotMonitor>" +                		
                "</Perfmon4JConfig>";        
        PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_DEFAULT));
        
        /**
         * First check that we created a the MBeanQuery.
         */
        MBeanQuery querys[] = config.getMBeanQueryArray().toArray(new MBeanQuery[] {}); 
        assertEquals("Expected mBeanQuery count", 1, querys.length);
        
        MBeanQuery query = querys[0];
        org.perfmon4j.util.mbean.SnapShotRatio[] ratios = query.getRatios();

        assertNotNull("MBeanQuery.getRatios() should never return null", ratios);
        assertEquals("Expected number of ratios", 1, ratios.length);
        
        assertEquals("Expected ratio name", "inUsePercent", ratios[0].getName());
        assertEquals("Expected ratio numerator", "inUse", ratios[0].getNumerator());
        assertEquals("Expected ratio denominator", "poolSize", ratios[0].getDenominator());
        assertTrue("Should format as percent", ratios[0].isFormatAsPercent());
    }
    
	
	public static final class SimpleListAppender extends Appender {
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
	
    final String XML_GC_MONITOR =
            "<Perfmon4JConfig enabled='true'>" +
            "   <appender name='inMemory' className='" + SimpleListAppender.class.getName() + "' interval='250 millis'/>" +
            "	<mBeanSnapshotMonitor name='JVMRuntime'" + 
        	"		jmxName='java.lang:type=Runtime'" + 
        	"		gauges='uptime(displayName=\"My Custom Gauge\")'>" +
            "    	<appender name='inMemory'/>" +
        	"	</mBeanSnapshotMonitor>" +                		
            "	<mBeanSnapshotMonitor name='JVMThreading'" + 
        	"		jmxName='java.lang:type=Threading'" + 
        	"		counters=\"TotalStartedThreadCount(displayName='My Custom Counter')\"" +
        	"		gauges='ThreadCount,DaemonThreadCount'" +
        	"		ratios='DaemonPercent=daemonThreadCount/threadCount(formatAsPercent=true)'>" +
            "    	<appender name='inMemory'/>" +
        	"	</mBeanSnapshotMonitor>" +      
            "	<mBeanSnapshotMonitor name='GarbageCollector'" + 
        	"		jmxName='java.lang:type=GarbageCollector'" + 
        	"		instanceKey='name'" + 
        	"		counters='CollectionTime(displayName=\"collectionMillis\"),CollectionCount(displayName=\"numCollections\")'>" +
            "    	<appender name='inMemory'/>" +
        	"	</mBeanSnapshotMonitor>" +      
            "</Perfmon4JConfig>";    


	public void testFullConfigureMBeanSnapShot() throws Exception {
		PerfMonConfiguration config = XMLConfigurationParser.parseXML(new StringReader(XML_GC_MONITOR));
		PerfMon.configure(config);
        try {
        	Thread.sleep(350);
        	Appender.flushAllAppenders();
        	
        	String output = SimpleListAppender.extractOutput();
//System.out.println(output);        	
			assertTrue("Expected output from MemoryPool monitor", output.contains("JVMRuntime"));
			assertTrue("Expected output from JVMThreading monitor", output.contains("JVMThreading"));
			assertTrue("Output should have contained my custom gauge name", output.contains("My Custom Gauge"));
			assertTrue("Output should have contained my custom counter name", output.contains("My Custom Counter"));
			assertTrue("Output should have my Ratio", output.contains("DaemonPercent"));
			assertTrue("Output should have my Gauge ThreadCount", output.contains("ThreadCount"));
			assertTrue("Output should have my Gauge DaemonThreadCount", output.contains("DaemonThreadCount"));
			
        } finally {
        	PerfMon.deInit();
        }
	}
    
	private boolean hasMonitor(PerfMonConfiguration config, String monitorName) {
		for (MonitorConfig monitorConfig : config.getMonitorConfigArray()) {
			if (monitorName.equals(monitorConfig.getMonitorName())) {
				return true;
			}
		}
		return false;
	}
    
	private boolean hasSnapShot(PerfMonConfiguration config, String snapShotName) {
		for (SnapShotMonitorConfig monitorConfig : config.getSnapShotMonitorArray()) {
			if (snapShotName.equals(monitorConfig.getMonitorID().getName())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean hasThreadTrace(PerfMonConfiguration config, String threadTraceName) {
		for (String name : config.getThreadTraceConfigMap().keySet()) {
			if (threadTraceName.equals(name)) {
				return true;
			}
		}
		return false;
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
//        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreatedWhenMissing_intervalMonitor"));
//        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreatedWhenMissing_snapShotMonitor"));
//        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreated_intervalMonitor"));
//        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreated_snapShotMonitor"));
//        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreatedWhenMissing_threadTraceMonitor"));
//        newSuite.addTest(new XMLConfigurationParserTest("testDefaultAppenderCreated_threadTraceMonitor"));
        
        
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

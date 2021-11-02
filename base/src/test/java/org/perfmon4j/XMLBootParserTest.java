/*
 *	Copyright 2012 Follett Software Company 
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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class XMLBootParserTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public XMLBootParserTest(String name) {
        super(name);
    }

    
    /*----------------------------------------------------------------------------*/
    public void testParseDefaultBootConfiguration() throws Exception {
        final String XML_NO_BOOT =
            "<Perfmon4JConfig enabled='true'>" +
            "</Perfmon4JConfig>";

        final String XML_EMPTY_BOOT =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot/>" +
            "</Perfmon4JConfig>";
        
        BootConfiguration boot = XMLBootParser.parseXML(XML_NO_BOOT);
        assertNotNull("Should have a default boot configuration", boot);
        
        boot = XMLBootParser.parseXML(XML_EMPTY_BOOT);
        assertNotNull("Should have a default boot configuration", boot);
    }
    
    public void testParseBootConfigurationIgnoresNonBootTags() throws Exception {
        final String XML_WITH_NON_BOOT_TAGS =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve/>" +
            "	</boot>" +
            "	<nonBoot-root>" +
            "		<child/>" +		
            "	</nonBoot-root>" +	
            "</Perfmon4JConfig>";
        
        BootConfiguration boot = XMLBootParser.parseXML(XML_WITH_NON_BOOT_TAGS);
        assertNotNull("Should NOT have a default boot configuration", boot.getServletValveConfig());
    }
    
    public void testParseServletValve() throws Exception {
        final String XML =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve baseFilterCategory='a'" +
            "			abortTimerOnRedirect='true'" +
            "			abortTimerOnImageResponse='true'" +
            "			outputRequestAndDuration='true'" +
            "			pushClientInfoOnNDC='true'" +
            "			pushSessionAttributesOnNDC='name'" +
            "			pushURLOnNDC='true'" +
            "			abortTimerOnURLPattern='abortPattern'" +
            "			skipTimerOnURLPattern='skipPattern'" +
            "			pushCookiesOnNDC='pushCookies'" +
            "			servletPathTransformationPattern='/this/ => /that/'" +
            "		/>" +
            "	</boot>" +
            "</Perfmon4JConfig>";
        BootConfiguration boot = XMLBootParser.parseXML(XML);
        
        BootConfiguration.ServletValveConfig valveConfig = boot.getServletValveConfig();
        assertNotNull("Should have a servlet valve config", valveConfig);
        
        assertEquals("a", valveConfig.getBaseFilterCategory());
        assertTrue(valveConfig.isAbortTimerOnRedirect());
        assertTrue(valveConfig.isAbortTimerOnImageResponse());
        assertTrue(valveConfig.isOutputRequestAndDuration());
        assertTrue(valveConfig.isPushClientInfoOnNDC());
        assertTrue(valveConfig.isPushURLOnNDC());
        
        assertEquals("name",valveConfig.getPushSessionAttributesOnNDC());
        assertEquals("abortPattern",valveConfig.getAbortTimerOnURLPattern());
        assertEquals("skipPattern",valveConfig.getSkipTimerOnURLPattern());
        assertEquals("pushCookies",valveConfig.getPushCookiesOnNDC());
        assertEquals("/this/ => /that/", valveConfig.getServletPathTransformationPattern());
    }

    public void testParseServletValveBoolean() throws Exception {
        final String XML_A =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve" +
            "			abortTimerOnRedirect='true'" +
            "		/>" +
            "	</boot>" +
            "</Perfmon4JConfig>";

        BootConfiguration boot = XMLBootParser.parseXML(XML_A);
        BootConfiguration.ServletValveConfig valveConfig = boot.getServletValveConfig();

        assertTrue(valveConfig.isAbortTimerOnRedirect());
        assertFalse(valveConfig.isAbortTimerOnImageResponse());
        assertFalse(valveConfig.isOutputRequestAndDuration());
        assertFalse(valveConfig.isPushClientInfoOnNDC());

        final String XML_B =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve" +
            "			abortTimerOnImageResponse='true'" +
            "		/>" +
            "	</boot>" +
            "</Perfmon4JConfig>";

        boot = XMLBootParser.parseXML(XML_B);
        valveConfig = boot.getServletValveConfig();
        
        assertFalse(valveConfig.isAbortTimerOnRedirect());
        assertTrue(valveConfig.isAbortTimerOnImageResponse());
        assertFalse(valveConfig.isOutputRequestAndDuration());
        assertFalse(valveConfig.isPushClientInfoOnNDC());

        final String XML_C =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve" +
            "			outputRequestAndDuration='true'" +
            "		/>" +
            "	</boot>" +
            "</Perfmon4JConfig>";
        
        boot = XMLBootParser.parseXML(XML_C);
        valveConfig = boot.getServletValveConfig();
        
        assertFalse(valveConfig.isAbortTimerOnRedirect());
        assertFalse(valveConfig.isAbortTimerOnImageResponse());
        assertTrue(valveConfig.isOutputRequestAndDuration());
        assertFalse(valveConfig.isPushClientInfoOnNDC());
        
        final String XML_D =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<servletValve" +
            "			pushClientInfoOnNDC='true'" +
            "		/>" +
            "	</boot>" +
            "</Perfmon4JConfig>";

        boot = XMLBootParser.parseXML(XML_D);
        valveConfig = boot.getServletValveConfig();
        
        assertFalse(valveConfig.isAbortTimerOnRedirect());
        assertFalse(valveConfig.isAbortTimerOnImageResponse());
        assertFalse(valveConfig.isOutputRequestAndDuration());
        assertTrue(valveConfig.isPushClientInfoOnNDC());
    }
    
    
    public void testDefault() throws Exception {
        final String  XML_BOGUS =
            "<asdf bogus xml";

        BootConfiguration boot = XMLBootParser.parseXML(XML_BOGUS);
        assertNotNull("Should have a default boot configuration", boot);
        assertNull("Servlet valve config should be null by default", boot.getServletValveConfig());
    }
    
    public void testParseEmptyExceptionTrackerConfig() throws Exception {
        final String XML =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<exceptionTracker/>" +
            "	</boot>" +
            "</Perfmon4JConfig>";
        BootConfiguration boot = XMLBootParser.parseXML(XML);
        
        BootConfiguration.ExceptionTrackerConfig etConfig = boot.getExceptionTrackerConfig();
        assertNotNull("Should have an Exception Tracker Configuration", etConfig);
        assertEquals("Not very useful since no exceptions have been defined", 0, etConfig.getElements().size());
    }
    
    public void testParseExceptionWithClassName() throws Exception {
        final String XML =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<exceptionTracker>" +
            "			<exception className='java.lang.Exception'/>" +
            "		</exceptionTracker>" +
            "	</boot>" +
            "</Perfmon4JConfig>";
        BootConfiguration boot = XMLBootParser.parseXML(XML);
        
        BootConfiguration.ExceptionTrackerConfig etConfig = boot.getExceptionTrackerConfig();
        assertNotNull("Should have an Exception Tracker Configuration", etConfig);
        assertEquals("Should have one exception", 1, etConfig.getElements().size());
        
        BootConfiguration.ExceptionElement element = etConfig.getElements().iterator().next();
        
        assertEquals("expected className", "java.lang.Exception", element.getClassName());
        assertEquals("displayName defaults to className if not defined", 
        		"java.lang.Exception", element.getDisplayName());
    }

    public void testParseExceptionWithClassAndDisplayName() throws Exception {
        final String XML =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<exceptionTracker>" +
            "			<exception className='java.lang.Error' displayName='Java Error' />" +
            "		</exceptionTracker>" +
            "	</boot>" +
            "</Perfmon4JConfig>";
        BootConfiguration boot = XMLBootParser.parseXML(XML);
        
        BootConfiguration.ExceptionTrackerConfig etConfig = boot.getExceptionTrackerConfig();
        assertNotNull("Should have an Exception Tracker Configuration", etConfig);
        assertEquals("Should have one exception", 1, etConfig.getElements().size());
        
        BootConfiguration.ExceptionElement element = etConfig.getElements().iterator().next();
        
        assertEquals("expected className", "java.lang.Error", element.getClassName());
        assertEquals("expected displayName", "Java Error", element.getDisplayName());
    }
    
    public void testParseMultipleExceptionElements() throws Exception {
        final String XML =
            "<Perfmon4JConfig enabled='true'>" +
            "	<boot>" +
            "		<exceptionTracker>" +
            "			<exception className='java.lang.Error' displayName='Java Error' />" +
            "			<exception className='java.lang.Exception' />" +
            "			<exception className='java.lang.RuntimeException' />" +
            "		</exceptionTracker>" +
            "	</boot>" +
            "</Perfmon4JConfig>";
        BootConfiguration boot = XMLBootParser.parseXML(XML);
        
        BootConfiguration.ExceptionTrackerConfig etConfig = boot.getExceptionTrackerConfig();
        assertNotNull("Should have an Exception Tracker Configuration", etConfig);
        assertEquals("Expected number of elements", 3, etConfig.getElements().size());
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(XMLBootParserTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {XMLBootParserTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new XMLBootParserTest("testParseDefaultBootConfiguration"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(XMLBootParserTest.class);
        }

        return( newSuite);
    }
}

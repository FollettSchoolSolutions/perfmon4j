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
package org.perfmon4j.util;

import java.util.Properties;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTestCase;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class PropertyStringFilterTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public PropertyStringFilterTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void setUp() throws Exception {
        super.setUp();
        PerfMon.configure();
    }
    
/*----------------------------------------------------------------------------*/    
    public void tearDown() throws Exception {
        PerfMon.deInit();
        super.tearDown();
    }
    
/*----------------------------------------------------------------------------*/    
    public void testSimple() throws Exception {
        ConfigurationProperties props = new ConfigurationProperties();
        PropertyStringFilter filter = new PropertyStringFilter(props);
        
        final String sourceString = "abc,${last.name},${first.name}.xyz";
        
        assertEquals("If matching properties are not set... do nothing",
            sourceString, filter.doFilter(sourceString));
        
        props.setProperty("last.name", "Doe");
        assertEquals("Mathing property should be substituted",
            "abc,Doe,${first.name}.xyz", filter.doFilter(sourceString));
       
        
        props.setProperty("first.name", "John");
        assertEquals("Mathing property should be substituted",
            "abc,Doe,John.xyz", filter.doFilter(sourceString));
    }
    
    public void testKeyCanContainPunctuation() throws Exception {
        ConfigurationProperties props = new ConfigurationProperties();
        PropertyStringFilter filter = new PropertyStringFilter(props);
        
        // The key can contain any character except '$', '{', '}', and ':'.
        final String keyContainingWideRangeOfCharacter = "~My~Crazy.Key[]Value.to,use including\twhitespace ";
        
        final String sourceString = "abc.${" + keyContainingWideRangeOfCharacter + "}.xyz";
        
        props.setProperty(keyContainingWideRangeOfCharacter, "REPLACEMENT");
        assertEquals("Mathing property should be substituted",
            "abc.REPLACEMENT.xyz", filter.doFilter(sourceString));
    }    
    
 /*----------------------------------------------------------------------------*/    
    public void testPropertyEmbeddedInProperty() throws Exception {
        ConfigurationProperties props = new ConfigurationProperties();
        PropertyStringFilter filter = new PropertyStringFilter(props);
        
        final String sourceString = "${full.name}";
        
        props.setProperty("full.name", "${first.name} ${last.name}");
        props.setProperty("last.name", "Doe");
        props.setProperty("first.name", "John");

        assertEquals("Mathing property should be substituted",
            "John Doe", filter.doFilter(sourceString));
    }
   
 /*----------------------------------------------------------------------------*/    
    public void testPreventRecursion() throws Exception {
        ConfigurationProperties props = new ConfigurationProperties();
        PropertyStringFilter filter = new PropertyStringFilter(props);
        
        final String sourceString = "${full.name}";
        
        props.setProperty("full.name", "${complete.name} cant't expand");
        props.setProperty("complete.name", "${full.name}");

        try {
            assertEquals("Can't expand when we have recursion", "${full.name} cant't expand", 
                filter.doFilter(sourceString));
            
        } catch (StackOverflowError se) {
            fail("Should not allow stack overflow");
        }
    }
    
    
    public void testSystemPropertyIsPreferred() throws Exception {
    	Random r = new Random();
    	
    	final String envKey = Long.toBinaryString(r.nextLong());
    	final String envValue = Long.toBinaryString(r.nextLong());
    	final String systemPropertyValue = "System Property Override";
    	final String sourceString = "${" + envKey + "}";
    	
    	
    	Properties envProps = new Properties();
    	Properties systemProps = new Properties();
    	ConfigurationProperties configurationProperties = new ConfigurationProperties(envProps, systemProps);
    	
    	envProps.setProperty(envKey, envValue);
    	
    	PropertyStringFilter filter = new PropertyStringFilter(configurationProperties, true);

    	// Since envKey is only set as an environment variable that should be returned
    	assertEquals("Should find mock environment variable", envValue, filter.doFilter(sourceString));
    	
    	// Now use envKey to set a system property.  This should now be preferred and returned.
    	systemProps.setProperty(envKey, systemPropertyValue);
    	
    	assertEquals("System property should override environment property", systemPropertyValue, filter.doFilter(sourceString));
    }

    
    /**
     * For backwards compatibility if someone was using a system property that fit 
     * the pattern ${env.KEY_NAME} we should still retrieve this value (if KEY_NAME is
     * not found in the Environment or System properties)
     * @throws Exception
     */
    public void testForceBackwardCompatibilityWithEnvPrefix() throws Exception {
    	Random r = new Random();
    	
    	final String envKey = "env." + Long.toBinaryString(r.nextLong());
    	final String envValue = Long.toBinaryString(r.nextLong());
    	final String sourceString = "${" + envKey + "}";
    	
    	try {
        	PropertyStringFilter filter = new PropertyStringFilter(true);
        	// Set the system property with the "env.XXX" pattern
        	System.setProperty(envKey, envValue);
        	
        	assertEquals("Should return system property based on raw key", envValue, filter.doFilter(sourceString));
    	} finally {
    		System.getProperties().remove(envKey);
    	}
    }
    
	public void testUseDefaultValueWhenProvided() throws Exception {
		try {
			PropertyStringFilter filter = new PropertyStringFilter();
			assertEquals("System property is not set should return default value", "defaultValue",
					filter.doFilter("${testProp:defaultValue}"));

			// Now set the system property.
			System.getProperties().setProperty("testProp", "notTheDefaultValue");
			assertEquals("When system property is set the value of the property should be returned",
					"notTheDefaultValue", filter.doFilter("${testProp:defaultValue}"));

		} finally {
			System.getProperties().remove("testProp");
		}
	}
    
    /*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {PropertyStringFilterTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new MedianCalculatorTest("testWithinLimit"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PropertyStringFilterTest.class);
        }

        return( newSuite);
    }
}

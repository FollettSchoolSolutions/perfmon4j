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
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;

public class PropertyStringFilterTest extends TestCase {
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
        Properties props = new Properties();
        
        final String sourceString = "abc,${last.name},${first.name}.xyz";
        
        assertEquals("If matching properties are not set... do nothing",
            sourceString, PropertyStringFilter.filter(props, sourceString));
        
        props.setProperty("last.name", "Doe");
        assertEquals("Mathing property should be substituted",
            "abc,Doe,${first.name}.xyz", PropertyStringFilter.filter(props, sourceString));
       
        
        props.setProperty("first.name", "John");
        assertEquals("Mathing property should be substituted",
            "abc,Doe,John.xyz", PropertyStringFilter.filter(props, sourceString));
    }
    
 /*----------------------------------------------------------------------------*/    
    public void testPropertyEmbeddedInProperty() throws Exception {
        Properties props = new Properties();
        
        final String sourceString = "${full.name}";
        
        props.setProperty("full.name", "${first.name} ${last.name}");
        props.setProperty("last.name", "Doe");
        props.setProperty("first.name", "John");

        assertEquals("Mathing property should be substituted",
            "John Doe", PropertyStringFilter.filter(props, sourceString));
    }
   
 /*----------------------------------------------------------------------------*/    
    public void testPreventRecursion() throws Exception {
        Properties props = new Properties();
        
        final String sourceString = "${full.name}";
        
        props.setProperty("full.name", "${complete.name} cant't expand");
        props.setProperty("complete.name", "${full.name}");

        try {
            assertEquals("Can't expand when we have recursion", "${full.name} cant't expand", 
                PropertyStringFilter.filter(props, sourceString));
            
        } catch (StackOverflowError se) {
            fail("Should not allow stack overflow");
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

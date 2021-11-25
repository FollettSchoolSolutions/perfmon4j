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

import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.ThresholdCalculator;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class AppenderTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public AppenderTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/
    public void testAppenderClassNameHashCode() throws Exception {
        Appender.AppenderID a1 = AppenderID.getAppenderID("a");
        Appender.AppenderID a2 = AppenderID.getAppenderID("a");
        Appender.AppenderID b = AppenderID.getAppenderID("b");
        
        assertTrue("Hash codes for items with same className should match", a1.hashCode() == a2.hashCode());
        assertTrue("AppenderID with same className should be equal", a1.equals(a2));
        assertTrue("Hash codes should be different", a1.hashCode() != b.hashCode());
    }
    
    
/*----------------------------------------------------------------------------*/
    public void testAppenderIntervalHashCode() throws Exception {
        Appender.AppenderID a1 = AppenderID.getAppenderID("a", 1);
        Appender.AppenderID a2 = AppenderID.getAppenderID("a", 1);
        Appender.AppenderID b = AppenderID.getAppenderID("a", 2);
        
        assertTrue("Hash codes for items with same className and interval should match", a1.hashCode() == a2.hashCode());
        assertTrue("AppenderID with same className and interval should be equal", a1.equals(a2));
        assertTrue("Hash codes with same class name but different intervals should not match", a1.hashCode() != b.hashCode());
    }

/*----------------------------------------------------------------------------*/
    public void testAppenderAttributesHashCode() throws Exception {
        Properties attrA1 = new Properties();
        Properties attrA2 = new Properties();
        Properties attrB = new Properties();
        
        // Both attrA1 and attrA2 will have the same properties....
        // only in different orders...
        attrA1.setProperty("A", "B");
        attrA1.setProperty("C", "D");
        attrA1.setProperty("E", "F");
        
        attrA2.setProperty("C", "D");
        attrA2.setProperty("A", "B");
        attrA2.setProperty("E", "F");
        
        // attrB will add a property
        attrB.setProperty("A", "B");
        attrB.setProperty("C", "D");
        attrB.setProperty("E", "F");
        attrB.setProperty("G", "H");
        
        Appender.AppenderID a1 = AppenderID.getAppenderID("a", 1, attrA1);
        Appender.AppenderID a2 = AppenderID.getAppenderID("a", 1, attrA2);
        Appender.AppenderID b = AppenderID.getAppenderID("a", 1, attrB);
        
        assertTrue("Hash codes for items with same className, interval and attributes should match", a1.hashCode() == a2.hashCode());
        assertTrue("AppenderID with same className, interval and attributes should be equal", a1.equals(a2));
        assertTrue("Hash codes with same class name and interval but different attributes should not match", a1.hashCode() != b.hashCode());
    }
    
/*----------------------------------------------------------------------------*/
    /**
     * If a threshold calculator defined by the monitor, if it exists is preferred.
     * If not we use the threshold calculator defined by the appender
     * If neither is defined null should be returned.
     * 
     * IMPORTANT - When a threshold calculator is returned it must always
     * be a clone of the existing calculator. 
     *  
     * @throws Exception
     */
    public void testGetPreferredThresholdCalculator() throws Exception {
    	ThresholdCalculator preferred, definedByMonitor, definedByAppender;
    	preferred = definedByMonitor = definedByAppender = null;

    	// Neither Appender or Monitor have calculator
    	preferred = Appender.getPreferredThresholdCalculator(definedByMonitor, definedByAppender);
    	assertNull("No calculator defined by Monitor or Appender", preferred);
    	
    	// Appender has calculator but not Monitor.
    	definedByAppender = new ThresholdCalculator("1 second");
    	preferred = Appender.getPreferredThresholdCalculator(definedByMonitor, definedByAppender);
    	assertFalse("Should be a clone of Appender calculator", definedByAppender == preferred);
    	assertEquals("Should have same definition as Appender's calculator", 1000L, preferred.getThresholdMillis()[0]);
    			
    	// Both Monitor and Appender have calculators
    	definedByMonitor = new ThresholdCalculator("2 seconds");
    	preferred = Appender.getPreferredThresholdCalculator(definedByMonitor, definedByAppender);
    	assertFalse("Should be a clone of Monitor's calculator", definedByMonitor == preferred);
    	assertEquals("Should have same definition as Monitor's calculator", 2000L, preferred.getThresholdMillis()[0]);
    			
    	// Monitor has calculator, but not Appender
    	definedByAppender = null;
    	preferred = Appender.getPreferredThresholdCalculator(definedByMonitor, definedByAppender);
    	assertFalse("Should be a clone of Monitor's calculator", definedByMonitor == preferred);
    	assertEquals("Should have same definition as Monitor's calculator", 2000L, preferred.getThresholdMillis()[0]);
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(AppenderTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {AppenderTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new PerfMonConfigurationTest("testGetAppendersForMonitor"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(AppenderTest.class);
        }

        return( newSuite);
    }
}

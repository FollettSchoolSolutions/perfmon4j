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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.util.ThresholdCalculator.ThresholdResult;

public class ThresholdCalculatorTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public ThresholdCalculatorTest(String name) {
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
    public void testClone() {
        ThresholdCalculator calc = new ThresholdCalculator(new long[]{100, 200, 300});
        calc.putValue(200);
        
        ThresholdCalculator cloned = calc.clone();
        
        assertEquals("number of thresholds", 3, cloned.getThresholdMillis().length);
        assertEquals(100, cloned.getResult(100).getThreshold());
        assertEquals(200, cloned.getResult(200).getThreshold());
        assertEquals(300, cloned.getResult(300).getThreshold());
        
        assertTrue("Should not do a shallow copy", cloned.getResult(100) != calc.getResult(100));
   
        assertEquals("Original has some counter",1, calc.getResult(100).getCountOverThreshold());
        assertEquals("Clone does not copy values", 0, cloned.getResult(100).getCountOverThreshold());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testConstructorWithString() throws Exception {
        ThresholdCalculator calc = new ThresholdCalculator("5 minutes, 3 seconds, 10 ms, 7 hours");

        long results[] = calc.getThresholdMillis();
        assertEquals("result length", 4, results.length);
        
        assertEquals("Should be in order low to high", 10, results[0]);
        assertEquals("Should be in order low to high", 3000, results[1]);
        assertEquals("Should be in order low to high", 5 * 60 * 1000, results[2]);
        assertEquals("Should be in order low to high", 7 * 60 * 60 * 1000, results[3]);
    }
    
/*----------------------------------------------------------------------------*/    
    public void testSimpleCalculator() throws Exception {
        ThresholdCalculator calc = new ThresholdCalculator(new long[]{4, 2, 6, 8});

        long results[] = calc.getThresholdMillis();
        assertEquals("result length", 4, results.length);
        
        assertEquals("Should be in order low to high", 2, results[0]);
        assertEquals("Should be in order low to high", 4, results[1]);
        assertEquals("Should be in order low to high", 6, results[2]);
        assertEquals("Should be in order low to high", 8, results[3]);
        
        calc.putValue(2);
        calc.putValue(2);
        calc.putValue(2);
        calc.putValue(4);
        calc.putValue(4);
        calc.putValue(4);
        calc.putValue(6);
        calc.putValue(6);
        calc.putValue(6);
        calc.putValue(8);

        ThresholdResult result = calc.getResult(1);
        assertNull("Did not define a threshold result for 1 millisecond", result);
        
        result = calc.getResult(2);
        assertNotNull("Should have a result for 2 milliseconds", result);
        assertEquals("Total samples", 10, result.getTotalCount());
        assertEquals("Samples over threshold", 7, result.getCountOverThreshold());
        
        result = calc.getResult(4);
        assertNotNull("Should have a result for 4 milliseconds", result);
        assertEquals("Total samples", 10, result.getTotalCount());
        assertEquals("Samples over threshold", 4, result.getCountOverThreshold());
        
        result = calc.getResult(6);
        assertNotNull("Should have a result for 6 milliseconds", result);
        assertEquals("Total samples", 10, result.getTotalCount());
        assertEquals("Samples over threshold", 1, result.getCountOverThreshold());

        result = calc.getResult(8);
        assertNotNull("Should have a result for 8 milliseconds", result);
        assertEquals("Total samples", 10, result.getTotalCount());
        assertEquals("Samples over threshold", 0, result.getCountOverThreshold());

        result = calc.getResult(10);
        assertNull("Did not define a threshold result for 10 millisecond", result);
    }

/*----------------------------------------------------------------------------*/    
    public void testThresholdResultToString() throws Exception {
        final long minute = 60 * 1000;
        ThresholdCalculator calc = new ThresholdCalculator(new long[]{2000, minute});
        
        calc.putValue(2000);
        calc.putValue(2000);
        calc.putValue(2000);
        calc.putValue(2000);
        calc.putValue(2000);
        calc.putValue(2000);
        calc.putValue(4000);
        calc.putValue(4000);
        calc.putValue(4000);

        ThresholdResult result1 = calc.getResult(2000);
        ThresholdResult result2 = calc.getResult(minute);
        assertEquals("Expected pretty toString", "33.33% > 2 seconds", result1.toString());
        assertEquals("Expected pretty toString", "0.00% > 1 minute", result2.toString());
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {ThresholdCalculatorTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new ThresholdCalculatorTest("testOverLimit"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(ThresholdCalculatorTest.class);
        }

        return( newSuite);
    }
}

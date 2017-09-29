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

import java.util.Random;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;

public class MedianCalculatorTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public MedianCalculatorTest(String name) {
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
    public void testSimpleWithOddNumberOfEntrys() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 1);

        calc.putValue(0);
        calc.putValue(1);
        calc.putValue(1);
        calc.putValue(2);
        calc.putValue(3);
        calc.putValue(3);
        calc.putValue(1000);
        
        assertEquals("Median", 2, calc.getMedian().getResult().longValue());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testSimpleWithEvenNumberOfEntrys() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 1);

        calc.putValue(1);
        calc.putValue(2);
        calc.putValue(3);
        calc.putValue(4);
        calc.putValue(5);
        calc.putValue(6);
        calc.putValue(7);
        calc.putValue(8);
        
        double median = calc.getMedian().getResult().doubleValue();
        assertTrue("Median expected 4.5 but was: " + median, 4.5 == median);
    }

/*----------------------------------------------------------------------------*/    
    public void testSimpleWithBatchOfEven() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 1);

        calc.putValue(4);
        calc.putValue(4);
        calc.putValue(4);
        calc.putValue(5);
        
        double median = calc.getMedian().getResult().doubleValue();
        assertTrue("Median expected 4 but was: " + median, 4 == median);
    }
   
/*----------------------------------------------------------------------------*/    
    public void testSingleValueBoundry() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 1);

        calc.putValue(3);
        
        double median = calc.getMedian().getResult().doubleValue();
        assertTrue("Median expected 3 but was: " + median, 3 == median);
    }

/*----------------------------------------------------------------------------*/    
    public void testTwoValueBoundry() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 1);

        calc.putValue(3);
        calc.putValue(4);
        
        double median = calc.getMedian().getResult().doubleValue();
        assertTrue("Median expected 3.5 but was: " + median, 3.5 == median);
    }

/*----------------------------------------------------------------------------*/    
    public void testNullBoundry() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 1);
        
        assertNull("Should return null value to indicate no median", calc.getMedian().getResult());
        assertEquals("String should map null to NA", "NA", calc.getMedianAsString());
    }

/*----------------------------------------------------------------------------*/    
    public void testOverLimit() throws Exception {
        MedianCalculator calc = new MedianCalculator(2, 1);
        
        calc.putValue(1);
        calc.putValue(1);
        calc.putValue(2);
        calc.putValue(3);
        calc.putValue(3);
        calc.putValue(3);
        calc.putValue(3);
        calc.putValue(3);
        calc.putValue(3);
        
        MedianCalculator.MedianResult result = calc.getMedian();
        assertEquals("Median should be designated as overflow", MedianCalculator.OVERFLOW_HIGH, 
            result.getOverflowFlag());
        assertEquals("Median should be designated as overflow", new Double(2.0), 
            result.getResult());
        
        
        assertEquals("Overflow must return >= estimate", ">= 2.0", calc.getMedianAsString());
    }


/*----------------------------------------------------------------------------*/    
    public void testOverFlowFlagOnResult() throws Exception {
        // First test without overflow...
        MedianCalculator calc = new MedianCalculator(2, 1);
        calc.putValue(1);
        calc.putValue(2);
        
        assertEquals("Should not have overflow", MedianCalculator.OVERFLOW_NONE, 
            calc.getMedian().getOverflowFlag());
        
        // Now we have an overflow at the top..
        calc.putValue(3);
        assertEquals("Should not have overflow", MedianCalculator.OVERFLOW_HIGH, 
            calc.getMedian().getOverflowFlag());

        // Should be back to no overflow...
        calc.putValue(1);
        assertEquals("Should not have overflow", MedianCalculator.OVERFLOW_NONE, 
            calc.getMedian().getOverflowFlag());
        
        // Now we have an overflow at bottom..
        calc.putValue(0);
        assertEquals("Should not have overflow", MedianCalculator.OVERFLOW_LOW, 
            calc.getMedian().getOverflowFlag());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testUsingFactor() throws Exception {
        MedianCalculator calc = new MedianCalculator(10, 100);
        
        calc.putValue(151); // 200
        calc.putValue(145); // 100
        calc.putValue(223); // 200
        calc.putValue(375); // 400
        calc.putValue(512); // 500
        
        assertEquals("Will be rounded to the factor", "200.0", 
            calc.getMedianAsString());
    }
    

    
/*----------------------------------------------------------------------------*/    
    public void testConstructUsingString() throws Exception {
        // To allow the value to be set via a bean property
        // we must have a constructor that takes a string.
        
        // Empty String should set to defaults...
        MedianCalculator calc = new MedianCalculator("");
        
        assertEquals(MedianCalculator.DEFAULT_MAX_ELEMENTS, calc.getMaxElements());
        assertEquals(MedianCalculator.DEFAULT_FACTOR, calc.getFactor());

        // NULL should set defaults
        calc = new MedianCalculator(null);
        
        assertEquals(MedianCalculator.DEFAULT_MAX_ELEMENTS, calc.getMaxElements());
        assertEquals(MedianCalculator.DEFAULT_FACTOR, calc.getFactor());

        // Garbage should set defaults
        /** todo It would probably be nicer to give some sort of warning! **/
        calc = new MedianCalculator("this is junk");
        
        assertEquals(MedianCalculator.DEFAULT_MAX_ELEMENTS, calc.getMaxElements());
        assertEquals(MedianCalculator.DEFAULT_FACTOR, calc.getFactor());
        
        // Should be able to override maxElements
        calc = new MedianCalculator("maxElements=10");
        
        assertEquals(10, calc.getMaxElements());
        assertEquals(MedianCalculator.DEFAULT_FACTOR, calc.getFactor());
       
        // Should be able to override maxElements
        calc = new MedianCalculator("factor=250");
        
        assertEquals(MedianCalculator.DEFAULT_MAX_ELEMENTS, calc.getMaxElements());
        assertEquals(250, calc.getFactor());
        
        // Should be able to override maxElements and factor
        calc = new MedianCalculator("maxElements=10 factor=250");
        
        assertEquals(10, calc.getMaxElements());
        assertEquals(250, calc.getFactor());
        
        // Any othre garbage is ignored...
        calc = new MedianCalculator("maxElements=10,!sdfasd this is garbage, factor=250");
        
        assertEquals(10, calc.getMaxElements());
        assertEquals(250, calc.getFactor());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testWithinLimit() throws Exception {
        MedianCalculator calc = new MedianCalculator(3, 1);

        calc.putValue(2);
        calc.putValue(2);
        
        calc.putValue(3);
        calc.putValue(3);

        calc.putValue(4);
        calc.putValue(4);
        
        calc.putValue(1);
        calc.putValue(1);
        
        calc.putValue(5);
        calc.putValue(5);
        
        assertEquals("No need for an estimate", "3.0", calc.getMedianAsString());
        
        calc.putValue(5);
        calc.putValue(5);
        
        assertEquals("Still do not need an estimate", "3.5", calc.getMedianAsString());

        calc.putValue(5);
        assertEquals("Now we need an estimate", ">= 4.0", calc.getMedianAsString());

        calc.putValue(0);
        assertEquals("Don't need an estimate anymore", "3.5", calc.getMedianAsString());
    }
    
/*----------------------------------------------------------------------------*/
    public void testPerformance() throws Exception {
        MedianCalculator calc = new MedianCalculator(MedianCalculator.DEFAULT_MAX_ELEMENTS, 10);
        Random random = new Random();
        
        System.gc();
        int count = 0;
        long start = System.currentTimeMillis();
        while (count++ < 500000) {
            calc.putValue(random.nextInt(999999));
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Median: " + calc.getMedianAsString());
        
        // This is far from a perfect test but we should be able to add 500,000 per second
        assertTrue("Expected to do " + (--count) + " in less than 1 second - Actual duration: " + duration,
            duration < 1000);
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {MedianCalculatorTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new MedianCalculatorTest("testPerformance"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(MedianCalculatorTest.class);
        }

        return( newSuite);
    }
}

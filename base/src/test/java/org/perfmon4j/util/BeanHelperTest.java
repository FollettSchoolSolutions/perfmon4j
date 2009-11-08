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

public class BeanHelperTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public BeanHelperTest(String name) {
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
    public void testSetString() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "value", "bogus");
        assertEquals("to.value", "bogus", to.value);
    }

    
/*----------------------------------------------------------------------------*/
    public void testSetInteger() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "integerValue", "100");
        assertEquals("to.integerValue", new Integer(100), to.integerValue);
    }

/*----------------------------------------------------------------------------*/
    public void testSetNativeInt() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "intValue", "100");
        assertEquals(100, to.intValue);
    }
    
/*----------------------------------------------------------------------------*/
    public void testSetNativeLong() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "longValue", "100");
        assertEquals(100, to.longValue);
    }


/*----------------------------------------------------------------------------*/
    public void testSetNativeFloat() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "floatValue", "100.1");
        assertEquals(new Float(100.1), new Float(to.floatValue));
    }
    
/*----------------------------------------------------------------------------*/
    public void testSetNativeDouble() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "doubleValue", "100.1");
        assertEquals(new Double(100.1), new Double(to.doubleValue));
    }

/*----------------------------------------------------------------------------*/
    public void testSetNativeChar() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "charValue", "A");
        assertEquals('A', to.charValue);
    }
    
/*----------------------------------------------------------------------------*/
    public void testSetNativeShort() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "shortValue", "100");
        assertEquals(100, to.shortValue);
    }

/*----------------------------------------------------------------------------*/
    public void testSetNativeByte() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "byteValue", "8");
        assertEquals(8, to.byteValue);
    }

/*----------------------------------------------------------------------------*/
    public void testSetNativeBoolean() throws Exception {
        TestObject to = new TestObject();
        
        BeanHelper.setValue(to, "booleanValue", "true");
        assertTrue(to.booleanValue);
    }
    
/*----------------------------------------------------------------------------*/
    private static class TestObject {
        private String value;
        private Integer integerValue;
        
        // Primitives
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
        private char charValue;
        private short shortValue;
        private byte byteValue;
        private boolean booleanValue;
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public void setIntegerValue(Integer integerValue) {
            this.integerValue = integerValue;
        }
        
        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }
        
        public void setLongValue(long longValue) {
            this.longValue = longValue;
        }
    
        public void setDoubleValue(double doubleValue) {
            this.doubleValue = doubleValue;
        }
        
        public void setFloatValue(float floatValue) {
            this.floatValue = floatValue;
        }

        public void setCharValue(char charValue) {
            this.charValue = charValue;
        }
        
        public void setShortValue(short shortValue) {
            this.shortValue = shortValue;
        }
        
        public void setBooleanValue(boolean booleanValue) {
            this.booleanValue = booleanValue;
        }
        
        public void setByteValue(byte byteValue) {
            this.byteValue = byteValue;
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {BeanHelperTest.class.getName()};

        TestRunner.main(testCaseName);
    }
    

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new BeanHelperTest("testSetNativeLong"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(BeanHelperTest.class);
        }

        return( newSuite);
    }
}

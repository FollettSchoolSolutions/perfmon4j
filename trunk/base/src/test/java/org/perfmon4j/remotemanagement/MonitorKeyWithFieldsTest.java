/*
 *	Copyright 2011 Follett Software Company 
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

package org.perfmon4j.remotemanagement;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.perfmon4j.remotemanagement.intf.FieldKey;


public class MonitorKeyWithFieldsTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";
    
/*----------------------------------------------------------------------------*/
    public MonitorKeyWithFieldsTest(String name) {
        super(name);
    }

    public void testHashCodes() throws Exception {
    	FieldKey keyA = FieldKey.parse("INTERVAL(name=org.perfmon4j):FIELD(name=average;type=LONG)");
    	FieldKey keyB = FieldKey.parse("INTERVAL(name=org.perfmon4j):FIELD(name=max;type=LONG)");
    	
    	// Change the order of the fields, still should be considered equals...
    	MonitorKeyWithFields mA[] = MonitorKeyWithFields.groupFields(new FieldKey[]{keyA, keyB}); 
    	MonitorKeyWithFields mB[] = MonitorKeyWithFields.groupFields(new FieldKey[]{keyB, keyA}); 
 

    	assertEquals("Hash codes match regardless of field order",
    			mA[0].hashCode(), mB[0].hashCode());
    }

    public void testEquals() throws Exception {
    	FieldKey keyA = FieldKey.parse("INTERVAL(name=org.perfmon4j):FIELD(name=average;type=LONG)");
    	FieldKey keyB = FieldKey.parse("INTERVAL(name=org.perfmon4j):FIELD(name=max;type=LONG)");
    	
    	// Change the order of the fields, still should be considered equals...
    	MonitorKeyWithFields mA[] = MonitorKeyWithFields.groupFields(new FieldKey[]{keyA, keyB}); 
    	MonitorKeyWithFields mB[] = MonitorKeyWithFields.groupFields(new FieldKey[]{keyB, keyA}); 
 

    	assertTrue("Should be equal regardless of field order",
    			mA[0].equals(mB[0]));
    }
        
    public void testGrouping() throws Exception {
    	FieldKey keyA = FieldKey.parse("INTERVAL(name=org.perfmon4j):FIELD(name=average;type=LONG)");
    	FieldKey keyB = FieldKey.parse("INTERVAL(name=org.perfmon4j.sys):FIELD(name=max;type=LONG)");
    	FieldKey keyC = FieldKey.parse("INTERVAL(name=org.perfmon4j):FIELD(name=max;type=LONG)");
    	
    	// Change the order of the fields, still should be considered equals...
    	MonitorKeyWithFields mA[] = MonitorKeyWithFields.groupFields(new FieldKey[]{keyA, keyB, keyC}); 
    	assertEquals("mA[].length", 2, mA.length);
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
//        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {MonitorKeyWithFieldsTest.class.getName()};

        TestRunner.main(testCaseName);
    }
    
/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new SessionManagerTest("testLifeCycleTimeout"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(MonitorKeyWithFieldsTest.class);
        }

        return( newSuite);
    }
}

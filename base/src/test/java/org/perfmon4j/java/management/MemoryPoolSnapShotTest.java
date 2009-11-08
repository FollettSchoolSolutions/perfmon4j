/*
 *	Copyright 2008, 2009 Follett Software Company 
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
package org.perfmon4j.java.management;

import java.lang.management.MemoryPoolMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;

public class MemoryPoolSnapShotTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public MemoryPoolSnapShotTest(String name) {
        super(name);
    }
    
/*----------------------------------------------------------------------------*/    
    public void testGetMemoryPools() throws Exception {
    	MemoryPoolMXBean mpBeans[] = MemoryPoolSnapShot.getAllMemoryPools();
    	assertNotNull(mpBeans);
    	assertTrue("should have at least 1 memory pool", mpBeans.length >= 1);

    	// Now try to get a single garbage collector by name...
    	MemoryPoolMXBean bean = MemoryPoolSnapShot.getMemoryPool("SHOULD NOT EXIST");
    	assertNull("Should not exist", bean);
    	
    	String mpName = mpBeans[0].getName();
    	bean = MemoryPoolSnapShot.getMemoryPool(mpName);
    	assertNotNull("Should exist", bean);
    	
    	assertEquals("poolName should match", mpName, bean.getName());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testValidateDataInterface() throws Exception {
    	Bundle bundle = SnapShotGenerator.generateBundle(MemoryPoolSnapShot.class);
    	SnapShotData data = bundle.newSnapShotData();
    	
    	assertTrue("Should implement interface", data instanceof MemoryPoolSnapShot.MemoryPoolData);
    	
    	MemoryPoolSnapShot.MemoryPoolData d = (MemoryPoolSnapShot.MemoryPoolData)data;
    	Method methods[] = MemoryPoolSnapShot.MemoryPoolData.class.getDeclaredMethods();
    	for (int i = 0; i < methods.length; i++) {
    		try {
    			methods[i].invoke(d, new Object[]{});
    		} catch (InvocationTargetException it) {
    			fail("Invalid method on data interface.  method name: " + methods[i].getName());
    		}
		}
    }
  
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(MemoryPoolSnapShotTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {MemoryPoolSnapShotTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new GarbageCollectorSnapShotTest("testGetGarbageCollectors"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(MemoryPoolSnapShotTest.class);
        }

        return( newSuite);
    }
}

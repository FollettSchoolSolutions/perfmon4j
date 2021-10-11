/*
 *	Copyright 2021 Follett School Solutions, LLC 
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
package org.perfmon4j.util;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTestCase;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class ServletPathTransformerTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public ServletPathTransformerTest(String name) {
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
    public void testBuildWithEmptyString() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("");
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());
    }

    /*----------------------------------------------------------------------------*/    
    public void testBuildWithNull() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer(null);
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testBuildWithGarbage() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("=>aike7b,aa*sd8,=>asdo933,98sr");
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());
    }

    /*----------------------------------------------------------------------------*/    
    public void testLiteralTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/a/b/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /a/b/ with /", "/api/rest/checkout", 
    			t.transform("/api/a/b/rest/a/b/checkout"));
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testAstriskTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/*/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/destiny/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/destiny/rest/checkout"));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testTrailingAstriskTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/saas*/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/saas_001/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/saas_001/rest/checkout"));
    	assertEquals("Should NOT replace /context/destiny/ with /", "/api/context/destiny/rest/checkout", 
    			t.transform("/api/context/destiny/rest/checkout"));
    }

    /*----------------------------------------------------------------------------*/    
    public void testLeadingAstriskTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/*001/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/saas_001/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/saas_001/rest/checkout"));
    	assertEquals("Should NOT replace /context/destiny/ with /", "/api/context/destiny/rest/checkout", 
    			t.transform("/api/context/destiny/rest/checkout"));
    }

    /*----------------------------------------------------------------------------*/    
    public void testEmbeddedAstriskTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/s*1/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/saas_0000001/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/saas_0000001/rest/checkout"));
    	assertEquals("Should NOT replace /context/saas_0000002/ with /", "/api/context/saas_0000002/rest/checkout", 
    			t.transform("/api/context/saas_0000002/rest/checkout"));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testEmbeddedQuestionMarkTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/saas?001/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/saas_001/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/saas_001/rest/checkout"));
    	assertEquals("Should NOT replace /context/saas_002/ with /", "/api/context/saas_002/rest/checkout", 
    			t.transform("/api/context/saas_002/rest/checkout"));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testAstriskAndQuestionMarkTransformation() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/*_00?/ => /");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/saas_001/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/saas_001/rest/checkout"));
    	assertEquals("Should NOT replace /context/saas_0001/ with /", "/api/context/saas_0001/rest/checkout", 
    			t.transform("/api/context/saas_0001/rest/checkout"));
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testMultipleTransformations() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/saas_001/ => /,/context/saas_002/ => /different/");
    	
    	assertEquals("Num transformations expected", 2, t.getNumTransforms());
    	
    	assertEquals("Should replace /context/saas_001/ with /", "/api/rest/checkout", 
    			t.transform("/api/context/saas_001/rest/checkout"));
    	assertEquals("Should replace /context/saas_002/ with /", "/api/different/rest/checkout", 
    			t.transform("/api/context/saas_002/rest/checkout"));
    	assertEquals("Should NOT replace /context/saas_003/ with /", "/api/context/saas_003/rest/checkout", 
    			t.transform("/api/context/saas_003/rest/checkout"));
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        String[] testCaseName = {ServletPathTransformerTest.class.getName()};

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
            newSuite = new TestSuite(ServletPathTransformerTest.class);
        }

        return( newSuite);
    }
}

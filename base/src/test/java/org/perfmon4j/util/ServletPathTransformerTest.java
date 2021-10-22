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
    public void testBuildWithEmptyString() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("");
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());
    	
    	assertEquals("WebRequest.circulation.checkout_do", t.transformToCategory("/circulation/checkout.do"));
    }

    /*----------------------------------------------------------------------------*/    
    public void testBuildWithNull() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer(null);
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());

    	assertEquals("WebRequest.circulation.checkout_do", t.transformToCategory("/circulation/checkout.do"));
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testBuildWithGarbage() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("=>aike7b,aa*sd8,=>asdo933,98sr");
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());

    	assertEquals("WebRequest.circulation.checkout_do", t.transformToCategory("/circulation/checkout.do"));
    }

    /*----------------------------------------------------------------------------*/    
    public void testBuildBlankString() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("   ");
    	
    	assertNotNull("Should have a transformer", t);
    	assertEquals("Should not have any transformations", 0, t.getNumTransforms());

    	assertEquals("WebRequest.circulation.checkout_do", t.transformToCategory("/circulation/checkout.do"));
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

    /**
     * The ** wild card includes any path character, like the *, but also includes the path separator /
     * @throws Exception
     */
    public void testReplaceWithDoubleWildcard() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/images/**/image.png => /static, /passthrough/*/image.png => /static");
    	
    	assertEquals("Num transformations expected", 2, t.getNumTransforms());
    	
    	assertEquals("** should replace multiple characters includes the path character", "/static", 
    			t.transform("/images/context/saas_001/image.png"));
    	assertEquals("* should NOT include the path character", "/passthrough/context/saas_001/image.png", 
    			t.transform("/passthrough/context/saas_001/image.png"));
    }
    
    public void testDoubleWildcardWithSingleWildCard() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/images/**/*.png => /static");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Make sure path matches the end /*.png", "/static", 
    			t.transform("/images/context/saas_001/image.png"));
    	assertEquals("* should NOT match because it does not end in /*.png", "/images/context/saas_001/image.gif", 
    			t.transform("/images/context/saas_001/image.gif"));
    }
    
    /**
     * You can separate multiple patterns with a | character
     * @throws Exception
     */
    public void testMultiplePatterns() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("**.png|**.jpg|**.jpeg => /static");
    	
    	assertEquals("Num transformations expected", 1, t.getNumTransforms());
    	
    	assertEquals("Should match any path ending in.png", "/static", 
    			t.transform("/images/context/saas_001/image.png"));
    	assertEquals("Should match any path ending in.jpg", "/static", 
    			t.transform("/images/context/saas_001/image.jpg"));
    	assertEquals("Should match any path ending in.jpeg", "/static", 
    			t.transform("/images/context/saas_001/image.jpeg"));

    	assertEquals("Does not match any of the extensions, should no match", "/style/default.css", 
    			t.transform("/style/default.css"));

    }
    
    /*----------------------------------------------------------------------------*/    
    public void testTransformToCategory() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/context/saas_001/ => /",
    			"WebRequest");
    	
    	assertEquals("Should replace /context/saas_001/ with /", "WebRequest.api.rest.checkout_do", 
    			t.transformToCategory("/api/context/saas_001/rest/checkout.do"));
    }

    /*----------------------------------------------------------------------------*/
    public void testCategoryNeverEndsWithTrailingPeriods() throws Exception {
    	// Option 1) Full replacement
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/** => /staticcontent/",
    		"WebRequest");
    	
    	assertEquals("Category must never end with trailing periods", "WebRequest.staticcontent", 
    			t.transformToCategory("/image/icons/home.ico"));
    }
    
    /*----------------------------------------------------------------------------*/
    public void testCategoryNeverEndsWithTrailingUnderscores() throws Exception {
    	// Option 1) Full replacement
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/** => /staticcontent.",
    		"WebRequest");
    	
    	assertEquals("Category must never end with trailing underscores", "WebRequest.staticcontent", 
    			t.transformToCategory("/image/icons/home.ico"));
    }
    
    
    /*----------------------------------------------------------------------------*/
    /**
     * This provides an option to completely replace the basecategory.
     * 
     * This is done by prepending the replacement value with a $ sign.
     * 
     * This is only valid for one of the following
     * 	1) (Full Replacement) Replaces the entire value of the ServletPath; OR
     *  2) (Leading Replacement) Replaces the start of the ServletPath
     * 
     * 
     * @throws Exception
     */
    public void testBaseCategoryReplacement() throws Exception {
    	// Option 1) Full replacement
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/** => $StaticWebRequest",
    		"WebRequest");
    	
    	assertEquals("Will NOT prepend the base category", "StaticWebRequest", 
    			t.transformToCategory("/image/icons/home.ico"));

    	
    	// Option 2) LeadingReplacement
    	t = ServletPathTransformer.newTransformer("/image/ => $StaticWebRequest/",
    		"WebRequest");
    	
    	assertEquals("Base category will be StaticWebRequest", "StaticWebRequest.icons.home_ico", 
    			t.transformToCategory("/image/icons/home.ico"));

    	t = ServletPathTransformer.newTransformer("/image/ => $StaticWebRequest",
        		"WebRequest");
        	
    	assertEquals("Trailing slash is NOT required on replacement", "StaticWebRequest.icons.home_ico", 
    			t.transformToCategory("/image/icons/home.ico"));
    	
    	assertEquals("Category replacement will ALWAYS be considered as a leading or full replacement"
    			+ "it will not do embedded replacements", "WebRequest.other.image.icons.home_ico", 
    			t.transformToCategory("/other/image/icons/home.ico"));
    }

    
    /*----------------------------------------------------------------------------*/
    /**
     * When you are trying to completely replace the baseCategory,
     * using the $ sign, the pattern will always be assumed to
     * be a "startsWith" pattern.
     * 
     * @throws Exception
     */
    public void testBaseCategoryReplacement_doesNotSupportEmbedded() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/** => $StaticWebRequest",
        		"WebRequest");

		assertEquals("This will NOT match, when using a base category replacement ($)"
				+ " the pattern will always be treated as a 'startsWith' pattern", "WebRequest.other.image.icons.home_ico", 
				t.transformToCategory("/other/image/icons/home.ico"));

    	t = ServletPathTransformer.newTransformer("**/image/** => $StaticWebRequest",
        		"WebRequest");
    	
		// If you really wanted any request that included /image/ to be put included in the 
		// category replacement, you can just add a ** wildcard to the start of your patern
		assertEquals("This will  match, when using a base category replacement ($)", "StaticWebRequest", 
				t.transformToCategory("/other/image/icons/home.ico"));
    }

    public void testMultiBaseCategoryReplacement() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/**|/passthrough => $StaticWebRequest",
        		"WebRequest");

		assertEquals("Should be a match and replace entire path", "StaticWebRequest", 
				t.transformToCategory("/image/icons/home.ico"));
		assertEquals("Should replace /passthrough with StaticWebRequest", "StaticWebRequest.icons.home_ico", 
				t.transformToCategory("/passthrough/icons/home.ico"));
    }

    
    public void testMultiBaseCategoryReplacement_doesNotSupportEmbedded() throws Exception {
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/**|/passthrough => $StaticWebRequest",
        		"WebRequest");

		assertEquals("This will NOT match, when using a base category replacement ($)"
				+ " the pattern will always be treated as a 'startsWith' pattern", "WebRequest.other.image.icons.home_ico", 
				t.transformToCategory("/other/image/icons/home.ico"));
		assertEquals("This will NOT match, when using a base category replacement ($)"
				+ " the pattern will always be treated as a 'startsWith' pattern", "WebRequest.other.passthrough.icons.home_ico", 
				t.transformToCategory("/other/passthrough/icons/home.ico"));
    }
    
    
    /*----------------------------------------------------------------------------*/    
    public void testTransformationAlwaysStartsWithBackslash() throws Exception {
    	// In this example our replacement will fail to ensure the the pattern
    	// will start with a / we will add one if it is missing
    	ServletPathTransformer t = ServletPathTransformer.newTransformer("/image/** => staticcontent");
    	
    	assertEquals("Should automatically append the / if it is missing", "/staticcontent", 
    			t.transform("/image/icons/home.ico"));
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

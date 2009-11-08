/*
 *	Copyright 2008,2009 Follett Software Company 
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
package org.perfmon4j.instrument;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class TransformerParamsTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public TransformerParamsTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/    
    public void testValidateGarbageParams() {
        try {
            new TransformerParams("this is garbage params");
            fail("Should validate params");
        } catch (RuntimeException re) {
            // Expected...
        }
    }

/*----------------------------------------------------------------------------*/
    /*
     * Empty params denotes to annotate everything
     */
    public void testValidateEmptyParamsAreValid() {
        TransformerParams params = new TransformerParams("");
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
                params.getTransformMode(String.class));
    }
    

/*----------------------------------------------------------------------------*/
    /*
     * Empty params denotes to annotate everything
     */
    public void testPassXMLConfigFile() {
        TransformerParams params = new TransformerParams("f=c:/dave.xml");
        assertEquals("c:/dave.xml", params.getXmlFileToConfig());
    }
    
    
    public void testEnableBootstrapClassLoading() {
    	TransformerParams params = new TransformerParams();
    	
    	assertFalse("bootStrap instrumentation should NOT be on by default", params.isBootStrapInstrumentationEnabled());
    
    	params = new TransformerParams("b=anything");
    	assertFalse("b=anything should disable bootStrap instrumentation", params.isBootStrapInstrumentationEnabled());
    	
    	params = new TransformerParams("b=true");
    	assertTrue("b=true should enable bootStrap instrumentation", params.isBootStrapInstrumentationEnabled());
    	
    	params = new TransformerParams("b=TRUE");
    	assertTrue("b=TRUE should enable bootStrap instrumentation", params.isBootStrapInstrumentationEnabled());
    }
    
/*----------------------------------------------------------------------------*/    
    public void testBlackList() {
        TransformerParams params = new TransformerParams("-eorg.apache");
        
        assertEquals("log4j is blacklisted... Should not allow any logging regardless of the parameters", 
        		TransformerParams.MODE_NONE, 
            params.getTransformMode("org.apache.log4j.LogManager"));
        
        assertEquals("catalina is an org.apache package that is NOT blacklisted", 
        		TransformerParams.MODE_EXTREME, 
            params.getTransformMode("org.apache.catalina.RequestProcessor"));        
    }


    /*----------------------------------------------------------------------------*/    
    public void testLimitToAnnotations() {
        TransformerParams params = new TransformerParams("\"a=java.lang.String a=org.perfmon4j\"");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.* classes should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(this.getClass()));        
    }
    
    

    /*----------------------------------------------------------------------------*/    
    public void testGetterSetterDisabledByDefault() {
        TransformerParams params = new TransformerParams("-ejava.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertFalse("instrument setters should be off by default", options.isInstrumentSetters());
        assertFalse("instrument getters should be off by default", options.isInstrumentGetters());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testEnableGetter() {
        TransformerParams params = new TransformerParams("-e(+getter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertTrue("instrument getters should be enabled", options.isInstrumentGetters());
        assertFalse("instrument setters should default to false", options.isInstrumentSetters());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testEnableSetter() {
        TransformerParams params = new TransformerParams("-e(+setter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertTrue("instrument setters should be enabled", options.isInstrumentSetters());
        assertFalse("instrument getters should default to false", options.isInstrumentGetters());
    }

    /*----------------------------------------------------------------------------*/    
    public void testEnableGetterAndSetter() {
        TransformerParams params = new TransformerParams("-e(+setter,+getter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertTrue("instrument setters should be enabled", options.isInstrumentSetters());
        assertTrue("instrument getters should be enabled", options.isInstrumentGetters());
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testExplicitDisable() {
        TransformerParams params = new TransformerParams("-e(-setter,-getter)java.lang.String");
        
        TransformerParams.TransformOptions options = params.getTransformOptions(String.class);
        assertFalse("instrument setters should NOT be enabled", options.isInstrumentSetters());
        assertFalse("instrument getters should NOT be enabled", options.isInstrumentGetters());
    }
    
    
    /*----------------------------------------------------------------------------*/    
    public void testLimitAnnotationsAndExtreme() {
        TransformerParams params = new TransformerParams("a=java.lang.String e=org.perfmon4j.instrument " +
        		"a=org.perfmon4j");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.instrument.* classes should be extreme", 
            TransformerParams.MODE_BOTH, params.getTransformMode(this.getClass()));        
        assertEquals("org.perfmon4j.* classes should be annotate", 
            TransformerParams.MODE_ANNOTATE, params.getTransformMode(PerfMon.class));        
    }    

    /*----------------------------------------------------------------------------*/    
    public void testAllowsNBSP() {
        TransformerParams params = new TransformerParams("a=java.lang.String&nbsp;e=org.perfmon4j.instrument&nbsp;" +
        		"a=org.perfmon4j");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.instrument.* classes should be extreme", 
            TransformerParams.MODE_BOTH, params.getTransformMode(this.getClass()));        
        assertEquals("org.perfmon4j.* classes should be annotate", 
            TransformerParams.MODE_ANNOTATE, params.getTransformMode(PerfMon.class));        
    }    
    
    /*----------------------------------------------------------------------------*/    
    /**
     * In JBoss run.bat inserts the entire JAVA_OPTS in quotes.  Since nested quotes do not
     * work we need a javaagent string that does not contain whitespace.
     * 
     */
    public void testJBossCompatibleJavaAgent() {
        TransformerParams params = new TransformerParams("-ajava.lang.String,-eorg.perfmon4j.instrument,-btrue");
        
        assertEquals("String should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        assertEquals("String buffer should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(StringBuffer.class));        
        assertEquals("org.perfmon4j.instrument.* classes should be extreme", 
            TransformerParams.MODE_EXTREME, params.getTransformMode(this.getClass()));        
        assertTrue("Should be doing bootstrap instrumentation", params.isBootStrapInstrumentationEnabled());        
    }    

    public void testValidateReloadFileParam() {
        TransformerParams params = new TransformerParams("-r10");
        
        assertEquals(10, params.getReloadConfigSeconds());

        params = new TransformerParams("-rgarbage");
        assertEquals("If we cant parse it use the default", 60, params.getReloadConfigSeconds());
        
        params = new TransformerParams("-r1");
        assertEquals("Minimum allowed is 10 seconds", 10, params.getReloadConfigSeconds());

        params = new TransformerParams("-r0");
        assertEquals("0 OR less is used to indicate no reloading at all", 0, params.getReloadConfigSeconds());
        
        params = new TransformerParams("-r-1");
        assertEquals("0 is used to indicate no reloading at all", 0, params.getReloadConfigSeconds());
    }    
    
    
    /*----------------------------------------------------------------------------*/    
    public void testIgnoreList() {
    	// Annotate everything in java.lang except for java.lang.String
        TransformerParams params = new TransformerParams("-ajava.lang,-ijava.lang.String,-b=true");
        
        assertEquals("Object should annotate", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(Object.class));
        assertEquals("String should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(String.class));        

        // Ignore everything in java.lang EXCEPT for java.lang.String
        params = new TransformerParams("-ijava.lang,-ejava.lang.String,-b=true");
        
        assertEquals("Object should NOT annotate", TransformerParams.MODE_NONE, 
            params.getTransformMode(Object.class));
        assertEquals("String shouldannotate", TransformerParams.MODE_EXTREME, 
            params.getTransformMode(String.class));        
    }    
    
/*----------------------------------------------------------------------------*/    
    public void testValidateNoParams() {
        TransformerParams params = new TransformerParams();
        
        assertEquals("Default params should enable annotations on all classes", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
        
        params = new TransformerParams("");
        
        assertEquals("Default params should enable annotations on all classes", TransformerParams.MODE_ANNOTATE, 
            params.getTransformMode(String.class));
    }
    
    
    
    /*----------------------------------------------------------------------------*/    
        public void testValidateDebugFlag() {
            TransformerParams params = new TransformerParams();
            assertFalse("On default debug should be off", params.isDebugEnabled());
            
            params = new TransformerParams("-dtrue");
            assertTrue("Parameter alone", params.isDebugEnabled());
            assertTrue("Debug parameter also enables verbose", params.isVerboseInstrumentationEnabled());
            // When debug is enabled, verbose will also be enabled.
            
            params = new TransformerParams("-dfalse");
            assertFalse("Can force it false", params.isDebugEnabled());
            
            params = new TransformerParams("-danything");
            assertFalse("Anything value other than \"true\" is considered false", params.isDebugEnabled());
            
            
            
            // System property can change the default
            System.setProperty("PerfMon4j.debugEnabled", "true");
            try {
            	params = new TransformerParams();
            	assertTrue("System property can set debug on by default", params.isDebugEnabled());
            	assertTrue("Debug system property also enables verbose", params.isVerboseInstrumentationEnabled());
            } finally {
            	System.getProperties().remove("PerfMon4j.debugEnabled");
            }
        }

        /*----------------------------------------------------------------------------*/    
        public void testValidateVerboseFlag() {
            TransformerParams params = new TransformerParams();
            assertFalse("On default verbose should be off", params.isVerboseInstrumentationEnabled());
            
            params = new TransformerParams("-vtrue");
            assertTrue("Parameter alone", params.isVerboseInstrumentationEnabled());
            
            params = new TransformerParams("-vfalse");
            assertFalse("Can force it false", params.isVerboseInstrumentationEnabled());
            
            params = new TransformerParams("-vanything");
            assertFalse("Anything value other than \"true\" is considered false", params.isVerboseInstrumentationEnabled());
        }
        
        
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(TransformerParamsTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {TransformerParamsTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new TransformerParamsTest("testJBossCompatibleJavaAgent"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(TransformerParamsTest.class);
        }

        return( newSuite);
    }
}

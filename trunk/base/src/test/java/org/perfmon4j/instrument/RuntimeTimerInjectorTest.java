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

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Method;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.javassist.SerialVersionUIDHelper;


public class RuntimeTimerInjectorTest extends TestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public RuntimeTimerInjectorTest(String name) {
        super(name);
    }
    
    
/*----------------------------------------------------------------------------*/
    public void setUp() throws Exception {
        super.setUp();
        PerfMonConfiguration config = new PerfMonConfiguration();
      
        final String APPENDER_NAME = "TextAppender";
        config.defineMonitor(PerfMon.ROOT_MONITOR_NAME);
        config.defineAppender(APPENDER_NAME, TextAppender.class.getName(), "1 minute");
      
        config.attachAppenderToMonitor(PerfMon.ROOT_MONITOR_NAME, APPENDER_NAME);
        
        PerfMon.configure(config);
    }

/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	PerfMon.deInit();
        super.tearDown();
    }
        

/*----------------------------------------------------------------------------*/    
    public static class TestAnnotation {
        @DeclarePerfMonTimer("TestAnnotation.test1")
        public static void annotation() {
            System.err.println("asdfasdf");
        }
    }
    
    public void testAnnotation() throws Exception {
        CtClass clazz = cloneLoadedClass(TestAnnotation.class);
        
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false);
        assertEquals("Number of inserted timers", 1, numTimersInserted);
        
        try {
            clazz.getField("pm$MonitorArray");
        } catch (NotFoundException nfe) {
            fail("Should have added a moniter array");
        }

        invokeMethodOnCtClass(clazz, "annotation");
        PerfMon mon = PerfMon.getMonitor("TestAnnotation.test1");
        assertEquals("total hits", 1, mon.getTotalCompletions());
    }

    public static class TestSerializable implements Serializable {
		private static final long serialVersionUID = 1L;

		public void doNothing() {
        }
    }    
/*----------------------------------------------------------------------------*/    
    public static class TestNoAnnotation {
        public static void noAnnotation() {
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public void testNoAnnotation() throws Exception {
        CtClass clazz = cloneLoadedClass(TestNoAnnotation.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, false));
        
        assertEquals("Number of inserted timers", 0, numTimersInserted);
        try {
            clazz.getField("pm$MonitorArray");
            fail("Should NOT have added a monitor array");
        } catch (NotFoundException nfe) {
            // Expected
        }
        
        assertEquals("Number of methods", 1, clazz.getDeclaredMethods().length);
        
        // Make sure there is no performance monitoring on this class
        invokeMethodOnCtClass(clazz, "noAnnotation");
        
        PerfMon mon = PerfMon.getMonitor(clazz.getName());
        assertEquals("total hits", 0, mon.getTotalCompletions());
    }


    private static class JavaAssistClassLoader extends ClassLoader {
        protected Class findClass(String name) throws ClassNotFoundException {
            try {
                CtClass cc = ClassPool.getDefault().get(name);
                byte[] b = cc.toBytecode();
                return defineClass(name, b, 0, b.length);
            } catch (CannotCompileException e) {
                throw new ClassNotFoundException();
            } catch (NotFoundException ex) {
                throw new ClassNotFoundException("Class " + name + " not found", ex);
            } catch (IOException ex) {
                throw new ClassNotFoundException("Class " + name + " IOException", ex);
            }
        }
}
    
    /*----------------------------------------------------------------------------*/    
    public void testMaintainSerialVersionID() throws Exception {
        JavaAssistClassLoader loader = new JavaAssistClassLoader();
        ClassPool.doPruning = false;
        CtClass clazz = cloneLoadedClass(TestSerializable.class);
        
        long originalSerialVersionID = ObjectStreamClass.lookup(loader.loadClass(clazz.getName())).getSerialVersionUID();
        
        
        clazz.defrost();
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
        assertEquals("Number of inserted timers", 1, numTimersInserted);
        
        loader = new JavaAssistClassLoader();
        long newSerialVersionID = ObjectStreamClass.lookup(loader.loadClass(clazz.getName())).getSerialVersionUID();
        
        assertEquals("Should have maintained serialVersionID", originalSerialVersionID, 
            newSerialVersionID);
    }
    
    
    public static class TestSerializableNoExplicitID implements Serializable {
    	/*
    	 * NOTE This class does not have a defined serialVersionUID
    	 *  
    	 */
		public void doNothing() {
        }
    }   
    
    /*----------------------------------------------------------------------------*/    
    public void testMaintainSerialVersionIDOnClassWithNoExplicitSerialVersionID() throws Exception {
        JavaAssistClassLoader loader = new JavaAssistClassLoader();
        ClassPool.doPruning = false;
        CtClass clazz = cloneLoadedClass(TestSerializableNoExplicitID.class);
        
        long originalSerialVersionID = ObjectStreamClass.lookup(loader.loadClass(clazz.getName())).getSerialVersionUID();
        clazz.defrost();
        
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
        assertEquals("Number of inserted timers", 1, numTimersInserted);
        
        loader = new JavaAssistClassLoader();
        long newSerialVersionID = ObjectStreamClass.lookup(loader.loadClass(clazz.getName())).getSerialVersionUID();
        
        assertEquals("Should have maintained serialVersionID", originalSerialVersionID, 
            newSerialVersionID);
    }

    public static class TestSkipSerializableNoExplicitID implements Serializable {
    	/*
    	 * NOTE This class does not have a defined serialVersionUID
    	 *  
    	 */
		public void doNothing() {
        }
    }   
    
    /*----------------------------------------------------------------------------*/    
/*
 	TODO!!!  These test needs to be rewritten!
    
    public void testSkipSerialVersionIDOnClassWithNoExplicitSerialVersionID() throws Exception {
        JavaAssistClassLoader loader = new JavaAssistClassLoader();
        
        System.setProperty(SerialVersionUIDHelper.REQUIRE_EXPLICIT_SERIAL_VERSION_UID, "true");
        try {
            CtClass clazz = cloneLoadedClass(TestSkipSerializableNoExplicitID.class);
            
            int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
            assertEquals("Class should NOT have been instrumented", 0, numTimersInserted);
        	
        } finally {
            System.getProperties().remove(SerialVersionUIDHelper.REQUIRE_EXPLICIT_SERIAL_VERSION_UID);
        }
    }
*/
    
    public static class TestDoNOTSkipSerializableWithExplicitID implements Serializable {
		private static final long serialVersionUID = 1L;
		public void doNothing() {
        }
    }   
    
    /*----------------------------------------------------------------------------*/    
    public void testIgnoreSkipSerialVersionIDOnClassWithExplicitSerialVersionID() throws Exception {
        JavaAssistClassLoader loader = new JavaAssistClassLoader();
        
        System.setProperty(SerialVersionUIDHelper.REQUIRE_EXPLICIT_SERIAL_VERSION_UID, "true");
        try {
            CtClass clazz = cloneLoadedClass(TestDoNOTSkipSerializableWithExplicitID.class);
            
            int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
            assertEquals("Class should have been instrumented", 1, numTimersInserted);
        	
        } finally {
            System.getProperties().remove(SerialVersionUIDHelper.REQUIRE_EXPLICIT_SERIAL_VERSION_UID);
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static class TestExtremeOnlyInjection {
        @DeclarePerfMonTimer("Dave")
        public static void noAnnotation() {
        }
    }
    
    public void testExtremeOnlyInjection() throws Exception {
        CtClass clazz = cloneLoadedClass(TestExtremeOnlyInjection.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, false, true));
        
        assertEquals("Should not add a timer for the annotation", 1, numTimersInserted);
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static class TestExtremeAndAnnotatedInjection {
        @DeclarePerfMonTimer("dave")
        public static void annotation() {
        }
        public static void noAnnotation() {
        }
    }
    
    public void testExtremeAndAnnotatedInjection() throws Exception {
        CtClass clazz = cloneLoadedClass(TestExtremeAndAnnotatedInjection.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
        
        // Should have 3 timers... 1 for the annotation and a seperate one for each method....
        assertEquals("Number of inserted timers", 3, numTimersInserted);
        try {
            clazz.getField("pm$MonitorArray");
        } catch (NotFoundException nfe) {
            fail("Should have added a moniter array");
        }
   
        invokeMethodOnCtClass(clazz, "annotation");
        
        PerfMon mon = PerfMon.getMonitor(clazz.getName());
        assertEquals("Should have a hit on the extreme monitor", 1, mon.getTotalCompletions());
        
        mon = PerfMon.getMonitor("dave");
        assertEquals("Should have a hit on the annotation", 1, mon.getTotalCompletions());
        
    }

    
    
    /*----------------------------------------------------------------------------*/    
    public static class TestValidPartialInjection<T extends Serializable>{
        public TestValidPartialInjection clone(T t) {
        	return null;
        }
        
        public TestValidPartialInjection clone(Object x) {
        	return null;
        }
        
        public static void simpleMethod() {
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static class TestAnnotationDoesNotDuplicateExtreme {
        @DeclarePerfMonTimer("org.perfmon4j.instrument")
        public static void annotation() {
        }
        public static void noAnnotation() {
        }
    }
    
    public void testAnnotationDoesNotDuplicateExtreme() throws Exception {
        CtClass clazz = cloneLoadedClass(TestAnnotationDoesNotDuplicateExtreme.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
        
        // Should have 2 timers...  Since the annotation is a subset of the
        // extreme don't bother including it...
        assertEquals("Number of inserted timers", 2, numTimersInserted);

        invokeMethodOnCtClass(clazz, "annotation");
        
        PerfMon mon = PerfMon.getMonitor("org.perfmon4j.instrument");
        assertEquals("Should not double up our counters", 1, mon.getTotalCompletions());
    }

    public static class TestSimpleBean {
    	private int value;
    	
    	public void notAGettorOrSetter() {
    	}
    	
    	public void setValue(int value) {
    		this.value = value;
    	}
    	
    	public int getValue() {
    		return value;
    	}
    	
    	public boolean isValueOverTen() {
    		return value > 10;
    	}
    }
    
    public void testExtremeDoesNotInstrumentGettersAndSettersByDefault() throws Exception {
        CtClass clazz = cloneLoadedClass(TestSimpleBean.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, paramsForClass(clazz, true, true));
        
        // Should have 2 timers...  Since the annotation is a subset of the
        // extreme don't bother including it...
        assertEquals("Should not have instrumented getter, setter, or isser", 1, numTimersInserted);
    }

    
    public void testExtremeOverrideGetter() throws Exception {
        CtClass clazz = cloneLoadedClass(TestSimpleBean.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, 
        		paramsForClass(clazz, true, true, "(+getter)"));
        
        // Should have 2 timers...  Since the annotation is a subset of the
        // extreme don't bother including it...
        assertEquals("Should have instrumented getter and isser but not setter", 3, numTimersInserted);
    }
    
    public void testExtremeOverrideSetter() throws Exception {
        CtClass clazz = cloneLoadedClass(TestSimpleBean.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, 
        		paramsForClass(clazz, true, true, "(+setter)"));
        
        // Should have 2 timers...  Since the annotation is a subset of the
        // extreme don't bother including it...
        assertEquals("Should have instrumented setter but not getter and isser", 2, numTimersInserted);
    }

    public void testExtremeOverrideGetterAndSetter() throws Exception {
        CtClass clazz = cloneLoadedClass(TestSimpleBean.class);
        int numTimersInserted = RuntimeTimerInjector.injectPerfMonTimers(clazz, false, 
        		paramsForClass(clazz, true, true, "(+setter,+getter)"));
        
        // Should have 2 timers...  Since the annotation is a subset of the
        // extreme don't bother including it...
        assertEquals("Should have instrumented setter, getter and isser", 4, numTimersInserted);
    }
    
    
    /**
     * This method will create a clone of the specified class...
     * The clone will have a suffix of "Injected" appended to it.
     */
    private static CtClass cloneLoadedClass(Class clazz) throws Exception {
        CtClass result = ClassPool.getDefault().get(clazz.getName());
        result.setName(clazz.getName() + "Injected");
        
        return result;
    }

    private static void invokeMethodOnCtClass(CtClass clazz, String methodName) throws Exception {
        Method method = clazz.toClass().getMethod(methodName, new Class[]{});
        method.invoke(null, new Object[]{});
    }
    
    private static TransformerParams paramsForClass(CtClass clazz, boolean annotate, boolean extreme) {
    	return paramsForClass(clazz, annotate, extreme, "");
    
    }
    
    private static TransformerParams paramsForClass(CtClass clazz, boolean annotate, 
    		boolean extreme, String extremeOptions) {
        String param = "";
        if (annotate) {
            param = "-a" + clazz.getName();
        }
        if (extreme) {
        	if (annotate) {
        		param += ",";
        	}
            param += "-e" + extremeOptions + clazz.getName();
        }
        return new TransformerParams(param);
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger(RuntimeTimerInjectorTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        String[] testCaseName = {RuntimeTimerInjectorTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
        newSuite.addTest(new RuntimeTimerInjectorTest("testMaintainSerialVersionIDOnClassWithNoExplicitSerialVersionID"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(RuntimeTimerInjectorTest.class);
        }

        return( newSuite);
    }
}

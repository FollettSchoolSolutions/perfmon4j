/*
 *	Copyright 2008-2011 Follett Software Company 
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

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.perfmon4j.Appender;
import org.perfmon4j.ExceptionTracker;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class PerfMonTimerTransformerTest extends PerfMonTestCase {
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

	private File perfmon4jJar = null;
	private File javassistJar = null;
	
/*----------------------------------------------------------------------------*/
    public PerfMonTimerTransformerTest(String name) {
        super(name);
    }
    
/*----------------------------------------------------------------------------*/
    public void setUp() throws Exception {
        super.setUp();
        
        perfmon4jJar = File.createTempFile("perfmon4j", "tmpdir");
        perfmon4jJar.delete(); // Just wanted the unique temporary file name.
        perfmon4jJar.mkdir();
        perfmon4jJar = new File(perfmon4jJar, "perfmon4j.jar");
        
        Properties props = new Properties();	
		props.setProperty("Premain-Class", "org.perfmon4j.instrument.PerfMonTimerTransformer");
		props.setProperty("Can-Redefine-Classes", "true");
		
		File classesFolder = new File("./target/classes");
		if (!classesFolder.exists()) {
			classesFolder = new File("./base/target/classes");
		}
		
		File testClassesFolder = new File("./target/test-classes");
		if (!testClassesFolder.exists()) {
			testClassesFolder = new File("./base/target/test-classes");
		}
		
		assertTrue("Could not find classes folder in: "  + classesFolder.getCanonicalPath(), classesFolder.exists());
		
        MiscHelper.createJarFile(perfmon4jJar.getAbsolutePath(), props, new File[]{classesFolder, testClassesFolder});
        
//        System.out.println("perfmon4j jar file: " + perfmon4jJar.getCanonicalPath());
    }
    
    

/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	File folder = perfmon4jJar.getParentFile();
        perfmon4jJar.delete();
        if (javassistJar != null) {
        	javassistJar.delete();
        }
        folder.delete();
        
        perfmon4jJar = null;
        javassistJar = null;
        
    	super.tearDown();
    }
    
	
	public void testSystemPropertyDisablesInstrumentation() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "true");
    	
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(TemplatedClassTest.class, "-dtrue,-btrue,-eorg.perfmon4j", props, perfmon4jJar);
    	assertTrue("Should have an $impl method indicating class was annotated" + output,
    			output.contains("thisMethodShouldBeAnnotated$1$Impl()"));
    	
    	props.setProperty(PerfMonTimerTransformer.DISABLE_CLASS_INSTRUMENTATION_PROPERTY, "true");
    	output = LaunchRunnableInVM.loadClassAndPrintMethods(TemplatedClassTest.class, "-dtrue,-btrue,-eorg.perfmon4j", props, perfmon4jJar);
//System.out.println(output);
    	assertFalse("SystemPropert Perfmon4j.DisableClassInstrumentation should disable class instrumentation: " + output,
    			output.contains("thisMethodShouldBeAnnotated$1$Impl()"));
    }
    
    
    public static class TemplatedClassTest<T> {
    	final T value;
    	
    	public TemplatedClassTest(T value) {
    		this.value = value;
    	}
    	
    	public void thisMethodShouldBeAnnotated() {
    	}
    }

//    private void initJavaAssistJar() {
//    	String javaAssistProp = System.getProperty("JAVASSIST_JAR");
//    	if (javaAssistProp == null) {
//    		String filePath = System.getenv("M2_REPO") + 
//    			"/org/javassist/javassist/3.20.0-GA/javassist-3.20.0-GA.jar";
//        	logger.logWarn("JAVASSSIST_JAR system property NOT set...  Trying default location: " + filePath);
//        	System.setProperty("JAVASSIST_JAR", filePath);
//    	}
//    }
    
    /*----------------------------------------------------------------------------*/    
    public void testTemplatedClassIsAnnotated() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "true");
    	
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(TemplatedClassTest.class, "-dtrue,-btrue,-eorg.perfmon4j", props, perfmon4jJar);

    	assertTrue("Should have an $impl method indicating class was annotated" + output,
    			output.contains("thisMethodShouldBeAnnotated$1$Impl()"));
    }

    
    /*----------------------------------------------------------------------------*/    
    /**
     * This test is disabled because the -b functionality seems to
     * be broken on current java 11 implementation,  This test should
     * be retried under different JVM;
     * 
     * @throws Exception
     */
    public void DISABLEtestAnnotateBootStrapClass() throws Exception {
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(String.class, "-ejava.lang.String,-btrue", perfmon4jJar);
    	final String validationString = "BootStrapMonitor: java.lang.String.equals";
//System.out.println(output);

    	assertTrue("Should have added a bootstrap monitor: " + output,
    			output.contains(validationString));
    	
    	// Now check to ensure that the inserting bootstrap timers is not on by default.
       	output = LaunchRunnableInVM.loadClassAndPrintMethods(String.class, "e=java.lang.String", perfmon4jJar);
       	
       	
    	assertFalse("b=true must be passed to java agent for bootstrap class insturmentation: " + output,
    			output.contains(validationString));
    	
    }

    /*----------------------------------------------------------------------------*/    
    /**
     * This test is disabled because the -b functionality seems to
     * be broken on current java 11 implementation,  This test should
     * be retried under different JVM;
     * 
     * @throws Exception
     */
    public void DISABLEtestAnnotateSystemClass() throws Exception {
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(String.class, "-dtrue,-ejava.lang.System,-btrue", perfmon4jJar);

    	final String validationString = "BootStrapMonitor: java.lang.System.checkKey";
    	
    	assertTrue("Should have added a bootstrap monitor: " + output,
    			output.contains(validationString));
    }

    
    /*----------------------------------------------------------------------------*/    
    /**
     * This test is implemented to ensure that the GlobalClassLoader does NOT contain
     * references to ClassLoaders once they are unloaded.  This is VERY important to ensure
     * that we do not have a permgen leak (See tracker defect id: 2889034)
     * 
     * This test will do the following:
     * 	1) Instantiate a class loader and use it to load a class
     * 	2) Store the class it Loaded in a WeakReference
     * 	3) Dereference the ClassLoader
     * 	4) Invoke a GC
     * 	5) Validate the WeakReference to the class is NULL...  This indicates that the global
     * 		classloader released the reference to the ClassLoader
     */
    public void testGlobalClassLoaderUsesWeakReferences() throws Exception {
    	String output = LaunchRunnableInVM.run(GlobalClassLoaderTester.class, "-dfalse", "", perfmon4jJar);

System.out.println(output);
		final String validateClassLoaderAddedToGlobalClassLoader 
			= "Loaders added to the GlobalClassLoader: 1";
		
		assertTrue("ClassLoader was not added to GlobalClassLoader", 
				output.contains(validateClassLoaderAddedToGlobalClassLoader));
		
		
		final String validateClassWasLoaded 
			= "BEFORE -- WeakReference IS NOT NULL";
		assertTrue("Class load failed!", 
				output.contains(validateClassWasLoaded));
		
		
		final String validateClassLoaderWasReleased 
			= "AFTER -- WeakReference IS NULL";
		assertTrue("ClassLoader was not released by the global classloader (PERMGEN LEAK!)!", 
				output.contains(validateClassLoaderWasReleased));
    }
    
    
	public static class GlobalClassLoaderTester implements Runnable {

		public void run() {
			try {
				long numLoaders = GlobalClassLoader.getClassLoader().getTotalClassLoaders();
				System.out.println("Initial Num ClassLoaders: " + numLoaders);

				BogusClassLoader loader = new BogusClassLoader();
				WeakReference<Class<?>>  clazzReference = 
					new WeakReference<Class<?>>(loader.loadClass("org.perfmon4j.instrument.TestClass"));
				
				long afterLoaders = GlobalClassLoader.getClassLoader().getTotalClassLoaders();
				System.out.println("Loaders added to the GlobalClassLoader: " + (afterLoaders - numLoaders));
				
				System.out.println("BEFORE -- WeakReference IS" 
						+ (clazzReference.get() == null ? "" : " NOT") + " NULL");

				loader = null;
				System.gc();

				System.out.println("AFTER -- WeakReference IS" 
						+ (clazzReference.get() == null ? "" : " NOT") + " NULL");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
	private static void validateOutput(String output, String contains, boolean mustContain) throws Exception {
		if (mustContain) {
			assertTrue("Expected output to contain: " + contains, output.contains(contains));
		} else {
			assertFalse("Did NOT expect output to contain: " + contains, output.contains(contains));
		}
	}
	
	public void testVerboseOutput() throws Exception {
		final String CLASS_NAME = VerboseClassTest.class.getName();
		final String BASE_PARAMS = "-e" + CLASS_NAME + ",-a" + CLASS_NAME;
		final String VERBOSE_PARAMS = "-vtrue," + BASE_PARAMS;
		final String DEBUG_PARAMS = "-dtrue," + BASE_PARAMS;
		
		final String VERBOSE_CLASS_MSG = "** Instrumenting class: " + CLASS_NAME;
		final String VERBOSE_METHOD_MSG = "** Adding extreme monitor: " + CLASS_NAME + ".doSomething";
		final String VERBOSE_SETTER_MSG = "** Skipping extreme monitor (setter method): " + CLASS_NAME + ".setX";
		final String VERBOSE_GETTER_MSG = "** Skipping extreme monitor (getter method): " + CLASS_NAME + ".getX";
		final String VERBOSE_ISSER_MSG = "** Skipping extreme monitor (getter method): " + CLASS_NAME + ".isXPositive";
		final String VERBOSE_ANOTATION_MSG = "** Adding annotation monitor (MyTimer) to method: " + CLASS_NAME + ".doSomething";
		
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "true");
    	
		// Messages should appear when verbose is enabled "-vtrue"
		String output = LaunchRunnableInVM.loadClassAndPrintMethods(VerboseClassTest.class, VERBOSE_PARAMS, props, perfmon4jJar);
    	validateOutput(output, VERBOSE_CLASS_MSG, true);
    	validateOutput(output, VERBOSE_METHOD_MSG, true);
    	validateOutput(output, VERBOSE_SETTER_MSG, true);
    	validateOutput(output, VERBOSE_GETTER_MSG, true);
    	validateOutput(output, VERBOSE_ISSER_MSG, true);
    	validateOutput(output, VERBOSE_ANOTATION_MSG, true);
    	
		// Messages should appear when debug is enabled "-dtrue"
    	// Version 1.5.1 Made a change.  Now Verbose includes debug output, but debug does NOT include verbose.
		output = LaunchRunnableInVM.loadClassAndPrintMethods(VerboseClassTest.class, DEBUG_PARAMS, props, perfmon4jJar);
    	validateOutput(output, VERBOSE_CLASS_MSG, false);
    	validateOutput(output, VERBOSE_METHOD_MSG, false);
    	validateOutput(output, VERBOSE_SETTER_MSG, false);
    	validateOutput(output, VERBOSE_GETTER_MSG, false);
    	validateOutput(output, VERBOSE_ISSER_MSG, false);
    	validateOutput(output, VERBOSE_ANOTATION_MSG, false);

		// Messages should NOT appear unless debug OR verbose is enabled!
    	output = LaunchRunnableInVM.loadClassAndPrintMethods(VerboseClassTest.class, BASE_PARAMS, props, perfmon4jJar);
    	validateOutput(output, VERBOSE_CLASS_MSG, false);
    	validateOutput(output, VERBOSE_METHOD_MSG, false);
    	validateOutput(output, VERBOSE_SETTER_MSG, false);
    	validateOutput(output, VERBOSE_GETTER_MSG, false);
    	validateOutput(output, VERBOSE_ISSER_MSG, false);
    	validateOutput(output, VERBOSE_ANOTATION_MSG, false);
	}

	
    public static class VerboseClassTest {
    	private int x;
    	
    	public int getX() {
    		return x;
    	}
    	
    	public void setX(int x) {
    		this.x = x;
    	}
    	
    	public boolean isXPositive() {
    		return x > 0;
    	}
    	
    	@DeclarePerfMonTimer("MyTimer")
    	public void doSomething() {
    		x++;
    	}
    }

	public static class SQLStatementTester implements Runnable {
		
		public static final class BogusAppender extends Appender {
			public BogusAppender(AppenderID id) {
				super(id);
			}
			
			@Override
			public void outputData(PerfMonData data) {
				if (data instanceof IntervalData) {
					IntervalData d = (IntervalData)data;
					System.out.println("Monitor: " + d.getOwner().getName() + " Completions:" + d.getTotalCompletions());
				} else {
					System.out.println(data.toAppenderString());
				}
			}
		}
		
	    final String DERBY_CREATE_1 = "CREATE TABLE Bogus(Name VARCHAR(200) NOT NULL)";
	    
		public void run() {
			try {
				Connection conn = null;
				
				Statement s = null;
				
				try {
					PerfMonConfiguration config = new PerfMonConfiguration();
					final String monitorName = "SQL.executeQuery";
					final String appenderName = "bogus";
					
					config.defineMonitor(monitorName);
					config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
					config.attachAppenderToMonitor(monitorName, appenderName, ".");
					
					ThreadTraceConfig tcConfig = new ThreadTraceConfig();
					tcConfig.addAppender(config.getAppenderForName(appenderName));
					config.addThreadTraceConfig("MyManualTimer", tcConfig);
					
					PerfMon.configure(config);

					PerfMonTimer timer = null;
					try {
						Driver driver = (Driver)Class.forName("org.apache.derby.jdbc.EmbeddedDriver", true, PerfMon.getClassLoader()).newInstance();
						conn = driver.connect("jdbc:derby:memory:derbyDB;create=true", new Properties());
						s = conn.createStatement();
						s.execute(DERBY_CREATE_1);						
						timer = PerfMonTimer.start("MyManualTimer");
						s.executeQuery("SELECT * FROM BOGUS");
						Thread.sleep(2000);
					} finally {
						PerfMonTimer.stop(timer);	
					}
					Appender.flushAllAppenders();
				} finally {
					if (conn != null) {
						conn.close();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
    public void testInstrumentSQLStatement() throws Exception {
    	String output = LaunchRunnableInVM.run(SQLStatementTester.class, "-dtrue,-eSQL(DERBY)", "", perfmon4jJar);
//    	System.out.println(output);   	
    	assertTrue("Should have 1 completion for SQL.executeQuery", output.contains("SQL.executeQuery Completions:1"));
    }

    public void testThreadTraceWithSQLTime() throws Exception {
    	String output = LaunchRunnableInVM.run(SQLStatementTester.class, "-eSQL(DERBY)", "", perfmon4jJar);
//System.out.println(output);    
    	// Running with extreme SQL instrumentation enabled...
    	// Looking for a line like:
    	// +-14:37:29:653 (5436)(SQL:3211) MyManualTimer
    	Pattern p = Pattern.compile("\\d{1,2}:\\d{2}:\\d{2}:\\d{3} \\(\\d{4,8}\\)\\(SQL:\\d{1,8}\\) MyManualTimer");
    	Matcher m = p.matcher(output);
    	assertTrue("Should have included SQL time specification", m.find());
    	
    	// Now run it again without extreme SQL logging... We 
    	// do NOT expect the (SQL:dddd) identifier to be present.
    	// Should expect something like: +-15:07:32:969 (4722) MyManualTimer
    	output = LaunchRunnableInVM.run(SQLStatementTester.class, "", "", perfmon4jJar);
//System.out.println(output);

    	p = Pattern.compile("\\d{1,2}:\\d{2}:\\d{2}:\\d{3} \\(\\d{4,10}\\) MyManualTimer");
    	m = p.matcher(output);
    	assertTrue("Should NOT have included SQL time specification", m.find());
    }
	
	    
	public static class FixupAnnotationsTest implements Runnable {
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.PARAMETER)
		public @interface BogusParameterAnnotation {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.METHOD)
		public @interface BogusMethodAnnotation {
		}
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.METHOD)
		public @interface XmlElementDecl {
			// simulate javax.xml.bind.annotation.XmlElementDecl
		}
		
		
		@BogusMethodAnnotation
		public  String doSomethingCool(@BogusParameterAnnotation int a) {
			return "This is cool!";
		}
		
		@XmlElementDecl
		public Object testOfJaxBMethod(int a) {
			return "";
		}
		
		
		public void run() {
			try {
				Method methodRenamed = null;
				Method jaxbMethodRenamed = null;
				Method methods[] = FixupAnnotationsTest.class.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					System.out.println("Found Method: " + methods[i].getName());
					if (methods[i].getName().startsWith("doSomethingCool$")) { // method should have been renamed doSomethingCool$1$Impl
						methodRenamed = methods[i];
					}
					if (methods[i].getName().startsWith("testOfJaxBMethod$")) { 
						jaxbMethodRenamed = methods[i];
					}
				}
				
				Method method = FixupAnnotationsTest.class.getMethod("doSomethingCool", new Class<?>[]{int.class});
				

				// Get the number of annotations on the first parameter....
				int numParameterAnnotations  = method.getParameterAnnotations()[0].length;
				int numParameterAnnotationsRenamed  = methodRenamed.getParameterAnnotations()[0].length;
				
				
				System.out.println("numParameterAnnotations: " + numParameterAnnotations);
				System.out.println("numParameterAnnotationsRenamed: " + numParameterAnnotationsRenamed);
				if (numParameterAnnotations == 1 && numParameterAnnotationsRenamed == 0)  {
					System.out.println("Parameter annotation Moved");
				}
				
				int numMethodAnnotations = method.getAnnotations().length;
				int numMethodAnnotationsRenamed = methodRenamed.getAnnotations().length;
				
				System.out.println("numMethodAnnotations: " + numMethodAnnotations);
				System.out.println("numMethodAnnotationsRenamed: " + numMethodAnnotationsRenamed);
				if (numMethodAnnotations == 1 && numMethodAnnotationsRenamed == 0) {
					System.out.println("Method annotation Moved");
				}
				
				Method jaxbMethod = FixupAnnotationsTest.class.getMethod("testOfJaxBMethod", new Class<?>[]{int.class});
				int numJaxbMethodAnnotations = jaxbMethod.getAnnotations().length;
				int numJaxbMethodAnnotationsRenamed = jaxbMethodRenamed.getAnnotations().length;
				
				System.out.println("numJaxbMethodAnnotations: " + numJaxbMethodAnnotations);
				System.out.println("numJaxbMethodAnnotationsRenamed: " + numJaxbMethodAnnotationsRenamed);
				if (numJaxbMethodAnnotations == 0 && numJaxbMethodAnnotationsRenamed == 1) {
					System.out.println("XmlElementDecl annotation was NOT moved");
				}
				
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
    
	/**
	 * When the transformer instruments a method it does the following:
	 * 	1) renames the existing method methodName$123$Impl
	 * 	2) Creates a new method: methodName that wraps the previous method.
	 * 	3) We need to ensure that any anotation associated with the original method 
	 * 		are moved to the method associated with the original method name.
	 */
	public void testMoveParameterAnnotation() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "true");
    	
    	String output = LaunchRunnableInVM.run(FixupAnnotationsTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", props, perfmon4jJar);
//    	System.out.println(output);   	
    	assertTrue("Parameter annotation should have moved", output.contains("Parameter annotation Moved"));
    }

	public static class DontInstrumentMeTest implements Runnable{
		@SuppressWarnings("unused")
		private static final String NO_PERFMON4J_INSTRUMENTATION = "";
		
		public void run() {
		}
	}
	
	public void testNoPerfmonInstrumentationStaticFlag() throws Exception {
    	String output = LaunchRunnableInVM.run(DontInstrumentMeTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", perfmon4jJar);
//    	System.out.println(output);
    	
    	assertFalse("Should have skipped file because it contained the static NO_PERFMON4J_INSTRUMENTATION flag",
    			output.contains("Instrumenting class: org.perfmon4j.instrument.PerfMonTimerTransformerTest$DontInstrumentMeTest"));
    	assertTrue("Should contain verbose skip indicator", output.contains("Skipping class (found NO_PERFMON4J_INSTRUMENTATION static field)"));
    }
	
    public void testMoveMethodAnnotation() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "true");
    	
    	String output = LaunchRunnableInVM.run(FixupAnnotationsTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", props, perfmon4jJar);
//    	System.out.println(output);   	
    	assertTrue("Method annotation should have moved", output.contains("Method annotation Moved"));
    }
    
//    public void testXmlElementDeclMethodAnnotationIsNOTMoved() throws Exception {
//    	String output = LaunchRunnableInVM.run(FixupAnnotationsTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", perfmon4jJar);
//    	System.out.println(output);   	
//    	assertTrue("XmlElementDecl should NOT be moved!", output.contains("XmlElementDecl annotation was NOT moved"));
//    }
	
    public static class SystemGCDisablerTester implements Runnable {
		public void run() {
			WeakReference<byte[]> x = new WeakReference<byte[]>(new byte[10240]);
			System.gc();
			
			if (x.get() == null) {
				System.out.println("System.gc() appears enabled.");
			} else {
				System.out.println("System.gc() appears disabled.");
			}
		}
	}
	
    public void testDisableSystemGCWithBootstrapEnabled() throws Exception {
    	String output = LaunchRunnableInVM.run(SystemGCDisablerTester.class, "-gtrue,-btrue", "", perfmon4jJar);
//System.out.println(output);
    	final String validateInstalled = "System.gc() appears disabled.";
		assertTrue("SystemGC should have been disabled", output.contains(validateInstalled));
    	
    	output = LaunchRunnableInVM.run(SystemGCDisablerTester.class, "-gfalse,-btrue", "", perfmon4jJar);
    	final String validateNOTInstalled = "System.gc() appears enabled.";
		assertTrue("SystemGC should NOT have been disabled", output.contains(validateNOTInstalled));
    }

    public void testDisableSystemGCWithBootstrapDisabled() throws Exception {
    	String output = LaunchRunnableInVM.run(SystemGCDisablerTester.class, "-gtrue", "", perfmon4jJar);
    	final String validateInstalled = "System.gc() appears disabled.";
		assertTrue("SystemGC should have been disabled", output.contains(validateInstalled));
//System.out.println(output);		
    	
    	output = LaunchRunnableInVM.run(SystemGCDisablerTester.class, "-gfalse", "", perfmon4jJar);
    	final String validateNOTInstalled = "System.gc() appears enabled.";
		assertTrue("SystemGC should NOT have been disabled", output.contains(validateNOTInstalled));
    }

    public static class Log4jRuntimeLoggerTest implements Runnable {
    	static final Logger l = LoggerFactory.initLogger(Log4jRuntimeLoggerTest.class); 
    	
		public void run() {
			l.logInfo("info - pre initialize");
			
			// Because perfmon4j.jar will be loaded via
			// the endorsed folder, this class (because it is loaded with perfmon4j) will not be 
			// able to access the log4j jars directly..  So we have to dynamically 
			// load the log4j classes...
			try {
				Class<?> clazzBasicConfig = Class.forName("org.apache.log4j.BasicConfigurator",
						true, PerfMon.getClassLoader());
				Method configure = clazzBasicConfig.getMethod("configure", new Class[]{});
				configure.invoke(null, new Object[]{});
				l.logInfo("LOG4J Successfully configured");
			} catch (Exception e) {
				l.logError("Unable to configure LOG4J", e);
			}
			
			l.logDebug("debug - post initialize");
			l.logInfo("info - post initialize");
		}
    }
    
    public void testPerfmon4jLoggerUsesLog4jWhenInitialized() throws Exception {
    	String output = LaunchRunnableInVM.run(Log4jRuntimeLoggerTest.class, "", "", perfmon4jJar);
//    	System.out.println(output);
    	
    	assertTrue("Before LOG4J initialize, logging should be through java logging",
    			output.contains("info - pre initialize"));
    	
    	assertTrue("After LOG4J initialize, logging should be through LOG4J",
    			output.contains("[main] INFO org.perfmon4j.instrument.PerfMonTimerTransformerTest$Log4jRuntimeLoggerTest  - info - post initialize"));
    }
    
    
	public static interface DoSomething {
		public void doSomething();
	}
    
    public static class DoSomethingElseTest implements DoSomething {
		public void doSomething() {
		}
		
		public void doSomethingElse() {
		}
		
		public void doSomethingWithVarArgs(String...strings) {
		}
	}
	
    public void testInterfacesAreInstrumented() throws Exception {
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(DoSomethingElseTest.class, "-vtrue,-eorg.perfmon4j", perfmon4jJar);
    	
    	assertTrue("Should have instrumented method declared in iterface",
    			output.contains("Adding extreme monitor: org.perfmon4j.instrument.PerfMonTimerTransformerTest$DoSomethingElseTest.doSomething"));
    }
    
    
    public void testVarArgsMethodIsInstrumented() throws Exception {
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(DoSomethingElseTest.class, "-vtrue,-eorg.perfmon4j", perfmon4jJar);
//System.out.println(output);    	

//		CtClass clazz = ClassPool.getDefault().getCtClass(DoSomethingElseTest.class.getName());
//		RuntimeTimerInjector.injectPerfMonTimers(clazz, false);
    	assertTrue("Should have instrumented var args method",
    			output.contains("Adding extreme monitor: org.perfmon4j.instrument.PerfMonTimerTransformerTest$DoSomethingElseTest.doSomethingWithVarArgs"));
    }
    
    public static class ExceptionTrackerTest implements Runnable {

		@Override
		public void run() {
			
			try {
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass("generated.perfmon4j.ExceptionBridge");
				System.out.println("Loaded class: " + clazz);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			
			
			System.out.println("Before Exception count: " + ExceptionTracker.getExceptionCount());
			System.out.println("Before RuntimeException count: " + ExceptionTracker.getRuntimeExceptionCount());
			System.out.println("Before Error count: " + ExceptionTracker.getErrorCount());
			Exception ex = new Exception();
			ex = new Exception("Message", new Throwable());
			
			Error er = new Error();
			
			RuntimeException rex = new RuntimeException();
			
			System.out.println("After Exception count: " + ExceptionTracker.getExceptionCount());
			System.out.println("After RuntimeException count: " + ExceptionTracker.getRuntimeExceptionCount());
			System.out.println("After Error count: " + ExceptionTracker.getErrorCount());
		}
	}    

    public void testExceptionTrackerInstallation() throws Exception {
    	String output = LaunchRunnableInVM.run(ExceptionTrackerTest.class, "-vtrue,-eorg.perfmon4j,-eExceptionTracker", null, perfmon4jJar);
System.out.println(output);    	
    }
    
    
	public final static class ValveClass {
	}
	
	public final static class EngineClass {
		String hostName = null;
		ValveClass valve = null;
		
		public void setDefaultHost(String host) {
			this.hostName = host;
		}
		
		public void addValve(ValveClass valve) {
			this.valve = valve;
		}
		
		public Object getFirst() {
			return valve;
		}
	}

    public static class ValveInstaller implements Runnable {
		public void run() {
			EngineClass engine = new EngineClass();
			engine.setDefaultHost("myhost");
			
			if (engine.valve != null) {
				System.out.println("Valve hook has been installed");
			} else {
				System.out.println("Valve hook has NOT been installed");
			}
			
		}
    }    
    
    public void testValveInstalledInCatalinaEngine() throws Exception {
    	Properties props = new Properties();
    	
    	props.setProperty("Perfmon4j.catalinaEngine", EngineClass.class.getName());
    	props.setProperty("Perfmon4j.webValve", ValveClass.class.getName());
    	
    	
    	
    	String output = LaunchRunnableInVM.run(ValveInstaller.class, "-eVALVE", "", props, perfmon4jJar);
//    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(ValveInstaller.class, "", perfmon4jJar);
//System.out.println(output);    	
		assertTrue("Should have installed valve hook",
				output.contains("Valve hook has been installed"));
    }

    
    
    public static class NoWrapperMethodTest implements Runnable {
		public static final class BogusAppender extends Appender {
			public BogusAppender(AppenderID id) {
				super(id);
			}
			
			@Override
			public void outputData(PerfMonData data) {
				if (data instanceof IntervalData) {
					IntervalData d = (IntervalData)data;
					System.out.println("Monitor: " + d.getOwner().getName() + " Completions:" + d.getTotalCompletions());
				} else {
					System.out.println(data.toAppenderString());
				}
			}
		}
		
    	@DeclarePerfMonTimer("PerfmonTest")
    	public void thisMethodShouldInstrumented() {
    	}
    	
    	public void thisMethodShouldHandleException() throws Throwable {
    		throw new ThreadDeath();
    	}
    	

		public void run() {
			try {
				
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "org.perfmon4j.instrument.PerfMonTimerTransformerTest$NoWrapperMethodTest";
				final String monitorNameAnnotation = "PerfmonTest";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineMonitor(monitorNameAnnotation);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "/*");
				config.attachAppenderToMonitor(monitorNameAnnotation, appenderName, ".");
				
				PerfMon.configure(config);
				
				Method methods[] = NoWrapperMethodTest.class.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					System.out.println("Found Method: " + methods[i].getName());
				}
				
				thisMethodShouldInstrumented();
				try {
					thisMethodShouldHandleException();
				} catch (Throwable th) {
					// nothing todo... 
				}
				
				// Give time for the appender to write it's output...
				Thread.sleep(5000);
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
    	
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testForceNoWrapperMethod() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "false");
    	
    	String output = LaunchRunnableInVM.run(NoWrapperMethodTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", props, perfmon4jJar);
//    	System.out.println(output);   	
    	
    	assertFalse("Should not have added a wrapper method", output.contains("Found Method: thisMethodShouldInstrumented$"));
    	
    	assertTrue("Should have an extreme monitor active", output.contains("org.perfmon4j.instrument.PerfMonTimerTransformerTest$NoWrapperMethodTest.thisMethodShouldInstrumented Completions:1"));
    }
    
    public void testValidateNoWrapperMethodInstrumentationHandlesException() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "false");
    	
    	String output = LaunchRunnableInVM.run(NoWrapperMethodTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", props, perfmon4jJar);
//    	System.out.println(output);   	
    	
    	assertTrue("Should have an extreme monitor active", output.contains("org.perfmon4j.instrument.PerfMonTimerTransformerTest$NoWrapperMethodTest.thisMethodShouldHandleException Completions:1"));
    }

    public void testValidateNoWrapperMethodAnnotation() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "false");
    	
    	String output = LaunchRunnableInVM.run(NoWrapperMethodTest.class, "-vtrue,-btrue,-eorg.perfmon4j,-aorg.perfmon4j", "", props, perfmon4jJar);
//    	System.out.println(output);   	
    	
    	assertTrue("Should have an annotation monitor", output.contains("PerfmonTest Completions:1"));
    }

    
    public static class PerformanceTest implements Runnable {
		public static final class BogusAppender extends Appender {
			public BogusAppender(AppenderID id) {
				super(id);
			}
			
			@Override
			public void outputData(PerfMonData data) {
				if (data instanceof IntervalData) {
					IntervalData d = (IntervalData)data;
					System.out.println("Monitor: " + d.getOwner().getName() + " Completions:" + d.getTotalCompletions());
				} else {
					System.out.println(data.toAppenderString());
				}
			}
		}
		
    	@DeclarePerfMonTimer("PerfmonTest")
    	public void thisMethodShouldInstrumented() {
    		call1();
    	}
    	
    	public void call1() {
    		call2();
    	}
    	
    	public void call2() {
    		call3();
    	}
    	
    	public void call4() {
    		call5();
    	}
    	
    	public void call5() {
    	}
    	

    	public void call3() {
    	}
    	
		public void run() {
			long start = System.currentTimeMillis();
			for (int x = 0; x < 1000; x++) {
				thisMethodShouldInstrumented();
			}
			System.out.println("Total Duration: " + (System.currentTimeMillis() - start));
		}
    }

    
//    public void testComparePerformanceWrapperVsNoWrapper() throws Exception {
//    	Properties props = new Properties();
//    	props.setProperty("Perfmon4j.NoWrapperMethod", "false");
//    	
//    	String output = LaunchRunnableInVM.run(PerformanceTest.class, "-eorg.perfmon4j,-aorg.perfmon4j", "", props, perfmon4jJar);
//System.out.println(output);
//
//		props.setProperty("Perfmon4j.NoWrapperMethod", "true");
//		output = LaunchRunnableInVM.run(PerformanceTest.class, "-eorg.perfmon4j,-aorg.perfmon4j", "", props, perfmon4jJar);
//System.out.println(output);
//    }
        
    public void testThreadTraceWithSQLTimeWithNoWrapperMethod() throws Exception {
    	Properties props = new Properties();
    	props.setProperty(PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY, "false");
    	
    	String output = LaunchRunnableInVM.run(SQLStatementTester.class, "-eSQL(DERBY)", "", props, perfmon4jJar);
//System.out.println(output);    
    	// Running with extreme SQL instrumentation enabled...
    	// Looking for a line like:
    	// +-14:37:29:653 (5436)(SQL:3211) MyManualTimer
    	Pattern p = Pattern.compile("\\d{1,2}:\\d{2}:\\d{2}:\\d{3} \\(\\d{4,8}\\)\\(SQL:\\d{1,8}\\) MyManualTimer");
    	Matcher m = p.matcher(output);
    	assertTrue("Should have included SQL time specification", m.find());
     }
    
	public static class UnbalancedThreadTraceTest implements Runnable {
		private static String THREAD_TRACE_CATEGORY = "ThreadTrace";
		public static final class BogusAppender extends Appender {
			public BogusAppender(AppenderID id) {
				super(id);
			}
			
			@Override
			public void outputData(PerfMonData data) {
				System.out.println(data.toAppenderString());
			}
		}
		
		// This creates an unbalanced thread trace.
		public PerfMonTimer startMonitor() {
			return PerfMonTimer.start(THREAD_TRACE_CATEGORY);
		}
		
		public void methodA() {
			methodB();
		}
			
		public void methodB() {
		}
		
		
		public void test() {
			PerfMonTimer timer = startMonitor();
			try {
				methodA();
				methodB();
			} finally {
				PerfMonTimer.stop(timer);
			}
		}
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String appenderName = "bogus";
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				
				ThreadTraceConfig tcConfig = new ThreadTraceConfig();
				tcConfig.addAppender(config.getAppenderForName(appenderName));
				
				// Put the thread trace on the test method.
				config.addThreadTraceConfig(this.getClass().getName() + ".test", tcConfig);
				
				PerfMon.configure(config);
				
				test();
				Appender.flushAllAppenders();
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * The manual timer contained in the startMonitor method 
	 * creates an unbalanced call stack for the thread trace.
	 * To clean this up the call to startMonitor should NOT be part of
	 * the thread trace output. 
	 * @throws Exception
	 */
    public void testUnbalancedThreadTrace() throws Exception {
    	String output = LaunchRunnableInVM.run(UnbalancedThreadTraceTest.class, "-eorg.perfmon4j", "", perfmon4jJar);
//    	System.out.println(output);
    	
    	assertTrue("Should not display the startMonitor method", !output.contains(".startMonitor"));
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        System.setProperty("Perfmon4j.debugEnabled", "true");
		System.setProperty("JAVASSIST_JAR",  "G:\\projects\\perfmon4j\\.repository\\javassist\\javassist\\3.20.0-GA\\javassist-3.20.0-GA.jar");
		
        org.apache.log4j.Logger.getLogger(PerfMonTimerTransformerTest.class.getPackage().getName()).setLevel(Level.INFO);
        String[] testCaseName = {PerfMonTimerTransformerTest.class.getName()};

        TestRunner.main(testCaseName);
    }

    

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
    	String testType = System.getProperty("UNIT");
//    	if (testType == null) {
//    		System.setProperty("Perfmon4j.debugEnabled", "true");
//    		System.setProperty("JAVASSIST_JAR",  "G:\\projects\\perfmon4j\\.repository\\javassist\\javassist\\3.10.0.GA\\javassist-3.10.0.GA.jar");
//    	}
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//		newSuite.addTest(new PerfMonTimerTransformerTest("testForceNoWrapperMethod"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testValidateNoWrapperMethodInstrumentationHandlesException"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testValidateNoWrapperMethodAnnotation"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testThreadTraceWithSQLTimeWithNoWrapperMethod"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testComparePerformanceWrapperVsNoWrapper"));
        
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonTimerTransformerTest.class);
        }

        return( newSuite);
    }
}

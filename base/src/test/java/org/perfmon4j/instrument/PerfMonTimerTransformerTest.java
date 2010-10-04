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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.Appender;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.MiscHelper;


public class PerfMonTimerTransformerTest extends TestCase {
	private static Logger logger = Logger.getLogger(PerfMonTimerTransformerTest.class);
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

	private File perfmon4jJar = null;
	
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
        
        System.out.println("perfmon4j jar file: " + perfmon4jJar.getCanonicalPath());
        
        initJavaAssistJar();
    }
    
    

/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	File folder = perfmon4jJar.getParentFile();
        perfmon4jJar.delete();
        folder.delete();
    	super.tearDown();
    }
    
    public static class TemplatedClassTest<T> {
    	final T value;
    	
    	public TemplatedClassTest(T value) {
    		this.value = value;
    	}
    	
    	public void thisMethodShouldBeAnnotated() {
    	}
    }

    private void initJavaAssistJar() {
    	String javaAssistProp = System.getProperty("JAVASSIST_JAR");
    	if (javaAssistProp == null) {
    		String filePath = System.getProperty("user.home") + 
    			"/.m2/repository/javassist/javassist/3.11.0.GA/javassist-3.11.0.GA.jar";
        	logger.warn("JAVASSSIST_JAR system property NOT set...  Trying default location: " + filePath);
        	System.setProperty("JAVASSIST_JAR", filePath);
    	}
    }
    
    /*----------------------------------------------------------------------------*/    
    public void testTemplatedClassIsAnnotated() throws Exception {
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(TemplatedClassTest.class, "-dtrue,-btrue,-eorg.perfmon4j", perfmon4jJar);

    	assertTrue("Should have an $impl method indicating class was annotated" + output,
    			output.contains("thisMethodShouldBeAnnotated$1$Impl()"));
    }

    
    /*----------------------------------------------------------------------------*/    
    public void testAnnotateBootStrapClass() throws Exception {
    	String output = LaunchRunnableInVM.loadClassAndPrintMethods(String.class, "-dtrue,-ejava.lang.String,-btrue", perfmon4jJar);

    	final String validationString = "BootStrapMonitor: java.lang.String.equals";
    	
    	assertTrue("Should have added a bootstrap monitor: " + output,
    			output.contains(validationString));
    	
    	// Now check to ensure that the inserting bootstrap timers is not on by default.
       	output = LaunchRunnableInVM.loadClassAndPrintMethods(String.class, "e=java.lang.String", perfmon4jJar);
    	
    	assertFalse("b=true must be passed to java agent for bootstrap class insturmentation: " + output,
    			output.contains(validationString));
    	
    }

    /*----------------------------------------------------------------------------*/    
    public void testAnnotateSystemClass() throws Exception {
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
    	String output = LaunchRunnableInVM.run(GlobalClassLoaderTester.class, "-dfalse,-ejava.lang.String,-btrue", "", perfmon4jJar);

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
				WeakReference<Class>  clazzReference = 
					new WeakReference(loader.loadClass("org.perfmon4j.instrument.TestClass"));
				
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
		
		
		// Messages should appear when verbose is enabled "-vtrue"
		String output = LaunchRunnableInVM.loadClassAndPrintMethods(VerboseClassTest.class, VERBOSE_PARAMS, perfmon4jJar);
    	validateOutput(output, VERBOSE_CLASS_MSG, true);
    	validateOutput(output, VERBOSE_METHOD_MSG, true);
    	validateOutput(output, VERBOSE_SETTER_MSG, true);
    	validateOutput(output, VERBOSE_GETTER_MSG, true);
    	validateOutput(output, VERBOSE_ISSER_MSG, true);
    	validateOutput(output, VERBOSE_ANOTATION_MSG, true);
    	
		// Messages should appear when debug is enabled "-dtrue"
		output = LaunchRunnableInVM.loadClassAndPrintMethods(VerboseClassTest.class, DEBUG_PARAMS, perfmon4jJar);
    	validateOutput(output, VERBOSE_CLASS_MSG, true);
    	validateOutput(output, VERBOSE_METHOD_MSG, true);
    	validateOutput(output, VERBOSE_SETTER_MSG, true);
    	validateOutput(output, VERBOSE_GETTER_MSG, true);
    	validateOutput(output, VERBOSE_ISSER_MSG, true);
    	validateOutput(output, VERBOSE_ANOTATION_MSG, true);

		// Messages should NOT appear unless debug OR verbose is enabled!
    	output = LaunchRunnableInVM.loadClassAndPrintMethods(VerboseClassTest.class, BASE_PARAMS, perfmon4jJar);
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
    	String output = LaunchRunnableInVM.run(SQLStatementTester.class, "-eSQL(DERBY)", "", perfmon4jJar);
    	System.out.println(output);   	
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
	
    
	public static class MoveParameterAnnotationTest implements Runnable {
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.PARAMETER)
		public @interface BogusAnnotation {
		}

		public void doSomethingCool(@BogusAnnotation int a) {
		}
		
		public void run() {
			try {
				Method methods[] = MoveParameterAnnotationTest.class.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					System.out.println("Found Method: " + methods[i].getName());
				}
				
				Method method = MoveParameterAnnotationTest.class.getMethod("doSomethingCool", new Class<?>[]{int.class});

				// Get the number of annotations on the first parameter....
				int numParameterAnnotations  = method.getParameterAnnotations()[0].length;
				
				System.out.println("numParameterAnnotations: " + numParameterAnnotations);
				if (numParameterAnnotations == 1) {
					System.out.println("Parameter annotation FOUND");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
    public void testMoveAnnotation() throws Exception {
    	String output = LaunchRunnableInVM.run(MoveParameterAnnotationTest.class, "-vtrue,-btrue,-eorg.perfmon4j", "", perfmon4jJar);
    	System.out.println(output);   	
    	assertTrue("Annotation should have moved parameter annotation", output.contains("Parameter annotation FOUND"));
    }
	
	
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
System.out.println(output);		
    	
    	output = LaunchRunnableInVM.run(SystemGCDisablerTester.class, "-gfalse", "", perfmon4jJar);
    	final String validateNOTInstalled = "System.gc() appears enabled.";
		assertTrue("SystemGC should NOT have been disabled", output.contains(validateNOTInstalled));
    }
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        System.setProperty("Perfmon4j.debugEnabled", "true");
        System.setProperty("JAVASSIST_JAR",  "C:\\Users\\ddeucher\\.m2\\repository\\javassist\\javassist\\3.10.0.GA\\javassist-3.10.0.GA.jar");
        
        Logger.getLogger(PerfMonTimerTransformerTest.class.getPackage().getName()).setLevel(Level.INFO);
        String[] testCaseName = {PerfMonTimerTransformerTest.class.getName()};

        TestRunner.main(testCaseName);
    }

    
/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
    	String testType = System.getProperty("UNIT");
    	if (testType == null) {
    		System.setProperty("Perfmon4j.debugEnabled", "true");
    		System.setProperty("JAVASSIST_JAR",  "C:\\Users\\ddeucher\\.m2\\repository\\javassist\\javassist\\3.10.0.GA\\javassist-3.10.0.GA.jar");
    	}
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//		newSuite.addTest(new PerfMonTimerTransformerTest("testThreadTraceWithSQLTime"));
//        newSuite.addTest(new PerfMonTimerTransformerTest("testInstrumentSQLStatement"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(PerfMonTimerTransformerTest.class);
        }

        return( newSuite);
    }
}

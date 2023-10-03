package org.perfmon4j;

import java.io.File;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.emitter.EmitterController;
import org.perfmon4j.emitter.EmitterData;
import org.perfmon4j.emitter.Emitter;
import org.perfmon4j.emitter.EmitterRegistry;
import org.perfmon4j.instrument.LaunchRunnableInVM;
import org.perfmon4j.util.MiscHelper;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class EmitterSnapShotMonitorTest extends TestCase {
	private File perfmon4jJar = null;
	
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

		File apiClassesFolder = new File("../agent-api/target/classes");
		if (!apiClassesFolder.exists()) {
			apiClassesFolder = new File("./agent-api/target/classes");
		}
		
		assertTrue("Could not find classes folder in: "  + classesFolder.getCanonicalPath(), classesFolder.exists());
		assertTrue("Could not find test classes folder in: "  + testClassesFolder.getCanonicalPath(), testClassesFolder.exists());
		assertTrue("Could not find agent-api classes folder in: "  + apiClassesFolder.getCanonicalPath(), apiClassesFolder.exists());
		
        MiscHelper.createJarFile(perfmon4jJar.getAbsolutePath(), props, new File[]{classesFolder, testClassesFolder, apiClassesFolder});
    }
    
/*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	File folder = perfmon4jJar.getParentFile();
        perfmon4jJar.delete();
        folder.delete();
        
        perfmon4jJar = null;
        
    	super.tearDown();
    }

	public static class MySnapShotEmitter implements Emitter {
		private EmitterController controller = null;
		private int counter = 0;
		
		MySnapShotEmitter() {
		}

		@Override
		public void acceptController(EmitterController controller) {
			this.controller = controller;
		}

		EmitterController getController() {
			return controller;
		}
		
		public void emitData() {
			EmitterData data = controller.initData(System.currentTimeMillis() - (60 * 1000 * 60));
			data.addData("myCounter", counter++);
			data.addData("myString", "Dave" + Long.toString(System.currentTimeMillis()));
			
			controller.emit(data);
		}
	}
	
	public static class TestRunEmitter implements Runnable {
		@Override
		public void run() {
			try {
				EmitterRegistry registry = EmitterRegistry.getSingleton();
				MySnapShotEmitter myEmitter = new MySnapShotEmitter();
				
				registry.register(myEmitter);
				
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "MyEmitter";
				final String appenderName = "MyAppender";
				
				config.defineSnapShotMonitor(monitorName, MySnapShotEmitter.class.getName());
				config.defineAppender(appenderName, TextAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(monitorName, appenderName);

				
				
				PerfMon.configure(config);

				myEmitter.emitData();
				myEmitter.emitData();
				myEmitter.emitData();
				myEmitter.emitData();
				
				
				Thread.sleep(1500);
				Appender.flushAllAppenders();
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/*----------------------------------------------------------------------------*/
    public void testRunEmitter() throws Exception {
    	String output = LaunchRunnableInVM.run(TestRunEmitter.class,"-vtrue", "", perfmon4jJar);
System.out.println(output);
    	TestHelper.validateNoFailuresInOutput(output);
		// Should not include instance name when POJO is registered without an instance name
//		assertTrue("Expected appender output not found", output.contains("AppenderOutput: counter(1)"));
    }    
    
    
    
	public static final class SystemOutAppender extends Appender {
		public SystemOutAppender(AppenderID id) {
			super(id);
		}
		
		@Override
		public void outputData(PerfMonData data) {
			if (data instanceof PerfMonObservableData) {
				PerfMonObservableData obData = (PerfMonObservableData)data;
				String dataString = null; 
				for (PerfMonObservableDatum<?> datum : obData.getObservations()) {
					dataString = (dataString == null) ? "" : dataString + ",";
					dataString += datum.getFieldName() + "(" + datum.getComplexObject() + ")";
				}
				System.out.println("AppenderOutput: " + dataString);
			}
		}
	}    
    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {EmitterSnapShotMonitorTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(EmitterSnapShotMonitorTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new POJOSnapShotMonitorTest("testSimpleGetMonitorKeyWithFieldsFromMonitorWithInstances"));
//        newSuite.addTest(new POJOSnapShotMonitorTest("testSimpleGetMonitorKeyWithStringField"));
        
        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(EmitterSnapShotMonitorTest.class);
        }

        return(newSuite);
    }
}

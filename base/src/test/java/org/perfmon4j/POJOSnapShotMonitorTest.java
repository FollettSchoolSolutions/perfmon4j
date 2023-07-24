package org.perfmon4j;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.instrument.LaunchRunnableInVM;
import org.perfmon4j.instrument.LaunchRunnableInVM.ProcessArgs;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotPOJO;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.MiscHelper;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class POJOSnapShotMonitorTest extends TestCase {
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

	@SnapShotPOJO
	public static class MyPOJOSnapShot {
		private final AtomicLong counter = new AtomicLong();
		private final int incrementValue;
		
		MyPOJOSnapShot() {
			this(1);
		}
		
		MyPOJOSnapShot(int incrementValue) {
			this.incrementValue = incrementValue;
		}

		@SnapShotCounter
		public long getCounter() {
			return counter.addAndGet(incrementValue);
		}
	}
	
	public static class TestRunSnapShotPOJO implements ProcessArgs {
		public static final String MULTI_PARAM = "MULTI_PARAM";
		private String arg1 = null;
		
		@Override
		public void processArgs(String[] args) {
			if (args.length > 1) {
				arg1 = args[1];
			}
		}
		
		@Override
		public void run() {
			try {
				POJOSnapShotRegistry registry = POJOSnapShotRegistry.getSingleton();
				if (arg1 == null) {
					registry.register(new MyPOJOSnapShot(), false);
				} else if (MULTI_PARAM.equals(arg1)) {
					for (int i = 1; i <= 5; i++) {
						registry.register(new MyPOJOSnapShot(i), "Instance" + i, false);
					}
				} else {
					registry.register(new MyPOJOSnapShot(), arg1, false);
				}
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "MySnapShot";
				final String appenderName = "MyAppender";
				
				config.defineSnapShotMonitor(monitorName, MyPOJOSnapShot.class.getName());
				config.defineAppender(appenderName, SystemOutAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(monitorName, appenderName);
				PerfMon.configure(config);
				
				Thread.sleep(1500);
				Appender.flushAllAppenders();
			} catch (Exception ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/*----------------------------------------------------------------------------*/
    public void testRunSnapShotPOJO() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestRunSnapShotPOJO.class, perfmon4jJar));
//System.out.println(output);
		// Should not include instance name when POJO is registered without an instance name
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: counter(1)"));
    	
    }    
    
	/*----------------------------------------------------------------------------*/
    public void testRunSnapShotPOJOWithInstanceName() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestRunSnapShotPOJO.class, perfmon4jJar)
        		.setProgramArguments("myInstanceName"));
//System.out.println(output);
		// Check to see if the appender output included the instanceName:
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: instanceName(myInstanceName),counter(1)"));
    }
    
	/*----------------------------------------------------------------------------*/
    public void testRunSnapShotPOJOWithMultipleNamedInstances() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestRunSnapShotPOJO.class, perfmon4jJar)
        		.setProgramArguments(TestRunSnapShotPOJO.MULTI_PARAM));
//System.out.println(output);
		// Check to make sure all 5 instances reported.:
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: instanceName(Instance1),counter(1)"));
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: instanceName(Instance2),counter(2)"));
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: instanceName(Instance3),counter(3)"));
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: instanceName(Instance4),counter(4)"));
		assertTrue("Expected appender output not found", output.contains("AppenderOutput: instanceName(Instance5),counter(5)"));
    }
	
	@SnapShotPOJO(usePriorityTimer = true)
	public static class MySpyOnCallingThread {
		@SnapShotString
		public String getCallingThreadName() {
			return Thread.currentThread().getName();
		}
	}

	public static class TestSpyOnCallingThread implements Runnable {
		@Override
		public void run() {
			try {
				POJOSnapShotRegistry registry = POJOSnapShotRegistry.getSingleton();
				registry.register(new MySpyOnCallingThread(), false);
				
				
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "MySnapShot";
				final String appenderName = "MyAppender";
				
				config.defineSnapShotMonitor(monitorName, MySpyOnCallingThread.class.getName());
				config.defineAppender(appenderName, SystemOutAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(monitorName, appenderName);
				PerfMon.configure(config);
				
				Thread.sleep(1500);
				Appender.flushAllAppenders();
			} catch (Exception ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	
    
	/*----------------------------------------------------------------------------*/
    public void testPriorityTimer() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestSpyOnCallingThread.class, perfmon4jJar));
//System.out.println(output);
    	// Verify the snapshot was called by the priority Timer.
		assertTrue("Expected the snapshot to use the priority timer", output.contains("AppenderOutput: callingThreadName(PerfMon.priorityTimer)"));
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
        String[] testCaseName = {SnapShotManagerTest.class.getName()};
        
        BasicConfigurator.configure();
        Logger.getLogger(POJOSnapShotMonitorTest.class.getPackage().getName()).setLevel(Level.DEBUG);
        
        
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
            newSuite = new TestSuite(POJOSnapShotMonitorTest.class);
        }

        return(newSuite);
    }
}

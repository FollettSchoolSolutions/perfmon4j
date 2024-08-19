/*
 *	Copyright 2019 Follett School Solutions 
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
 * 	ddeuchert@follett.com
 * 	David Deuchert
 * 	Follett School Solutions
*/
package org.perfmon4j.instrument;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.perfmon4j.Appender;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.TestHelper;
import org.perfmon4j.TextAppender;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.perfmon4j.emitter.EmitterRegistry;
import org.perfmon4j.instrument.PerfMonTimerTransformerTest.SQLStatementTester.BogusAppender;
import org.perfmon4j.util.MiscHelper;

import api.org.perfmon4j.agent.PerfMonTimer;
import api.org.perfmon4j.agent.util.SingletonTracker;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


public class PerfMonAgentAPITest extends PerfMonTestCase {
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

	private File perfmon4jJar = null;
	
/*----------------------------------------------------------------------------*/
    public PerfMonAgentAPITest(String name) {
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

	public static class AgentAPIUsageTest implements Runnable {
		public void run() {
			if (api.org.perfmon4j.agent.PerfMon.isAttachedToAgent()) {
				System.err.println("Agent API for PerfMon class has been instrumented");
			} else {
				System.err.println("Agent API for PerfMon class has NOT been instrumented");
			}
			if (api.org.perfmon4j.agent.PerfMonTimer.isAttachedToAgent()) {
				System.err.println("Agent API for PerfMonTimer class has been instrumented");
			} else {
				System.err.println("Agent API for PerfMonTimer class has NOT been instrumented");
			}
			if (api.org.perfmon4j.agent.SQLTime.isAttachedToAgent()) {
				System.err.println("Agent API for SQLTime class has been instrumented");
			} else {
				System.err.println("Agent API for SQLTime class has NOT been instrumented");
			}
			if (api.org.perfmon4j.agent.POJOSnapShotRegistry.isAttachedToAgent()) {
				System.err.println("Agent API for POJOSnapShotRegistry class has been instrumented");
			} else {
				System.err.println("Agent API for POJOSnapShotRegistry class has NOT been instrumented");
			}
		}
	}

    public void testObjectsAreAttached() throws Exception {
    	String output = LaunchRunnableInVM.run(
    		new LaunchRunnableInVM.Params(AgentAPIUsageTest.class, perfmon4jJar));
//System.out.println("org.perfmon4j.instrument.PerfMonAgentAPITest#testObjectsAreAttached\r\n" +output);   	
    	TestHelper.validateNoFailuresInOutput(output);
    	
    	assertTrue("PerfMon API class was not attached to agent", output.contains("Agent API for PerfMon class has been instrumented"));
    	assertTrue("PerfMonTimer API class was not attached to agent", output.contains("Agent API for PerfMonTimer class has been instrumented"));
    	assertTrue("SQLTime API class was not attached to agent", output.contains("Agent API for SQLTime class has been instrumented"));
    	assertTrue("POJOSnapShotRegistry API class was not attached to agent", output.contains("Agent API for POJOSnapShotRegistry class has been instrumented"));
    }
	
	
	public static class AgentAPIPerfMonInstTest implements Runnable {
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test.category";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "./*");
				
				if (api.org.perfmon4j.agent.PerfMon.isConfigured()) {
					System.out.println("**FAIL: PerfMon agent incorrectly showing as configured prior to configuration");
				}
				
				PerfMon.configure(config);

				if (!api.org.perfmon4j.agent.PerfMon.isConfigured()) {
					System.out.println("**FAIL: PerfMon agent incorrectly showing as NOT configured after configuration");
				}
				
				api.org.perfmon4j.agent.PerfMon apiPerfMon = api.org.perfmon4j.agent.PerfMon.getMonitor("not.active");
				if (apiPerfMon.isActive()) {
					System.out.println("**FAIL: 'not.active' is NOT configured to be monitored/active");
				}
				
				if (!"not.active".equals(apiPerfMon.getName())) {
					System.out.println("**FAIL: Incorrect monitor name, should have been 'not.active'");
				}

				apiPerfMon = api.org.perfmon4j.agent.PerfMon.getMonitor("test.category");
				if (!apiPerfMon.isActive()) {
					System.out.println("**FAIL: 'test.category' is configured to be monitored/active");
				}
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
    public void testAttachedPerfMonAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(AgentAPIPerfMonInstTest.class, perfmon4jJar));
    	TestHelper.validateNoFailuresInOutput(output);
    }

	public static class AgentAPIPerfMonTimerInstTest implements Runnable {
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test123.category";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "./*");
				PerfMon.configure(config);
				
				
				/* Test start with passing in an agent and abort */
				api.org.perfmon4j.agent.PerfMon apiPerfMon = api.org.perfmon4j.agent.PerfMon.getMonitor(monitorName);
				api.org.perfmon4j.agent.PerfMonTimer apiTimer = api.org.perfmon4j.agent.PerfMonTimer.start(apiPerfMon);
				api.org.perfmon4j.agent.PerfMonTimer.abort(apiTimer);
				
				PerfMon nativePerfMon = ((PerfMonAgentApiWrapper)apiPerfMon).getNativeObject();
				if (nativePerfMon.getTotalHits() != 1) {
					System.out.println("**FAIL: expected 1 hit");
				}
				if (nativePerfMon.getTotalCompletions() != 0) {
					System.out.println("**FAIL: still should have 0 completions, because we aborted");
				}
				
				
				/* Test start with passing in an string and stop */
				apiTimer = api.org.perfmon4j.agent.PerfMonTimer.start(monitorName);
				api.org.perfmon4j.agent.PerfMonTimer.stop(apiTimer);
				if (nativePerfMon.getTotalHits() != 2) {
					System.out.println("**FAIL: expected 2 hits");
				}
				if (nativePerfMon.getTotalCompletions() != 1) {
					System.out.println("**FAIL: still should have 1 completion, we aborted first time but passed the second");
				}
				
				// Make sure a nullTimer functions as expected for stop and abort
				api.org.perfmon4j.agent.PerfMonTimer.stop(api.org.perfmon4j.agent.PerfMonTimer.getNullTimer());
				api.org.perfmon4j.agent.PerfMonTimer.abort(api.org.perfmon4j.agent.PerfMonTimer.getNullTimer());
				
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAttachedPerfMonTimerAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(AgentAPIPerfMonTimerInstTest.class, perfmon4jJar));
    	TestHelper.validateNoFailuresInOutput(output);
    }
	
	public static class AgentAPIPerfMonTimerStartReactiveTest implements Runnable {
		
		public void validateTimersAreReactive(api.org.perfmon4j.agent.PerfMon apiPerfMon, 
				api.org.perfmon4j.agent.PerfMonTimer timer1,
				api.org.perfmon4j.agent.PerfMonTimer timer2,
				api.org.perfmon4j.agent.PerfMonTimer timer3) {
			
			api.org.perfmon4j.agent.PerfMonTimer.stop(timer3);
			api.org.perfmon4j.agent.PerfMonTimer.stop(timer2);
			api.org.perfmon4j.agent.PerfMonTimer.stop(timer1);
			
			PerfMon nativePerfMon = ((PerfMonAgentApiWrapper)apiPerfMon).getNativeObject();
			if (nativePerfMon.getTotalHits() != 3) {
				System.out.println("**FAIL: expected 3 hits");
			}
			if (nativePerfMon.getTotalCompletions() != 3) {
				System.out.println("**FAIL: still should have 3 completions");
			}
		}
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorRootName = "test123";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorRootName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorRootName, appenderName, "./*");
				PerfMon.configure(config);
				
				
				
				
				
				
				
				/* Test start with passing in an agent and abort */
				// When starting in reactive mode, a single thread must be able to 
				// start the same monitor multiple times.
				// In non-reactive mode, a new start will not occur on the thread, unless the previous one is stopped.

				
				// Try perfMonTimer.startReactive(PerfMon) overload
				String monitorName = monitorRootName + ".1";
				api.org.perfmon4j.agent.PerfMon apiPerfMon = api.org.perfmon4j.agent.PerfMon.getMonitor(monitorName);
				
				api.org.perfmon4j.agent.PerfMonTimer timer1 = api.org.perfmon4j.agent.PerfMonTimer.startReactive(apiPerfMon, false);
				api.org.perfmon4j.agent.PerfMonTimer timer2 = api.org.perfmon4j.agent.PerfMonTimer.startReactive(apiPerfMon, false);
				api.org.perfmon4j.agent.PerfMonTimer timer3 = api.org.perfmon4j.agent.PerfMonTimer.startReactive(apiPerfMon, false);

				validateTimersAreReactive(apiPerfMon, timer1, timer2, timer3);
				
				// Try perfMonTimer.startReactive(String) overload
				monitorName = monitorRootName + ".2";
				apiPerfMon = api.org.perfmon4j.agent.PerfMon.getMonitor(monitorName);
				
				timer1 = api.org.perfmon4j.agent.PerfMonTimer.startReactive(monitorName);
				timer2 = api.org.perfmon4j.agent.PerfMonTimer.startReactive(monitorName);
				timer3 = api.org.perfmon4j.agent.PerfMonTimer.startReactive(monitorName);

				validateTimersAreReactive(apiPerfMon, timer1, timer2, timer3);
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAttachedPerfMonTimerAPIStartReactive() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(AgentAPIPerfMonTimerStartReactiveTest.class, perfmon4jJar));
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }    
    
	public static class TestStartReactiveBase implements Runnable {
		public static enum Mode {
			EXPLICIT_OUTER_WITH_ATTACH_INNER,
			EXPLICIT_OUTER_NO_ATTACH_INNER,
			NO_OUTER_REACTIVE_WITH_ATTACH_INNER
		}
		
		private final Mode mode;
		
		public TestStartReactiveBase(Mode mode) {
			this.mode = mode;
		}
		
		public void run() {
			try {
				final String monitorWebRequest = "WebRequest";
				final String monitorOperation = "Operation";
				final String appenderName = "bogus";
				PerfMonConfiguration config = new PerfMonConfiguration();
				
				config.defineMonitor(monitorWebRequest);
				config.defineMonitor(monitorOperation);
				
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorWebRequest, appenderName, ".");
				config.attachAppenderToMonitor(monitorOperation, appenderName, ".");
				
				PerfMon.configure(config);
				
				final String webReactiveContext = "CTX_1";

				// Simulate an HttpRequest monitor that starts a reactive context
				
				
				api.org.perfmon4j.agent.PerfMonTimer requestTimer; 
				if (!mode.equals(Mode.NO_OUTER_REACTIVE_WITH_ATTACH_INNER)) {
					// Start an implicit reactive context
					requestTimer = api.org.perfmon4j.agent.PerfMonTimer.start(monitorWebRequest, false, webReactiveContext);
					System.out.println("!!! - Simulating Quarkus and starting an explicit reactive context around the entire request - !!!");
				} else {
					// No implicit reactive context
					requestTimer = api.org.perfmon4j.agent.PerfMonTimer.start(monitorWebRequest, false);
					System.out.println("!!! - No implicit reactive context is being created - !!!");
				}
				
				try {
					runNestedOperations(monitorOperation, 5);
				} finally {
					api.org.perfmon4j.agent.PerfMonTimer.stop(requestTimer);
				}
				
				PerfMon webRequestMonitor = PerfMon.getMonitor(monitorWebRequest);
				PerfMon operationMonitor = PerfMon.getMonitor(monitorOperation);
				
				assertEquals("Should have 1 webRequest completion", 1, webRequestMonitor.getTotalCompletions());
				
				if (mode.equals(Mode.EXPLICIT_OUTER_WITH_ATTACH_INNER)) {
					// Everything should roll up under the explicit context.
					System.out.println("!!! By default we roll up if there is an explicit reactive context. We expect all of the operations within that context to rollup, "
							+ "just like they would in a traditional stack based java application.");
					System.out.println("!!! Expecting only 1 invocation of the operation.");
					assertEquals("Should have 1 operation completion", 1, operationMonitor.getTotalCompletions());
				} else {
					// Each should run in its own, or the default stack based, context
					if (mode.equals(Mode.EXPLICIT_OUTER_NO_ATTACH_INNER)) {
						System.out.println("!!! Since we did not request startReactive to attach to explicit context, we do not expect a rollup.");
					} else {
						System.out.println("!!! Since we did not start an explicit context startReactive would not rollup.");
					}
					System.out.println("!!! Expecting 5 individual invocations of the operation.");
					assertEquals("Should have 5 operation completion", 5, operationMonitor.getTotalCompletions());
				}
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
		
	    private void stopTimerInSeparateThread(PerfMonTimer timer) {
	    	final CountDownLatch latch = new CountDownLatch(1);
	    	
	    	(new Thread(() -> {
	    		PerfMonTimer.stop(timer);
	    		latch.countDown();
	    	})).start();
	    	
	    	try {
				latch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }

		private void runNestedOperations(String operationTimerKey, int nestedDepth) {
			final boolean attachToExplicitReactiveContext = !mode.equals(Mode.EXPLICIT_OUTER_NO_ATTACH_INNER);
			
			PerfMonTimer operationTimer = PerfMonTimer.startReactive(operationTimerKey, false, 
					attachToExplicitReactiveContext);
			try {
				if (nestedDepth > 1) {
					runNestedOperations(operationTimerKey, nestedDepth-1);
				}
			} finally {
				stopTimerInSeparateThread(operationTimer);
			}
		}

	}
	
	private static class TestStartReactiveWithExplicitReactiveContext extends TestStartReactiveBase {
		public TestStartReactiveWithExplicitReactiveContext() {
			super(Mode.EXPLICIT_OUTER_WITH_ATTACH_INNER);
		}
	}

    public void testStartReactiveWithExplicitReactiveContext() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestStartReactiveWithExplicitReactiveContext.class, perfmon4jJar));
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }    

	private static class TestStartReactiveWithExplicitReactiveContextNoAttachInner extends TestStartReactiveBase {
		public TestStartReactiveWithExplicitReactiveContextNoAttachInner() {
			super(Mode.EXPLICIT_OUTER_NO_ATTACH_INNER);
		}
	}
    
    public void testStartReactiveWithExplicitReactiveContextNoAttachInner() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestStartReactiveWithExplicitReactiveContextNoAttachInner.class, perfmon4jJar));
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }    

	private static class TestStartReactiveWithNoExplicitContextAndAttachInner extends TestStartReactiveBase {
		public TestStartReactiveWithNoExplicitContextAndAttachInner() {
			super(Mode.NO_OUTER_REACTIVE_WITH_ATTACH_INNER);
		}
	}

	/**
	 * We are asking the nested startReactive calls to attach to an existing
	 * explicitRequestContext. Since no context has been established, each of these should
	 * run within it's own context.
	 * 
	 * @throws Exception
	 */
    public void testStartReactiveWithNoExplicitContextAndAttachInner() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(TestStartReactiveWithNoExplicitContextAndAttachInner.class, perfmon4jJar));
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }    
    
    
	public static class AgentAPISQLTimeInstTest implements Runnable {
		public void run() {
			try {
				org.perfmon4j.SQLTime.setEnabled(true);
				
				// Call the API code.
				boolean isEnabled = api.org.perfmon4j.agent.SQLTime.isEnabled();
				if (!isEnabled) {
					System.out.println("**FAIL: API should indicate sqlTime is enabled");
				}
				
				org.perfmon4j.SQLTime.startTimerForThread();
				Thread.sleep(11);
				org.perfmon4j.SQLTime.stopTimerForThread();
				
				long sqlDuration = api.org.perfmon4j.agent.SQLTime.getSQLTime();
				System.out.println("SQLDuration: " + sqlDuration);
				
				if (sqlDuration < 10) {
					System.out.println("**FAIL: Should have at least 10 millis SQLTime on this thread");
				}
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAttachedSQLTimeAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(AgentAPISQLTimeInstTest.class, perfmon4jJar));
    	TestHelper.validateNoFailuresInOutput(output);
    }

	public static class AgentDeclarePerfMonTimerInstTest implements Runnable {
		
		@api.org.perfmon4j.agent.instrument.DeclarePerfMonTimer("test345.category")
		private void doSomething() {
			
		}
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test345.category";
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToMonitor(monitorName, appenderName, "./*");
				PerfMon.configure(config);
				
				// Invoke the annotated method
				doSomething();
				
				PerfMon nativePerfMon = PerfMon.getMonitor(monitorName);
				if (nativePerfMon.getTotalCompletions() != 1) {
					System.out.println("**FAIL: expected 1 hit from the annotation");
				}
				
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
    
	
    public void testAttachedDeclarePerfmonTimerAPI() throws Exception { 
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(AgentDeclarePerfMonTimerInstTest.class, perfmon4jJar)
        		.setJavaAgentParams( "-a" + AgentDeclarePerfMonTimerInstTest.class.getName()));
//System.out.println("org.perfmon4j.instrument.PerfMonAgentAPITest#testAttachedDeclarePerfmonTimerAPI\r\n" + output);    	 
    	TestHelper.validateNoFailuresInOutput(output);
    } 
     	
	public static class SnapShotMonitorWithAPIAnnotationTest implements Runnable {
		
		@api.org.perfmon4j.agent.instrument.SnapShotProvider
		public static class MySnapShotClass {
			private int counterValue = 0;

			@api.org.perfmon4j.agent.instrument.SnapShotCounter
			public int getCounter() {
				return counterValue++;
			}
			
			@api.org.perfmon4j.agent.instrument.SnapShotGauge
			public int getGauge() {
				return 1;
			}
			
			@api.org.perfmon4j.agent.instrument.SnapShotString
			public String getString() {
				return "MyString";
			}
		}
		
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String simpleSnapShotName = "Simple";
				final String appenderName = "bogus";
				
				config.defineSnapShotMonitor(simpleSnapShotName, MySnapShotClass.class.getName());
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(simpleSnapShotName, appenderName);
				PerfMon.configure(config);
				
				Thread.sleep(2000);
				Appender.flushAllAppenders();
				
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAPISnapShotAnnotations() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(SnapShotMonitorWithAPIAnnotationTest.class, perfmon4jJar));
   
//System.out.println("org.perfmon4j.instrument.PerfMonAgentAPITest#testAPISnapShotAnnotations\r\n" + output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    	
    	assertTrue("Should have the counter value in text output", output.contains("counter.................. 1/per duration"));
    	assertTrue("Should have the gauge value in text output", output.contains("gauge.................... 1"));
    	assertTrue("Should have the string value in text output", output.contains("string................... MyString"));
    }
    
    

	public static class SnapShotMonitorWithAPIRatiosTest implements Runnable {
		@api.org.perfmon4j.agent.instrument.SnapShotProvider
		@api.org.perfmon4j.agent.instrument.SnapShotRatios(values = { 
			@api.org.perfmon4j.agent.instrument.SnapShotRatio(name="cacheHitRate", numerator="cacheHits", denominator="totalCalls", displayAsPercentage=true) 
		})
		public static class MySnapShotClass {
			private int totalCalls = 0;
			private int cacheHits = 0;

			@api.org.perfmon4j.agent.instrument.SnapShotCounter
			public int getTotalCalls() {
				return totalCalls += 4;
			}
			
			@api.org.perfmon4j.agent.instrument.SnapShotCounter
			public int getCacheHits() {
				return ++cacheHits;
			}
		}
		
		
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String simpleSnapShotName = "Simple";
				final String appenderName = "bogus";
				
				config.defineSnapShotMonitor(simpleSnapShotName, MySnapShotClass.class.getName());
				config.defineAppender(appenderName, BogusAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(simpleSnapShotName, appenderName);
				PerfMon.configure(config);
				
				Thread.sleep(2000);
				Appender.flushAllAppenders();
				
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
	
    public void testAPISnapShotRatiosAnnotations() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(SnapShotMonitorWithAPIRatiosTest.class, perfmon4jJar));
//System.out.println("org.perfmon4j.instrument.PerfMonAgentAPITest#testAPISnapShotRatiosAnnotations\r\n" + output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    	
    	assertTrue("Should have the cacheHitRate value in text output", output.contains("cacheHitRate............. 25.000%"));
    }

	public static class AgentGetConfiguredSettings implements Runnable {
		public void run() {
			try {
				for (Map.Entry<Object, Object> entry : api.org.perfmon4j.agent.PerfMon.getConfiguredSettings().entrySet()) {
					System.out.println(entry.getKey() + "=" + entry.getValue());
				}
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
    public void testGetConfiguredSettings() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(AgentGetConfiguredSettings.class, perfmon4jJar));
//System.out.println(output);    	
		assertTrue("should find property for perfmon4j agent version (even though it might be null)",
				output.contains("perfmon4j.javaagent.version="));
    	TestHelper.validateNoFailuresInOutput(output);
    }

    private static ThreadTraceConfig createThreadTraceConfig(AppenderID appenderID, Trigger... triggers) {
    	ThreadTraceConfig config = new ThreadTraceConfig();
    	
    	config.addAppender(appenderID);
    	config.setTriggers(triggers);
    	
    	return config;
    }
    
    
	public static class HasTriggers implements Runnable {
		protected void configurePerfmon4j() throws Exception {
			final String monitorName = "WebRequest";
			final String appenderName = "textAppender";
			PerfMonConfiguration config = new PerfMonConfiguration();
			
			config.defineAppender(appenderName, TextAppender.class.getName(), "1 minute", null);
			final AppenderID appenderID = config.getAppenderForName(appenderName);
			
			config.defineMonitor(monitorName);
			config.attachAppenderToMonitor(monitorName, appenderName);

			config.addThreadTraceConfig(monitorName, createThreadTraceConfig(appenderID,
				new Trigger[] {
						new ThreadTraceConfig.HTTPRequestTrigger("name", "dave"),
						new ThreadTraceConfig.HTTPSessionTrigger("userName", "dave"),
						new ThreadTraceConfig.HTTPCookieTrigger("jsessionid", "1234")
				}));
			PerfMon.configure(config);
		}
		
		
		public void run() {
			try {
				assertFalse("Should NOT have a request based trigger before config", 
						api.org.perfmon4j.agent.PerfMon.hasHttpRequestBasedThreadTraceTriggers());
				assertFalse("Should NOT have a session based trigger before config", 
						api.org.perfmon4j.agent.PerfMon.hasHttpSessionBasedThreadTraceTriggers());
				assertFalse("Should NOT have a cookie based trigger before config", 
						api.org.perfmon4j.agent.PerfMon.hasHttpCookieBasedThreadTraceTriggers());
				

				configurePerfmon4j();
				
				assertTrue("Should have a request based trigger after config", 
						api.org.perfmon4j.agent.PerfMon.hasHttpRequestBasedThreadTraceTriggers());
				assertTrue("Should have a session based trigger after config", 
						api.org.perfmon4j.agent.PerfMon.hasHttpSessionBasedThreadTraceTriggers());
				assertTrue("Should have a cookie based trigger after config", 
						api.org.perfmon4j.agent.PerfMon.hasHttpCookieBasedThreadTraceTriggers());
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	
	
	
	
    public void testHasTriggers() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(HasTriggers.class, perfmon4jJar));
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }

	public static class InstallAgentValidator extends HasTriggers {
		static final String AFTER_POPVALIDATOR = "***After PopValidator***"; 
		
		public void run() {
			try {
				configurePerfmon4j();

				// Our validator should be invoked for each trigger.
				api.org.perfmon4j.agent.ThreadTraceConfig.pushValidator(new api.org.perfmon4j.agent.SimpleTriggerValidator() {
					@Override
					public boolean isValid(String triggerString) {
						System.out.println("[" + triggerString + "]");
						return false;
					}
				});
				try {
					api.org.perfmon4j.agent.PerfMonTimer requestTimer = api.org.perfmon4j.agent.PerfMonTimer.start("WebRequest");
					api.org.perfmon4j.agent.PerfMonTimer.stop(requestTimer);
				} finally {
					api.org.perfmon4j.agent.ThreadTraceConfig.popValidator();
				}
	
				System.out.println(AFTER_POPVALIDATOR);
	
				// Since we removed our validator it should no longer be called.
				api.org.perfmon4j.agent.PerfMonTimer requestTimer = api.org.perfmon4j.agent.PerfMonTimer.start("WebRequest");
				api.org.perfmon4j.agent.PerfMonTimer.stop(requestTimer);
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
    
    public void testInstallAgentValidator() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(InstallAgentValidator.class, perfmon4jJar));
    	
    	String[] split = output.split(Pattern.quote(InstallAgentValidator.AFTER_POPVALIDATOR));
    	String withValidatorsInstalled = split[0];
    	String withValidatorsRemoved = split[1];
    	
    	assertTrue("Should have indication our validator was invoked", withValidatorsInstalled.contains("[HTTP:name=dave]"));
    	assertFalse("Validator should not be accessed after the popValidator was called", withValidatorsRemoved.contains("[HTTP:name=dave]"));
    	
    	TestHelper.validateNoFailuresInOutput(output);
    }

    public static class CaptureLastAppender extends Appender {
    	public CaptureLastAppender(AppenderID id) {
			super(id);
		}

		public static final AtomicReference<String> lastAppenderString = new AtomicReference<String>();

		@Override
		public void outputData(PerfMonData data) {
			lastAppenderString.set(data.toAppenderString());
		}
    }
    
	public static class POJOSnapShotRegistryExample implements Runnable {
		@api.org.perfmon4j.agent.instrument.SnapShotPOJO
		public static class MySnapShotPOJO {
			private long counter = 0;

			@api.org.perfmon4j.agent.instrument.SnapShotCounter
			public long getCounter() {
				return ++counter;
			}
		}
		
		protected void configurePerfmon4j() throws Exception {
			final String monitorName = "MySnapShot";
			final String appenderName = "textAppender";
			PerfMonConfiguration config = new PerfMonConfiguration();
			
			config.defineAppender(appenderName, CaptureLastAppender.class.getName(), "50 millis", null);
			
			config.defineSnapShotMonitor(monitorName, MySnapShotPOJO.class.getName());
			config.attachAppenderToSnapShotMonitor(monitorName, appenderName);

			PerfMon.configure(config);
		}
		
		public void run() {
			try {
				MySnapShotPOJO pojo = new MySnapShotPOJO();
				api.org.perfmon4j.agent.POJOSnapShotRegistry.register(pojo);
				configurePerfmon4j();
				
				Thread.sleep(100);
				Appender.flushAllAppenders();
				
				String lastAppenderString = CaptureLastAppender.lastAppenderString.get();
				assertNotNull("Should have appender output", lastAppenderString);
				assertTrue("Should have output counter value to appender", lastAppenderString.contains("counter.................. 1/per duration"));
				
				// DeRegister our pojo 
				api.org.perfmon4j.agent.POJOSnapShotRegistry.deRegister(pojo);
				
				// Clear out lastAppender output.
				Thread.sleep(100);
				Appender.flushAllAppenders();
				CaptureLastAppender.lastAppenderString.set(null);
				
				// Wait for another chance for the output..
				Thread.sleep(100);
				Appender.flushAllAppenders();
				
				lastAppenderString = CaptureLastAppender.lastAppenderString.get();
				assertNull("Should no longer be outputing data to the appender", lastAppenderString);
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
    
    public void testPOJOSnapShotRegistry() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(POJOSnapShotRegistryExample.class, perfmon4jJar));
System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }

    
	public static class POJOSnapShotRegistryWithBadSnapshotExample implements Runnable {
		@api.org.perfmon4j.agent.instrument.SnapShotPOJO
		public static class MySnapShotPOJO {
			@api.org.perfmon4j.agent.instrument.SnapShotGauge
			public void getValue() {
				// This is an invalid use of SnapShotGauge, it must return a value.
				// When using the agent API we just want to warn
				// and fail the registration.  We do not want to throw
				// an exception.
			}
		}
		
		public void run() {
			try {
				MySnapShotPOJO pojo = new MySnapShotPOJO();
				api.org.perfmon4j.agent.POJOSnapShotRegistry.register(pojo);
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
    
    public void testPOJOSnapShotRegistryWithBadSnapshotExample() throws Exception {
    	String output = LaunchRunnableInVM.run(
        		new LaunchRunnableInVM.Params(POJOSnapShotRegistryWithBadSnapshotExample.class, perfmon4jJar));
//System.out.println(output);    	
		assertTrue("Warning about bad POJO should have been written to the log",
			output.contains("WARN  POJOSnapShotRegistry - SKIPPING - Unable to generate snapshot from POJO"));

    	TestHelper.validateNoFailuresInOutput(output);
    }
    
    
	public static class TestEmitterRegistryEmitterAPI implements Runnable {
		public static class MySnapShotEmitter implements api.org.perfmon4j.agent.Emitter, Runnable {
			private api.org.perfmon4j.agent.EmitterController controller = null;
			private int counter = 0;
			
			MySnapShotEmitter() {
			}

			@Override
			public void acceptController(api.org.perfmon4j.agent.EmitterController controller) {
				this.controller = controller;
			}

			api.org.perfmon4j.agent.EmitterController getController() {
				return controller;
			}
			
			public void run() {
				api.org.perfmon4j.agent.EmitterData data = controller.initData(System.currentTimeMillis() - (60 * 1000 * 60));
				data.addData("myCounter", counter++);
				data.addData("myString", "Dave" + Long.toString(System.currentTimeMillis()));
				
				controller.emit(data);
			}
		}
		
		@Override
		public void run() {
			try {
				EmitterRegistry.getSingleton().setDefaultTimerIntervalMillis(500);
				
				assertTrue("EmitterRegistry should be attached to the agent", api.org.perfmon4j.agent.EmitterRegistry.isAttachedToAgent());
				
				MySnapShotEmitter myEmitter = new MySnapShotEmitter();
				api.org.perfmon4j.agent.EmitterRegistry.register(myEmitter);
				
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "MyEmitter";
				final String appenderName = "MyAppender";
				
				config.defineSnapShotMonitor(monitorName, MySnapShotEmitter.class.getName());
				config.defineAppender(appenderName, TextAppender.class.getName(), "1 second");
				config.attachAppenderToSnapShotMonitor(monitorName, appenderName);
				
				PerfMon.configure(config);
				
				Thread.sleep(1500);
				Appender.flushAllAppenders();
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/*----------------------------------------------------------------------------*/
    public void testEmitterRegistryAPI() throws Exception {
    	String output = LaunchRunnableInVM.run(TestEmitterRegistryEmitterAPI.class,"-vtrue", "", perfmon4jJar);
//System.out.println(output);
    	TestHelper.validateNoFailuresInOutput(output);
		// Should not include instance name when POJO is registered without an instance name
		assertTrue("Expected appender output not found", output.contains("myCounter................ 0"));
    }    

	public static class ReactiveContextChildren implements Runnable {
		public void run() {
			try {
				PerfMonConfiguration config = new PerfMonConfiguration();
				final String monitorName = "test";
				final String childMonitorName = monitorName + ".child";
				final String grandChildMonitorName =  childMonitorName + ".grandChild";
				
				final String appenderName = "bogus";
				
				config.defineMonitor(monitorName);
				config.defineAppender(appenderName, BogusAppender.class.getName(), "100 ms");
				config.attachAppenderToMonitor(monitorName, appenderName, "./**");
				PerfMon.configure(config);
				
				final String ctxA = "ctxA";
				final String ctxB = "ctxB";
				
				// This class verifies that providing a reactive context overrides Perfmon4j's default
				// thread based model.
				
				api.org.perfmon4j.agent.PerfMonTimer outerA = api.org.perfmon4j.agent.PerfMonTimer.start(monitorName, false, ctxA);
				api.org.perfmon4j.agent.PerfMonTimer outerB = api.org.perfmon4j.agent.PerfMonTimer.start(monitorName, false, ctxB);
				
				api.org.perfmon4j.agent.PerfMonTimer childA = api.org.perfmon4j.agent.PerfMonTimer.start(childMonitorName, false, ctxA);
				api.org.perfmon4j.agent.PerfMonTimer childB = api.org.perfmon4j.agent.PerfMonTimer.start(childMonitorName, false, ctxB);

				api.org.perfmon4j.agent.PerfMonTimer grandChildA = api.org.perfmon4j.agent.PerfMonTimer.start(grandChildMonitorName, false,ctxA);
				api.org.perfmon4j.agent.PerfMonTimer grandChildB = api.org.perfmon4j.agent.PerfMonTimer.start(grandChildMonitorName, false, ctxB);

				api.org.perfmon4j.agent.PerfMonTimer.stop(grandChildB);
				api.org.perfmon4j.agent.PerfMonTimer.stop(grandChildA);
				
				api.org.perfmon4j.agent.PerfMonTimer.stop(childB);
				api.org.perfmon4j.agent.PerfMonTimer.stop(childA);
				
				api.org.perfmon4j.agent.PerfMonTimer.stop(outerB);
				api.org.perfmon4j.agent.PerfMonTimer.stop(outerA);
			
				// In a thread based model, we would only expect 1 completion per thread for each monitor.
				// Since we have 2 reactiveContexts we expect to see 2.
				assertEquals("Expected completions on: " + monitorName, 2, PerfMon.getMonitor(monitorName).getTotalCompletions());
				assertEquals("Expected completions on: " + childMonitorName, 2, PerfMon.getMonitor(childMonitorName).getTotalCompletions());
				assertEquals("Expected completions on: " + grandChildMonitorName, 2, PerfMon.getMonitor(grandChildMonitorName).getTotalCompletions());
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
    public void testEmbeddedReactiveContext() throws Exception {
    	String output = LaunchRunnableInVM.run(ReactiveContextChildren.class,"-vtrue", "", perfmon4jJar);
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }    
    
	public static class TestStopAndAbortNullAPITimer implements Runnable {
		public void run() {
			try {
				try {
					api.org.perfmon4j.agent.PerfMonTimer.stop(null);
				} catch (NullPointerException npe) {
					fail("Expected PerfMonTimer.stop() static method to be null safe");
				}
				
				try {
					api.org.perfmon4j.agent.PerfMonTimer.abort(null);
				} catch (NullPointerException npe) {
					fail("Expected PerfMonTimer.abort() static method to be null safe");
				}
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
	
    public void testStopAndAbortNullAPITimer() throws Exception {
    	String output = LaunchRunnableInVM.run(TestStopAndAbortNullAPITimer.class,"-vtrue", "", perfmon4jJar);
//System.out.println(output);    	
    	TestHelper.validateNoFailuresInOutput(output);
    }    

    
	public static class TestSingletonTrackerAPI implements Runnable {
		@Override
		public void run() {
			try {
				SingletonTracker singletonTracker = SingletonTracker.getSingleton();
				
				System.out.println("SingletonTracker isAttachedToAgent: " + SingletonTracker.isAttachedToAgent());
				System.out.println("SingletonTracker isEnabled: " + singletonTracker.isEnabled());
				
				singletonTracker.register(this.getClass());
				// This should force a Singleton Duplicate error in the log -- if tracking is enabled.
				singletonTracker.register(this.getClass());
				
			} catch (Throwable ex) {
				System.out.println("**FAIL: Unexpected Exception thrown: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}    
    
	/*----------------------------------------------------------------------------*/
    public void testSingletonTrackerAPIDisabled() throws Exception {
    	String output = LaunchRunnableInVM.run(TestSingletonTrackerAPI.class,"-vtrue", "", perfmon4jJar);
//System.out.println(output);
    	TestHelper.validateNoFailuresInOutput(output);
		// Should not include instance name when POJO is registered without an instance name
		assertTrue("SingletonTrackerImpl should have been rewritten by the agent", 
			output.contains("SingletonTracker isAttachedToAgent: true"));
		assertTrue("SingletonTracker was not enabled via system property", 
				output.contains("SingletonTracker isEnabled: false"));
    }    
    
	/*----------------------------------------------------------------------------*/
    public void testSingletonTrackerAPIEnabled() throws Exception {
    	Properties systemProperties = new Properties();
    	systemProperties.setProperty("org.perfmon4j.util.SingletonTracker.enabled", "true");
    	
    	String output = LaunchRunnableInVM.run(TestSingletonTrackerAPI.class,"-vtrue", "", systemProperties, perfmon4jJar);
//System.out.println(output);
    	TestHelper.validateNoFailuresInOutput(output);
		// Should not include instance name when POJO is registered without an instance name
		assertTrue("SingletonTrackerImpl should have been rewritten by the agent", 
			output.contains("SingletonTracker isAttachedToAgent: true"));
		assertTrue("SingletonTracker was not enabled via system property", 
				output.contains("SingletonTracker isEnabled: true"));
		assertTrue("SingletonTracker should have detected duplicate registration", 
				output.contains("**** Duplicate Singleton Detected! ****"));
    }    
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
//        System.setProperty("Perfmon4j.debugEnabled", "true");
//		System.setProperty("JAVASSIST_JAR",  "G:\\projects\\perfmon4j\\.repository\\javassist\\javassist\\3.20.0-GA\\javassist-3.20.0-GA.jar");
		
        org.apache.log4j.Logger.getLogger(PerfMonAgentAPITest.class.getPackage().getName()).setLevel(Level.INFO);
        String[] testCaseName = {PerfMonAgentAPITest.class.getName()};

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
            newSuite = new TestSuite(PerfMonAgentAPITest.class);
        }

        return( newSuite);
    }
}

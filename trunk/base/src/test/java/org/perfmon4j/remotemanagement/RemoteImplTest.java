/*
 *	Copyright 2011 Follett Software Company 
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
package org.perfmon4j.remotemanagement;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.java.management.GarbageCollectorSnapShot;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.remotemanagement.intf.SimpleRunnable;
import org.perfmon4j.remotemanagement.intf.ThinRunnableInVM;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class RemoteImplTest extends TestCase {
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.initLogger(RemoteImplTest.class);
	
	public static final String TEST_ALL_TEST_TYPE = "UNIT";

	private File perfmon4jManagementInterface = null;
	
/*----------------------------------------------------------------------------*/
    public RemoteImplTest(String name) {
        super(name);
    }
    
/*----------------------------------------------------------------------------*/
    public void setUp() throws Exception {
        super.setUp();
        
        perfmon4jManagementInterface = File.createTempFile("perfmon4j-remote-management", "tmpdir");
        perfmon4jManagementInterface.delete(); // Just wanted the unique temporary file name.
        perfmon4jManagementInterface.mkdir();
        
        perfmon4jManagementInterface = new File(perfmon4jManagementInterface, "perfmon4j-remote-management.jar");
        
        Properties props = new Properties();	
		
		File classesFolder = new File("./target/classes");
		if (!classesFolder.exists()) {
			classesFolder = new File("./base/target/classes");
		}
		
		File testClassesFolder = new File("./target/test-classes");
		if (!testClassesFolder.exists()) {
			testClassesFolder = new File("./base/target/test-classes");
		}
		
		assertTrue("Could not find classes folder in: "  + classesFolder.getCanonicalPath(), classesFolder.exists());
	
        MiscHelper.createJarFile(perfmon4jManagementInterface.getAbsolutePath(), props, 
        		new File[]{classesFolder, testClassesFolder}, new MyFilter());
        
        System.out.println("perfmon4j jar file: " + perfmon4jManagementInterface.getCanonicalPath());
        
        RemoteImpl.registerRMIListener(8571);
    }
    
    /*----------------------------------------------------------------------------*/
    public void tearDown() throws Exception {
    	RemoteImpl.unregisterRMIListener();
    	
    	File folder = perfmon4jManagementInterface.getParentFile();
        perfmon4jManagementInterface.delete();
        folder.delete();
    	super.tearDown();
    }
    
    
    public static final class MyFilter implements FileFilter {

		public boolean accept(File pathname) {
			boolean result = true;
			
			if (!pathname.isDirectory()) {
				String p = pathname.getAbsolutePath();
				p = p.replaceAll("\\\\", "/");
				result = p.contains("classes/org/perfmon4j/remotemanagement/intf")
					|| p.contains("test-classes/org/perfmon4j/remotemanagement/intf/SimpleRunnable");
			}
			return result;
		}
    }
    

    public void testSystemPortIsSetAsSystemProperty() {
    	assertEquals("8571", System.getProperty(RemoteInterface.P4J_LISTENER_PORT));
    	assertNotNull(RemoteImpl.getRegisteredPort());
    	assertEquals(8571, RemoteImpl.getRegisteredPort().intValue());
    	
    	RemoteImpl.unregisterRMIListener();
    	
    	assertNull(RemoteImpl.getRegisteredPort());
    	assertNull(System.getProperty(RemoteInterface.P4J_LISTENER_PORT));
    }
    
    
    public void testConnect() throws Exception {
    	String result = ThinRunnableInVM.run(SimpleRunnable.TestSimpleConnect.class, "", perfmon4jManagementInterface);
//System.out.println(result);
    	assertTrue("Session should have been established", result.contains("Retrieved sessionID:"));
    }

    
    public void testMajorVersionMismatch() throws Exception {
    	String result = ThinRunnableInVM.run(SimpleRunnable.TestMajorVersionMismatch.class, "", perfmon4jManagementInterface);
//System.out.println(result);
		assertTrue("Should have caught incompatible version exception", 
				result.contains("IncompatibleClientVersionException thrown"));
    }
    
    public void testGetMonitorsIncludesIntervalMonitors() throws Exception {
    	ExternalAppender.setEnabled(true);
    	
    	PerfMonTimer t = PerfMonTimer.start("testGetMonitorsIncludesIntervalMonitors");
    	PerfMonTimer.stop(t);
    	PerfMon.deInit();	
    	
    	String result = ThinRunnableInVM.run(SimpleRunnable.TestGetMonitors.class, "", perfmon4jManagementInterface);
    	ExternalAppender.setEnabled(true);

    	System.out.println(result);
		assertTrue("Should contain the interval monitor", 
				result.contains("Monitor: INTERVAL(name=testGetMonitorsIncludesIntervalMonitors)"));
		
		assertTrue("Validate we retrieved the interval fields", 
				result.contains("FieldDefinition: INTERVAL(name=testGetMonitorsIncludesIntervalMonitors):FIELD(name=AverageDuration;type=LONG)"));
    }

    public void testGetMonitorsIncludesSnapShotMonitors() throws Exception {
    	String sessionID = RemoteImpl.getSingleton().connect(ManagementVersion.VERSION);
    	
    	String keys[] = RemoteImpl.getSingleton().getMonitors(sessionID);
    	
    	boolean found = false;
    	for (int i = 0; i < keys.length && !found; i++) {
    		found = "SNAPSHOT(name=org.perfmon4j.java.management.JVMSnapShot)".equals(keys[i]);
		}
    	assertTrue("Should have found snapShotMonitor", found);
    	
    	RemoteImpl.getSingleton().disconnect(sessionID);
    }
    

    public void testGetFieldsForSnapShotMonitor() throws Exception {
    	String sessionID = RemoteImpl.getSingleton().connect(ManagementVersion.VERSION);
    	
    	String fields[] = RemoteImpl.getSingleton().getFieldsForMonitor(sessionID, "SNAPSHOT(name=org.perfmon4j.java.management.JVMSnapShot)");
    	assertNotNull(fields);
    	assertTrue("Should have one or more fields", fields.length > 0);
    	
    	RemoteImpl.getSingleton().disconnect(sessionID);
    }
    
    /**
     * This test exposes a defect where there was a problem subscribing and
     * re-subscribing to a SnapShot monitor.  On each subsequent attempt to
     * re-subscribe the snapshot monitor was not being reinitialized.
     * 
     * @throws Exception
     */
    public void testSubscribeReSubscribeToSnapShotMonitor() throws Exception {
    	RemoteInterface impl = RemoteImpl.getSingleton();
    	
    	String sessionID = impl.connect(ManagementVersion.VERSION);

    	String field = RemoteImpl.getSingleton().getFieldsForMonitor(sessionID, "SNAPSHOT(name=org.perfmon4j.java.management.JVMSnapShot)")[0];
 
    	impl.subscribe(sessionID, new String[]{field});
    	assertTrue("Should be subscribed to snapshot monitor", impl.getData(sessionID).containsKey(field));

    	impl.subscribe(sessionID, new String[]{}); // Unsubscribe (by passing an empty field array)
    	assertFalse("Should be unsubscribed from snapshot monitor", impl.getData(sessionID).containsKey(field));
    	
    	impl.subscribe(sessionID, new String[]{field}); // Re-subscribe!
    	assertTrue("Should be subscribed to snapshot monitor ON re-subscribe", impl.getData(sessionID).containsKey(field));
    	
    	impl.disconnect(sessionID);
    }    
    

    public void testGetFieldsForMultiInstanceSnapShotMonitor() throws Exception {
    	String sessionID = RemoteImpl.getSingleton().connect(ManagementVersion.VERSION);
    	
    	MonitorKey monitorKey = MonitorKey.newSnapShotKey(GarbageCollectorSnapShot.class.getName(),
    			GarbageCollectorSnapShot.getInstanceNames()[0]);
    	
    	String fields[] = RemoteImpl.getSingleton().getFieldsForMonitor(sessionID, monitorKey.toString());
    	assertNotNull(fields);
    	assertTrue("Should have one or more fields", fields.length > 0);
    	
    	RemoteImpl.getSingleton().disconnect(sessionID);
    }

    public void testGetFieldsForThreadTraceMonitor() throws Exception {
    	MonitorKey threadTraceKey = MonitorKey.newThreadTraceKey("org.apache");
    	
    	RemoteInterface i = RemoteImpl.getSingleton();
    	String sessionID = i.connect(ManagementVersion.VERSION);
    	try {
    		String fields[] = i.getFieldsForMonitor(sessionID, threadTraceKey.toString());
    		assertNotNull(fields);
    		assertEquals("fields.length", 1, fields.length);
    		
    		FieldKey expectedKey = new FieldKey(threadTraceKey, "stack", FieldKey.STRING_TYPE); 
    		assertEquals("expected field key", fields[0], expectedKey.toString());
    	} finally {
    		i.disconnect(sessionID);
    	}
    }
    
    public void testScheduleThreadTrace() throws Exception {
		FieldKey key = new FieldKey(MonitorKey.newThreadTraceKey("my.monitor"), "stack", FieldKey.STRING_TYPE); 
    	RemoteInterface i = RemoteImpl.getSingleton();
    	String sessionID = i.connect(ManagementVersion.VERSION);
    	try {
    		i.scheduleThreadTrace(sessionID, key.toString());
    		
    		i.getData(sessionID);
    		Map<String, Object> map = i.getData(sessionID);
    		
    		// Should get a pending notification for our thread trace...
    		String traceData = (String)map.get(key.toString());
System.err.println(traceData);    		
    		
    		assertNotNull("traceData", traceData);
    		assertEquals(FieldKey.THREAD_TRACE_PENDING, traceData);
    		PerfMonTimer timer = PerfMonTimer.start("my.monitor");
    			PerfMonTimer.stop(PerfMonTimer.start("internal"));
    		PerfMonTimer.stop(timer);
    		
    		// Should get a pending notification for our thread trace...
    		map = i.getData(sessionID);
    		traceData = (String)map.get(key.toString());
System.err.println(traceData);    		
    		assertFalse("Should have thread trace data", traceData.equals(FieldKey.THREAD_TRACE_PENDING));
    		
    		map = i.getData(sessionID);
    		traceData = (String)map.get(key.toString());
    		assertNull("Trace is now done", traceData);
    	} finally {
    		i.disconnect(sessionID);
    	}
    }
    
    public void testSubscribeToIntervalMonitor() throws Exception {
    	ExternalAppender.setEnabled(true);
    	
    	String keyX = "INTERVAL(name=x):FIELD(name=AverageDuration;type=LONG)";
    	String keyY = "INTERVAL(name=y):FIELD(name=AverageDuration;type=LONG)";
    	String keyZ = "INTERVAL(name=z):FIELD(name=AverageDuration;type=LONG)";

    	RemoteInterface i = RemoteImpl.getSingleton();
    	
    	String sessionID = i.connect(ManagementVersion.VERSION);
    	try {
    		List<String> fieldKeys = new ArrayList<String>(); 

    		Map<String,Object> data = i.getData(sessionID);
    		assertEquals("By default should not be subscribed to anything", 0, data.size());
    		
    		fieldKeys.add(keyX);
    		i.subscribe(sessionID, fieldKeys.toArray((new String[]{})));
    		
    		data = i.getData(sessionID);
    		assertEquals("Should now be subscribed", 1, data.size());
   
    		// Add 2 more monitors...
    		fieldKeys.add(keyY);
    		fieldKeys.add(keyZ);
    		
    		i.subscribe(sessionID, fieldKeys.toArray((new String[]{})));
    		
    		data = i.getData(sessionID);
    		assertEquals("Should now be subscribed", 3, data.size());
    		
    		// Remove first two monitors...
    		fieldKeys.remove(keyX);
    		fieldKeys.remove(keyY);
    		i.subscribe(sessionID, fieldKeys.toArray((new String[]{})));
    		
    		data = i.getData(sessionID);
    		assertEquals("Should now be subscribed", 1, data.size());
    		assertTrue("Should have monitor Z", data.keySet().contains(keyZ));
    	} finally {
    		i.disconnect(sessionID);
    	}
    }
    
    
    public static final class FakeLoad implements Runnable {
    	private final Random random = new Random();
    	private final int durationMillis;
    	private final String monitorName;
    	private long start;
    	
    	FakeLoad(String monitorName, int durationMillis) {
    		this.monitorName = monitorName;
    		this.durationMillis = durationMillis;
    	}
    	
		public void run() {
			start = System.currentTimeMillis();
    		while (start + durationMillis > System.currentTimeMillis()) {
    			try {
					Thread.sleep(random.nextInt(50));
				} catch (InterruptedException e) {
				}
				PerfMonTimer t = PerfMonTimer.start(monitorName);
    			try {
    				int secondsActive = Math.max((int)((System.currentTimeMillis() - start)/1000), 1);
					Thread.sleep(random.nextInt(10*secondsActive));
				} catch (InterruptedException e) {
				} finally {
					PerfMonTimer.stop(t);
				}
    		}
		}
    }

    // This is NOT really a test... More like a demo!
    public void XtestFullLifecycle() throws Exception {
    	ExternalAppender.setEnabled(true);
    	
    	for (int x = 0; x < 15; x++) {
    		new Thread(new FakeLoad("a.b." + x, 15000)).start();
    	}
    	String result = ThinRunnableInVM.run(SimpleRunnable.TestFullRemoteMonitorLifecycle.class, "", perfmon4jManagementInterface);
    	ExternalAppender.setEnabled(true);

    	
    	System.out.println(result);
//		assertTrue("Should contain the interval monitor", 
//				result.contains("Monitor: INTERVAL:testGetMonitorsIncludesIntervalMonitors"));
//		assertTrue("Validate we retrieved the monitor Definition", 
//				result.contains("FieldDefinition(monitorType:INTERVAL, fieldName:MaxActiveThreadCount, fieldType:INTEGER)"));
    }
    
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        BasicConfigurator.configure();
        System.setProperty("Perfmon4j.debugEnabled", "true");
		
        org.apache.log4j.Logger.getLogger(RemoteImplTest.class.getPackage().getName()).setLevel(Level.INFO);
        String[] testCaseName = {RemoteImplTest.class.getName()};

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
		newSuite.addTest(new RemoteImplTest("testConnect"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(RemoteImplTest.class);
        }

        return( newSuite);
    }
}

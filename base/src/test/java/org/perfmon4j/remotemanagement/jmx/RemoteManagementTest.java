/*
 *	Copyright 2026 Follett Software Company
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
*/

package org.perfmon4j.remotemanagement.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTestCase;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.java.management.JVMSnapShot;
import org.perfmon4j.remotemanagement.ExternalAppender;
import org.perfmon4j.remotemanagement.RemoteImpl;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;

public class RemoteManagementTest extends PerfMonTestCase {

	public RemoteManagementTest(String name) {
		super(name);
	}

	public void testObjectNameIsWellFormedAndStable() throws Exception {
		ObjectName objectName = new ObjectName(RemoteManagement.OBJECT_NAME);
		assertEquals("org.perfmon4j:type=RemoteManagement", objectName.toString());
	}

	public void testRegisterMBeanIsIdempotent() throws Exception {
		RemoteManagement.registerMBean();
		RemoteManagement.registerMBean();

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(RemoteManagement.OBJECT_NAME);
		assertTrue(server.isRegistered(objectName));
	}

	public void testMBeanIsRegisteredAndReachableViaMBeanServer() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(RemoteManagement.OBJECT_NAME);
		assertTrue("Remote management MBean should be registered", server.isRegistered(objectName));

		String sessionID = (String)server.invoke(objectName, "connect",
				new Object[]{ManagementVersion.VERSION}, new String[]{"java.lang.String"});
		try {
			assertNotNull(sessionID);

			Object monitors = server.invoke(objectName, "getMonitors",
					new Object[]{sessionID}, new String[]{"java.lang.String"});
			assertTrue(monitors instanceof String[]);
		} finally {
			server.invoke(objectName, "disconnect",
					new Object[]{sessionID}, new String[]{"java.lang.String"});
		}
	}

	public void testConnectWithIncompatibleClientVersion() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		try {
			mgmt.connect("99.001");
			fail("Should have thrown IncompatibleClientVersionException");
		} catch (IncompatibleClientVersionException ex) {
			// Expected.
		}
	}

	public void testConnectEnablesExternalAppender() throws Exception {
		assertFalse("No sessions open yet - should not be active", ExternalAppender.isActive());

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			assertTrue("connect() should activate ExternalAppender", ExternalAppender.isActive());

			String monitorName = "testConnectEnablesExternalAppender";
			mgmt.subscribe(sessionID, new String[]{
					"INTERVAL(name=" + monitorName + "):FIELD(name=AverageDuration;type=LONG)"});

			PerfMonTimer t = PerfMonTimer.start(monitorName);
			PerfMonTimer.stop(t);

			Map<String, Object> data = mgmt.getData(sessionID);
			assertTrue("Should have real timer data now that ExternalAppender is active",
					data.containsKey("INTERVAL(name=" + monitorName + "):FIELD(name=AverageDuration;type=LONG)"));
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testDisconnectThenOperationsThrowSessionNotFound() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		mgmt.disconnect(sessionID);

		try {
			mgmt.getMonitors(sessionID);
			fail("Should have thrown SessionNotFoundException");
		} catch (SessionNotFoundException ex) {
			// Expected.
		}
	}

	public void testGetMonitorsIncludesIntervalMonitors() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			String monitorName = "testGetMonitorsIncludesIntervalMonitors";
			PerfMonTimer t = PerfMonTimer.start(monitorName);
			PerfMonTimer.stop(t);

			String[] keys = mgmt.getMonitors(sessionID);
			assertTrue("Should contain the interval monitor",
					Arrays.asList(keys).contains("INTERVAL(name=" + monitorName + ")"));
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testGetMonitorsIncludesSnapShotMonitors() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			String[] keys = mgmt.getMonitors(sessionID);
			assertTrue("Should have found snapShotMonitor",
					Arrays.asList(keys).contains("SNAPSHOT(name=" + JVMSnapShot.class.getName() + ")"));
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testGetFieldsForSnapShotMonitor() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			String[] fields = mgmt.getFieldsForMonitor(sessionID, "SNAPSHOT(name=" + JVMSnapShot.class.getName() + ")");
			assertNotNull(fields);
			assertTrue("Should have one or more fields", fields.length > 0);
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testGetFieldsForThreadTraceMonitor() throws Exception {
		MonitorKey threadTraceKey = MonitorKey.newThreadTraceKey("org.apache");

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			String[] fields = mgmt.getFieldsForMonitor(sessionID, threadTraceKey.toString());
			assertNotNull(fields);
			assertEquals(1, fields.length);

			FieldKey expectedKey = new FieldKey(threadTraceKey, "stack", FieldKey.STRING_TYPE);
			assertEquals(expectedKey.toString(), fields[0]);
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	/**
	 * Mirrors RemoteImplTest.testScheduleThreadTrace's exact three-poll sequence:
	 * pending (no data captured yet) -&gt; real captured stack data -&gt; gone (delivered
	 * exactly once, then cleared).
	 */
	public void testScheduleThreadTrace() throws Exception {
		FieldKey key = new FieldKey(MonitorKey.newThreadTraceKey("jmx.my.monitor"), "stack", FieldKey.STRING_TYPE);

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			mgmt.scheduleThreadTrace(sessionID, key.toString());

			Map<String, Object> map = mgmt.getData(sessionID);
			String traceData = (String)map.get(key.toString());
			assertNotNull("traceData", traceData);
			assertEquals(FieldKey.THREAD_TRACE_PENDING, traceData);

			PerfMonTimer timer = PerfMonTimer.start("jmx.my.monitor");
			PerfMonTimer.stop(PerfMonTimer.start("jmx.internal"));
			PerfMonTimer.stop(timer);

			map = mgmt.getData(sessionID);
			traceData = (String)map.get(key.toString());
			assertFalse("Should have thread trace data", traceData.equals(FieldKey.THREAD_TRACE_PENDING));

			map = mgmt.getData(sessionID);
			traceData = (String)map.get(key.toString());
			assertNull("Trace is now done", traceData);
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testUnScheduleThreadTrace() throws Exception {
		FieldKey key = new FieldKey(MonitorKey.newThreadTraceKey("jmx.unschedule.monitor"), "stack", FieldKey.STRING_TYPE);

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			mgmt.scheduleThreadTrace(sessionID, key.toString());
			assertTrue("Should be pending", mgmt.getData(sessionID).containsKey(key.toString()));

			mgmt.unScheduleThreadTrace(sessionID, key.toString());
			assertFalse("Should no longer be scheduled", mgmt.getData(sessionID).containsKey(key.toString()));
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testScheduleThreadTraceOnNonThreadTraceKeyThrowsInvalidMonitorType() throws Exception {
		FieldKey key = new FieldKey(MonitorKey.newIntervalKey("jmx.not.a.threadtrace"), "AverageDuration", FieldKey.LONG_TYPE);

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			mgmt.scheduleThreadTrace(sessionID, key.toString());
			fail("Should have thrown InvalidMonitorTypeException");
		} catch (InvalidMonitorTypeException ex) {
			// Expected.
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	/**
	 * Mirrors RemoteImplTest.testSubscribeReSubscribeToSnapShotMonitor - regression
	 * coverage for a defect where re-subscribing to a snapshot monitor left stale/no
	 * data.
	 */
	public void testSubscribeReSubscribeToSnapShotMonitor() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			String field = mgmt.getFieldsForMonitor(sessionID, "SNAPSHOT(name=" + JVMSnapShot.class.getName() + ")")[0];

			mgmt.subscribe(sessionID, new String[]{field});
			assertTrue("Should be subscribed to snapshot monitor", mgmt.getData(sessionID).containsKey(field));

			mgmt.subscribe(sessionID, new String[]{}); // Unsubscribe (by passing an empty field array)
			assertFalse("Should be unsubscribed from snapshot monitor", mgmt.getData(sessionID).containsKey(field));

			mgmt.subscribe(sessionID, new String[]{field}); // Re-subscribe!
			assertTrue("Should be subscribed to snapshot monitor ON re-subscribe", mgmt.getData(sessionID).containsKey(field));
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testSubscribeToIntervalMonitor() throws Exception {
		String keyX = "INTERVAL(name=jmxX):FIELD(name=AverageDuration;type=LONG)";
		String keyY = "INTERVAL(name=jmxY):FIELD(name=AverageDuration;type=LONG)";
		String keyZ = "INTERVAL(name=jmxZ):FIELD(name=AverageDuration;type=LONG)";

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			List<String> fieldKeys = new ArrayList<String>();

			Map<String, Object> data = mgmt.getData(sessionID);
			assertEquals("By default should not be subscribed to anything", 0, data.size());

			fieldKeys.add(keyX);
			mgmt.subscribe(sessionID, fieldKeys.toArray(new String[]{}));

			data = mgmt.getData(sessionID);
			assertEquals("Should now be subscribed", 1, data.size());

			// Add 2 more monitors...
			fieldKeys.add(keyY);
			fieldKeys.add(keyZ);
			mgmt.subscribe(sessionID, fieldKeys.toArray(new String[]{}));

			data = mgmt.getData(sessionID);
			assertEquals("Should now be subscribed", 3, data.size());

			// Remove first two monitors...
			fieldKeys.remove(keyX);
			fieldKeys.remove(keyY);
			mgmt.subscribe(sessionID, fieldKeys.toArray(new String[]{}));

			data = mgmt.getData(sessionID);
			assertEquals("Should now be subscribed", 1, data.size());
			assertTrue("Should have monitor Z", data.keySet().contains(keyZ));
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	/**
	 * Demonstrates the coexistence property called out in the design: RemoteImpl (the
	 * legacy RMI-facing singleton) and RemoteManagement (this MBean) both delegate to
	 * the same static ExternalAppender session table, so a session opened through one
	 * does not disrupt a session concurrently open through the other.
	 */
	public void testCoexistsWithRemoteImplSessions() throws Exception {
		String rmiSessionID = RemoteImpl.getSingleton().connect(ManagementVersion.VERSION);
		RemoteManagement mgmt = new RemoteManagement();
		String mbeanSessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			assertFalse("Sessions should be independent", rmiSessionID.equals(mbeanSessionID));

			// Both sessions remain independently valid and usable at the same time.
			assertNotNull(RemoteImpl.getSingleton().getMonitors(rmiSessionID));
			assertNotNull(mgmt.getMonitors(mbeanSessionID));

			// Disconnecting one session doesn't disturb the other.
			mgmt.disconnect(mbeanSessionID);
			assertNotNull("RMI session should still be valid", RemoteImpl.getSingleton().getMonitors(rmiSessionID));
		} finally {
			RemoteImpl.getSingleton().disconnect(rmiSessionID);
		}
	}

	public void testGetServerManagementVersion() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		assertEquals(ManagementVersion.VERSION, mgmt.getServerManagementVersion());
	}

	/**
	 * getServerManagementVersion() is a no-arg "getXxx()" method, so JMX Standard
	 * MBean introspection classifies it as a read-only attribute ("ServerManagementVersion"),
	 * not an operation - unlike forceDynamicChildCreation/unForceDynamicChildCreation,
	 * whose parameters keep them as ops. Confirmed against a real MBeanServer (not
	 * just a direct Java call) so this JMX convention doesn't silently regress.
	 */
	public void testGetServerManagementVersionReachableAsAttributeViaMBeanServer() throws Exception {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(RemoteManagement.OBJECT_NAME);

		Object version = server.getAttribute(objectName, "ServerManagementVersion");
		assertEquals(ManagementVersion.VERSION, version);
	}

	/**
	 * Mirrors ExternalAppenderTest.testForceDynamicChildCreation - confirms the MBean
	 * op reaches the exact same PerfMon effect via ExternalAppender, not a
	 * reimplementation.
	 */
	public void testForceDynamicChildCreation() throws Exception {
		final String baseMonitor = "jmxForceDynamicChildCreationBase";
		final String child1 = baseMonitor + ".1";
		final String child2 = baseMonitor + ".2";

		MonitorKey baseKey = MonitorKey.newIntervalKey(baseMonitor);
		PerfMon.getMonitor(baseMonitor);

		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			PerfMon mon = PerfMon.getMonitor(child1, true);
			assertEquals("Should not create child since dynamicPath = true", baseMonitor, mon.getName());

			mgmt.forceDynamicChildCreation(sessionID, baseKey.toString());

			mon = PerfMon.getMonitor(child1, true);
			assertEquals("Should create child since monitor specified base should create children",
					child1, mon.getName());

			mgmt.unForceDynamicChildCreation(sessionID, baseKey.toString());

			mon = PerfMon.getMonitor(child2, true);
			assertEquals("Should no longer create child since dynamicPath = true", baseMonitor, mon.getName());
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	public void testForceDynamicChildCreationWithUnparsableKeyThrowsIllegalArgument() throws Exception {
		RemoteManagement mgmt = new RemoteManagement();
		String sessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			mgmt.forceDynamicChildCreation(sessionID, "not a valid monitor key");
			fail("Should have thrown IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			// Expected.
		} finally {
			mgmt.disconnect(sessionID);
		}
	}

	/**
	 * Mirrors testMBeanIsRegisteredAndReachableViaMBeanServer, exercising
	 * forceDynamicChildCreation/unForceDynamicChildCreation through actual MBeanServer
	 * reflection (JMX signature dispatch) rather than a direct Java call, since that's
	 * the code path Jolokia actually uses.
	 */
	public void testForceDynamicChildCreationReachableViaMBeanServer() throws Exception {
		final String baseMonitor = "jmxMBeanServerForceDynamicChildCreationBase";
		final String child = baseMonitor + ".1";
		MonitorKey baseKey = MonitorKey.newIntervalKey(baseMonitor);
		PerfMon.getMonitor(baseMonitor);

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(RemoteManagement.OBJECT_NAME);

		String sessionID = (String)server.invoke(objectName, "connect",
				new Object[]{ManagementVersion.VERSION}, new String[]{"java.lang.String"});
		try {
			server.invoke(objectName, "forceDynamicChildCreation",
					new Object[]{sessionID, baseKey.toString()},
					new String[]{"java.lang.String", "java.lang.String"});

			PerfMon mon = PerfMon.getMonitor(child, true);
			assertEquals(child, mon.getName());

			server.invoke(objectName, "unForceDynamicChildCreation",
					new Object[]{sessionID, baseKey.toString()},
					new String[]{"java.lang.String", "java.lang.String"});
		} finally {
			server.invoke(objectName, "disconnect",
					new Object[]{sessionID}, new String[]{"java.lang.String"});
		}
	}

	public void testForceDynamicChildCreationWithUnknownSessionThrowsSessionNotFound() throws Exception {
		MonitorKey baseKey = MonitorKey.newIntervalKey("jmxForceDynamicChildCreationUnknownSession");
		RemoteManagement mgmt = new RemoteManagement();
		try {
			mgmt.forceDynamicChildCreation("bogus-session-id", baseKey.toString());
			fail("Should have thrown SessionNotFoundException");
		} catch (SessionNotFoundException ex) {
			// Expected.
		}
	}

	/**
	 * Same coexistence property as testCoexistsWithRemoteImplSessions, applied to the
	 * Ext1 ops specifically: a session opened over RMI can drive
	 * forceDynamicChildCreation and have the effect observed (and reversed) through a
	 * concurrently open MBean session, since both ultimately share the same
	 * process-wide PerfMon state.
	 */
	public void testForceDynamicChildCreationCoexistsWithRemoteImplSession() throws Exception {
		final String baseMonitor = "jmxForceDynamicChildCreationCoexist";
		final String child = baseMonitor + ".1";
		MonitorKey baseKey = MonitorKey.newIntervalKey(baseMonitor);
		PerfMon.getMonitor(baseMonitor);

		String rmiSessionID = RemoteImpl.getSingleton().connect(ManagementVersion.VERSION);
		RemoteManagement mgmt = new RemoteManagement();
		String mbeanSessionID = mgmt.connect(ManagementVersion.VERSION);
		try {
			mgmt.forceDynamicChildCreation(mbeanSessionID, baseKey.toString());

			PerfMon mon = PerfMon.getMonitor(child, true);
			assertEquals("Effect of MBean-driven force-dynamic-creation should be visible globally",
					child, mon.getName());
		} finally {
			mgmt.disconnect(mbeanSessionID);
			RemoteImpl.getSingleton().disconnect(rmiSessionID);
		}
	}
}

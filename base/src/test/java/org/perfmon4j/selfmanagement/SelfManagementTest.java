package org.perfmon4j.selfmanagement;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.perfmon4j.PerfMon;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class SelfManagementTest extends TestCase {
	private final Logger logger = LoggerFactory.initLogger(SelfManagementTest.class);

	public void testObjectNameIsWellFormedAndStable() throws Exception {
		ObjectName objectName = new ObjectName(SelfManagement.OBJECT_NAME);
		assertEquals("org.perfmon4j:type=SelfManagement", objectName.toString());
	}

	public void testGetVersionReflectsSystemProperty() {
		String expected = System.getProperty(PerfMon.PERFMON4J_VERSION);
		assertEquals(expected, new SelfManagement().getVersion());
	}

	public void testMBeanIsRegisteredWithVersionAttribute() throws Exception {
		// Force PerfMon's static initializer to have run.
		String rootMonitorName = PerfMon.ROOT_MONITOR_NAME;
		assertNotNull(rootMonitorName);

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(SelfManagement.OBJECT_NAME);

		assertTrue("Self management MBean should be registered", server.isRegistered(objectName));

		Object version = server.getAttribute(objectName, "Version");
		assertNotNull(version);
		assertTrue(version instanceof String);
		assertFalse(((String)version).isEmpty());
		assertEquals(System.getProperty(PerfMon.PERFMON4J_VERSION), version);
	}

	public void testRegisterMBeanIsIdempotent() throws Exception {
		// Simulates the same-JVM "already registered" collision without needing an
		// actual second classloader.
		SelfManagement.registerMBean(logger);
		SelfManagement.registerMBean(logger);

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(SelfManagement.OBJECT_NAME);
		assertTrue(server.isRegistered(objectName));
		assertNotNull(server.getAttribute(objectName, "Version"));
	}
}

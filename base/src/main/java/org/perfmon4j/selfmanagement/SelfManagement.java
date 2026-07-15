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

package org.perfmon4j.selfmanagement;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.perfmon4j.PerfMon;

/**
 * perfmon4j's own self-management MBean. Registered once, on a best-effort basis,
 * from PerfMon's static initializer.
 * <p>
 * PerfMon.class can legitimately load under more than one classloader in a single
 * JVM (see org.perfmon4j.util.SingletonTracker). The platform MBeanServer is
 * JVM-wide, not per-classloader, so a second PerfMon class-load will attempt to
 * register this same ObjectName again - that is an expected, benign condition,
 * not a bug, and registerMBean() must handle it without throwing.
 * <p>
 * OBJECT_NAME is a cross-language contract with the perfmon4j Hawtio plugin
 * (hawtio-plugin/src/jolokia/readPerfmon4jVersion.ts), which reads this MBean's
 * Version attribute via Jolokia - do not rename casually.
 */
public final class SelfManagement implements SelfManagementMBean {
	public static final String OBJECT_NAME = "org.perfmon4j:type=SelfManagement";

	@Override
	public String getVersion() {
		return System.getProperty(PerfMon.PERFMON4J_VERSION, "NA(Running in test)");
	}

	// Deliberately logs via System.err rather than org.perfmon4j.util.Logger: this runs
	// from PerfMon's static initializer, at -javaagent premain time, before an app
	// server's own logging (or java.util.logging.LogManager) can safely be touched -
	// see LoggerWrapper's own diagnostics for the same reasoning. A log call here that
	// falls through Logger's auto-detection to java.util.logging can permanently pin
	// the JDK's default LogManager ahead of e.g. WildFly's org.jboss.logmanager,
	// breaking that server's own logging subsystem boot later.
	public static void registerMBean() {
		try {
			ObjectName objectName = new ObjectName(OBJECT_NAME);
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			if (server.isRegistered(objectName)) {
				// Expected on a second class-load of PerfMon under a different
				// classloader in the same JVM - not an error.
				System.err.println("perfmon4j self-management MBean already registered "
					+ "(likely a second PerfMon class-load under a different classloader) - skipping.");
				return;
			}
			server.registerMBean(new SelfManagement(), objectName);
		} catch (InstanceAlreadyExistsException ex) {
			// Race: two classloaders' static initializers both saw isRegistered() == false
			// before either completed registerMBean(). Same benign explanation as above.
			System.err.println("perfmon4j self-management MBean registration race - "
				+ "already registered by another classloader.");
		} catch (Exception ex) {
			// Registration is best-effort - it must never break PerfMon's static init.
			System.err.println("Unable to register perfmon4j self-management MBean: " + ex);
			ex.printStackTrace();
		}
	}
}

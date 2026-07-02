/*
 *	Copyright 2024 Follett Software Company
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
package org.perfmon4j.util;

import junit.framework.TestCase;

/**
 * Exercises {@link JBossLogger} against a real JBoss Logging (jboss-logging is a
 * test-scoped dependency). The logger binds reflectively through the
 * {@link GlobalClassLoader} and only binds when running inside a JBoss/WildFly
 * server, so the test registers the test classloader and simulates a JBoss
 * environment via the {@code jboss.server.type} system property.
 */
public class JBossLoggerTest extends TestCase {

	private String priorJBossServerType = null;

	public JBossLoggerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		GlobalClassLoader.getClassLoader().addClassLoader(getClass().getClassLoader());
		// JBossLogger only binds when MiscHelper.isRunningInJBossAppServer() is
		// true; simulate that for the duration of the test.
		priorJBossServerType = System.getProperty("jboss.server.type");
		System.setProperty("jboss.server.type", "test");
	}

	protected void tearDown() throws Exception {
		if (priorJBossServerType == null) {
			System.clearProperty("jboss.server.type");
		} else {
			System.setProperty("jboss.server.type", priorJBossServerType);
		}
		super.tearDown();
	}

	/**
	 * The reflective bind must succeed and return a usable logger when JBoss
	 * Logging is available and we are running in a (simulated) JBoss server.
	 */
	public void testGetLoggerBindsToJBossLogging() {
		JBossLogger logger = JBossLogger.getLogger("org.perfmon4j.test.jboss.bind", false, false);
		assertNotNull("JBossLogger should bind when JBoss Logging is available in a JBoss server", logger);
	}

	/**
	 * Verifies every reflectively-resolved log method can be invoked without
	 * throwing -- this is what catches an incorrect JBoss Logging method signature.
	 */
	public void testAllLogMethodsInvokeCleanly() {
		JBossLogger logger = JBossLogger.getLogger("org.perfmon4j.test.jboss.methods", false, false);
		assertNotNull(logger);

		Throwable th = new RuntimeException("expected-test-throwable");
		logger.logError("error message");
		logger.logError("error message", th);
		logger.logWarn("warn message");
		logger.logWarn("warn message", th);
		logger.logInfo("info message");
		logger.logInfo("info message", th);
		logger.logDebug("debug message");
		logger.logDebug("debug message", th);
		logger.logVerbose("verbose message");
		logger.logVerbose("verbose message", th);

		// enableInfo()/enableDebug() are no-ops for JBoss Logging (the container
		// governs level); simply verify they do not throw and the level checks work.
		logger.enableInfo();
		logger.enableDebug();
		logger.isInfoEnabled();
		logger.isDebugEnabled();
	}
}

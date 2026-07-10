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
 * Exercises {@link Log4J2Logger} against a real Log4j 2.x (log4j-api + log4j-core
 * are test-scoped dependencies). The logger binds reflectively through the
 * {@link GlobalClassLoader}, so we register the test classloader with it first.
 */
public class Log4J2LoggerTest extends TestCase {

	public Log4J2LoggerTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		// Log4J2Logger resolves the Log4j 2.x API/core classes across the
		// classloaders the agent has registered. In a plain unit test the agent
		// isn't present, so register this test's classloader explicitly.
		GlobalClassLoader.getClassLoader().addClassLoader(getClass().getClassLoader());
	}

	/**
	 * The reflective bind must succeed and return a usable logger when Log4j 2.x
	 * is on the classpath.
	 */
	public void testGetLoggerBindsToLog4j2() {
		Log4J2Logger logger = Log4J2Logger.getLogger("org.perfmon4j.test.log4j2.bind", false, false);
		assertNotNull("Log4J2Logger should bind when Log4j 2.x is available", logger);
	}

	/**
	 * Verifies every reflectively-resolved log method can be invoked without
	 * throwing -- this is what catches an incorrect Log4j 2.x method signature.
	 */
	public void testAllLogMethodsInvokeCleanly() {
		Log4J2Logger logger = Log4J2Logger.getLogger("org.perfmon4j.test.log4j2.methods", true, false);
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
	}

	/**
	 * With log4j-core present, enableInfo()/enableDebug() should adjust the
	 * effective level via the Log4j 2.x Configurator.
	 */
	public void testLevelControlWithCore() {
		String category = "org.perfmon4j.test.log4j2.level";
		Log4J2Logger logger = Log4J2Logger.getLogger(category, true, false);
		assertNotNull(logger);

		logger.enableInfo();
		assertTrue("INFO should be enabled after enableInfo()", logger.isInfoEnabled());

		logger.enableDebug();
		assertTrue("DEBUG should be enabled after enableDebug()", logger.isDebugEnabled());
	}
}

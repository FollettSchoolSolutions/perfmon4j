/*
 *	Copyright 2008, 2011 Follett Software Company 
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
package org.perfmon4j.util;

import org.perfmon4j.instrument.InstrumentationRecursionPreventor;

/**
 * Based on classloader issues the Log4j Logger class may not be loadable from
 * the default classloader of PerfMon. This wrapper will log to stdout/stderr
 * until log4j can be loaded .
 */
class LoggerWrapper implements Logger {
	private final String category;

	static private final String PREFERRED_LOGGER_LOG4J = "log4j";
	static private final String PREFERRED_LOGGER_JAVA_LOGGING = "java";
	static private final String PREFERRED_LOGGER_SYSTEM_OUT = "stdout";
	static private final String PREFERRED_LOGGER_AUTO = "auto";

	static private final String PREFERRED_LOGGER;

	static private final int LOG4J_LOGGING = 1;
	static private final int JAVA_LOGGING = 2;
	static private final int STDOUT_LOGGING = 3;
	static private final int AUTO_LOGGING = 4;

	static private final int perferredLogging;

	static private String getApacheCommonsPreferredLogging() {
		String result = null;

		String commonsPreferred = System
				.getProperty("org.apache.commons.logging.log");
		if ("org.apache.commons.logging.impl.Log4JLogger"
				.equals(commonsPreferred)) {
			result = PREFERRED_LOGGER_LOG4J;
		} else if ("org.apache.commons.logging.impl.Jdk14Logger"
				.equals(commonsPreferred)) {
			result = PREFERRED_LOGGER_JAVA_LOGGING;
		}

		return result;
	}

	static {
		String tmp = System.getProperty("PerfMon4j.preferredLogger");

		
		if (tmp == null && "org.jboss.logmanager.LogManager".equals( System.getProperty("java.util.logging.manager"))) {
			tmp = PREFERRED_LOGGER_LOG4J;
		}
		
		if (tmp == null) {
			tmp = getApacheCommonsPreferredLogging();
			if (tmp != null) {
				System.err
						.println("PerfMon4j deferring to org.apache.commons.logging.log setting of: "
								+ tmp + " logging.");
			}
		}

		if (tmp == null
				&& System.getProperty("java.util.logging.manager") != null) {
			tmp = PREFERRED_LOGGER_JAVA_LOGGING;
			if (tmp != null) {
				System.err
						.println("Found \"java.util.logging.manager\" system property.  PerfMon4j defaulting to: "
								+ tmp
								+ " logging.\r\n"
								+ "To specify an alternate logging type set system property PerfMon4j.preferredLogger=[log4j|stdout]");
			}
		}

		PREFERRED_LOGGER = (tmp == null) ? PREFERRED_LOGGER_AUTO : tmp;
		if (PREFERRED_LOGGER_LOG4J.equalsIgnoreCase(PREFERRED_LOGGER)) {
			perferredLogging = LOG4J_LOGGING;
			System.out
					.println("PerfMon4j will output logging to Log4j when service is available.");
		} else if (PREFERRED_LOGGER_JAVA_LOGGING
				.equalsIgnoreCase(PREFERRED_LOGGER)) {
			perferredLogging = JAVA_LOGGING;
			System.out
					.println("PerfMon4j will output logging to java.util.logging when service is available.");
		} else if (PREFERRED_LOGGER_SYSTEM_OUT
				.equalsIgnoreCase(PREFERRED_LOGGER)) {
			perferredLogging = STDOUT_LOGGING;
			System.out.println("PerfMon4j will output logging to stdout.");
		} else if (PREFERRED_LOGGER_AUTO.equalsIgnoreCase(PREFERRED_LOGGER)) {
			perferredLogging = AUTO_LOGGING;
			System.err
					.println("PerfMon4j will output log to stdout until log4j OR java.util.logging service becomes available.\r\n"
							+ "To specify a logging type set system property PerfMon4j.preferredLogger=[log4j|java|stdout]");
		} else {
			System.err
					.println("Unrecognized system property: \"PerfMon4j.preferredLogger="
							+ PREFERRED_LOGGER
							+ "\" - All PerfMon4j logging defaulting to stdout");
			perferredLogging = STDOUT_LOGGING;
		}
	}

	private boolean forceEnableInfo = LoggerFactory.isDefaultDebugEnabled();
	private boolean forceEnableDebug = LoggerFactory.isDefaultDebugEnabled();
	private Logger log4jDelegate = null;
	private Logger javaLoggerDelegate = null;

	Logger getDelegate() {
		return getDelegate(perferredLogging);
	}

	private Logger getDelegate(int mode) {
		Logger result = null;
		
		// Very important!!!  If the logging operation is occurring
		// while we are actively instrumenting objects, we don't 
		// want any other classloading going on.
		if (InstrumentationRecursionPreventor.allowThreadInLogging()) {
			if (mode == LOG4J_LOGGING) {
				result = log4jDelegate;
				if (result == null) {
					result = log4jDelegate = Log4JLogger.getLogger(category,
							forceEnableInfo, forceEnableDebug);
				}
			} else if (mode == JAVA_LOGGING) {
				result = javaLoggerDelegate;
				if (result == null) {
					result = javaLoggerDelegate = JavaLoggingLogger.getLogger(
							category, forceEnableInfo, forceEnableDebug);
				}
			} else if (mode == AUTO_LOGGING) {
				result = getDelegate(LOG4J_LOGGING);
				if (result == null) {
					result = getDelegate(JAVA_LOGGING);
				}
			}
		}
		return result;
	}

	LoggerWrapper(String category) {
		this.category = category;
	}
	
	private void markDelegateSuspect() {
		log4jDelegate = null;
		javaLoggerDelegate = null;
	}

	public void enableInfo() {
		forceEnableDebug = false;
		forceEnableInfo = true;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.enableInfo();
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}
	}
	
	
	public void enableDebug() {
		forceEnableDebug = forceEnableInfo = true;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.enableDebug();
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}
	}


	public boolean isDebugEnabled() {
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				return delgate.isDebugEnabled();
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}
		return forceEnableDebug;
	}

	public boolean isInfoEnabled() {
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				return delgate.isInfoEnabled();
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}
		return true;
	}

	public void logDebug(String msg) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logDebug(msg);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput && forceEnableDebug) {
			System.out.println(msg);
		}
	}

	public void logDebug(String msg, Throwable th) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logDebug(msg, th);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput && forceEnableDebug) {
			System.out.println(msg);
			th.printStackTrace();
		}
	}

	public void logError(String msg) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logError(msg);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput) {
			System.err.println(msg);
		}
	}

	public void logError(String msg, Throwable th) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logError(msg, th);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput) {
			System.err.println(msg);
			th.printStackTrace(System.err);
		}
	}

	public void logInfo(String msg) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logInfo(msg);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput) {
			System.out.println(msg);
		}
	}

	public void logInfo(String msg, Throwable th) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logInfo(msg, th);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput) {
			System.out.println(msg);
			th.printStackTrace();
		}
	}

	public void logWarn(String msg) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logWarn(msg);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput) {
			System.out.println(msg);
		}
	}

	public void logWarn(String msg, Throwable th) {
		boolean wasOutput = false;
		try {
			Logger delgate = getDelegate();
			if (delgate != null) {
				delgate.logWarn(msg, th);
				wasOutput = true;
			}
		} catch (Exception ex) {
			markDelegateSuspect();
		}

		if (!wasOutput) {
			System.out.println(msg);
			th.printStackTrace();
		}
	}
}

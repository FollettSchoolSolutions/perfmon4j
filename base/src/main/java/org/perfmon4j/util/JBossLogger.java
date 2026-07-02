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

import java.lang.reflect.Method;

/**
 * Logging delegate that binds to the JBoss Logging facade
 * (org.jboss.logging.Logger).
 *
 * <p>This is the most reliable logging binding when Perfmon4j is running inside
 * JBoss/WildFly: the {@code org.jboss.logging} module is core server
 * infrastructure that is always present and loaded very early, and it routes
 * natively into the server's logging subsystem (jboss-logmanager) so output
 * lands in {@code server.log}. Contrast this with the Log4j 1.x/2.x facades,
 * which may be absent (newer WildFly releases no longer expose the Log4j 1.x
 * facade) or never triggered.</p>
 *
 * <p>Like the other delegates this class carries NO compile-time dependency on
 * JBoss Logging; everything is resolved reflectively through the
 * {@link GlobalClassLoader}. Outside of a JBoss/WildFly environment
 * {@code org.jboss.logging.Logger} is generally not present, so
 * {@link #getLogger} simply returns {@code null} and {@link LoggerWrapper} falls
 * through to another delegate.</p>
 *
 * <p>Note on level control: the JBoss Logging {@code Logger} has no per-logger
 * {@code setLevel} -- the effective level is governed by the container's logging
 * configuration -- so {@link #enableInfo()}/{@link #enableDebug()} are no-ops.</p>
 */
class JBossLogger implements Logger {
	final private static Class<?>[] NO_PARAMS = new Class[] {};
	final private static Class<?>[] STRING_PARAMS = new Class[] { String.class };
	final private static Class<?>[] OBJECT_PARAMS = new Class[] { Object.class };
	final private static Class<?>[] OBJECT_THROWABLE_PARAMS = new Class[] {
			Object.class, Throwable.class };

	static private Class<?> loggerClazz = null;
	static private Method getLoggerMethod = null;
	static private Method errorMethod = null;
	static private Method errorMethodWithThrow = null;
	static private Method warnMethod = null;
	static private Method warnMethodWithThrow = null;
	static private Method infoMethod = null;
	static private Method infoMethodWithThrow = null;
	static private Method debugMethod = null;
	static private Method debugMethodWithThrow = null;
	static private Method debugEnabledMethod = null;
	static private Method infoEnabledMethod = null;

	static private long numClassLoadersOnLastCheck = -1;
	static private long nextCheckForClassLoadTime = -1;

	final private Object loggerObject;

	/**
	 * @param category
	 * @param forceEnableInfo
	 * @param forceEnableDebug
	 * @return a JBossLogger bound to org.jboss.logging.Logger, or null if JBoss
	 *         Logging is not (yet) available.
	 */
	public static JBossLogger getLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		JBossLogger result = null;

		// The JBoss Logging facade (org.jboss.logging) can also be present in
		// non-JBoss applications (e.g. it is a Hibernate dependency). Only bind to
		// it when we are actually running inside a JBoss/WildFly app server, so we
		// don't hijack logging for an ordinary application that happens to have it
		// on the classpath.
		if (loggerClazz == null && !MiscHelper.isRunningInJBossAppServer()) {
			return null;
		}

		if (loggerClazz == null) {
			GlobalClassLoader loader = GlobalClassLoader.getClassLoader();
			long totalClassLoaders = loader.getTotalClassLoaders();
			final long lastCheck = numClassLoadersOnLastCheck;
			if (totalClassLoaders > lastCheck) {
				final long now = System.currentTimeMillis();
				if (now > nextCheckForClassLoadTime) {
					numClassLoadersOnLastCheck = totalClassLoaders;
					nextCheckForClassLoadTime = now
							+ Logger.DYNAMIC_LOAD_RETRY_MILLIS;
					try {
						Class<?> tmpLoggerClazz = Class.forName(
								"org.jboss.logging.Logger", true, loader);

						getLoggerMethod = tmpLoggerClazz.getMethod("getLogger",
								STRING_PARAMS);
						errorMethod = tmpLoggerClazz.getMethod("error",
								OBJECT_PARAMS);
						errorMethodWithThrow = tmpLoggerClazz.getMethod("error",
								OBJECT_THROWABLE_PARAMS);
						warnMethod = tmpLoggerClazz.getMethod("warn",
								OBJECT_PARAMS);
						warnMethodWithThrow = tmpLoggerClazz.getMethod("warn",
								OBJECT_THROWABLE_PARAMS);
						infoMethod = tmpLoggerClazz.getMethod("info",
								OBJECT_PARAMS);
						infoMethodWithThrow = tmpLoggerClazz.getMethod("info",
								OBJECT_THROWABLE_PARAMS);
						debugMethod = tmpLoggerClazz.getMethod("debug",
								OBJECT_PARAMS);
						debugMethodWithThrow = tmpLoggerClazz.getMethod("debug",
								OBJECT_THROWABLE_PARAMS);
						debugEnabledMethod = tmpLoggerClazz.getMethod(
								"isDebugEnabled", NO_PARAMS);
						infoEnabledMethod = tmpLoggerClazz.getMethod(
								"isInfoEnabled", NO_PARAMS);

						loggerClazz = tmpLoggerClazz;
					} catch (Exception ex) {
						// Ignore, just assume JBoss Logging has not been loaded...
					} catch (Error er) {
						// Ignore, just assume JBoss Logging has not been loaded...
					}
				}
			}
		}
		if (loggerClazz != null) {
			try {
				result = new JBossLogger(category, forceEnableInfo, forceEnableDebug);
			} catch (Exception ex) {
				// Unable to obtain a logger... treat as not available.
			}
		}
		return result;
	}

	private JBossLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		try {
			this.loggerObject = getLoggerMethod.invoke(null,
					new Object[] { category });
		} catch (Exception e) {
			throw new RuntimeException("Error wrapping JBoss Logging", e);
		}
		// enableInfo()/enableDebug() are intentionally not called here: JBoss
		// Logging exposes no per-logger setLevel; the effective level is owned by
		// the container's logging configuration.
	}

	/**
	 * No-op. The JBoss Logging Logger has no per-logger setLevel; the effective
	 * level is governed by the container's logging configuration.
	 */
	public void enableInfo() {
	}

	/**
	 * No-op. See {@link #enableInfo()}.
	 */
	public void enableDebug() {
	}

	public boolean isDebugEnabled() {
		if (LoggerFactory.isDefaultDebugEnabled()) {
			return true;
		}
		boolean result = false;
		try {
			result = ((Boolean) debugEnabledMethod.invoke(loggerObject,
					new Object[] {})).booleanValue();
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
		return result;
	}

	public boolean isInfoEnabled() {
		boolean result = false;
		try {
			result = ((Boolean) infoEnabledMethod.invoke(loggerObject,
					new Object[] {})).booleanValue();
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
		return result;
	}

	private void handleReflectionException(Exception ex) {
		throw new RuntimeException("Unable to invoke JBoss Logging", ex);
	}

	public void logDebug(String msg) {
		if (LoggerFactory.isDefaultDebugEnabled()) {
			// If perfmon4j verbose or debug mode is enabled, upgrade debug statements to info
			logInfo(msg);
		} else {
			try {
				debugMethod.invoke(loggerObject, new Object[] { msg });
			} catch (Exception ex) {
				handleReflectionException(ex);
			}
		}
	}

	public void logDebug(String msg, Throwable th) {
		if (LoggerFactory.isDefaultDebugEnabled()) {
			// If perfmon4j verbose or debug mode is enabled, upgrade debug statements to info
			logInfo(msg, th);
		} else {
			try {
				debugMethodWithThrow.invoke(loggerObject, new Object[] { msg, th });
			} catch (Exception ex) {
				handleReflectionException(ex);
			}
		}
	}

	public void logError(String msg) {
		try {
			errorMethod.invoke(loggerObject, new Object[] { msg });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	public void logError(String msg, Throwable th) {
		try {
			errorMethodWithThrow.invoke(loggerObject, new Object[] { msg, th });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	public void logInfo(String msg) {
		try {
			infoMethod.invoke(loggerObject, new Object[] { msg });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	public void logInfo(String msg, Throwable th) {
		try {
			infoMethodWithThrow.invoke(loggerObject, new Object[] { msg, th });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	public void logWarn(String msg) {
		try {
			warnMethod.invoke(loggerObject, new Object[] { msg });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	public void logWarn(String msg, Throwable th) {
		try {
			warnMethodWithThrow.invoke(loggerObject, new Object[] { msg, th });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	@Override
	public void logVerbose(String msg) {
		logDebug(msg);
	}

	@Override
	public void logVerbose(String msg, Throwable th) {
		logDebug(msg, th);
	}
}

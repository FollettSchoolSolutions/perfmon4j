/*
 *	Copyright 2008,2009,2011 Follett Software Company
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
 * Alternate Log4j delegate that binds to the Log4j 2.x API
 * (org.apache.logging.log4j.*) rather than the legacy Log4j 1.x API
 * (org.apache.log4j.*) used by {@link Log4JLogger}.
 *
 * <p>Like {@link Log4JLogger} this class carries NO compile-time dependency on
 * Log4j; everything is resolved reflectively through the {@link GlobalClassLoader}
 * so it can be initialized from the boot classloader and bind to the logging
 * framework once it becomes available (e.g. once an application server module is
 * loaded). {@link LoggerWrapper} prefers the legacy Log4j 1.x delegate for
 * backwards compatibility and falls through to this one when only Log4j 2.x is
 * present (as is the case in newer WildFly / JBoss releases that no longer expose
 * the Log4j 1.x facade).</p>
 *
 * <p>Note on level control: under an application server the Log4j 2.x API is
 * typically backed by a provider (e.g. jboss-logmanager) with no log4j-core on
 * the classpath, so programmatic level changes via
 * org.apache.logging.log4j.core.config.Configurator are unavailable. When
 * Configurator cannot be resolved, {@link #enableInfo()}/{@link #enableDebug()}
 * are best-effort no-ops and the effective level is governed by the container's
 * logging configuration.</p>
 */
class Log4J2Logger implements Logger {
	final private static Class<?>[] NO_PARAMS = new Class[] {};
	final private static Class<?>[] STRING_PARAMS = new Class[] { String.class };
	final private static Class<?>[] STRING_THROWABLE_PARAMS = new Class[] {
			String.class, Throwable.class };

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

	// Optional (log4j-core only) support for programmatically forcing a level.
	static private boolean configuratorChecked = false;
	static private Method configuratorSetLevel = null;
	static private Object levelInfo = null;
	static private Object levelDebug = null;

	static private long numClassLoadersOnLastCheck = -1;
	static private long nextCheckForClassLoadTime = -1;

	final private Object loggerObject;
	final private String category;

	/**
	 * @param category
	 * @param forceEnableInfo
	 * @param forceEnableDebug
	 * @return a Log4J2Logger bound to the Log4j 2.x API, or null if Log4j 2.x is
	 *         not (yet) available.
	 */
	public static Log4J2Logger getLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		Log4J2Logger result = null;

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
						Class<?> logManagerClazz = Class.forName(
								"org.apache.logging.log4j.LogManager", true, loader);
						Class<?> tmpLoggerClazz = Class.forName(
								"org.apache.logging.log4j.Logger", true, loader);

						getLoggerMethod = logManagerClazz.getMethod("getLogger",
								STRING_PARAMS);
						errorMethod = tmpLoggerClazz.getMethod("error",
								STRING_PARAMS);
						errorMethodWithThrow = tmpLoggerClazz.getMethod("error",
								STRING_THROWABLE_PARAMS);
						warnMethod = tmpLoggerClazz.getMethod("warn",
								STRING_PARAMS);
						warnMethodWithThrow = tmpLoggerClazz.getMethod("warn",
								STRING_THROWABLE_PARAMS);
						infoMethod = tmpLoggerClazz.getMethod("info",
								STRING_PARAMS);
						infoMethodWithThrow = tmpLoggerClazz.getMethod("info",
								STRING_THROWABLE_PARAMS);
						debugMethod = tmpLoggerClazz.getMethod("debug",
								STRING_PARAMS);
						debugMethodWithThrow = tmpLoggerClazz.getMethod("debug",
								STRING_THROWABLE_PARAMS);
						debugEnabledMethod = tmpLoggerClazz.getMethod(
								"isDebugEnabled", NO_PARAMS);
						infoEnabledMethod = tmpLoggerClazz.getMethod(
								"isInfoEnabled", NO_PARAMS);

						loggerClazz = tmpLoggerClazz;
					} catch (Exception ex) {
						// Ignore, just assume log4j 2.x has not been loaded...
					} catch (Error er) {
						// Ignore, just assume log4j 2.x has not been loaded...
					}
				}
			}
		}
		if (loggerClazz != null) {
			try {
				result = new Log4J2Logger(category, forceEnableInfo, forceEnableDebug);
			} catch (Exception ex) {
				// Unable to obtain a logger... treat as not available.
			}
		}
		return result;
	}

	private Log4J2Logger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		this.category = category;
		try {
			this.loggerObject = getLoggerMethod.invoke(null,
					new Object[] { category });
		} catch (Exception e) {
			throw new RuntimeException("Error wrapping log4j 2.x", e);
		}
		if (forceEnableInfo) {
			enableInfo();
		}
		if (forceEnableDebug) {
			enableDebug();
		}
	}

	/**
	 * Programmatically forcing a level requires log4j-core's Configurator, which
	 * is not present when the Log4j 2.x API is backed by an application-server
	 * provider (e.g. jboss-logmanager). Resolve it lazily and best-effort.
	 */
	private static synchronized void initConfiguratorIfNeeded() {
		if (configuratorChecked) {
			return;
		}
		configuratorChecked = true;
		try {
			GlobalClassLoader loader = GlobalClassLoader.getClassLoader();
			Class<?> configuratorClazz = Class.forName(
					"org.apache.logging.log4j.core.config.Configurator", true, loader);
			Class<?> levelClazz = Class.forName(
					"org.apache.logging.log4j.Level", true, loader);
			levelInfo = levelClazz.getField("INFO").get(null);
			levelDebug = levelClazz.getField("DEBUG").get(null);
			configuratorSetLevel = configuratorClazz.getMethod("setLevel",
					new Class[] { String.class, levelClazz });
		} catch (Exception ex) {
			// log4j-core not available; level is controlled by container config.
		} catch (Error er) {
			// log4j-core not available; level is controlled by container config.
		}
	}

	private void setLevel(Object level) {
		initConfiguratorIfNeeded();
		if (configuratorSetLevel != null && level != null) {
			try {
				configuratorSetLevel.invoke(null, new Object[] { category, level });
			} catch (Exception ex) {
				// Best effort only.
			}
		}
	}

	public void enableInfo() {
		if (!isInfoEnabled() || isDebugEnabled()) {
			setLevel(levelInfo != null ? levelInfo : resolveLevel("INFO"));
		}
	}

	public void enableDebug() {
		if (!isDebugEnabled()) {
			setLevel(levelDebug != null ? levelDebug : resolveLevel("DEBUG"));
		}
	}

	private Object resolveLevel(String name) {
		initConfiguratorIfNeeded();
		return "DEBUG".equals(name) ? levelDebug : levelInfo;
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
		throw new RuntimeException("Unable to invoke log4j 2.x", ex);
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

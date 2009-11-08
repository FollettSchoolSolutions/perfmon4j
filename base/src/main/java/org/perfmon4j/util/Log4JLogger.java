/*
 *	Copyright 2008,2009 Follett Software Company 
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

class Log4JLogger implements Logger {
	final private static Class[] NO_PARAMS = new Class[] {};
	final private static Class[] STRING_PARAMS = new Class[] { String.class };
	final private static Class[] OBJECT_PARAMS = new Class[] { Object.class };
	final private static Class[] OBJECT_THROWABLE_PARAMS = new Class[] {
			Object.class, Throwable.class };

	static private Class loggerClazz = null;
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
	static private Method setLevelMethod = null;
	static private Method getLoggerMethod = null;
	static private Object levelInfo = null;
	static private Object levelDebug = null;

	static private long numClassLoadersOnLastCheck = -1;
	static private long nextCheckForClassLoadTime = -1;

	final private Object loggerObject;

	/**
	 * @param category
	 * @param forceEnable
	 * @return
	 */
	public static Log4JLogger getLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		Log4JLogger result = null;

		if (loggerClazz == null
				&& !LoggerFactory.isInstrumetationCategory(category)) {
			GlobalClassLoader loader = GlobalClassLoader.getClassLoader();
			long totalClassLoaders = loader.getTotalClassLoaders();
			final long lastCheck = numClassLoadersOnLastCheck;
			numClassLoadersOnLastCheck = totalClassLoaders;
			if (totalClassLoaders > lastCheck) {
				final long now = System.currentTimeMillis();
				if (now > nextCheckForClassLoadTime) {
					nextCheckForClassLoadTime = now
							+ Logger.DYNAMIC_LOAD_RETRY_MILLIS;
					try {
						Class tmpLoggerClazz = Class.forName(
								"org.apache.log4j.Logger", true, loader);
						Class levelClazz = Class.forName(
								"org.apache.log4j.Level", true, loader);
						
						/**
						 * Just check for a few more classes to make sure log4j is
						 * in a state where it is operational
						 */
						Class.forName(
								"org.jboss.logging.appender.FileAppender", true, loader);
						
						
						getLoggerMethod = tmpLoggerClazz.getMethod("getLogger",
								STRING_PARAMS);
						errorMethod = tmpLoggerClazz.getMethod("error",
								OBJECT_PARAMS);
						errorMethodWithThrow = tmpLoggerClazz.getMethod(
								"error", OBJECT_THROWABLE_PARAMS);
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
						debugMethodWithThrow = tmpLoggerClazz.getMethod(
								"debug", OBJECT_THROWABLE_PARAMS);
						debugEnabledMethod = tmpLoggerClazz.getMethod(
								"isDebugEnabled", NO_PARAMS);
						infoEnabledMethod = tmpLoggerClazz.getMethod(
								"isInfoEnabled", NO_PARAMS);
						levelInfo = levelClazz.getField("INFO").get(null);
						levelDebug = levelClazz.getField("DEBUG").get(null);
						
						setLevelMethod = tmpLoggerClazz.getMethod("setLevel",
								new Class[] { levelClazz });
						loggerClazz = tmpLoggerClazz;
					} catch (Exception ex) {
					} catch (Error er) {
					}
				}
			}
		}

		if (loggerClazz != null) {
			result = new Log4JLogger(category, forceEnableInfo, forceEnableDebug);
		}
		return result;
	}

	private Log4JLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		try {
			loggerObject = getLoggerMethod.invoke(null,
					new Object[] { category });
		} catch (Exception e) {
			throw new RuntimeException("Error wrapping log4j", e);
		}
		if (forceEnableInfo) {
			enableInfo();
		}
		if (forceEnableDebug) {
			enableDebug();
		}
	}

	public void enableInfo() {
		if (!isInfoEnabled()) {
			try {
				setLevelMethod.invoke(loggerObject, new Object[] { levelInfo });
			} catch (Exception ex) {
				handleReflectionException(ex);
			}
		}
	}

	public void enableDebug() {
		if (!isDebugEnabled()) {
			try {
				setLevelMethod.invoke(loggerObject, new Object[] { levelDebug });
			} catch (Exception ex) {
				handleReflectionException(ex);
			}
		}
	}
	
	public boolean isDebugEnabled() {
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
		throw new RuntimeException("Unable to invoke log4j", ex);
	}

	public void logDebug(String msg) {
		try {
			debugMethod.invoke(loggerObject, new Object[] { msg });
		} catch (Exception ex) {
			handleReflectionException(ex);
		}
	}

	public void logDebug(String msg, Throwable th) {
		try {
			debugMethodWithThrow.invoke(loggerObject, new Object[] { msg, th });
		} catch (Exception ex) {
			handleReflectionException(ex);
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
			errorMethodWithThrow.invoke(loggerObject, new Object[] { msg, th });
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
}

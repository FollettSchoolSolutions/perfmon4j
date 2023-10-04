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
import java.util.Enumeration;

class Log4JLogger implements Logger {
	final private static Class<?>[] NO_PARAMS = new Class[] {};
	final private static Class<?>[] STRING_PARAMS = new Class[] { String.class };
	final private static Class<?>[] OBJECT_PARAMS = new Class[] { Object.class };
	final private static Class<?>[] OBJECT_THROWABLE_PARAMS = new Class[] {
			Object.class, Throwable.class };

	static private Class<?> loggerClazz = null;
	static private Method errorMethod = null;
	static private Method errorMethodWithThrow = null;
	static private Method warnMethod = null;
	static private Method warnMethodWithThrow = null;
	static private Method infoMethod = null;
	static private Method debugMethod = null;
	static private Method debugMethodWithThrow = null;
	static private Method debugEnabledMethod = null;
	static private Method infoEnabledMethod = null;
	static private Method setLevelMethod = null;
	static private Method getLoggerMethod = null;
	
	
	static private Method getAllAppenders = null;
	static private Object levelInfo = null;
	static private Object levelDebug = null;

	static private Object rootLogger = null;
	static boolean log4jInitialized = false;

	static private long numClassLoadersOnLastCheck = -1;
	static private long nextCheckForClassLoadTime = -1;
	

	final private Object loggerObject;

	private static boolean isLog4JInitialized() {
		// This make the assumption that once log4j is initialized,
		// it will NOT become uninitialized.
		try {
			if (!log4jInitialized && (rootLogger != null)) {
				Enumeration<?> allAppendersOnRoot = (Enumeration<?>)getAllAppenders.invoke(rootLogger, new Object[]{}); 
				log4jInitialized = allAppendersOnRoot != null;
			}
		} catch (Exception ex) {
			// Ignore exceptions...  Just assume it is not initialized.
		}
		return log4jInitialized;
	}
	
	/**
	 * @param category
	 * @param forceEnable
	 * @return
	 */
	public static Log4JLogger getLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		Log4JLogger result = null;

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
								"org.apache.log4j.Logger", true, loader);
						Class<?> levelClazz = Class.forName(
								"org.apache.log4j.Level", true, loader);
						

						levelInfo = levelClazz.getField("INFO").get(null);
						levelDebug = levelClazz.getField("DEBUG").get(null);
						
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
						getAllAppenders = tmpLoggerClazz.getMethod("getAllAppenders", NO_PARAMS);
						
						setLevelMethod = tmpLoggerClazz.getMethod("setLevel",
								new Class[] { levelClazz });
						
						if (MiscHelper.isRunningInJBossAppServer() && !loader.isJBossLoggerModuleLoaded()) {
							// Under JBoss we can't load the log4j root logger until
							// the following class has been loaded....
							Class.forName("org.jboss.logging.appender.FileAppender", true, loader);
						}
						
						Method getRootLogger = tmpLoggerClazz.getMethod("getRootLogger", NO_PARAMS);
						rootLogger = getRootLogger.invoke(null, new Object[]{});
						
						loggerClazz = tmpLoggerClazz;
					} catch (Exception ex) {
						// Ignore, just assume log4j has not been loaded...
					} catch (Error er) {
						// Ignore, just assume log4j has not been loaded...
					}
				}
			}
		}
		if (loggerClazz != null && isLog4JInitialized()) {
			result = new Log4JLogger(category, forceEnableInfo, forceEnableDebug);
		}
		return result;
	}

	private Log4JLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		try {
			this.loggerObject = getLoggerMethod.invoke(null,
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
		if (!isInfoEnabled() || isDebugEnabled()) {
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
		if (LoggerFactory.isDefaultDebugEnabled()) {
			return true;
		} else {
			boolean result = false;
			try {
				result = ((Boolean) debugEnabledMethod.invoke(loggerObject,
						new Object[] {})).booleanValue();
			} catch (Exception ex) {
				handleReflectionException(ex);
			}
	
			return result;
		}
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

	@Override
	public void logVerbose(String msg) {
		logDebug(msg);
	}

	@Override
	public void logVerbose(String msg, Throwable th) {
		logDebug(msg, th);
	}
}

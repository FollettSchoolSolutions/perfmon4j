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

import java.util.logging.LogManager;

class JavaLoggingLogger implements Logger {
	private static boolean javaLoggingFound = false;
	private static long numClassLoadersOnLastCheck = -1;
	
	private final java.util.logging.Logger javaLogger;
	
	public static Logger getLogger(String category, boolean forceEnableInfo, boolean forceEnableDebug) {
		Logger logger = null;
		if (javaLoggingFound) {
			logger = new JavaLoggingLogger(java.util.logging.Logger.getLogger(category), forceEnableInfo, forceEnableDebug);
		} else if (!LoggerFactory.isInstrumetationCategory(category)) {
			GlobalClassLoader loader = GlobalClassLoader.getClassLoader();
			final long currentNumClassLoaders = loader.getTotalClassLoaders();
			final boolean inCoreClassLoader = loader.isInCoreClassLoader();
			try {
				if (!javaLoggingFound && 
						numClassLoadersOnLastCheck != currentNumClassLoaders &&
						!inCoreClassLoader) {
					
					java.util.logging.Logger perfmon4jLogger = LogManager.getLogManager().getLogger("org.perfmon4j");
					java.util.logging.Logger perfmon4jWebLogger = LogManager.getLogManager().getLogger("web.org.perfmon4j");
					
					perfmon4jLogger.setUseParentHandlers(true);
					perfmon4jWebLogger.setUseParentHandlers(true);
					
					logger = new JavaLoggingLogger(java.util.logging.Logger.getLogger(category), 
							forceEnableInfo, forceEnableDebug);
					javaLoggingFound = true;
				}
			} catch (Exception e) {
				numClassLoadersOnLastCheck = currentNumClassLoaders;
			}
		}
		return logger;
	}
	
	private JavaLoggingLogger(java.util.logging.Logger javaLogger, boolean forceEnableInfo, boolean forceEnableDebug) {
		this.javaLogger = javaLogger;
		if (forceEnableInfo) {
			enableInfo();
		}
		if (forceEnableDebug) {
			enableDebug();
		}
	}

	public void enableInfo() {
		javaLogger.setLevel(java.util.logging.Level.INFO);
	}

	public void enableDebug() {
		javaLogger.setLevel(java.util.logging.Level.FINEST);
	}
	
	public boolean isDebugEnabled() {
		return javaLogger.isLoggable(java.util.logging.Level.FINEST);
	}

	public boolean isInfoEnabled() {
		return javaLogger.isLoggable(java.util.logging.Level.INFO);
	}

	public void logDebug(String msg) {
		javaLogger.log(java.util.logging.Level.FINEST, msg);
	}

	public void logDebug(String msg, Throwable th) {
		javaLogger.log(java.util.logging.Level.FINEST, msg, th);
	}

	public void logError(String msg) {
		javaLogger.log(java.util.logging.Level.SEVERE, msg);
	}

	public void logError(String msg, Throwable th) {
		javaLogger.log(java.util.logging.Level.SEVERE, msg, th);
	}

	public void logInfo(String msg) {
		javaLogger.log(java.util.logging.Level.INFO, msg);
	}

	public void logInfo(String msg, Throwable th) {
		javaLogger.log(java.util.logging.Level.INFO, msg, th);
	}

	public void logWarn(String msg) {
		javaLogger.log(java.util.logging.Level.WARNING, msg);
	}

	public void logWarn(String msg, Throwable th) {
		javaLogger.log(java.util.logging.Level.WARNING, msg, th);
	}
}

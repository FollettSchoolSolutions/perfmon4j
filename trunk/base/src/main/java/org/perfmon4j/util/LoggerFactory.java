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

public class LoggerFactory {
	private static boolean defaultDebugEnabled = Boolean.getBoolean("PerfMon4j.debugEnabled");
	private static boolean verboseInstrumentationEnabled = Boolean.getBoolean("PerfMon4j.debugEnabled");
	private static Logger verboseInstrumentationLogger = null;
	
	
	private LoggerFactory() {
    }

    public static Logger initLogger(String category) {
        return new LoggerWrapper(category);
    }

    public static Logger initLogger(Class<?> clazz) {
        return new LoggerWrapper(clazz.getName());
    }
    
    public static Logger getVerboseInstrumentationLogger() {
    	if (verboseInstrumentationLogger == null) {
    		verboseInstrumentationLogger = initLogger("org.perfmon4j.instrument.verbose");
    		if (verboseInstrumentationEnabled) {
    			verboseInstrumentationLogger.enableDebug();
    		}
    	}
        return verboseInstrumentationLogger;
    }
    
	/**
	 * (To prevent recursion, we don't want to attempt to load
	 * the log4j classes while we are performing instrumentation)
	 **/
	public static boolean isInstrumetationCategory(String category) {
		return (category.startsWith("org.perfmon4j.instrument")
			|| category.startsWith("org.perfmon4j.PerfMon")
			|| category.startsWith("org.perfmon4j.remotemanagement")
			|| category.startsWith("org.perfmon4j.XMLConfigurator")) 
			&& !category.endsWith("Test");
	}
	
	
	/**
	 * This default wrapper is used by LoggerWrapper.getActiveWrapper();
	 */
	private static LoggerWrapper defaultWrapper = null;
	
	
	/**
	 * This method returns the active logging framework.  
	 * Because perfmon4j may be active before the classes for the logging framework
	 * are loaded, it will dynamically chose the best available framework.
	 * It will switch to the preferred framework when it becomes available.
	 * 
	 * @return one of the following "stdout", "java", "log4j"
	 */
	public static String getLoggingFramework() {
		String result = "stdout";
		if (defaultWrapper == null) {
			defaultWrapper = new LoggerWrapper("defaultLogger");
		}
		try {
			Logger delegate = defaultWrapper.getDelegate();
			if (delegate instanceof Log4JLogger) {
				result = "log4j";
			} else if (delegate instanceof JavaLoggingLogger) {
				result = "java";
			}
		} catch (Exception ex) {
			// Nothing todo...
		}
		return result;
	}
	
	
	public static void setDefaultDebugEnbled(boolean v) {
		defaultDebugEnabled = v;
	}
	
	public static boolean isDefaultDebugEnabled() {
		return defaultDebugEnabled;
	}
	
	public static void setVerboseInstrumentationEnabled(boolean v) {
		verboseInstrumentationEnabled = v;
	}
	
	public static boolean isVerboseInstrumentationEnabled() {
		return verboseInstrumentationEnabled;
	}
	
}

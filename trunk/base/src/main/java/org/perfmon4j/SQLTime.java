/*
 *	Copyright 2008-2010 Follett Software Company 
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
package org.perfmon4j;

import org.perfmon4j.util.MiscHelper;

/**
 * Utility class used to implement SQLBased monitoring within 
 * interval timers.
 * @author ddeucher
 */
public class SQLTime {
	private static boolean enabled = false;
	
	/**
	 * Called on startup of the Perfmon4j javaagent.  
	 * - Monitoring requires run-time instrumentation of the JDBC driver instrumentation.
	 * 
	 *  THIS SHOULD only be called ONCE on VM STARTUP.
	 * 
	 */
	public static void setEnabled(boolean newEnabled) {
		enabled = newEnabled;
	}
	
	private static ThreadLocal<SQLTime> sqlTimeForThread = new ThreadLocal<SQLTime>() {
	    protected synchronized SQLTime initialValue() {
	        return new SQLTime();
	    }
	};
	
	private SQLTime() {
	}
	
	private long currentSQLTime = 0;
	private long startTime;
	private int refCount = 0;
	
	private void start() {
		if (refCount++ == 0) {
			startTime = MiscHelper.currentTimeWithMilliResolution();
		}
	}
	
	private void stop() {
		if (--refCount == 0) {
			// Fail-SAFE non negative durations to support clock slewing that can occur on some 
			// virtual machines.
			currentSQLTime += Math.max(0, (MiscHelper.currentTimeWithMilliResolution() - startTime));
		}
	}

	/**
	 * NOTE will always return 0 of SQLTime monitoring is NOT enabled...
	 * 
	 * @return total millis spent by the current thread in SQL/JDBC.
	 */
	public static long getSQLTime() {
		if (enabled) {
			return sqlTimeForThread.get().currentSQLTime;
		} else {
			return 0;
		}
	}
	
	/**
	 * SHOULD ONLY be called by the instrumented JDBC methods.   
	 */
	public static void startTimerForThread() {
		if (enabled) {
			sqlTimeForThread.get().start();
		}
	}
	
	/**
	 * SHOULD ONLY be called by the instrumented JDBC methods.   
	 */
	public static void stopTimerForThread() {
		if (enabled) {
			sqlTimeForThread.get().stop();
		}
	}
	
	public static boolean isEnabled() {
		return enabled;
	}
}


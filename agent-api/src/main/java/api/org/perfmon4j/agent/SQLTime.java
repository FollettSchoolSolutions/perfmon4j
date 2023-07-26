/*
 *	Copyright 2019 Follett School Solutions 
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
 * 	ddeuchert@follett.com
 * 	David Deuchert
 * 	Follett School solutions
*/
package api.org.perfmon4j.agent;

/**
 * This class will execute in one of two modes:
 * 	Unattached - When running in a JVM that was not booted with the Perfmon4j instrumentation agent this class will 
 * 		execute the code declared within this source file.  Essentially it will be running in a non-operative state.
 * 	Attached - When this class is loaded in a JVM that was booted with the Perfmon4j instrumentation agent, The agent  will
 * 		re-write this class and it will be in an operating state.
 * 
 * 	The code is instrumented in method JavassistRuntimeTimerInjector.attachAgentToSQLTimeAPIClass().
 */
public class SQLTime {

	public static long getSQLTime() {
		return 0;
	}
	
	public static boolean isEnabled() {
		return false;
	}
	
    /**
     * If true this class has been rewritten by the Perfmon4j agent.
     * @return
     */
    public static boolean isAttachedToAgent() {
    	return false;
    }
}

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
 */
public class PerfMonTimer {
	private static final PerfMonTimer noOpTimer = new PerfMonTimer();
	
	private PerfMonTimer() {
	}

	public static PerfMonTimer start(PerfMon mon) {
		return noOpTimer;
	}

	/**
	 * @param mon
	 * @param reactiveContextID - used when working with a reactive model
	 * where you want the timer to span across multiple threads in a reactive
	 * model.
	 * 
	 * IMPORTANT you must be using a programming framework (i.e. Quarkus)
	 * that supports tracing across threads.
	 * 
	 * @return
	 */
	public static PerfMonTimer start(PerfMon mon, String reactiveContextID) {
		return noOpTimer;
	}
	
	
    public static PerfMonTimer start(String key) {
    	return start(key, false);
    }
    
    /**
     * Pass in true if this is a dynamically generated key (i.e. not a method
     * name or some know value.  This prevents monitors from being created
     * that are not actively attached to appenders.
     * 
     * for example:
     * 	   private void lookupUser(String userName) {
     * 		    PerfMonTimer.start("lookupUser." + userName, true); 
     * 			...
     * 	   }
     */
    public static PerfMonTimer start(String key, boolean isDynamicKey) {
    	return noOpTimer;
    }

    /**
     * 
     * @param key
     * @param isDynamicKey
	 * @param reactiveContextID - used when working with a reactive model
	 * where you want the timer to span across multiple threads in a reactive
	 * model.
	 * 
	 * IMPORTANT you must be using a programming framework (i.e. Quarkus)
	 * that supports tracing across threads.
	 * 
     * @return
     */
    public static PerfMonTimer start(String key, boolean isDynamicKey, String reactiveContextID) {
    	return noOpTimer;
    }

    public static void abort(PerfMonTimer timer) {
    }

    public static void abort(PerfMonTimer timer, String reactiveContextID) {
    }
    
    public static void stop(PerfMonTimer timer) {
    }

    public static void stop(PerfMonTimer timer, String reactiveContextID) {
    }
    
    public static PerfMonTimer getNullTimer() {
    	return noOpTimer;
    }
    
    /**
     * If true this class has been rewritten by the Perfmon4j agent.
     * @return
     */
    public static boolean isAttachedToAgent() {
    	return false;
    }
}

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
package org.perfmon4j.agent.api;


/**
 * This class will execute in one of two modes:
 * 	Dormant - When the JVM has not been loaded with the Perfmon4j instrumentation agent this class will 
 * 		execute the code contained in this source file and be executing in a dormant/placeholder state.
 * 	Operating - when this class is loaded in a JVM with the Perfmon4j instrumentation agent this class will
 * 		be re-written to be into a operating state.
 * 
 */
public class PerfMonTimer {
	private static final PerfMonTimer noOpTimer = new PerfMonTimer();
	
	private PerfMonTimer() {
	}

	public static PerfMonTimer start(PerfMon mon) {
		return noOpTimer;
	}

    public static PerfMonTimer start(String key) {
    	return noOpTimer;
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

    public static void abort(PerfMonTimer timer) {
    }
    
    public static void stop(PerfMonTimer timer) {
    }
    
    /**
     * If true the perfmon4j agent was not found and this 
     * class is operating in a dormant state.
     */
    public static boolean isDormant() {
    	return true;
    }
    
}

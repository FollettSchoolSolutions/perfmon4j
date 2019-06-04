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
public class PerfMon {
	private final String name;
	
	private PerfMon(String name) {
		this.name = name;
	}

    public String getName() {
    	return name;
    }

	public static PerfMon getMonitor(String key) {
    	return getMonitor(key, false);
    }
	
    public static PerfMon getMonitor(String key, boolean isDynamicPath) {
        return new PerfMon(key);
    }

    public boolean isActive() {
        return false;
    }
    
    public static boolean hasBeenInstrumented() {
    	return false;
    }
}

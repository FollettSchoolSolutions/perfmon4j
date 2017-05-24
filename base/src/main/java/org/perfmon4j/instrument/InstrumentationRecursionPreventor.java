/*
 *	Copyright 2017 Follett School Solutions 
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
package org.perfmon4j.instrument;


public class InstrumentationRecursionPreventor {
	@SuppressWarnings("unused")
	private static final String NO_PERFMON4J_INSTRUMENTATION = "";
	
	private boolean threadInInstrumentation = false; 
	private boolean threadInPremain = false;
	
	private static ThreadLocal<InstrumentationRecursionPreventor> recursionPreventor = new ThreadLocal<InstrumentationRecursionPreventor>() {
        protected synchronized InstrumentationRecursionPreventor initialValue() {
            return new InstrumentationRecursionPreventor();
        }
    };    
    
    static void setThreadInInstrumentation(boolean value) {
    	recursionPreventor.get().threadInInstrumentation = value;
    }
    
    static void setThreadInPremain(boolean value) {
    	recursionPreventor.get().threadInPremain = value;
    }
    
    public static boolean isThreadInInstrumentation() {
    	return recursionPreventor.get().threadInInstrumentation;
    }

    public static boolean isThreadInPremain() {
    	return recursionPreventor.get().threadInPremain;
    }
    
    public static boolean allowThreadInLogging() {
    	return !isThreadInInstrumentation() && !isThreadInPremain();
    }

}

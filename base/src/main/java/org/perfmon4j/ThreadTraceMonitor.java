/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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

import org.perfmon4j.ThreadTracesBase.ActiveThreadTraceFlag;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


class ThreadTraceMonitor {
	// Dont use log4j here... The class may not have been loaded
    private static final Logger logger = LoggerFactory.initLogger(ThreadTraceMonitor.class);    
    
    
    /**
     * This allows us a quick way to indicate if a specific thread is actively
     * involved in any(one or more) ThreadTraces. Internal(Standard appender) or External(VisualVM).
     */
    public static ThreadLocal<ActiveThreadTraceFlag> activeThreadTraceFlag = new ThreadLocal<ActiveThreadTraceFlag>() {
        protected synchronized ActiveThreadTraceFlag initialValue() {
            return new ActiveThreadTraceFlag();
        }
    };

    
    private static ThreadLocal<ThreadTracesOnStack> internalMonitorsOnThread = new ThreadLocal<ThreadTracesOnStack>() {
         protected synchronized ThreadTracesOnStack initialValue() {
             return new ThreadTracesOnStack(PerfMon.MAX_ALLOWED_INTERNAL_THREAD_TRACE_ELEMENTS);
         }
     };

     private static ThreadLocal<ThreadTracesOnStack> externalMonitorsOnThread = new ThreadLocal<ThreadTracesOnStack>() {
         protected synchronized ThreadTracesOnStack initialValue() {
             return new ThreadTracesOnStack(PerfMon.MAX_ALLOWED_EXTERNAL_THREAD_TRACE_ELEMENTS);
         }
     };
     
    private ThreadTraceMonitor() {
    }

    static ThreadTracesOnStack getInternalThreadTracesOnStack() {
    	return internalMonitorsOnThread.get();
    }

    static ThreadTracesOnStack getExternalThreadTracesOnStack() {
    	return externalMonitorsOnThread.get();
    }
    
    
    /**
     * No need for synchronization of this class since it is bound to a single
     * thread....
     */
    public static class ThreadTracesOnStack extends ThreadTracesBase {
    	ThreadTracesOnStack(int maxElements) {
    		super(maxElements);
    	}

		@Override
		protected void incrementActiveThreadTraceFlag() {
			activeThreadTraceFlag.get().incActive();
		}

		@Override
		protected void decrementActiveThreadTraceFlag() {
			activeThreadTraceFlag.get().decActive();
		}
    }
}

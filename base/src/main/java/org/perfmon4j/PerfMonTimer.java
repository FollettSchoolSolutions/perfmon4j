/*
 *	Copyright 2008, 2011 Follett Software Company 
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

import org.perfmon4j.ThreadTraceMonitor.UniqueThreadTraceTimerKey;
import org.perfmon4j.remotemanagement.ExternalAppender;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class PerfMonTimer {
    // Dont use log4j here... The class may not have been loaded
    private static final Logger logger = LoggerFactory.initLogger(PerfMonTimer.class);
    
    private static final PerfMonTimer NULL_TIMER = new PerfMonTimer(null, null);
    
    // Package level for testing...
    final PerfMon perfMon;
    final PerfMonTimer next;
    
    /**
     * Package level... Only PerfMon.class should invoke this constructor
     * Applications using the PerfMonTimer should invoke the static method
     * PerfMonTimer.start();
     */
    PerfMonTimer(PerfMon perfMon, PerfMonTimer next) {
        this.perfMon = perfMon;
        this.next = next;
    }

    public static PerfMonTimer start(PerfMon mon) {
        if (!PerfMon.isConfigured() && !ExternalAppender.isActive()) {
            return NULL_TIMER;
        }

        PerfMonTimer result = mon.getPerfMonTimer();
        ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getThreadTracesOnStack();
        
    	final boolean haveActiveTimer = (NULL_TIMER != result);  // It is OK to do an object compare here.
    	final boolean haveActiveThreadTrace = tOnStack.isActive();
        
        if (haveActiveTimer  || haveActiveThreadTrace) {
	        String monitorName = "";
	        UniqueThreadTraceTimerKey wrapperKey = null;
	        try {
	        	long startTime = MiscHelper.currentTimeWithMilliResolution(); 
	            monitorName = mon.getName();
	            if (haveActiveThreadTrace) {
	            	wrapperKey = tOnStack.enterCheckpoint(monitorName, startTime);
	            }
	            if (haveActiveTimer) {
	            	result.start(startTime);
	            }
	        } catch (ThreadDeath th) {
	            throw th;   // Always rethrow this error
	        } catch (Throwable th) {
	            logger.logError("Error starting monitor: " + monitorName, th);
	            result = NULL_TIMER;
	        }
	        
	        if (wrapperKey != null) {
	            // To keep track of the checkpoints for thread tracing we 
	            // must be able to identify the timer passed to PerfMonTimer.stop()
	            result = new TimerWrapperWithThreadTraceKey(result, wrapperKey);
	        }
        }
        
        return result;
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
        PerfMonTimer result = NULL_TIMER;
        
        try {
            if (PerfMon.isConfigured() || ExternalAppender.isActive()) {
                result = start(PerfMon.getMonitor(key, isDynamicKey));
            }
        } catch (ThreadDeath th) {
            throw th;   // Always rethrow this error
        } catch (Throwable th) {
            logger.logError("Error starting monitor: " + key, th);
            result = NULL_TIMER;
        }
        
        return result;
    }

    
    private void start(long now) {
        if (perfMon != null) {
            perfMon.start(now);
            next.start(now);
        }
    }
    
    private static void stop(PerfMonTimer timer, boolean abort) {
        try {
            if (timer != NULL_TIMER && timer != null) {
                UniqueThreadTraceTimerKey key = timer.getUniqueTimerKey();
                if (key != null) {
                	ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getThreadTracesOnStack();
                    tOnStack.exitCheckpoint(key);
                }
                timer.stop(MiscHelper.currentTimeWithMilliResolution(), abort);
            }
        } catch (ThreadDeath th) {
            throw th;   // Always rethrow this error
        } catch (Throwable th) {
            logger.logError("Error stopping timer", th);
        }
    }

    public static void abort(PerfMonTimer timer) {
        stop(timer, true);
    }
    
    public static void stop(PerfMonTimer timer) {
        stop(timer, false);
    }
    
    private void stop(long now, boolean abort) {
        if (perfMon != null) {
            next.stop(now, abort);
            perfMon.stop(now, abort);
        }
    }
    
    public static PerfMonTimer getNullTimer() {
        return NULL_TIMER;
    }
    
    /**
     * Used for the ThreadTraceTimers...
     * @return
     */
    protected UniqueThreadTraceTimerKey getUniqueTimerKey() {
        return null;
    }
    
    /**
     * This class is only used when we return a Timer that is part of
     * a thread trace.
     */
    private static class TimerWrapperWithThreadTraceKey extends PerfMonTimer {
        final private UniqueThreadTraceTimerKey uniqueThreadTraceTimerKey;
        
        private TimerWrapperWithThreadTraceKey(PerfMonTimer timer, 
            UniqueThreadTraceTimerKey uniqueThreadTraceTimerKey) {
            super(timer.perfMon, timer.next); 
            this.uniqueThreadTraceTimerKey = uniqueThreadTraceTimerKey;
        }

        protected UniqueThreadTraceTimerKey getUniqueTimerKey() {
            return uniqueThreadTraceTimerKey;
        }
    }
}

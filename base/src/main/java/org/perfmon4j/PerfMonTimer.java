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
    	return start(mon, null);
    }

    public static PerfMonTimer start(PerfMon mon, String reactiveContextID) {
        if (!PerfMon.isConfigured() && !ExternalAppender.isActive()) {
            return NULL_TIMER;
        }
        PerfMonTimer result = mon.getPerfMonTimer();
    	final boolean haveActiveTimer = (NULL_TIMER != result);  // It is OK to do an object compare here.
    	final boolean haveActiveThreadTrace = ThreadTraceMonitor.activeThreadTraceFlag.get().isActive(); 
    	
        if (haveActiveTimer || haveActiveThreadTrace) {
        	String monitorName = "";
	        UniqueThreadTraceTimerKey wrapperInternalKey = null;
	        UniqueThreadTraceTimerKey wrapperExternalKey = null;
	        try {
	        	long startTime = MiscHelper.currentTimeWithMilliResolution(); 
	            monitorName = mon.getName();
	            if (haveActiveThreadTrace) {
	                ThreadTraceMonitor.ThreadTracesOnStack tInternalOnStack = ThreadTraceMonitor.getInternalThreadTracesOnStack();
	                ThreadTraceMonitor.ThreadTracesOnStack tExternalOnStack = ThreadTraceMonitor.getExternalThreadTracesOnStack();

	            	final boolean haveActiveInternalThreadTrace = tInternalOnStack.isActive();
	            	final boolean haveActiveExternalThreadTrace = tExternalOnStack.isActive();
	            
		            if (haveActiveInternalThreadTrace) {
		            	wrapperInternalKey = tInternalOnStack.enterCheckpoint(monitorName, startTime);
		            }
		            if (haveActiveExternalThreadTrace) {
		            	wrapperExternalKey = tExternalOnStack.enterCheckpoint(monitorName, startTime);
		            }
	            }
	            if (haveActiveTimer) {
	            	result.start(startTime, reactiveContextID);
	            }
	        } catch (ThreadDeath th) {
	            throw th;   // Always rethrow this error
	        } catch (Throwable th) {
	            logger.logError("Error starting monitor: " + monitorName, th);
	            result = NULL_TIMER;
	        }
	        
	        if (haveActiveThreadTrace) {
	            // To keep track of the checkpoints for thread tracing we 
	            // must be able to identify the timer passed to PerfMonTimer.stop()
	            result = new TimerWrapperWithThreadTraceKey(result, wrapperInternalKey, wrapperExternalKey);
	        }
        }
        
        return result;
    }

    public static PerfMonTimer start(String key) {
    	return start(key, false, null);
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
    	return start(key, isDynamicKey, null);
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
    public static PerfMonTimer start(String key, boolean isDynamicKey, String reactiveContextID) {
        PerfMonTimer result = NULL_TIMER;
        
        try {
            if (PerfMon.isConfigured() || ExternalAppender.isActive()) {
            	if (!isDynamicKey) {
            		setLastFullyQualifiedStartNameForThread(key);
            	}
            	try {
                    result = start(PerfMon.getMonitor(key, isDynamicKey), reactiveContextID);
            	} finally {
            		// Always clear the lastFullyQualifiedName.
            		if (!isDynamicKey) {
            			setLastFullyQualifiedStartNameForThread(null);
            		}
            	}
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
    	start(now, null);
    }

    
    private void start(long now, String reactiveContextID) {
        if (perfMon != null) {
            perfMon.start(now, reactiveContextID);
            next.start(now, reactiveContextID);
        }
    }
    
    private static void stop(PerfMonTimer timer, boolean abort) {
        try {
            if (timer != NULL_TIMER && timer != null) {
                UniqueThreadTraceTimerKey keyInternal = timer.getUniqueInternalTimerKey();
                if (keyInternal != null) {
                	ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getInternalThreadTracesOnStack();
                    tOnStack.exitCheckpoint(keyInternal);
                }
                UniqueThreadTraceTimerKey keyExternal = timer.getUniqueExternalTimerKey();
                if (keyExternal != null) {
                	ThreadTraceMonitor.ThreadTracesOnStack tOnStack = ThreadTraceMonitor.getExternalThreadTracesOnStack();
                    tOnStack.exitCheckpoint(keyExternal);
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
    protected UniqueThreadTraceTimerKey getUniqueInternalTimerKey() {
        return null;
    }

    /**
     * Used for the ThreadTraceTimers...
     * @return
     */
    protected UniqueThreadTraceTimerKey getUniqueExternalTimerKey() {
        return null;
    }
    
    /**
     * This class is only used when we return a Timer that is part of
     * a thread trace.
     */
    private static class TimerWrapperWithThreadTraceKey extends PerfMonTimer {
        final private UniqueThreadTraceTimerKey uniqueInternalThreadTraceTimerKey;
        final private UniqueThreadTraceTimerKey uniqueExternalThreadTraceTimerKey;
        
        private TimerWrapperWithThreadTraceKey(PerfMonTimer timer, 
            UniqueThreadTraceTimerKey uniqueInternalThreadTraceTimerKey,
            UniqueThreadTraceTimerKey uniqueExternalThreadTraceTimerKey) {
            super(timer.perfMon, timer.next); 
            this.uniqueInternalThreadTraceTimerKey = uniqueInternalThreadTraceTimerKey;
            this.uniqueExternalThreadTraceTimerKey = uniqueExternalThreadTraceTimerKey;
        }

        protected UniqueThreadTraceTimerKey getUniqueInternalTimerKey() {
            return uniqueInternalThreadTraceTimerKey;
        }
        protected UniqueThreadTraceTimerKey getUniqueExternalTimerKey() {
            return uniqueExternalThreadTraceTimerKey;
        }
    }
    
    private static class FullyQualifiedTimerStartName {
    	private String fullName;

		public String getFullName() {
			return fullName;
		}

		public void setFullName(String fullName) {
			this.fullName = fullName;
		}
    }
    
    static private final ThreadLocal<FullyQualifiedTimerStartName> last = new ThreadLocal<PerfMonTimer.FullyQualifiedTimerStartName>() {
        protected FullyQualifiedTimerStartName initialValue() {
            return new FullyQualifiedTimerStartName();
        }
    };
    
    // Package Level...
    static String getLastFullyQualifiedStartNameForThread() {
    	return last.get().getFullName();
    }
    
    static private void setLastFullyQualifiedStartNameForThread(String key) {
    	last.get().setFullName(key);
    }
}

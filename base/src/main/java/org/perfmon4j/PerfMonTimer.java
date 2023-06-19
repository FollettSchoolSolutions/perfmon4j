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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.perfmon4j.ThreadTracesBase.UniqueThreadTraceTimerKey;
import org.perfmon4j.reactive.ReactiveContext;
import org.perfmon4j.reactive.ReactiveContextManager;
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
        final boolean isReactiveTimer = reactiveContextID != null;
    	final boolean haveActiveTimer = (NULL_TIMER != result);  // It is OK to do an object compare here.
    	final boolean haveActiveThreadTrace = 
    			!isReactiveTimer // Reactive timers do not get tracked within thread traces.
    			&& (ThreadTraceMonitor.activeThreadTraceFlag.get().isActive()
    				|| ReactiveContext.isActiveThreadTracesOnContext()); 
    	
    	List<WeakReference<Procedure>> exitMethods = null;
        if (haveActiveTimer || haveActiveThreadTrace) {
        	String monitorName = "";
	        try {
	        	long startTime = MiscHelper.currentTimeWithMilliResolution(); 
	            monitorName = mon.getName();
	            if (haveActiveThreadTrace) {
	            	exitMethods = new ArrayList<WeakReference<Procedure>>();
	            	
	                ThreadTracesBase tInternalOnStack = ThreadTraceMonitor.getInternalThreadTracesOnStack();
	                ThreadTracesBase tExternalOnStack = ThreadTraceMonitor.getExternalThreadTracesOnStack();

	            	final boolean haveActiveInternalThreadTrace = tInternalOnStack.isActive();
	            	final boolean haveActiveExternalThreadTrace = tExternalOnStack.isActive();
	            
		            if (haveActiveInternalThreadTrace) {
		            	final UniqueThreadTraceTimerKey key = tInternalOnStack.enterCheckpoint(monitorName, startTime);
		            	exitMethods.add(new WeakReference<Procedure>(() -> tInternalOnStack.exitCheckpoint(key)));
		            }
		            if (haveActiveExternalThreadTrace) {
		            	final UniqueThreadTraceTimerKey key = tExternalOnStack.enterCheckpoint(monitorName, startTime);
		            	exitMethods.add(new WeakReference<Procedure>(() -> tExternalOnStack.exitCheckpoint(key)));
		            }

		            if (ReactiveContext.isActiveThreadTracesOnContext()) {
		            	for (ReactiveContext context : ReactiveContextManager.getContextManagerForThread().getActiveContexts()) {
		            		final ThreadTracesBase tExternal =  context.getExternalMonitorsOnContext();
		            		if (tExternal != null && tExternal.isActive()) {
		            			final UniqueThreadTraceTimerKey key = tExternal.enterCheckpoint(monitorName, startTime); 
				            	exitMethods.add(new WeakReference<Procedure>(() -> tExternal.exitCheckpoint(key)));
		            		}
		            		final ThreadTracesBase tInternal =  context.getInternalMonitorsOnContext();
		            		if (tInternal != null && tInternal.isActive()) {
		            			final UniqueThreadTraceTimerKey key = tInternal.enterCheckpoint(monitorName, startTime); 
				            	exitMethods.add(new WeakReference<Procedure>(() -> tInternal.exitCheckpoint(key)));
		            		}
		            	}
		            }
	            }
	            if (haveActiveTimer || haveActiveThreadTrace) {
		            // To keep track of the checkpoints for thread tracing we 
		            // must be able to identify the timer passed to PerfMonTimer.stop()
		            result = new TimerWrapper(result, wrapperInternalKey, wrapperExternalKey);
	            	result.start(startTime, reactiveContextID);
	            }
	        } catch (ThreadDeath th) {
	            throw th;   // Always rethrow this error
	        } catch (Throwable th) {
	            logger.logError("Error starting monitor: " + monitorName, th);
	            result = NULL_TIMER;
	        }
	        
	        if (reactiveContextID != null || (exitMethods != null && !exitMethods.isEmpty())) {
	            // To keep track of tracing exit methods AND/OR the reactiveContext we 
	            // must be able to identify the timer passed to PerfMonTimer.stop()
	            result = new TimerWrapper(result, reactiveContextID, exitMethods);
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
    
    private void start(long now, String reactiveContextID) {
        if (perfMon != null) {
            perfMon.start(now, this, reactiveContextID);
            next.start(now, reactiveContextID);
        }
    }
    
    private static void stop(PerfMonTimer timer, boolean abort, String reactiveContextID) {
        if (timer != NULL_TIMER && timer != null && !timer.hasBeenStopped()) {
        try {
            	timer.exitAllCheckpoints();
                timer.stop(MiscHelper.currentTimeWithMilliResolution(), abort, reactiveContextID);
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
    private void stop(long now, boolean abort, String reactiveContextID) {
        if (perfMon != null) {
            next.stop(now, abort, reactiveContextID);
            perfMon.stop(now, abort, this, reactiveContextID);
        }
    }
    
    public static PerfMonTimer getNullTimer() {
        return NULL_TIMER;
    }
    
    protected void exitAllCheckpoints() {
    	// Do Nothing - Functionality will be provided by TimerWrapper
    }
    
    protected String getReactiveContextID() {
    	// Do Nothing (except return null)- Functionality will be provided by TimerWrapper
    	return null;
    }
    
    
    /**
     * Implemented in TimerWrapper class
     * @return
     */
    protected boolean hasBeenStopped() {
    	return false;
    }
    
    /**
     * Implemented in TimerWrapper class
     * @return
     */
    protected void flagStopped() {
    	/** Empty **/
    }

    /**
     * Implemented in TimerWrapper class
     * @return
     */
	/* package */ void storeReferenceCountForOffThreadStop(ReferenceCount referenceCount) {
		/** Empty **/
	}

    /**
     * Implemented in TimerWrapper class
     * @return
     */
	/* package */ ReferenceCount getReferenceCount() {
		return null;
	}

    /**
     * If you need a mutable version, you must wrap the
     * Immutable timer with the mutable TimerWrapper.
     * 
     * This allows the TimerInstance to maintain state
     * between the start and stop calls.  
     * @return
     */
	/* package */ boolean isMutable() {
		// Will be overriden in WrapperClass to indicate it can
		// accept and maintain state.
		return false;
	}
	
	
    /**
     * Implemented in TimerWrapper class
     * 
     * This should only be used for testing!
     * 
     * The name of the effective monitor category associated
     * with this timer.
     * 
     * For example if you have appenders listening only to a 
     * parent monitor (i.e. "a") and not a child (i.e. "a.b"), 
     * by using appenderPattern = "./", both of the following 
     * PerfMonTimer start() would be associated with the
     * same effective category, "a":
     * 		PerfMonTimer.start("a")
     * 		PerfMonTimer.start("a.b");
     * 
     * @return
     */
	/* package */ String getEffectiveMonitorCategory() {
		return null;
	}
	
	
    /**
     * This class is only used when we return a Timer that is part of
     * a thread trace.
     */
    private static class TimerWrapper extends PerfMonTimer {
    	private boolean hasBeenStopped = false;
    	final private String reactiveContextID;
    	final private List<WeakReference<Procedure>> exitCheckpoints; 
        private ReferenceCount referenceCount = null;
        private final String effectiveCategory;
        
        TimerWrapper(PerfMonTimer timer, String reactiveContextID, List<WeakReference<Procedure>> exitCheckpoints) {
            super(timer.perfMon, timer.next); 
            this.reactiveContextID = reactiveContextID;
            this.exitCheckpoints = exitCheckpoints;
            if (timer.perfMon != null) {
            	effectiveCategory = timer.perfMon.getName();	
            } else {
            	effectiveCategory = "";
            }
        }

		public static PerfMonTimer wrapIfNeeded(PerfMonTimer timer) {
			PerfMonTimer result = timer;
			
			if (result != null
				&& !result.isMutable() 
				&& result != NULL_TIMER) {
				return new TimerWrapper(timer, null, null);
			}
			return result;
        @Override
        protected String getReactiveContextID() {
        	return reactiveContextID;
        }        
        
        @Override
        protected void exitAllCheckpoints() {
        	if (exitCheckpoints != null) {
	        	for (WeakReference<Procedure> c : exitCheckpoints) {
	        		Procedure exitCheckpoint = c.get();
	        		if (exitCheckpoint != null) {
	        			exitCheckpoint.execute();
	        		}
	        	}
        	}
        }
    }
    
    @FunctionalInterface
    private static interface Procedure {
    	public void execute();
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
    
    static private final ThreadLocal<PerfMonTimer.FullyQualifiedTimerStartName> last = new ThreadLocal<PerfMonTimer.FullyQualifiedTimerStartName>() {
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

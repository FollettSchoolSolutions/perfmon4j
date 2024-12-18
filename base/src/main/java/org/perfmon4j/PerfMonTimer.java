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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.PerfMon.ReferenceCount;
import org.perfmon4j.ThreadTracesBase.UniqueThreadTraceTimerKey;
import org.perfmon4j.reactive.ReactiveContext;
import org.perfmon4j.reactive.ReactiveContextManager;
import org.perfmon4j.remotemanagement.ExternalAppender;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class PerfMonTimer {
	private final static AtomicLong nextReactiveContextID = new AtomicLong(0);
    public static final String IMPLICIT_REACTIVE_CONTEXT_PREFIX = "$ImplicitCTX:";

	private static final String DISABLE_RECURSION_PREVENTION_KEY = PerfMonTimer.class.getName() + ".DisableRecursionPrevention";
	private static final boolean ENABLE_RECURSION_PREVENTION = !Boolean.getBoolean(DISABLE_RECURSION_PREVENTION_KEY);

	private static final RecursionPreventor onStartRecursionPreventor = new RecursionPreventor();

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

    public static PerfMonTimer startReactive(PerfMon mon) {
    	return startReactive(mon, true);
    }

	public static PerfMonTimer startReactive(PerfMon mon, boolean attachToExistingReactiveContext) {
    	return start(mon, getNextReactiveID(attachToExistingReactiveContext));
	}

    public static PerfMonTimer start(PerfMon mon, String reactiveContextID) {
        if (!PerfMon.isConfigured() && !ExternalAppender.isActive()) {
            return NULL_TIMER;
        }
        if (onStartRecursionPreventor.isFlagged()) {
        	// Must ensure we don't end up in recursion.
//System.err.println("!!!****  STOPPED RECURSION IN START! ****:" + mon.getName());
        	return NULL_TIMER;
        }

        try {
        	onStartRecursionPreventor.flag();

	        PerfMonTimer result = mon.getPerfMonTimer();
	        final boolean isReactiveTimer = reactiveContextID != null;
	    	final boolean haveActiveTimer = (NULL_TIMER != result);  // It is OK to do an object compare here.
	    	final boolean haveActiveThreadTrace =
	    			!isReactiveTimer // Reactive timers do not get tracked within thread traces.
	    			&& (ThreadTraceMonitor.activeThreadTraceFlag.get().isActive()
	    				|| ReactiveContext.isActiveThreadTracesOnContext());

	    	List<ExitCheckpoint> exitMethods = null;
	        if (haveActiveTimer || haveActiveThreadTrace) {
	        	String monitorName = "";
		        try {
		        	long startTime = MiscHelper.currentTimeWithMilliResolution();
		            monitorName = mon.getName();
		            if (haveActiveThreadTrace) {
			        	long sqlStartTime = SQLTime.getSQLTime();
		            	exitMethods = new ArrayList<ExitCheckpoint>();

		                ThreadTracesBase tInternalOnStack = ThreadTraceMonitor.getInternalThreadTracesOnStack();
		                ThreadTracesBase tExternalOnStack = ThreadTraceMonitor.getExternalThreadTracesOnStack();

		            	final boolean haveActiveInternalThreadTrace = tInternalOnStack.isActive();
		            	final boolean haveActiveExternalThreadTrace = tExternalOnStack.isActive();

			            if (haveActiveInternalThreadTrace) {
			            	final UniqueThreadTraceTimerKey key = tInternalOnStack.enterCheckpoint(monitorName, startTime, sqlStartTime);
			            	exitMethods.add((S, S1) -> tInternalOnStack.exitCheckpoint(key, S, S1));
			            }
			            if (haveActiveExternalThreadTrace) {
			            	final UniqueThreadTraceTimerKey key = tExternalOnStack.enterCheckpoint(monitorName, startTime, sqlStartTime);
			            	exitMethods.add((S, S1) -> tExternalOnStack.exitCheckpoint(key, S, S1));
			            }

			            if (ReactiveContext.isActiveThreadTracesOnContext()) {
			            	for (ReactiveContext context : ReactiveContextManager.getContextManagerForThread().getActiveContexts()) {
			            		final ThreadTracesBase tExternal =  context.getExternalMonitorsOnContext();
			            		if (tExternal != null && tExternal.isActive()) {
			            			final UniqueThreadTraceTimerKey key = tExternal.enterCheckpoint(monitorName, startTime, sqlStartTime);
					            	exitMethods.add((S, S1) -> tExternal.exitCheckpoint(key, S, S1));
			            		}
			            		final ThreadTracesBase tInternal =  context.getInternalMonitorsOnContext();
			            		if (tInternal != null && tInternal.isActive()) {
			            			final UniqueThreadTraceTimerKey key = tInternal.enterCheckpoint(monitorName, startTime, sqlStartTime);
					            	exitMethods.add((S, S1) -> tInternal.exitCheckpoint(key, S, S1));
			            		}
			            	}
			            }
		            }
		            if (haveActiveTimer) {
			            // To keep track of the checkpoints for thread tracing we
			            // must be able to identify the timer passed to PerfMonTimer.stop()
			            result = new TimerWrapper(result, reactiveContextID, exitMethods);
		            	result.start(startTime, reactiveContextID);
		            } else if (reactiveContextID != null || (exitMethods != null && !exitMethods.isEmpty())) {
		            	result = new TimerWrapper(result, reactiveContextID, exitMethods);
		            }
		        } catch (ThreadDeath th) {
		            throw th;   // Always rethrow this error
		        } catch (Throwable th) {
		            logger.logError("Error starting monitor: " + monitorName, th);
		            result = NULL_TIMER;
		        }
	        }

	        return result;
        } finally {
        	onStartRecursionPreventor.unFlag();
        }
    }

    public static PerfMonTimer start(String key) {
    	return start(key, false, null);
    }

    public static PerfMonTimer startReactive(String key) {
    	return startReactive(key, false, true);
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

    public static PerfMonTimer startReactive(String key, boolean isDynamicKey) {
    	return startReactive(key, isDynamicKey, true);
    }

    public static PerfMonTimer startReactive(String key, boolean isDynamicKey, boolean attachToExistingReactiveContext) {
    	return start(key, isDynamicKey, getNextReactiveID(attachToExistingReactiveContext));
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

    private static void stop(PerfMonTimer timer, boolean abort) {
        try {
            if (timer != null && timer != NULL_TIMER && !timer.hasBeenStopped()) {
            	timer.exitAllCheckpoints();
                timer.stop(MiscHelper.currentTimeWithMilliResolution(), abort);
            }
        } catch (ThreadDeath th) {
            throw th;   // Always rethrow this error
        } catch (Throwable th) {
            logger.logError("Error stopping timer", th);
        } finally {
        	if (timer != null) {
        		timer.flagStopped();
        	}
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
            perfMon.stop(now, abort, this, getReactiveContextID());
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


    @FunctionalInterface
    private static interface ExitCheckpoint {
    	public void exit(long stopTime, long sqlStopTime);
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

	private static class TimerWrapper extends PerfMonTimer {
    	final private String reactiveContextID;
    	final private List<ExitCheckpoint> exitCheckpoints;
    	private boolean hasBeenStopped = false;
        private ReferenceCount referenceCount = null;
        private final String effectiveCategory;

        TimerWrapper(PerfMonTimer timer, String reactiveContextID, List<ExitCheckpoint> exitCheckpoints) {
            super(timer.perfMon, wrapIfNeeded(timer.next, reactiveContextID));
            this.reactiveContextID = reactiveContextID;
            this.exitCheckpoints = exitCheckpoints;
            if (timer.perfMon != null) {
            	effectiveCategory = timer.perfMon.getName();
            } else {
            	effectiveCategory = "";
            }
        }

		public static PerfMonTimer wrapIfNeeded(PerfMonTimer timer, String reactiveContextID) {
			PerfMonTimer result = timer;

			if (result != null
				&& result != NULL_TIMER
				&& !result.isMutable()) {
				return new TimerWrapper(timer, reactiveContextID, null);
			}
			return result;
		}

        @Override
        protected String getReactiveContextID() {
        	return reactiveContextID;
        }

        @Override
        protected void exitAllCheckpoints() {
        	if (exitCheckpoints != null) {
        		long stopTime = MiscHelper.currentTimeWithMilliResolution();
        		long sqlStopTime = SQLTime.getSQLTime();

	        	for (ExitCheckpoint exitCheckpoint : exitCheckpoints) {
        			exitCheckpoint.exit(stopTime, sqlStopTime);
	        	}
        	}
        }

		@Override
		protected boolean hasBeenStopped() {
			return hasBeenStopped;
		}

		@Override
		protected void flagStopped() {
			hasBeenStopped = true;
		}

		@Override
		/* package */ void storeReferenceCountForOffThreadStop(ReferenceCount referenceCount) {
			this.referenceCount = referenceCount;
		}

		@Override
		/* package */ ReferenceCount getReferenceCount() {
			return referenceCount;
		}

		@Override
		/* package */ String getEffectiveMonitorCategory() {
			return effectiveCategory;
		}

		@Override
		boolean isMutable() {
			return true;
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

    static private String getNextReactiveID(boolean attachToExistingReactiveContext) {
    	if (attachToExistingReactiveContext) {
    		String explicitReactiveContextID = ReactiveContextManager.getContextManagerForThread().getExplicitReactiveContextID();
    		if (explicitReactiveContextID != null) {
				logger.logDebug("Attaching timer to outer explicitReactiveContext: " + explicitReactiveContextID);
				return explicitReactiveContextID;
    		}
    	}
    	return IMPLICIT_REACTIVE_CONTEXT_PREFIX + nextReactiveContextID.incrementAndGet();
    }

	private static class RecursionPreventor {
		private final ThreadLocal<Boolean> flag = ThreadLocal.withInitial(() -> Boolean.FALSE);

		private boolean isFlagged() {
			return ENABLE_RECURSION_PREVENTION && flag.get().booleanValue();
		}

		private void flag() {
			if (ENABLE_RECURSION_PREVENTION) {
				flag.set(Boolean.TRUE);
			}
		}

		private void unFlag() {
			if (ENABLE_RECURSION_PREVENTION) {
				flag.set(Boolean.FALSE);
			}
		}
	}

}



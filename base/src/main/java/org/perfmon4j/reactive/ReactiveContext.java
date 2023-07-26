package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.PerfMon;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceData;
import org.perfmon4j.ThreadTracesBase;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class ReactiveContext {
	private final static Logger logger = LoggerFactory.initLogger(ReactiveContext.class);
	

	private static final AtomicInteger activeThreadTraceFlag = new AtomicInteger(0);
	
	
	private final Serializable activeThreadLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
	/**
	 * A context can only be associated with one thread at a time.
	 * This allows the context to locate the one active thread
	 * and clear or move it to another thread.
	 * 
	 * This variable MUST be only modified by the ReactiveContextManager
	 * class!
	 */
	private ReactiveContextManagerIdentifier activeThread = null;

	
	private final String contextID;
	
	private final ThreadTracesBase internalMonitorsOnContext = new ThreadTracesOnReactiveContext(PerfMon.MAX_ALLOWED_INTERNAL_THREAD_TRACE_ELEMENTS);
	private final ThreadTracesBase externalMonitorsOnContext = new ThreadTracesOnReactiveContext(PerfMon.MAX_ALLOWED_EXTERNAL_THREAD_TRACE_ELEMENTS);
	private final AtomicLong sqlTimeAccumulator = new AtomicLong(0);
		
	/** START Comment - mutableMemberData
	 * The payloadMap and the triggerValidatorStack are the two data members 
	 * that are used to indicate when a context is no longer needed 
	 * and can be dereferenced.
	 * 
	 *  When there are no remaining payload objects (Used to store PerfMon referenceCounts accross Monitors)
	 *  or ThreadTraceTriggers the element can be considered empty.
	 * 
	 * If you are adding/removing elements from this list make sure you have 
	 * synchronize on the mutableMemberDataLockToken. 
	 * **/
	private final Serializable mutableMemberDataLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
	
	private volatile boolean empty = true;
	private volatile Object[] cachedPayloads = null;
	private volatile ThreadTraceConfig.TriggerValidator[] cachedTriggerValidatorArray = null;
	
	private final Map<Long, Object> payloadMap= new HashMap<Long, Object>();
	private final Stack<ThreadTraceConfig.TriggerValidator> triggerValidatorStack = new Stack<ThreadTraceConfig.TriggerValidator>();
	/** END Comment **/
	
	public ReactiveContext(String contextID) {
		this.contextID = contextID;
	}

	public String getContextID() {
		return contextID;
	}
	
	public Object getPayload(Long monitorID) {
		synchronized (mutableMemberDataLockToken) {
			return payloadMap.get(monitorID);
		}
	}
	
	public void addPayload(Long monitorID, Object payload) {
		synchronized (mutableMemberDataLockToken) {
			payloadMap.put(monitorID, payload);
			empty = false;
			cachedPayloads = null;
		}
	}

	public Object removePayload(Long monitorID) {
		synchronized (mutableMemberDataLockToken) {
			Object result = payloadMap.remove(monitorID); 
			empty = determineIsEmpty();
			cachedPayloads = null;
			return result;
		}
	}
	
	/**
	 * When a context is "empty" it can be dereferenced from all context manager
	 * maps and be made available for Garbage Collection (unless it is
	 * referenced by an external object).
	 * 
	 * For our purposes the context is empty once it has no more payload
	 * objects in the payloadMap or triggerValidators in the triggerValidatorStack.
	 * 
	 * @return
	 */
	private boolean determineIsEmpty() {
		synchronized (mutableMemberDataLockToken) {
			return (payloadMap.isEmpty() && triggerValidatorStack.isEmpty());
		}
	}
	
	
	public Object[] getPayloads() {
		Object[] result = cachedPayloads;
		
		if (result == null) {
			synchronized (mutableMemberDataLockToken) {
				result = cachedPayloads = payloadMap.values().toArray();
			}
		}
		
		return result;
	}
	
	public boolean isEmpty() {
		synchronized (mutableMemberDataLockToken) {
			return empty;
		}
	}

	ReactiveContextManagerIdentifier getActiveThread() {
		synchronized (activeThreadLockToken) {
			return activeThread;
		}
	}
	
	ReactiveContextManager getActiveOwner() {
		synchronized (activeThreadLockToken) {
			ReactiveContextManager result = null;
			if (activeThread != null) {
				result = activeThread.getThreadContextManager();
			} 
			return result;
		}
	}

	void setActiveOwner(ReactiveContextManager newOwner) {
		synchronized (activeThreadLockToken) {
			if (newOwner != null) {
				this.activeThread = newOwner.getManagerID();
			} else {
				this.activeThread = null;
			}
		}
	}

	
	public void setActiveThread(ReactiveContextManagerIdentifier activeThread) {
		synchronized (activeThreadLockToken) {
			this.activeThread = activeThread;
		}
	}
	
	static public boolean isActiveThreadTracesOnContext() {
		return activeThreadTraceFlag.get() > 0;
	}

	public ThreadTracesBase getInternalMonitorsOnContext() {
		return internalMonitorsOnContext;
	}

	public ThreadTracesBase getExternalMonitorsOnContext() {
		return externalMonitorsOnContext;
	}
	
	/**
	 * Returns the accumulated number of milliseconds that
	 * methods running under this context have spent 
	 * in the JDBC layer since the context was created
	 * (If SQLTime is enabled). 
	 * @return
	 */
	public long getSQLTime() {
		return sqlTimeAccumulator.get();
	}

	/**
	 * Should only be called by org.perfmon4j.SQLTime
	 * @param millis
	 */
	public void incrementSQLTime(long millis) {
		sqlTimeAccumulator.addAndGet(millis);
	}
	
	/* package */ void pushTriggerValidator(ThreadTraceConfig.TriggerValidator validator) {
		synchronized(mutableMemberDataLockToken) {
			triggerValidatorStack.push(validator);
			empty = false;
			cachedTriggerValidatorArray = null;
		}
	}
	
	/* package */  void popTriggerValidator() {
		synchronized(mutableMemberDataLockToken) {
			try {
				triggerValidatorStack.pop();
				empty = determineIsEmpty();
				cachedTriggerValidatorArray = null;
			} catch(EmptyStackException e) {
				logger.logWarn("Unbalanced call to ReactiveContext.popTriggerValidator");
			}
		}
	}
	
	/* package */ ThreadTraceConfig.TriggerValidator[] getValidators() {
		ThreadTraceConfig.TriggerValidator[] result = cachedTriggerValidatorArray;
		
		if (result == null) {
			synchronized(mutableMemberDataLockToken) {
				result = cachedTriggerValidatorArray = triggerValidatorStack.toArray(new ThreadTraceConfig.TriggerValidator[] {});
			}
		}
		
		return result;
	}
	
    private static class ThreadTracesOnReactiveContext extends ThreadTracesBase {
    	// Although this shouldn't be accessed across threads, it still could
    	// so we need to synchronize access to the linked list.
    	private final Serializable tracesLockToken = new Serializable() {
			private static final long serialVersionUID = 1L;
		};

		ThreadTracesOnReactiveContext(int maxElements) {
			super(maxElements);
		}
		
		@Override
		public void incrementActiveThreadTraceFlag() {
			ReactiveContext.activeThreadTraceFlag.incrementAndGet();
		}

		@Override
		public void decrementActiveThreadTraceFlag() {
			ReactiveContext.activeThreadTraceFlag.decrementAndGet();
		}

		@Override
		protected void start(String monitorName, int maxDepth, int minDurationToCapture, long startTime, long sqlStartTime) {
			synchronized (tracesLockToken) {
				super.start(monitorName, maxDepth, minDurationToCapture, startTime, sqlStartTime);
			}
		}

		@Override
		protected ThreadTraceData stop(String monitorName, long stopTime, long sqlStopTime) {
			synchronized (tracesLockToken) {
				return super.stop(monitorName, stopTime, sqlStopTime);
			}
		}

		@Override
		protected UniqueThreadTraceTimerKey enterCheckpoint(String timerMonitorName, long startTime, long sqlStartTime) {
			synchronized (tracesLockToken) {
				return super.enterCheckpoint(timerMonitorName, startTime, sqlStartTime);
			}
		}

		@Override
		protected void exitCheckpoint(UniqueThreadTraceTimerKey timerKey, long stopTime, long sqlStopTime) {
			synchronized (tracesLockToken) {
				super.exitCheckpoint(timerKey, stopTime, sqlStopTime);
			}
		}
    }
}

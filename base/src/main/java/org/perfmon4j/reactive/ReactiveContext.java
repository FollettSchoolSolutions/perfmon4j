package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.PerfMon;
import org.perfmon4j.ThreadTraceData;
import org.perfmon4j.ThreadTracesBase;

public class ReactiveContext {
	private final Serializable payloadLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
	private static final AtomicInteger activeThreadTraceFlag = new AtomicInteger(0);
	
	private final Map<Long, Object> payloadMap= new HashMap<Long, Object>();
	
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
	
	private volatile boolean empty = true;
	private volatile Object[] cachedPayloads = null;
	private final ThreadTracesBase internalMonitorsOnContext = new ThreadTracesOnReactiveContext(PerfMon.MAX_ALLOWED_INTERNAL_THREAD_TRACE_ELEMENTS);
	private final ThreadTracesBase externalMonitorsOnContext = new ThreadTracesOnReactiveContext(PerfMon.MAX_ALLOWED_EXTERNAL_THREAD_TRACE_ELEMENTS);
	private final AtomicLong sqlTimeAccumulator = new AtomicLong(0);
	
	public ReactiveContext(String contextID) {
		this.contextID = contextID;
	}

	public String getContextID() {
		return contextID;
	}
	
	public Object getPayload(Long monitorID) {
		synchronized (payloadLockToken) {
			return payloadMap.get(monitorID);
		}
	}
	
	public void addPayload(Long monitorID, Object payload) {
		synchronized (payloadLockToken) {
			payloadMap.put(monitorID, payload);
			empty = false;
			cachedPayloads = null;
		}
	}

	public Object removePayload(Long monitorID) {
		synchronized (payloadLockToken) {
			Object result = payloadMap.remove(monitorID); 
			empty = payloadMap.isEmpty();
			cachedPayloads = null;
			return result;
		}
	}
	
	public Object[] getPayloads() {
		Object[] result = cachedPayloads;
		
		if (result == null) {
			synchronized (payloadLockToken) {
				result = cachedPayloads = payloadMap.values().toArray();
			}
		}
		
		return result;
	}
	
	public boolean isEmpty() {
		synchronized (payloadLockToken) {
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
			this.activeThread = activeThread;;
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

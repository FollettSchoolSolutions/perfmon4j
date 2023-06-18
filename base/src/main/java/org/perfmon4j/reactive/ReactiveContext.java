package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ReactiveContext {
	private final Serializable payloadLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
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
}

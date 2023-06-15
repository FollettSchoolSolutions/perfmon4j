package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ReactiveContext {
	private final Serializable payloadLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
	private final Map<Long, Object> payloadMap= new HashMap<Long, Object>();
	
	/**
	 * This member must only be modified by the ReactiveContextManager 
	 * class
	 */
	volatile ReactiveContextManager activeThread = null;
	private volatile boolean empty = true;
	private volatile Object[] cachedPayloads = null;
	
	public ReactiveContext() {
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
}

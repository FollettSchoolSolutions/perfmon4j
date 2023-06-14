package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.PerfMon;

public abstract class ReactiveContext<T extends Object> {
	private final Object contextID;
	
	private final Serializable payloadLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
	private final Map<PerfMon, T> payloadMap= new HashMap<PerfMon,T>();
	
	/**
	 * This member must only be modified by the ReactiveContextManager 
	 * class
	 */
	volatile ReactiveContextManager activeThread = null;
	
	public ReactiveContext(Object contextID) {
		this.contextID = contextID;
	}
	
	public Object getContextID() {
		return contextID;
	}

	public T getPayload(PerfMon owner) {
		synchronized (payloadLockToken) {
			T result = payloadMap.get(owner);
			
			if (result == null) {
				result = newPayload();
				payloadMap.put(owner, result);
			}
			return result;
		}
	}
	
	
	protected abstract T newPayload(); 
}

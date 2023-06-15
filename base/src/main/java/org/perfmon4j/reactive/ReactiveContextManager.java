package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class ReactiveContextManager {
	private static final Logger logger = LoggerFactory.initLogger(ReactiveContextManager.class);

	/** GLOBAL Members - Start **/
	private static final Serializable bindToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};
	
	private static final Map<String, ReactiveContext> globalContextMap = new HashMap<String, ReactiveContext>();
	/** GLOBAL Members - End **/
		
	
	/** Thread Specific Members - Start **/
	private volatile ReactiveContext[] cachedContexts = null; 
	
	private final Map<String, ReactiveContext> threadContextMap = new HashMap<String, ReactiveContext>();

	private static final ThreadLocal<ReactiveContextManager> threadLocal = new ThreadLocal<ReactiveContextManager>() {
		@Override
		protected ReactiveContextManager initialValue() {
			return new ReactiveContextManager();
		}
	};
	/** Thread Specific Members - End **/
	
	public static ReactiveContextManager getContextManagerForThread() {
		return threadLocal.get();
	}
	
	
	public void addPayload(String contextID, Long monitorID, Object payload) {
		synchronized(bindToken) {
			ReactiveContext result = globalContextMap.get(contextID);
			if (result == null) {
				result = new ReactiveContext();

				// Associate with the global MAP
				globalContextMap.put(contextID, result);
				
				// Associate with the Thread
				threadContextMap.put(contextID, result);
				result.activeThread = this;
			}
			result.addPayload(monitorID, payload);
			
			// Clear the cache.
			cachedContexts = null;
		}
	}
	
	public Object deletePayload(String contextID, Long monitorID) {
		Object result = null;
		
		synchronized(bindToken) {
			ReactiveContext context = globalContextMap.get(contextID);
			if (context != null) {
				result = context.removePayload(monitorID);
				
				// If the context is empty remove it.
				if (context.isEmpty()) {
					// Remove from the global Map
					globalContextMap.remove(contextID);

					// Remove from the thread based map
					threadContextMap.remove(contextID);
				}
			}
			// Clear the cache.
			cachedContexts = null;
		}
		return result;
	}
	
	/**
	 * Finds the context and dissociates it from it's prior thread
	 * and associates it with the thread associated with the caller.
	 * @param contextID
	 */
	public void moveContext(String contextID) {
		synchronized(bindToken) {
			ReactiveContext context = globalContextMap.get(contextID);
			if (context == null) {
				logger.logError("Context not found, unable to move. contextID: " + contextID);
			} else {
				if (context.activeThread != null) {
					context.activeThread.threadContextMap.remove(context);
					// Clear cache of previous thread.
					context.activeThread.cachedContexts = null;
				}
				context.activeThread = this;
				threadContextMap.put(contextID, context);

				// Clear the cache.
				cachedContexts = null;
			}
		}
	}
	
	public ReactiveContext[] getContextsOnThread() {
		ReactiveContext[] result = cachedContexts;
		
		if (result == null) {
			synchronized (bindToken) {
				result = cachedContexts = threadContextMap.values().toArray(new ReactiveContext[]{});
			}
		}
		
		return result;
	}
}

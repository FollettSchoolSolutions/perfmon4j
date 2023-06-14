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
	
	private static final Map<Object, ReactiveContext<?>> globalContextMap = new HashMap<Object, ReactiveContext<?>>();
	/** GLOBAL Members - End **/
		
	
	/** Thread Specific Members - Start **/
	private volatile ReactiveContext<?>[] cachedContexts = null; 
	
	private final Map<Object, ReactiveContext<?>> threadContextMap = new HashMap<Object, ReactiveContext<?>>();

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
	
	public void newContext(ReactiveContext<?> context) {
		synchronized(bindToken) {
			// TODO: Assert that context.threadManager == null
			
			// Associate with the global MAP
			globalContextMap.put(context.getContextID(), context);
			
			// Associate with the Thread
			threadContextMap.put(context.getContextID(), context);
			context.activeThread = this;
			
			// Clear the cache.
			cachedContexts = null;
		}
	}
	
	public void deleteContext(Object contextID) {
		synchronized(bindToken) {
			// Remove from the global Map
			ReactiveContext<?> contextFromGlobal = globalContextMap.remove(contextID);
			
			// Remove from the thread based map
			ReactiveContext<?> contextFromThread = threadContextMap.remove(contextID);
	
			ReactiveContext<?> contextToCleanUp = contextFromGlobal != null ? contextFromGlobal : contextFromThread;
			if (contextToCleanUp != null) {
				contextToCleanUp.activeThread = null;
			}
			
			// Clear the cache.
			cachedContexts = null;
		}
	}
	
	/**
	 * Finds the context and dissociates it from it's prior thread
	 * and associates it with the thread associated with the caller.
	 * @param contextID
	 */
	public void moveContext(Object contextID) {
		synchronized(bindToken) {
			ReactiveContext<?> context = globalContextMap.get(contextID);
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
	
	public ReactiveContext<?>[] getContextsOnThread() {
		ReactiveContext<?>[] result = cachedContexts;
		
		if (result == null) {
			synchronized (bindToken) {
				result = cachedContexts = threadContextMap.values().toArray(new ReactiveContext<?>[]{});
			}
		}
		
		return result;
	}
}

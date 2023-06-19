package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class ReactiveContextManager {
	private static final Logger logger = LoggerFactory.initLogger(ReactiveContextManager.class);
	private static final ReactiveContext[] EMPTY_CONTEXT_ARRAY = new ReactiveContext[] {};
	
	/** GLOBAL Members - Start **/
	private static final Serializable bindToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};

	
	private static final Map<String, ReactiveContext> globalContextMap = new HashMap<String, ReactiveContext>();
	
	
	private static final AtomicInteger activeContextFlag = new AtomicInteger(0);
	/** GLOBAL Members - End **/
	
	/** Thread Specific Members - Start **/
	private final ReactiveContextManagerIdentifier managerID;
	
	private volatile ReactiveContext[] cachedContexts = null; 
	
	private final Map<String, ReactiveContext> managerContextMap = new HashMap<String, ReactiveContext>();
	
	private static final ThreadLocal<ReactiveContextManager> threadLocal = new ThreadLocal<ReactiveContextManager>() {
		@Override
		protected ReactiveContextManager initialValue() {
			return new ReactiveContextManager();
		}
	};
	/** Thread Specific Members - End **/

	private ReactiveContextManager() {
		managerID = new ReactiveContextManagerIdentifier(Thread.currentThread(), this);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(managerID);
	}

	public ReactiveContextManagerIdentifier getManagerID() {
		return managerID;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReactiveContextManager other = (ReactiveContextManager) obj;
		return Objects.equals(managerID, other.managerID);
	}
	
	/**
	 * Returns the ReactiveContextManager associated with the calling 
	 * thread.
	 * @return
	 */
	public static ReactiveContextManager getContextManagerForThread() {
		return threadLocal.get();
	}
	
	
	/**
	 * This is a cheap, inexpensive call, with very minimal synchronization required,
	 * to determine if any reactiveContexts are currently active
	 * within the JVM.
	 * 
	 * @return
	 */
	public static boolean areReactiveContextsActiveInJVM() {
		return activeContextFlag.get() > 0;
	}

	/**
	 * This method is called by a thread to populate a named (contextID/monitorID) payload on
	 * the ReactiveContext specified by contextID.
	 * 
	 * It is assumed that the calling thread is (or at least should be) the activeThread
	 * associated with this reactiveContext (associated with contextID).  If we find the 
	 * current thread is designated as the active thread we will move it.
	 * 
	 * SIDE EFFECTS - 
	 * 	1) This method WILL designate the caller as the active thread for the 
	 * associated reactiveContext.   
	 * 		 
	 *  2) If the reactiveContext does not exist it WILL be created.
	 * 
	 * Returns the specified payload
	 * @param contextID
	 * @param monitorID
	 * @param defaultSupplier
	 * @return
	 */
	public Object getPayload(String contextID, Long monitorID, ContextPayloadConstructor payloadConstructor) {
		String methodLine = buildMethodCallDebugLine("getPayload", 
				new String[] {"contextID", "monitorID", "payloadConstructor"}, 
				new Object[] {contextID, monitorID, payloadConstructor != null ? "(included=true)" : "(included=false)"});

		synchronized(bindToken) {
			boolean createdNewContext = false;
			ReactiveContext context = globalContextMap.get(contextID);
			if (context == null) {
				// For some reason the context does not exist... We will create it.
				context = new ReactiveContext(contextID);

				// Associate with the global MAP
				globalContextMap.put(contextID, context);
				activeContextFlag.set(globalContextMap.size());
				
				if (logger.isDebugEnabled()) {
					logger.logDebug(methodLine + " is initializing global context.");
				}
				createdNewContext = true;
			}
			
			if (!this.equals(context.getActiveOwner())) {
				String threadNameForContext = buildThreadNameForContext(context);
				
				moveContextToCurrentThread(contextID, context);
				
				if (logger.isDebugEnabled()) {
					if (createdNewContext) {
						logger.logDebug(methodLine + " new context has been assigned to thread.");
					} else {
						logger.logDebug(methodLine + " has moved context from " + threadNameForContext);
					}
				}
			}
			
			Object result = context.getPayload(monitorID);
			if (result == null && payloadConstructor != null) {
				result = payloadConstructor.buildPayload(context);
				context.addPayload(monitorID, result);
				if (logger.isDebugEnabled()) {
					logger.logDebug(methodLine + " has initialized the payload."); 
				}
			}
			return result;
		}
	}
	
	/**
	 * This method is (or at least should be called) by the current thread that
	 * actively** owns the context.  It will remove the named payload
	 * from the context (contextID/monitorID).
	 * 
	 *  ** Example:
	 *  	PerfMonTimer timer = PerfMonTimer.start("MyRequest", "MyReactiveContext");
	 *  	PerfMonTimer.stop(timer, "MyReactiveContext"); // <== you are here!
	 *  
	 *  SITE EFFECTS:
	 *  	1) If this is the last named Payload on the the ReactiveContext,
	 *  	the ReactiveContext will be discarded.
	 *  	2) If payloads remain the ReactiveContext will be transfered
	 *  	to the calling thread.
	 * 
	 * @param contextID
	 * @param monitorID
	 * @return returns the deletedPayload.
	 */
	public Object deletePayload(String contextID, Long monitorID) {
		String methodLine = buildMethodCallDebugLine("deletePayload", 
				new String[] {"contextID", "monitorID"}, 
				new Object[] {contextID, monitorID});
		Object result = null;
		
		synchronized(bindToken) {
			ReactiveContext context = globalContextMap.get(contextID);
			if (context != null) {
				String threadNameForContext = buildThreadNameForContext(context);
				
				result = context.removePayload(monitorID);
				if (logger.isDebugEnabled()) {
					logger.logDebug(methodLine + " has removed payload for context."); 
				}				
				// If the context does not contain any additional
				// payloads remove it.
				if (context.isEmpty()) {
					// Remove from the global Map
					globalContextMap.remove(contextID);
					activeContextFlag.set(globalContextMap.size());

					ReactiveContextManager activeOwner = context.getActiveOwner();
					activeOwner.managerContextMap.remove(contextID);
					context.setActiveOwner(null);

					if (logger.isDebugEnabled()) {
						logger.logDebug(methodLine + " has removed context."); 
					}
				} else if (!this.equals(context.getActiveOwner())){
					// Since the context has remaining payloads
					// transfer ownership to the calling thread
					moveContextToCurrentThread(contextID, context);
					if (logger.isDebugEnabled()) {
						logger.logDebug(methodLine + " has moved context from " + threadNameForContext); 
					}
				}
			} else {
				// Context not found.  Nothing to do.
			}
		}
		return result;
	}
	
	/**
	 * Finds the context and dissociates it from it's prior thread
	 * and associates it with the currently executing thread.
	 * @param contextID
	 */
	public void moveContext(String contextID) {
		if (contextID != null) {
			String methodLine = buildMethodCallDebugLine("moveContext", 
					new String[] {"contextID"}, 
					new Object[] {contextID});
			
			synchronized(bindToken) {
				ReactiveContext context = globalContextMap.get(contextID);
				if (context == null) {
					logger.logWarn(methodLine +  "Context not found, unable to move to Thread.");
				} else {
					if (!this.equals(context.getActiveOwner())) {
						String threadNameForContext = buildThreadNameForContext(context);
						moveContextToCurrentThread(contextID, context);
						if (logger.isDebugEnabled()) {
							logger.logDebug(methodLine + " has moved context from " + threadNameForContext); 
						}
					}
				}
			} 
		} 
	}

	public void dissociateContextFromThread(String contextID) {
		if (contextID != null) {
			String methodLine = buildMethodCallDebugLine("dissociateContextFromThread", 
					new String[] {"contextID"}, 
					new Object[] {contextID});
			
			synchronized(bindToken) {
				ReactiveContext context = globalContextMap.get(contextID);
				if (context != null && this.equals(context.getActiveOwner())) {
					// Owned by the current thread.  We will orphan
					// the context (It will still be available and
					// referenced in the global context)
					context.setActiveOwner(null);
					managerContextMap.remove(contextID);
					cachedContexts = null;
					
					if (logger.isDebugEnabled()) {
						logger.logDebug(methodLine +  "Context dissociated from thread. Context is now orphaned.");		
					}
				} else {
					// Context does not exist, or is owned by
					// a different thread.  Nothing to do.
				}
			}
		} else {
			// ContextID is null... Nothing left to do.
		}
	}
	
	/**
	 * This will return all of the active contexts currently associated
	 * with the currently executing thread.
	 * 
	 * You might ask, how can a single thread be working with more
	 * than a single reactiveContext at one time?
	 * 
	 *  The reason is there could be multiple contexts associated with multiple
	 * 	PerfMon monitors, tracking reactive workers for multiple tasks. 
	 * 
	 * @return
	 */
	public ReactiveContext[] getActiveContexts() {
		if (!areReactiveContextsActiveInJVM()) {
			return EMPTY_CONTEXT_ARRAY; 
		} else {
			ReactiveContext[] result = cachedContexts;
		
			if (result == null) {
				synchronized (bindToken) {
					result = cachedContexts = managerContextMap.values().toArray(EMPTY_CONTEXT_ARRAY); 
				}
			}
			return result;
		}
	}

	@FunctionalInterface
	public static interface ContextPayloadConstructor {
		Object buildPayload(ReactiveContext context);
	}
	
	/**
	 * A reactiveContext must only be associated with a single
	 * executing thread at a time.  
	 * 
	 * @param contextID
	 * @param context
	 */
	private void moveContextToCurrentThread(String contextID, ReactiveContext context) {
		synchronized (bindToken) {
			ReactiveContextManager currentOwner = context.getActiveOwner();
			if (!this.equals(currentOwner)) {
				if (currentOwner != null) {
					currentOwner.managerContextMap.remove(contextID);
					currentOwner.cachedContexts = null;
				} else {
					// Reactive context was orphaned.
					// Just need to assign it to the current thread.
				}
				this.managerContextMap.put(contextID, context);
				context.setActiveThread(this.managerID);
				cachedContexts = null;
			} else {
				// The current thread already owns the context.
				// nothing needs to be done.
			}
		}
	}

	private String buildMethodCallDebugLine(String methodName, String[]argNames, Object[]params) {
		if (logger.isDebugEnabled()) {
			StringBuilder result = new StringBuilder();
			
			result.append("**")
				.append(methodName)
				.append("(");
			
			for (int i = 0; i < argNames.length; i++ ) {
				if (i > 0) {
					result.append(", ");
				}
				result.append(argNames[i] + "=" + params[i]);
			}
			result.append(") -- Calling Thread(" + Thread.currentThread().getName() + ")** ");
			
			return result.toString();
		} else {
			return "";
		}
	}

	private String buildThreadNameForContext(ReactiveContext context) {
		if (logger.isDebugEnabled()) {
			ReactiveContextManager mgr = context.getActiveOwner();
			if (mgr != null) {
				return "Thread(" + mgr.getManagerID().getThreadName() + ")";
			} else {
				return "Thread(**orphaned**)";
			}
		} else {
			return"";
		}
	}
	
}

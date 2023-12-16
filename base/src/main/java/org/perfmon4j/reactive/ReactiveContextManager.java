package org.perfmon4j.reactive;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class ReactiveContextManager {
	private static final Logger logger = LoggerFactory.initLogger(ReactiveContextManager.class);
	private static final ReactiveContext[] EMPTY_CONTEXT_ARRAY = new ReactiveContext[] {};
	private static final ReactiveContextsVO EMPTY_CONTEXTS_VO = new ReactiveContextsVO(EMPTY_CONTEXT_ARRAY);
	
	/** GLOBAL Members - Start **/
	private static final Serializable bindToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};

	
	private static final Map<String, ReactiveContext> globalContextMap = new HashMap<String, ReactiveContext>();
	
	
	private static final AtomicInteger activeContextFlag = new AtomicInteger(0);
	/** GLOBAL Members - End **/
	
	/** Thread Specific Members - Start **/
	private final ReactiveContextManagerIdentifier managerID;
	
	private volatile ReactiveContextsVO cachedContexts = null; 
	
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
			Object result = null;
			boolean createContext = (payloadConstructor != null);
			ReactiveContext context = getOrCreateGlobalContext(contextID, createContext, methodLine);
			
			if (context != null) {
				result = context.getPayload(monitorID);
				if (result == null && payloadConstructor != null) {
					result = payloadConstructor.buildPayload(context);
					context.addPayload(monitorID, result);
					if (logger.isDebugEnabled()) {
						logger.logDebug(methodLine + " has initialized the payload."); 
					}
				}
				moveContextToCurrentThread(context);
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
			ReactiveContext context = getOrCreateGlobalContext(contextID, false, methodLine);
			if (context != null) {
				result = context.removePayload(monitorID);
				moveOrRemoveContextAfterDataChange(context, methodLine);
			}
		}
		return result;
	}
	
	
	/**
	 * This method is called after we remove data 
	 * (either a payload element or a triggerValidator) from
	 * the context.
	 * 
	 * If the context is now empty - we will remove it.
	 * If the context is not empty we will implicitly reassign it to the current
	 * thread.
	 */
	private void moveOrRemoveContextAfterDataChange(ReactiveContext context, String methodLine) {
		synchronized(bindToken) {
			// If the context does not contain any additional
			// data elements remove it
			if (context.isEmpty()) {
				String contextID = context.getContextID();
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
				// Since the context is still holding meaningful data,
				// transfer ownership to the calling thread
				moveContextToCurrentThread(context);
				if (logger.isDebugEnabled()) {
					logger.logDebug(methodLine + " has moved context from " + buildThreadNameForContext(context)); 
				}
			}
		}
	}
	
	private static ReactiveContext getOrCreateGlobalContext(String contextID, boolean allowCreate, String methodLine) {
		ReactiveContext context = null;
		synchronized(bindToken) {
			context = globalContextMap.get(contextID);
			if (context == null && allowCreate) {
				// For some reason the context does not exist... We will create it.
				context = new ReactiveContext(contextID);

				// Associate with the global MAP
				globalContextMap.put(contextID, context);
				activeContextFlag.set(globalContextMap.size());
				
				if (logger.isDebugEnabled()) {
					logger.logDebug(methodLine + " is initializing global context.");
				}
			}
		}
		return context;
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
					// This might not be a problem.  The context may have simply been completed.
					logger.logDebug(methodLine +  "Context not found, unable to move to Thread.");
				} else {
					if (!this.equals(context.getActiveOwner())) {
						moveContextToCurrentThread(context);
						if (logger.isDebugEnabled()) {
							String threadNameForContext = buildThreadNameForContext(context);
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
				ReactiveContext context = getOrCreateGlobalContext(contextID, false, methodLine);
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
		return getContextsVO().getContexts();
	}

	public String getExplicitReactiveContextID() {
		return getContextsVO().getExplicitContextID();
	}
	
	private ReactiveContextsVO getContextsVO() {
		if (!areReactiveContextsActiveInJVM()) {
			return EMPTY_CONTEXTS_VO; 
		} else {
			ReactiveContextsVO result = cachedContexts;
		
			if (result == null) {
				synchronized (bindToken) {
					result = cachedContexts = new ReactiveContextsVO(managerContextMap.values().toArray(EMPTY_CONTEXT_ARRAY)); 
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
	private void moveContextToCurrentThread(ReactiveContext context) {
		synchronized (bindToken) {
			ReactiveContextManager currentOwner = context.getActiveOwner();
			if (!this.equals(currentOwner)) {
				String contextID = context.getContextID();

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
	
	public void pushValidator(ThreadTraceConfig.TriggerValidator validator,  String contextID) {
		String methodLine = buildMethodCallDebugLine("pushValidator", 
				new String[] {"contextID"}, 
				new Object[] {contextID});
		
		ReactiveContext context = getOrCreateGlobalContext(contextID, true, methodLine);
		context.pushTriggerValidator(validator);
		moveContextToCurrentThread(context);
	}
	
	public void popValidator(String contextID) {
		String methodLine = buildMethodCallDebugLine("popValidator", 
				new String[] {"contextID"}, 
				new Object[] {contextID});
		
		
		ReactiveContext context = getOrCreateGlobalContext(contextID, false, methodLine);
		if (context != null) {
			context.popTriggerValidator();
			moveOrRemoveContextAfterDataChange(context, methodLine);
		} else {
			logger.logWarn("Warning attempt to pop TriggerValidator off a non-existant context: " + methodLine);
		}
	}
	
	private static final ThreadTraceConfig.TriggerValidator[] EMPTY_TRIGGER_VALIDATOR_ARRAY = new ThreadTraceConfig.TriggerValidator[] {};
	
	public ThreadTraceConfig.TriggerValidator[] getActiveContextTriggerValidatorsOnThread() {
		ThreadTraceConfig.TriggerValidator[] result = null;	
		
		ReactiveContext contexts[] = getActiveContexts();
		if (contexts.length == 0) {
			// Most likely scenario...  Let's make it fast.
			result = EMPTY_TRIGGER_VALIDATOR_ARRAY;
		} else if (contexts.length == 1) {
			// 2nd most likely scenario, while it is possible for a thread
			// to be servicing more than one context concurrently, it's
			// unlikely.  So lets make this one as fast as possible.
			result = contexts[0].getValidators();
		} else {
			// This is an unlikely scenario.  So we can afford to be a 
			// little less efficient.
			Set<ThreadTraceConfig.TriggerValidator> validatorSet = new HashSet<ThreadTraceConfig.TriggerValidator>();
			for (ReactiveContext context : contexts) {
				ThreadTraceConfig.TriggerValidator[] validators = context.getValidators();
				if (validators.length > 0) {
					validatorSet.addAll(Arrays.asList(validators));
				}
			}
			result = validatorSet.toArray(EMPTY_TRIGGER_VALIDATOR_ARRAY);
		}
		
		return result;
	}
	
	private static final class ReactiveContextsVO {
		private final ReactiveContext[] contexts;
		private final String explicitContextID;
		
		public ReactiveContextsVO(ReactiveContext[] contexts) {
			this.contexts = contexts;
			String newExplicitContextID = null;
			for (ReactiveContext context : contexts) {
				String contextID = context.getContextID();
				if (!contextID.startsWith(PerfMonTimer.IMPLICIT_REACTIVE_CONTEXT_PREFIX)) {
					newExplicitContextID = contextID;
					break;
				}
			}
			this.explicitContextID = newExplicitContextID;
		}

		public ReactiveContext[] getContexts() {
			return contexts;
		}

		public String getExplicitContextID() {
			return explicitContextID;
		}
	}
}

package web.org.perfmon4j.extras.quarkus.filter.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.org.perfmon4j.agent.PerfMon;


public class TracingContextProvider implements ThreadContextProvider {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TracingContextProvider.class);

    public static final ThreadLocal<RequestContext> REQUEST_CONTEXT = new ThreadLocal<RequestContext>() {
		@Override
		protected RequestContext initialValue() {
			return new RequestContext();
		}
    };	
	
    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
    	// Get the current requestContext off the calling thread.
    	/**
    	 * Step 1
    	 * 
    	 * This first section is invoked by the thread that 
    	 * is in that will be handling control of the request (i.e. WorkerThread1).
    	 * It prepares an object that is handed to another thread 
    	 * (i.e. WorkerThread2) that will be taking over processing 
    	 * the request.
    	 * 
    	 * We retrieve the current request context from WorkerThread1
    	 */
    	final String propagateContext = REQUEST_CONTEXT.get().getContext();
    	return () -> {
    		/**
    		 * Step 2
    		 * 
    		 * This section is invoked by WorkerThread2 once responsibility
    		 * for processing the request has been handed over from WorkerThread1.
    		 * 
    		 * It  assigns the request context (propagateContext) that was stored
    		 * in Step 1, from WorkerThread1. 
    		 */
    		final String restoreContext = REQUEST_CONTEXT.get().getContext();
    		REQUEST_CONTEXT.get().setContext(propagateContext);
    		return () -> {
    			/**
    			 * Step 3 
    			 * 
    			 * Perfmon4j does not really need this step.
    			 * It is called after thread2 has completed its
    			 * task and transfered it to another thread.
    			 * 
    			 * We just clear its context.
    			 */
    			REQUEST_CONTEXT.get().setContext(restoreContext);
    		};
    	};
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
    	return () -> {
    		final String restoreContext = REQUEST_CONTEXT.get().getContext();
    		REQUEST_CONTEXT.get().clearContext();
    		return () -> {
    			REQUEST_CONTEXT.get().setContext(restoreContext);
    		};
    	};
    }

    @Override
    public String getThreadContextType() {
        return "org.perfmon4j.Perfmon4j";
    }
    
    public static class RequestContext {
    	private static final AtomicLong nextContextID = new AtomicLong();
    	private String context = null;
    	
    	public String initContext() {
    		context = "ctx_" + nextContextID.incrementAndGet();
    		return context;
    	}
    	
    	public String clearContext() {
    		String previousContext = context;
    		context = null;
    		
    		if (previousContext != null) {
    			// Instruct Perfmon4j to dissociate thread 
    			// from the previous context.
    			PerfMon.dissociateReactiveContextFromCurrentThread(previousContext);	
    		} 
    		
    		return context;
    	}

		public String getContext() {
    		PerfMon.moveReactiveContextToCurrentThread(context);
    		return context;
		}

		public void setContext(String context) {
			this.context = context;
    		PerfMon.moveReactiveContextToCurrentThread(context);
		}
    }
}
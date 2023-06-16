package web.org.perfmon4j.extras.quarkus.filter.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracingContextProvider implements ThreadContextProvider {
	public static final String REQUEST_CONTEXT_NOT_SET = "NotSet"; 
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
    	String propagateContext = REQUEST_CONTEXT.get().getContext();
    	return () -> {
    		String restoreContext = REQUEST_CONTEXT.get().getContext();
    		// Propagate the request context to the new thread
    		REQUEST_CONTEXT.get().setContext(propagateContext);
    		logger.debug("Thread assigned to requestContext: " + propagateContext);
    		return () -> {
    			REQUEST_CONTEXT.get().setContext(restoreContext);
    		};
    	};
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
    	return () -> {
    		String restoreContext = REQUEST_CONTEXT.get().getContext();
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
    	private String context = REQUEST_CONTEXT_NOT_SET;
    	
    	public String initContext() {
    		context = "ctx_" + nextContextID.incrementAndGet();
    		return context;
    	}
    	
    	public void clearContext() {
    		context = REQUEST_CONTEXT_NOT_SET;
    	}

		public String getContext() {
			return context;
		}

		public void setContext(String context) {
			this.context = context;
		}
    }
}
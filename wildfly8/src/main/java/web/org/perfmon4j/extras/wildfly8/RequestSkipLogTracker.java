package web.org.perfmon4j.extras.wildfly8;

public class RequestSkipLogTracker {
   private static final ThreadLocal<RequestSkipLogTracker> tracker = new ThreadLocal<RequestSkipLogTracker>() {
        public RequestSkipLogTracker initialValue() {
            return new RequestSkipLogTracker();
        }
    }; 
    
    public static RequestSkipLogTracker getTracker() {
    	return tracker.get();
    }

    private boolean skipLogOutput = false; 
    
    
	public void removeAttribute(String name) {
		if (HandlerImpl.PERFMON4J_SKIP_LOG_FOR_REQUEST.equals(name)) {
			skipLogOutput = false;
		}
	}
	
	public void setAttribute(String name, Object value) {
		if (HandlerImpl.PERFMON4J_SKIP_LOG_FOR_REQUEST.equals(name)) {
			skipLogOutput = true;
		}
	}

	public boolean isSkipLogOutput() {
		return skipLogOutput;
	}

	public void setSkipLogOutput(boolean skipLogOutput) {
		this.skipLogOutput = skipLogOutput;
	}
}

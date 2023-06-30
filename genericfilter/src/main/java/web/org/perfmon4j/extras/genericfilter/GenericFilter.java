package web.org.perfmon4j.extras.genericfilter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import api.org.perfmon4j.agent.PerfMon;
import api.org.perfmon4j.agent.PerfMonTimer;
import api.org.perfmon4j.agent.SQLTime;
import api.org.perfmon4j.agent.ThreadTraceConfig;


/**
 * This GenericFilter can be used as a TWO ways:
 * 
 * 1) As a traditional Filter that wraps around an HttpRequestProcessor thread
 * 		Usage:
 * 			init() {
 * 				GenericFilter filter = new GenericFilter();
 * 			}
 * 
 * 			handleRequest() {
 * 				...
 * 				filter.handleRequest(request, response, requestChain);
 * 				...
 * 			}
 * 
 * 2) In a callback method, suitible for Vert.x or other reactive web contatiner
 * 		Usage:

 * 			init() {
 * 				GenericFilter filter = new GenericFilter();
 * 			}
 * 
 * 			handleRequest() {
 * 				...
 * 				AsyncRequestContext context = ...
 * 
 * 				AsyncFinishRequestCallback callback = filter.startAsyncRequest(request, response);
 * 				if (callback != null) {
 * 					context.addBodyEndHandler(callback::finishRequest);
 * 				}
 * 				...
 * 			}
 * 
 * 
 * @author ddeucher
 *
 */
public abstract class GenericFilter {
	public static final String PERFMON4J_SKIP_LOG_FOR_REQUEST = "PERFMON4J_SKIP_LOG_FOR_REQUEST";
//	private static final Logger logger = LoggerFactory.initLogger(GenericFilter.class);
//	private final HttpRequestChain next;
	private final String baseFilterCategory;
	private final boolean outputRequestAndDuration;
	private final boolean abortTimerOnImageResponse;
	private final boolean abortTimerOnRedirect;
	private final Pattern abortTimerOnURLPattern;
	private final Pattern skipTimerOnURLPattern;
	/*	
	 TODO: HandleNDC
	private final boolean pushNDC;
    private final boolean pushURLOnNDC;
    private final String[] pushCookiesOnNDC;
    private final String[] pushSessionAttributesOnNDC;
    private final boolean pushClientInfoOnNDC;
    */
    
    private final ServletPathTransformer servletPathTransformer;
    
	private ThreadLocal<AtomicInteger> recurseCount = new ThreadLocal<AtomicInteger>() {
		@Override
		protected AtomicInteger initialValue() {
			return new AtomicInteger(0);
		}
	};
    
    final static private boolean SKIP_HTTP_METHOD_ON_LOG_OUTPUT = Boolean.getBoolean("web.org.perfmon4j.servlet"
    	+ ".PerfMonFilter.SKIP_HTTP_METHOD_ON_LOG_OUTPUT");     
	
	
	private static final String CONTENT_TYPE = "Content-Type";
    private static final Pattern passwordParamPattern = Pattern.compile("[\\?|\\&]{1}password\\=([^\\&\\;]*)");

	protected GenericFilter(FilterParams params) {
		this.baseFilterCategory = params.getBaseFilterCategory();
		this.outputRequestAndDuration = params.isOutputRequestAndDuration();
		this.abortTimerOnImageResponse = params.isAbortTimerOnImageResponse();
		this.abortTimerOnRedirect = params.isAbortTimerOnRedirect();
		this.abortTimerOnURLPattern = compilePattern(params.getAbortTimerOnURLPattern());
		this.skipTimerOnURLPattern = compilePattern(params.getSkipTimerOnURLPattern());
/*	
 TODO: HandleNDC
  	
		this.pushURLOnNDC = params.isPushURLOnNDC();
		this.pushClientInfoOnNDC = params.isPushClientInfoOnNDC();
		this.pushCookiesOnNDC = tokenizeCSVString(params.getPushCookiesOnNDC());
		this.pushSessionAttributesOnNDC = tokenizeCSVString(params.getPushSessionAttributesOnNDC());
		this.pushNDC = pushURLOnNDC || pushClientInfoOnNDC || (pushCookiesOnNDC != null) || (pushSessionAttributesOnNDC != null);
*/		

		String servletPathTransformerStr = params.getServletPathTransformationPattern();
		if (servletPathTransformerStr != null && !servletPathTransformerStr.isBlank()) {
			this.servletPathTransformer = ServletPathTransformer.newTransformer(servletPathTransformerStr, baseFilterCategory);
		} else {
			this.servletPathTransformer = ServletPathTransformer.newTransformer("", baseFilterCategory);
		}
		
		PerfMon.getMonitor(this.baseFilterCategory, false);  // Always create the base category monitor, all child monitors are only created when a monitor is attached.
	}
	
	
	public void handleRequest(HttpRequest request, HttpResponse response, HttpRequestChain chain) throws Exception {

		int recurse = recurseCount.get().incrementAndGet();
		try {
			 // This handler is recursively invoked when the application forwards
			 // while processing a web request.  We only want to monitor the initial
			 // incoming request.
			boolean skip = true;
			if (recurse == 1) {
				skip = ((skipTimerOnURLPattern != null) 
						&& skipTimerOnURLPattern.matcher(request.getServletPath()).matches());
			}
			if (skip) {
				chain.next(request, response, chain);
			} else {
				doHandleRequest(request, response, chain);			
			}
		} finally {
			recurseCount.get().decrementAndGet();
		}
	}

	public class AsyncFinishRequestCallback {
        private final GenericFilter parent;
        private final String reactiveContext;
		private final HttpRequest request;
		private final PerfMonTimer timer;
        private final Long localStartTime;
        private final Long localSQLStartTime;
		private final boolean pushedRequestValidator;
		private final boolean pushedSessionValidator;
		private final boolean pushedCookieValidator;
        
		private AsyncFinishRequestCallback(GenericFilter parent, HttpRequest request) {
			this(parent, request, null);
		}
        
		private AsyncFinishRequestCallback(GenericFilter parent, HttpRequest request, String reactiveContext) {
			this.parent = parent;
			this.reactiveContext = reactiveContext;
			this.request = request;
			timer = startTimerForRequest(request, reactiveContext);
			localStartTime = outputRequestAndDuration ? Long.valueOf(System.currentTimeMillis()) : null;
    		localSQLStartTime = outputRequestAndDuration && SQLTime.isEnabled() ?  Long.valueOf(SQLTime.getSQLTime()) : null;

    		/*			
			if (pushNDC) {
				String ndc = buildNDC(request, pushURLOnNDC, pushClientInfoOnNDC, pushCookiesOnNDC, pushSessionAttributesOnNDC);
				if (ndc.length() > 0) {
					NDC.push(ndc);
					pushedElementOnNDCStack = true;
				}
			}
    				boolean pushedElementOnNDCStack = false;
    		
    		*/
        	if (PerfMon.hasHttpRequestBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new RequestValidator(request), reactiveContext);
        		pushedRequestValidator = true;
        	} else {
        		pushedRequestValidator = false;
        	}
			
        	if (PerfMon.hasHttpSessionBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new SessionValidator(request), reactiveContext);
        		pushedSessionValidator = true;
        	} else {
        		pushedSessionValidator = false;
        	}

        	if (PerfMon.hasHttpCookieBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new CookieValidator(request), reactiveContext);
        		pushedCookieValidator = true;
        	} else {
        		pushedCookieValidator = false;
        	}
		}

		public void finishRequest(HttpResponse response) {
			boolean doAbort = false;
			
			try {
				if (!doAbort && abortTimerOnRedirect) {
					doAbort = (response.getStatus() / 100) == 3;
				}
				
	            if (!doAbort && abortTimerOnImageResponse) {
	                String contentType = response.getHeader(CONTENT_TYPE);
	                doAbort = contentType != null && contentType.startsWith("image");
	            }
	            
	            if (!doAbort && (abortTimerOnURLPattern != null)) {
	            	Matcher m = abortTimerOnURLPattern.matcher(request.getServletPath());
	            	doAbort = m.matches();
	            }
				
	            if (doAbort) {
	            	parent.abortTimer(timer);
	            } else {
	            	parent.stopTimer(timer);
		        	if ((localStartTime != null) && !skipLogOutput() ) {
		        		outputToLog(request, localStartTime, localSQLStartTime);
		        	}
	            }
			} finally {
				if (pushedRequestValidator) {
					ThreadTraceConfig.popValidator(reactiveContext);
				}
				if (pushedSessionValidator) {
					ThreadTraceConfig.popValidator(reactiveContext);
				}
				if (pushedCookieValidator) {
					ThreadTraceConfig.popValidator(reactiveContext);
				}
				/*
				if (pushedElementOnNDCStack) {
					NDC.pop();
				}
				 */					
			}
			
		}
		
	}

	public AsyncFinishRequestCallback startAsyncRequest(HttpRequest request, String reactiveContext) {
		AsyncFinishRequestCallback result = null;

		boolean skip = ((skipTimerOnURLPattern != null) 
				&& skipTimerOnURLPattern.matcher(request.getServletPath()).matches());
		
		if (!skip) {
			result = new AsyncFinishRequestCallback(this, request, reactiveContext);
		}
		
		return result;
	}

	private void doHandleRequest(HttpRequest request, HttpResponse response, HttpRequestChain chain) throws Exception {
		AsyncFinishRequestCallback callback = null;
		try {
			callback = startAsyncRequest(request, null);
			chain.next(request, response, chain);
		} finally {
			if (callback != null) {
				callback.finishRequest(response);
			}
		}
	}
	
	private boolean skipLogOutput() {
		RequestSkipLogTracker tracker = RequestSkipLogTracker.getTracker();
		boolean result = tracker.isSkipLogOutput();
		if (result) {
			tracker.setSkipLogOutput(false);
		}
		return result;
	}
 	
	protected void outputToLog(HttpRequest request, Long localStartTime, Long localSQLStartTime) {
		String sqlDurationStr = "";
		if (localSQLStartTime != null) {
			long sqlDuration = SQLTime.getSQLTime() - localSQLStartTime.longValue();
			sqlDurationStr = "(SQL: " + sqlDuration + ")";
		}
		long duration = Math.max(System.currentTimeMillis() -
			localStartTime.longValue(), 0);
		logInfo(duration + sqlDurationStr + " " + buildRequestDescription(request));
	}
	
    protected PerfMonTimer startTimerForRequest(HttpRequest request, String requestContext) {
        PerfMonTimer result = PerfMonTimer.getNullTimer();
        if (isPerfMonConfigured()) {
        	String requestPath = request.getServletPath();
        	if (requestPath != null) {
        		if (requestPath.endsWith("/")) {
        			requestPath = requestPath.substring(0, requestPath.length()-1);
        		}
        		String monitorCategory = servletPathTransformer.transformToCategory(requestPath);
                result = startTimer(monitorCategory, true, requestContext);
        	}
        }
        return result;
    }	
    
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
    /* package */ boolean isPerfMonConfigured() {
    	return PerfMon.isConfigured() && PerfMon.isAttachedToAgent();
    }
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
    /* package */ PerfMonTimer startTimer(String category, boolean isDynamicTimer, String requestContext) {
    	return PerfMonTimer.start(category, true, requestContext);
    }
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
    /* package */ void abortTimer(PerfMonTimer timer) {
    	PerfMonTimer.abort(timer);;
    }
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
    /* package */ void stopTimer(PerfMonTimer timer) {
    	PerfMonTimer.stop(timer);;
    }

/* 
TODO: Restore PushNDC or MDC    
    static String buildNDC(HttpRequest request, boolean pushURLOnNDC, boolean pushClientInfoOnNDC, String[] pushCookiesOnNDC, String[] pushSessionAttributesOnNDC) {
    	StringBuilder result = new StringBuilder();
    	if (request != null) {
    		if (pushURLOnNDC) {
    			result.append(buildRequestDescription(request));
    		} // pushURLOnNDC
    		
    		if (pushClientInfoOnNDC) {
    			if (result.length() > 0) {
    				result.append(" ");
    			}
    			
    			InetSocketAddress socket = exchange.getSourceAddress();
    			if (socket != null) {
    				String address = socket.getAddress().toString();
    				if (address.startsWith("/")) {
    					address = address.replaceFirst("/", "");
    				}
    				result.append(address);
    			}
    			String xForwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
    			if (xForwarded != null) {
    				result.append("[").append(xForwarded).append("]");
    			}
    		} // pushClientInfoOnNDC
    		
    		if (pushCookiesOnNDC != null && pushCookiesOnNDC.length > 0) {
    			Map<String, Cookie> cookies = exchange.getRequestCookies(); 
    			if (cookies != null) {
	    			for (String name : pushCookiesOnNDC) {
	    				Cookie c = cookies.get(name);
	    				if (c != null) {
	    	    			if (result.length() > 0) {
	    	    				result.append(" ");
	    	    			}
	    	    			result.append(name).append(":").append(c.getValue());
	    				}
	    			}
    			}
    		} // pushCookiesOnNDC

    		if (pushSessionAttributesOnNDC != null && pushSessionAttributesOnNDC.length > 0) {
    			ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
    			if (context != null) {
    				HttpSessionImpl session = context.getSession();
    				if (session != null) {
		    			for (String name : pushSessionAttributesOnNDC) {
		    				Object value = session.getAttribute(name);
		    				if (value != null) {
		    	    			if (result.length() > 0) {
		    	    				result.append(" ");
		    	    			}
		    	    			result.append(name).append(":").append(value);
		    				}
		    			}
    				}
    			}
    		} // pushSessionAttributesOnNDC
    	}
    	return result.toString();
    }
*/    
    
    static String buildRequestDescription(HttpRequest request) {
    	StringBuilder result = new StringBuilder();
    	if (request != null) {
    		if (!SKIP_HTTP_METHOD_ON_LOG_OUTPUT) {
	    		String method = request.getMethod();
	    		if (method != null && !method.isBlank()) {
	    			result.append(method + " ");
	    		}
    		}
    		
    		result.append(request.getServletPath());
    		String queryString = request.getQueryString();
    		if (queryString != null && queryString.length() > 0) {
    			result.append(maskPassword("?" + queryString));
    		}
    	}
    	return result.toString();
    }
    
    static String maskPassword(String queryParams) {
    	Matcher m = passwordParamPattern.matcher(queryParams);
    	StringBuilder buffer = new StringBuilder(queryParams);
    	int offsetAdjust = 0;
    	while (m.find()) {
    		int start = m.start(1) + offsetAdjust;
    		int end = m.end(1) + offsetAdjust;
    		
    		int startLength = buffer.length();
    		buffer.replace(start, end, "*******");
    		
    		offsetAdjust += (buffer.length() - startLength);
    	}
    	
    	return buffer.toString();
    }

/*    
    private void notifyUserAgentMonitor(HttpServerExchange exchange) {
    	HeaderMap map  = exchange.getRequestHeaders();
    	if (map != null) {
    		String userAgentString = map.getFirst("User-agent");
            if (userAgentString != null) {
                UserAgentSnapShotMonitor.insertUserAgent(userAgentString);
            }
    	}
    }
*/    
    private static Pattern compilePattern(String pattern) {
    	Pattern result = null;
    	if (pattern != null) {
	        try {
	        	result = Pattern.compile(pattern);
	        } catch (PatternSyntaxException ex) {
	        
/*
TODO: Log this better.	        	
	            log.error("Error compiling pattern: " + pattern, ex);
*/
	        	ex.printStackTrace();
	        }
    	}
    	return result;
    }

    public static class RequestValidator implements ThreadTraceConfig.RequestTriggerValidator {
    	private final HttpRequest request;
    	
    	RequestValidator(HttpRequest request) {
    		this.request = request;
    	}
		@Override
		public boolean isValid(String parameterName, String parameterValue) {
			List<String> values = request.getQueryParameter(parameterName);
			return values != null && values.contains(parameterValue);
		}
    }
    
    public static class SessionValidator implements ThreadTraceConfig.SessionTriggerValidator {
    	private final HttpRequest request;

    	SessionValidator(HttpRequest request) {
    		this.request = request;
    	}
    	
		@Override
		public boolean isValid(String attributeName, Object attributeValue) {
			Object attribute = request.getSessionAttribute(attributeName);
			return attribute != null && attribute.equals(attributeValue);
		}
    }
    
    public static class CookieValidator implements ThreadTraceConfig.CookieTriggerValidator {
    	private final HttpRequest request;

		public CookieValidator(HttpRequest request) {
			this.request = request;
		}

		@Override
		public boolean isValid(String cookieName, String cookieValue) {
			String value = request.getCookieValue(cookieName);	
			return value != null && value.equals(cookieValue);
		}
    }
    
//	private String[] tokenizeCSVString(String src) {
//		String[] result = null;
//		
//		if (src != null && !src.trim().equals("")) {
//			List<String> x = new ArrayList<String>();
//			StringTokenizer t = new StringTokenizer(src, ",");
//			while (t.hasMoreTokens()) {
//				String str = t.nextToken().trim();
//				if (!str.equals("")) {
//					x.add(str);
//				}
//			}
//			result = x.toArray(new String[]{});
//		}
//		return result;
//	}
	
	abstract protected void logInfo(String value);
	abstract protected void logInfo(String value, Exception ex);
    
}

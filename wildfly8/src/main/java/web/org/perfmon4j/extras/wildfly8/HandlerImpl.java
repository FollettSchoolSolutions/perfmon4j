package web.org.perfmon4j.extras.wildfly8;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

import java.util.Deque;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.SQLTime;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.perfmon4j.UserAgentSnapShotMonitor;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


class HandlerImpl implements HttpHandler {
    private static final Logger logger = LoggerFactory.initLogger(HandlerImpl.class);	
	private final PerfmonHandlerWrapper parent;
	private final HttpHandler next;
	private final String baseFilterCategory;
	private final boolean outputRequestAndDuration;
	private final boolean abortTimerOnImageResponse;
	private final boolean abortTimerOnRedirect;
	private final Pattern abortTimerOnURLPattern;
	private final Pattern skipTimerOnURLPattern;
	private static final HttpString CONTENT_TYPE = new HttpString("Content-Type");

	HandlerImpl(PerfmonHandlerWrapper parent, HttpHandler next) {
		this.parent = parent;
		this.next = next;
		this.baseFilterCategory = parent.getBaseFilterCategory();
		this.outputRequestAndDuration = parent.isOutputRequestAndDuration();
		this.abortTimerOnImageResponse = parent.isAbortTimerOnImageResponse();
		this.abortTimerOnRedirect = parent.isAbortTimerOnRedirect();
		this.abortTimerOnURLPattern = compilePattern(parent.getAbortTimerOnURLPattern());
		this.skipTimerOnURLPattern = compilePattern(parent.getSkipTimerOnURLPattern());
		
		PerfMon.getMonitor(this.baseFilterCategory, false);  // Always create the base category monitor, all child monitors are only created when a monitor is attached.
	}
	
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		boolean skip = false;
		
		if (skipTimerOnURLPattern != null) {
			skip = skipTimerOnURLPattern.matcher(exchange.getRequestPath()).matches();
		}
		
		if (skip) {
			next.handleRequest(exchange);
		} else {
			doHandleRequest(exchange);
		}
	}

	
	private void doHandleRequest(HttpServerExchange exchange) throws Exception {
        Long localStartTime = null;
        Long localSQLStartTime = null;

		PerfMonTimer timer = null;
		boolean pushedRequestValidator = false;
		try {
        	if (PerfMon.hasHttpRequestBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new RequestValidator(exchange));
        		pushedRequestValidator = true;
        	}			
			
			try {
				timer = startTimerForRequest(exchange);
		        if (outputRequestAndDuration) {
		        	localStartTime = new Long(MiscHelper.currentTimeWithMilliResolution());
		        	if (SQLTime.isEnabled()) {
		        		localSQLStartTime = new Long(SQLTime.getSQLTime());
		        	}
		        }
				next.handleRequest(exchange);
			} finally {
				boolean doAbort = false;
				
				if (!doAbort && abortTimerOnRedirect) {
					doAbort = (exchange.getResponseCode() / 100) == 3;
				}
				
	            if (!doAbort && abortTimerOnImageResponse) {
	                String contentType = exchange.getResponseHeaders().get(CONTENT_TYPE, 0);
	                doAbort = contentType != null && contentType.startsWith("image");
	            }
	            
	            if (!doAbort && (abortTimerOnURLPattern != null)) {
	            	Matcher m = abortTimerOnURLPattern.matcher(exchange.getRequestPath());
	            	doAbort = m.matches();
	            }
				
	            if (doAbort) {
	            	PerfMonTimer.abort(timer);
	            } else {
	            	PerfMonTimer.stop(timer);
	            	notifyUserAgentMonitor(exchange);
		        	if (localStartTime != null) {
		        		String sqlDurationStr = "";
		        		if (localSQLStartTime != null) {
		        			long sqlDuration = SQLTime.getSQLTime() - localSQLStartTime.longValue();
		        			sqlDurationStr = "(SQL: " + sqlDuration + ")";
		        		}
		        		long duration = Math.max(MiscHelper.currentTimeWithMilliResolution() -
		        			localStartTime.longValue(), 0);
		        		logger.logInfo(duration + sqlDurationStr + " " + buildRequestDescription(exchange));
		        	}
	            }
			}
		} finally {
			if (pushedRequestValidator) {
				ThreadTraceConfig.popValidator();
			}
		}
	}
	
    protected PerfMonTimer startTimerForRequest(HttpServerExchange exchange ) {
        PerfMonTimer result = PerfMonTimer.getNullTimer();
        if (PerfMon.isConfigured()) {
        	String monitorCategory = baseFilterCategory;
        	
        	String requestPath = exchange.getRequestPath();
        	if (requestPath != null) {
        		if (requestPath.endsWith("/")) {
        			requestPath = requestPath.substring(0, requestPath.length()-1);
        		}
        		monitorCategory += requestPath.replaceAll("\\.", "_").replaceAll("/", "\\.");
        	}
            result = PerfMonTimer.start(monitorCategory, true);
        }
        return result;
    }	
    
    static String buildRequestDescription(HttpServerExchange exchange) {
    	StringBuilder result = new StringBuilder();
    	if (exchange != null) {
    		result.append(exchange.getRequestPath());
    		String queryString = exchange.getQueryString();
    		if (queryString != null && queryString.length() > 0) {
    			result.append(maskPassword("?" + queryString));
    		}
    	}
    	return result.toString();
    }
    
    private static final Pattern passwordParamPattern = Pattern.compile("[\\?|\\&]{1}password\\=([^\\&\\;]*)");
    
    
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
    
    private void notifyUserAgentMonitor(HttpServerExchange exchange) {
    	HeaderMap map  = exchange.getRequestHeaders();
    	if (map != null) {
    		String userAgentString = map.getFirst("User-agent");
            if (userAgentString != null) {
                UserAgentSnapShotMonitor.insertUserAgent(userAgentString);
            }
    	}
    }
    
    private static Pattern compilePattern(String pattern) {
    	ThreadTraceConfig x;
    	Pattern result = null;
    	if (pattern != null) {
	        try {
	        	result = Pattern.compile(pattern);
	        } catch (PatternSyntaxException ex) {
	            logger.logError("Error compiling pattern: " + pattern, ex);
	        }
    	}
    	return result;
    }

    public static class RequestValidator implements ThreadTraceConfig.TriggerValidator {
    	private final HttpServerExchange exchange;
    	RequestValidator(HttpServerExchange exchange) {
    		this.exchange = exchange;
    	}
    	
		public boolean isValid(Trigger trigger) {
			boolean result = false;
			
			if (trigger.getType() == ThreadTraceConfig.TriggerType.HTTP_REQUEST_PARAM) {
				ThreadTraceConfig.HTTPRequestTrigger t = (ThreadTraceConfig.HTTPRequestTrigger)trigger;
				
				Map<String, Deque<String>> map = exchange.getQueryParameters();
				if (map != null) {
					Deque<String> values = map.get(t.getName());
					result = values != null && values.contains(t.getValue());
				}
			}
			return result;
		}
    }


}


package web.org.perfmon4j.extras.wildfly8;

import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jboss.logging.NDC;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.SQLTime;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.perfmon4j.UserAgentSnapShotMonitor;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

class HandlerImpl implements HttpHandler {
    private static final Logger logger = LoggerFactory.initLogger(HandlerImpl.class);	
	private final HttpHandler next;
	private final String baseFilterCategory;
	private final boolean outputRequestAndDuration;
	private final boolean abortTimerOnImageResponse;
	private final boolean abortTimerOnRedirect;
	private final Pattern abortTimerOnURLPattern;
	private final Pattern skipTimerOnURLPattern;
	private final boolean pushNDC;
    private final boolean pushURLOnNDC;
    private final String[] pushCookiesOnNDC;
    private final String[] pushSessionAttributesOnNDC;
    private final boolean pushClientInfoOnNDC;    
    
    final static private boolean SKIP_HTTP_METHOD_ON_LOG_OUTPUT = Boolean.getBoolean("web.org.perfmon4j.servlet"
    	+ ".PerfMonFilter.SKIP_HTTP_METHOD_ON_LOG_OUTPUT");     
	
	
	private static final HttpString CONTENT_TYPE = new HttpString("Content-Type");
    private static final Pattern passwordParamPattern = Pattern.compile("[\\?|\\&]{1}password\\=([^\\&\\;]*)");

	HandlerImpl(PerfmonHandlerWrapper parent, HttpHandler next) {
		this.next = next;
		this.baseFilterCategory = parent.getBaseFilterCategory();
		this.outputRequestAndDuration = parent.isOutputRequestAndDuration();
		this.abortTimerOnImageResponse = parent.isAbortTimerOnImageResponse();
		this.abortTimerOnRedirect = parent.isAbortTimerOnRedirect();
		this.abortTimerOnURLPattern = compilePattern(parent.getAbortTimerOnURLPattern());
		this.skipTimerOnURLPattern = compilePattern(parent.getSkipTimerOnURLPattern());
		this.pushURLOnNDC = parent.isPushURLOnNDC();
		this.pushClientInfoOnNDC = parent.isPushClientInfoOnNDC();
		this.pushCookiesOnNDC = MiscHelper.tokenizeCSVString(parent.getPushCookiesOnNDC());
		this.pushSessionAttributesOnNDC = MiscHelper.tokenizeCSVString(parent.getPushSessionAttributesOnNDC());
		this.pushNDC = pushURLOnNDC || pushClientInfoOnNDC || (pushCookiesOnNDC != null) || (pushSessionAttributesOnNDC != null);
		
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
		boolean pushedSessionValidator = false;
		boolean pushedCookieValidator = false;
		boolean pushedElementOnNDCStack = false;
		try {
			if (pushNDC) {
				String ndc = buildNDC(exchange, pushURLOnNDC, pushClientInfoOnNDC, pushCookiesOnNDC, pushSessionAttributesOnNDC);
				if (ndc.length() > 0) {
					NDC.push(ndc);
					pushedElementOnNDCStack = true;
				}
			}
			
        	if (PerfMon.hasHttpRequestBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new RequestValidator(exchange));
        		pushedRequestValidator = true;
        	}			
			
        	if (PerfMon.hasHttpSessionBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new SessionValidator(exchange));
        		pushedSessionValidator = true;
        	}			

        	if (PerfMon.hasHttpCookieBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new CookieValidator(exchange));
        		pushedCookieValidator = true;
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
			if (pushedSessionValidator) {
				ThreadTraceConfig.popValidator();
			}
			if (pushedCookieValidator) {
				ThreadTraceConfig.popValidator();
			}
			
			if (pushedElementOnNDCStack) {
				NDC.pop();
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
    
    static String buildNDC(HttpServerExchange exchange, boolean pushURLOnNDC, boolean pushClientInfoOnNDC, String[] pushCookiesOnNDC, String[] pushSessionAttributesOnNDC) {
    	StringBuilder result = new StringBuilder();
    	if (exchange != null) {
    		if (pushURLOnNDC) {
    			result.append(buildRequestDescription(exchange));
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
    
    static String buildRequestDescription(HttpServerExchange exchange) {
    	StringBuilder result = new StringBuilder();
    	if (exchange != null) {
    		if (!SKIP_HTTP_METHOD_ON_LOG_OUTPUT) {
	    		HttpString method = exchange.getRequestMethod();
	    		if (method != null) {
	    			result.append(method + " ");
	    		}
    		}
    		
    		result.append(exchange.getRequestPath());
    		String queryString = exchange.getQueryString();
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

    public static class SessionValidator implements ThreadTraceConfig.TriggerValidator {
    	private final HttpServerExchange exchange;
    	
    	SessionValidator(HttpServerExchange exchange) {
    		this.exchange = exchange;
    	}
    	
		public boolean isValid(Trigger trigger) {
			boolean result = false;
			
			if (trigger.getType() == ThreadTraceConfig.TriggerType.HTTP_SESSION_PARAM) {
				ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				if (context != null) {
					HttpSessionImpl session = context.getSession();
					if (session != null) {
						ThreadTraceConfig.HTTPSessionTrigger t = (ThreadTraceConfig.HTTPSessionTrigger)trigger;
						Object value = session.getAttribute(t.getName());
						if (value != null) {
							result = t.getValue().equals(value.toString());
						}
					}
				}
			}
			return result;
		}
    }    
    
    public static class CookieValidator implements ThreadTraceConfig.TriggerValidator {
    	private final HttpServerExchange exchange;
    	
    	CookieValidator(HttpServerExchange exchange) {
    		this.exchange = exchange;
    	}
    	
		public boolean isValid(Trigger trigger) {
			boolean result = false;
			
			if (trigger.getType() == ThreadTraceConfig.TriggerType.HTTP_COOKIE_PARAM) {
				ThreadTraceConfig.HTTPCookieTrigger t = (ThreadTraceConfig.HTTPCookieTrigger)trigger;
				Map<String, Cookie> cookies = exchange.getRequestCookies();
				if (cookies != null) {
					Cookie cookie = cookies.get(t.getName());
					result = (cookie != null && t.getValue().equals(cookie.getValue()));
				}
			}
			return result;
		}  
    }
}

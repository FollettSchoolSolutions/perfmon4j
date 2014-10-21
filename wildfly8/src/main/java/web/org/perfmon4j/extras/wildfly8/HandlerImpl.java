package web.org.perfmon4j.extras.wildfly8;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.SQLTime;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


class HandlerImpl implements HttpHandler {
    private static final Logger logger = LoggerFactory.initLogger(HandlerImpl.class);	
	private final PerfmonHandlerWrapper parent;
	private final HttpHandler next;
	private final String baseFilterCategory;
	private final boolean outputRequestAndDuration;

	HandlerImpl(PerfmonHandlerWrapper parent, HttpHandler next) {
		this.parent = parent;
		this.next = next;
		this.baseFilterCategory = parent.getBaseFilterCategory();
		this.outputRequestAndDuration = parent.isOutputRequestAndDuration();
	}

	public void handleRequest(HttpServerExchange exchange) throws Exception {
        Long localStartTime = null;
        Long localSQLStartTime = null;
		
		
		// TODO Auto-generated method stub
System.out.println("RequestPath: " + exchange.getRequestPath());			
System.out.println("QueryString: " + exchange.getQueryString());			

		PerfMonTimer timer = null;
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
			PerfMonTimer.stop(timer);
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
	
    protected PerfMonTimer startTimerForRequest(HttpServerExchange exchange ) {
        PerfMonTimer result = PerfMonTimer.getNullTimer();
        if (PerfMon.isConfigured()) {
        	String monitorCategory = baseFilterCategory;
        	
        	String requestPath = exchange.getRequestPath();
        	if (requestPath != null) {
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
    
    private static final Pattern passwordParamPattern = Pattern.compile("[\\?|\\&]{1}password\\=([^\\&]*)");
    
    
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
    
   
    private static String encodeNoThrow(String value) {
    	String result = value;
    	try {
			result = URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			if (logger.isDebugEnabled()) {
				logger.logDebug("Error: unable to encode string: \"" + value + "\"", e);
			}
		}
    	return result;
    }
}


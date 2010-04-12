/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.servlet;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.UserAgentSnapShotMonitor;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class PerfMonFilter implements Filter {
    private static final Logger logger = LoggerFactory.initLogger(PerfMonFilter.class);
    
    final static public String BASE_FILTER_CATEGORY = "WebRequest";
    final static public String PROPERTY_OUTPUT_REQUEST_AND_DURATION = "OUTPUT_REQUEST_AND_DURATION";
    final static public String PROPERTY_BASE_FILTER_CATEGORY_CONFIG_INIT_PARAM = "BASE_FILTER_CATEGORY";
    final static public String PROPERTY_ABORT_TIMER_ON_REDIRECT = "ABORT_TIMER_ON_REDIRECT";
    final static public String PROPERTY_ABORT_TIMER_ON_IMAGE_RESPONSE = "ABORT_TIMER_ON_IMAGE_RESPONSE";
    final static public String PROPERTY_ABORT_TIMER_ON_URL_PATTERN = "ABORT_TIMER_ON_URL_PATTERN";
    
    // Default pattern /images/.*|.*\\.(css|gif|jpg|jpeg|tiff|wav|au)
    
    protected String baseFilterCategory = BASE_FILTER_CATEGORY;
    protected boolean abortTimerOnRedirect = false;
    protected boolean abortTimerOnImageResponse = false;
    protected Pattern abortTimerOnURLPattern = null;
    protected boolean outputRequestAndDuration = false;
 
    protected static String getInitParameter(FilterConfig filterConfig, String key, 
        String defaultValue) {
        String result = filterConfig.getInitParameter(key);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }
    
/*----------------------------------------------------------------------------*/    
    public void init(FilterConfig filterConfig) throws ServletException {
        baseFilterCategory = getInitParameter(filterConfig, PROPERTY_BASE_FILTER_CATEGORY_CONFIG_INIT_PARAM, baseFilterCategory);
        abortTimerOnRedirect = Boolean.parseBoolean(getInitParameter(filterConfig, PROPERTY_ABORT_TIMER_ON_REDIRECT, Boolean.FALSE.toString()));
        abortTimerOnImageResponse = Boolean.parseBoolean(getInitParameter(filterConfig, PROPERTY_ABORT_TIMER_ON_IMAGE_RESPONSE, Boolean.FALSE.toString()));
        outputRequestAndDuration = Boolean.parseBoolean(getInitParameter(filterConfig, PROPERTY_OUTPUT_REQUEST_AND_DURATION, Boolean.toString(outputRequestAndDuration)));
        
        if (outputRequestAndDuration) {
        	logger.enableInfo();
        }
         
        String pattern = getInitParameter(filterConfig, PROPERTY_ABORT_TIMER_ON_URL_PATTERN, null);
        if (pattern != null) {
            try {
                abortTimerOnURLPattern = Pattern.compile(pattern, 
                    Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                logger.logError("Error compiling pattern: " + pattern, ex);
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	if (request instanceof HttpServletRequest) {
    		doFilterHttpRequest((HttpServletRequest)request, (HttpServletResponse)response, chain);
    	} else {
    		chain.doFilter(request, response);
    	}
    }
    
    
/*----------------------------------------------------------------------------*/    
    protected void doFilterHttpRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {    
        Long localStartTime = null;
    	if (abortTimerOnRedirect) {
            response = new ResponseWrapper(response);
        }
        
        PerfMonTimer timer = startTimerForRequest(request);
        if (outputRequestAndDuration) {
        	localStartTime = new Long(MiscHelper.currentTimeWithMilliResolution());
        }
        boolean pushedRequestValidator = false;
        boolean pushedSessionValidator = false;
        try {
        	if (PerfMon.hasHttpRequestBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new HttpRequestValidator(request));
        		pushedRequestValidator = true;
        	}
        	if (PerfMon.hasHttpSessionBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new HttpSessionValidator(request));
        		pushedSessionValidator = true;
        	}
        	
            chain.doFilter(request, response);
        } finally {
        	if (pushedRequestValidator) {
        		ThreadTraceConfig.popValidator();
        	}
        	if (pushedSessionValidator) {
        		ThreadTraceConfig.popValidator();
        	}
            boolean doAbort = false;
            
            if (abortTimerOnRedirect && ResponseWrapper.isRedirect(response)) {
                doAbort = true;
            }
            
            if (!doAbort && abortTimerOnImageResponse) {
                String contentType = response.getContentType();
                doAbort = contentType != null && contentType.startsWith("image");
            }
            
            if (!doAbort && (abortTimerOnURLPattern != null)) {
                String path = request.getServletPath();
                Matcher matcher = abortTimerOnURLPattern.matcher(path);
                doAbort = matcher.matches();
            }
            
            if (doAbort) {
            	abortTimer(timer, request, response);
            } else {
            	stopTimer(timer, request, response);
            	if (localStartTime != null) {
            		long duration = Math.min(MiscHelper.currentTimeWithMilliResolution() -
            			localStartTime.longValue(), 0);
            		logger.logInfo(duration + " " + buildRequestDescription(request));
            	}
            }
        }
    }

/*----------------------------------------------------------------------------*/    
    public void destroy() {
    }
    
    
    protected String buildRequestDescription(HttpServletRequest request) {
    	String result = "";
    	if (request != null) {
			final String contextPath = request.getContextPath();
			if (contextPath != null) {
				result = contextPath;
			}
			result += request.getServletPath();
			
			final String queryString = request.getQueryString();
			if (queryString != null) {
				result += "?" + queryString; 
			}
    	}
    	return result;
    }
    
/*----------------------------------------------------------------------------*/    
    /**
     * buildMonitorCategory can be overriden by derived classes
     * to change the default behavior of mapping a a servlet request
     * to a category
     */
    protected String buildMonitorCategory(HttpServletRequest h) {
        String result = baseFilterCategory;

        String contextPath = h.getContextPath();
        if (contextPath != null && !"".equals(contextPath)) {
        	result += contextPath.replaceAll("\\.", "_").replaceAll("/", "\\.");
        }
        
        String servletPath = h.getServletPath();
        if (servletPath != null) {
            result += servletPath.replaceAll("\\.", "_").replaceAll("/", "\\.");
        } 
        return result;
    }

/*----------------------------------------------------------------------------*/    
    protected PerfMonTimer startTimerForRequest(HttpServletRequest request) {
        PerfMonTimer result = PerfMonTimer.getNullTimer();
        if (PerfMon.isConfigured()) {
            String monitorCategory = buildMonitorCategory(request);
            if (monitorCategory != null) {
                result = PerfMonTimer.start(monitorCategory);
            }
        }
        return result;
    }
    
/*----------------------------------------------------------------------------*/    
    protected void stopTimer(PerfMonTimer timer, ServletRequest request, ServletResponse response) {
    	PerfMonTimer.stop(timer);
        notifyUserAgentMonitor(request);
    }
    
/*----------------------------------------------------------------------------*/    
    protected void abortTimer(PerfMonTimer timer, ServletRequest request, ServletResponse response) {
    	PerfMonTimer.abort(timer);
    }

/*----------------------------------------------------------------------------*/    
    public String getBaseFilterCategory() {
        return baseFilterCategory;
    }
    
    private void notifyUserAgentMonitor(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest r = (HttpServletRequest)request;
            String userAgentString = r.getHeader("User-agent");
            if (userAgentString != null) {
                UserAgentSnapShotMonitor.insertUserAgent(userAgentString);
            }
        }
    }
    
    private static class ResponseWrapper extends HttpServletResponseWrapper {
        private boolean redirect = false;
        
        private static boolean isRedirect(ServletResponse response) {
            return (response instanceof ResponseWrapper) && 
                ((ResponseWrapper)response).redirect;
        }
        
        public ResponseWrapper(HttpServletResponse servletResponse) {
            super(servletResponse);
        }
        
        public void sendRedirect(String url) throws IOException {
            redirect = true;
            super.sendRedirect(url);
        }
    }
    
    public static class HttpRequestValidator implements ThreadTraceConfig.TriggerValidator {
    	private final HttpServletRequest request;
    	
    	HttpRequestValidator(HttpServletRequest request) {
    		this.request = request;
    	}
    	
		public boolean isValid(Trigger trigger) {
			boolean result = false;
			
			if (trigger.getType() == ThreadTraceConfig.TriggerType.HTTP_REQUEST_PARAM) {
				ThreadTraceConfig.HTTPRequestTrigger t = (ThreadTraceConfig.HTTPRequestTrigger)trigger;
				
				String values[] = request.getParameterValues(t.getName());
				if (values != null) {
					for (int i = 0; (i < values.length) && !result; i++) {
						result = t.getValue().equals(values[i]);
					}
				}
			}
			return result;
		}
    }

    public static class HttpSessionValidator implements ThreadTraceConfig.TriggerValidator {
    	private final HttpServletRequest request;
    	
    	HttpSessionValidator(HttpServletRequest request) {
    		this.request = request;
    	}
    	
		public boolean isValid(Trigger trigger) {
			boolean result = false;
			
			if (trigger.getType() == ThreadTraceConfig.TriggerType.HTTP_SESSION_PARAM) {
				HttpSession session = request.getSession(false);
				if (session != null) {
					ThreadTraceConfig.HTTPSessionTrigger t = (ThreadTraceConfig.HTTPSessionTrigger)trigger;
					Object value = session.getAttribute(t.getName());
					if (value != null) {
						result = t.getValue().equals(value.toString());
					}
				}
			}
			return result;
		}
    }
}

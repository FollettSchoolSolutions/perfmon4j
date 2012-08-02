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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.SQLTime;
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
    final static public String PROPERTY_SKIP_TIMER_ON_URL_PATTERN = "SKIP_TIMER_ON_URL_PATTERN";
    
    // Default pattern /images/.*|.*\\.(css|gif|jpg|jpeg|tiff|wav|au)
    
    protected String baseFilterCategory = BASE_FILTER_CATEGORY;
    protected boolean abortTimerOnRedirect = false;
    protected boolean abortTimerOnImageResponse = false;
    protected Pattern abortTimerOnURLPattern = null;
    protected Pattern skipTimerOnURLPattern = null;
    protected boolean outputRequestAndDuration = false;

    // Indicates if this filter is installed via a specific context, via web.xml OR
    // across contexts via a Tomcat Valve.
    private final boolean childOfPerfMonValve;
    
    public PerfMonFilter() {
    	this(false);
    }
    
    public PerfMonFilter(boolean childOfPerfmonValve) {
    	this.childOfPerfMonValve = childOfPerfmonValve; 
    }
 
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
        
        
        // Since all WEBREQEST children are by default dynamically created, create the base category by default.
        PerfMon.getMonitor(baseFilterCategory, false);
        
        String pattern= getInitParameter(filterConfig, PROPERTY_ABORT_TIMER_ON_URL_PATTERN, null);
        if (pattern != null) {
            try {
                abortTimerOnURLPattern = Pattern.compile(pattern, 
                    Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                logger.logError("Error compiling pattern: " + pattern, ex);
            }
        }
        
        
        pattern = getInitParameter(filterConfig, PROPERTY_SKIP_TIMER_ON_URL_PATTERN, null);
        if (pattern != null) {
            try {
                skipTimerOnURLPattern = Pattern.compile(pattern, 
                    Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException ex) {
                logger.logError("Error compiling pattern: " + pattern, ex);
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    	boolean handled = false;
    	if (request instanceof HttpServletRequest) {
    		if (skipTimerOnURLPattern == null 
    				|| !matchesURLPattern((HttpServletRequest)request, skipTimerOnURLPattern)) {
        		doFilterHttpRequest((HttpServletRequest)request, (HttpServletResponse)response, chain);
        		handled = true;
    		}
    	}
    	if (!handled) {
    		chain.doFilter(request, response);
    	}
    }
    
    
/*----------------------------------------------------------------------------*/    
    protected void doFilterHttpRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {    
        Long localStartTime = null;
        Long localSQLStartTime = null;
    	if (abortTimerOnRedirect) {
            response = new ResponseWrapper(response);
        }

        boolean pushedRequestValidator = false;
        boolean pushedSessionValidator = false;
        boolean pushedCookieValidator = false;
        
    	try {
        	if (PerfMon.hasHttpRequestBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new HttpRequestValidator(request));
        		pushedRequestValidator = true;
        	}
        	if (PerfMon.hasHttpSessionBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new HttpSessionValidator(request));
        		pushedSessionValidator = true;
        	}
        	
        	if (PerfMon.hasHttpCookieBasedThreadTraceTriggers()) {
        		ThreadTraceConfig.pushValidator(new HttpCookieValidator(request));
        		pushedCookieValidator = true;
        	}
        	
	        PerfMonTimer timer = startTimerForRequest(request);
	        if (outputRequestAndDuration) {
	        	localStartTime = new Long(MiscHelper.currentTimeWithMilliResolution());
	        	if (SQLTime.isEnabled()) {
	        		localSQLStartTime = new Long(SQLTime.getSQLTime());
	        	}
	        }
	        try {
	            chain.doFilter(request, response);
	        } finally {
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
	            		String sqlDurationStr = "";
	            		if (localSQLStartTime != null) {
	            			long sqlDuration = SQLTime.getSQLTime() - localSQLStartTime.longValue();
	            			sqlDurationStr = "(SQL: " + sqlDuration + ")";
	            		}
	            		long duration = Math.max(MiscHelper.currentTimeWithMilliResolution() -
	            			localStartTime.longValue(), 0);
	            		logger.logInfo(duration + sqlDurationStr + " " + buildRequestDescription(request));
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
        }
    }

/*----------------------------------------------------------------------------*/    
    public void destroy() {
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
    
    
    static String buildRequestDescription(HttpServletRequest request) {
    	StringBuilder result = new StringBuilder();
    	if (request != null) {
			final String contextPath = request.getContextPath();
			if (contextPath != null) {
				result.append(contextPath);
			}
			result.append(request.getServletPath());

			String pathInfo = request.getPathInfo();
			if (pathInfo != null) {
				result.append(pathInfo);
			}
			
			Enumeration<String> names = request.getParameterNames();
			boolean firstParam = true;
			while (names != null && names.hasMoreElements()) {
				String paramName = names.nextElement();
				final boolean isPassword = paramName.contains("password");
				String params[] = request.getParameterValues(paramName);
				for (int i = 0; i < params.length; i++) {
					if (firstParam) {
						result.append("?");
						firstParam = false;
					} else {
						result.append("&");
					}
					String value = isPassword ? "*******" : encodeNoThrow(params[i]);
					result.append(encodeNoThrow(paramName))
						.append("=")
						.append(value);
				}
			}
    	}
    	return result.toString();
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
        
        String pathInfo = h.getPathInfo();
        if (pathInfo != null) {
            result += pathInfo.replaceAll("\\.", "_").replaceAll("/", "\\.");
        }
      
        return result;
    }

/*----------------------------------------------------------------------------*/    
    protected PerfMonTimer startTimerForRequest(HttpServletRequest request) {
        PerfMonTimer result = PerfMonTimer.getNullTimer();
        if (PerfMon.isConfigured()) {
            String monitorCategory = buildMonitorCategory(request);
            if (monitorCategory != null) {
                result = PerfMonTimer.start(monitorCategory, true);
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


    public static class HttpCookieValidator implements ThreadTraceConfig.TriggerValidator {
    	private final HttpServletRequest request;
    	
    	HttpCookieValidator(HttpServletRequest request) {
    		this.request = request;
    	}
    	
		public boolean isValid(Trigger trigger) {
			boolean result = false;
			
			if (trigger.getType() == ThreadTraceConfig.TriggerType.HTTP_COOKIE_PARAM) {
				Cookie cookies[] = request.getCookies();
				ThreadTraceConfig.HTTPCookieTrigger t = (ThreadTraceConfig.HTTPCookieTrigger)trigger;
				for (int i = 0; (cookies != null) && (i < cookies.length) && !result; i++) {
					Cookie c = cookies[i];
					result = t.getName().equalsIgnoreCase(c.getName()) &&
						t.getValue().equalsIgnoreCase(c.getValue());
				}
			}
			return result;
		}  
    }
    
    Pattern getAbortTimerOnURLPattern() {
		return abortTimerOnURLPattern;
	}

	Pattern getSkipTimerOnURLPattern() {
		return skipTimerOnURLPattern;
	}

	// Package level for testing....
    boolean matchesURLPattern(HttpServletRequest request, Pattern pattern) {
		String path = request.getServletPath();
		
		String pathInfo = request.getPathInfo();
		if (pathInfo != null) {
			path+= pathInfo;
		}
		if (childOfPerfMonValve) {
			String contextPath = request.getContextPath();
			if (contextPath != null) {
				path = contextPath + path;
			}
		}
        Matcher matcher = pattern.matcher(path);
        return matcher.matches();
	}
    
}

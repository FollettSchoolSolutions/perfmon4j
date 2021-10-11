/*
 *	Copyright 2008,2009 Follett Software Company 
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

package org.perfmon4j.extras.tomcat7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.servlet.PerfMonFilter;
import web.org.perfmon4j.servlet.PerfMonNDCFilter;

public class PerfMonValve extends ValveBase implements Lifecycle {
	private static final Logger logger = LoggerFactory.initLogger(PerfMonValve.class);
	private final List<LifecycleListener> listeners = new ArrayList<LifecycleListener>();
	private final FilterChain filterChain = new FilterChainImpl(this);
	private PerfMonFilter filter = null;

	private String baseFilterCategory = PerfMonFilter.BASE_FILTER_CATEGORY;
    private boolean abortTimerOnRedirect = false;
    private boolean abortTimerOnImageResponse = false;
    private String abortTimerOnURLPattern = null;
    private String skipTimerOnURLPattern = null;
    private boolean outputRequestAndDuration = false;
    private String pushCookiesOnNDC = null;
    private String pushSessionAttributesOnNDC = null;
    private boolean pushClientInfoOnNDC = false;
    private String servletPathTransformationPattern = null; 

    protected void initInternal() throws LifecycleException {
    	super.initInternal();
    	
		try {
			FilterConfigImpl filterConfig = new FilterConfigImpl();
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_BASE_FILTER_CATEGORY_CONFIG_INIT_PARAM, getBaseFilterCategory());
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_ABORT_TIMER_ON_REDIRECT, Boolean.toString(isAbortTimerOnRedirect()));
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_ABORT_TIMER_ON_IMAGE_RESPONSE, Boolean.toString(isAbortTimerOnImageResponse()));
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_ABORT_TIMER_ON_URL_PATTERN, getAbortTimerOnURLPattern());
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_SKIP_TIMER_ON_URL_PATTERN, getSkipTimerOnURLPattern());
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_SERVLET_PATH_TRANSFORMATION_PATTERN, getServletPathTransformationPattern()); 		
			filterConfig.setInitParameter(PerfMonFilter.PROPERTY_OUTPUT_REQUEST_AND_DURATION, 
					Boolean.toString(isOutputRequestAndDuration()));
			
			filterConfig.setInitParameter(PerfMonNDCFilter.PROPERTY_PUSH_CLIENT_INFO, 
					Boolean.toString(isPushClientInfoOnNDC()));
			filterConfig.setInitParameter(PerfMonNDCFilter.PROPERTY_PUSH_COOKIES, 
				getPushCookiesOnNDC());
			filterConfig.setInitParameter(PerfMonNDCFilter.PROPERTY_PUSH_SESSION_ATTRIBUTES, 
					getPushSessionAttributesOnNDC());
			
			filter = new PerfMonNDCFilter(true);
			filter.init(filterConfig);
			
			logger.logInfo(this.getClass().getSimpleName() + " started.");
		} catch (ServletException e) {
			throw new LifecycleException("Error starting " + this.getClass().getName(), e);
		}
    }
    
    protected void destroyInternal() throws LifecycleException {
		filter.destroy();
		filter = null;
		logger.logInfo(this.getClass().getSimpleName() + " stopped.");

		super.destroyInternal();
    }
    
	public void invoke(Request request, Response response) throws IOException, ServletException {
		if ((filter != null) && (request instanceof ServletRequest) 
				&& (response instanceof ServletResponse)) {
			filter.doFilter((ServletRequest)request, (ServletResponse)response, filterChain);
		} else {
			getNext().invoke(request, response);
		}
	}
	
	public String getBaseFilterCategory() {
		return baseFilterCategory;
	}

	public void setBaseFilterCategory(String baseFilterCategory) {
		this.baseFilterCategory = baseFilterCategory;
	}

	public boolean isAbortTimerOnRedirect() {
		return abortTimerOnRedirect;
	}

	public void setAbortTimerOnRedirect(boolean abortTimerOnRedirect) {
		this.abortTimerOnRedirect = abortTimerOnRedirect;
	}
	
	public String getSkipTimerOnURLPattern() {
		return skipTimerOnURLPattern;
	}

	public void setSkipTimerOnURLPattern(String skipTimerOnURLPattern) {
		this.skipTimerOnURLPattern = skipTimerOnURLPattern;
	}

	public boolean isAbortTimerOnImageResponse() {
		return abortTimerOnImageResponse;
	}

	public void setAbortTimerOnImageResponse(boolean abortTimerOnImageResponse) {
		this.abortTimerOnImageResponse = abortTimerOnImageResponse;
	}

	public String getAbortTimerOnURLPattern() {
		return abortTimerOnURLPattern;
	}

	public boolean isOutputRequestAndDuration() {
		return outputRequestAndDuration;
	}

	public void setOutputRequestAndDuration(boolean outputRequestAndDuration) {
		this.outputRequestAndDuration = outputRequestAndDuration;
	}

	public void setAbortTimerOnURLPattern(String abortTimerOnURLPattern) {
		this.abortTimerOnURLPattern = abortTimerOnURLPattern;
	}
	
	public String getPushCookiesOnNDC() {
		return pushCookiesOnNDC;
	}

	public void setPushCookiesOnNDC(String pushCookiesOnNDC) {
		this.pushCookiesOnNDC = pushCookiesOnNDC;
	}

	public String getPushSessionAttributesOnNDC() {
		return pushSessionAttributesOnNDC;
	}

	public void setPushSessionAttributesOnNDC(String pushSessionAttributesOnNDC) {
		this.pushSessionAttributesOnNDC = pushSessionAttributesOnNDC;
	}

	public boolean isPushClientInfoOnNDC() {
		return pushClientInfoOnNDC;
	}

	public void setPushClientInfoOnNDC(boolean pushClientInfoOnNDC) {
		this.pushClientInfoOnNDC = pushClientInfoOnNDC;
	}

	public String getServletPathTransformationPattern() { 
		return servletPathTransformationPattern; 
	} 
 
	public void setServletPathTransformationPattern(String servletPathTransformationPattern) { 
		this.servletPathTransformationPattern = servletPathTransformationPattern; 
	} 
 	
	public void addLifecycleListener(LifecycleListener listener) {
		listeners.add(listener);
	}

	public LifecycleListener[] findLifecycleListeners() {
		return listeners.toArray(new LifecycleListener[listeners.size()]);
	}

	public void removeLifecycleListener(LifecycleListener listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(String eventType) {
		LifecycleEvent event = new LifecycleEvent(this, eventType);
		Iterator<LifecycleListener> itr = listeners.iterator();
		while(itr.hasNext()) {
			itr.next().lifecycleEvent(event);
		}
	}

	private static class FilterChainImpl implements FilterChain {
		private final Valve valve;
		
		public FilterChainImpl(Valve valve) {
			this.valve = valve;
		}

		public void doFilter(ServletRequest request, ServletResponse response)
				throws IOException, ServletException {
			valve.getNext().invoke((org.apache.catalina.connector.Request)request, 
					(org.apache.catalina.connector.Response)response);
		}
	}
	
	private static class FilterConfigImpl implements FilterConfig {
		final private Properties initParameters = new Properties();
		
		public String getFilterName() {
			return "PerfMonValve";
		}

		public String getInitParameter(String key) {
			return initParameters.getProperty(key);
		}

		public Enumeration getInitParameterNames() {
			return null;
		}

		public ServletContext getServletContext() {
			return null;
		}

		public void setInitParameter(String key, String value) {
			if (value == null) {
				initParameters.remove(key);
			} else {
				logger.logInfo(this.getClass().getSimpleName() + " " + key + "=" + value);
				initParameters.setProperty(key, value);
			}
		}
	}
}

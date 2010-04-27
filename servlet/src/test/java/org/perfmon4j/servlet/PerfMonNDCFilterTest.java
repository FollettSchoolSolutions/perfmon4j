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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.apache.log4j.NDC;
import org.mockito.Mockito;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;

public class PerfMonNDCFilterTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		PerfMon.deInit();
		super.tearDown();
	}

	private static class MyChain implements FilterChain {
		String ndc = null;
		private final boolean throwExcepton;
		
		MyChain(boolean throwException) {
			this.throwExcepton = throwException;
		}
		
		
		public void doFilter(ServletRequest arg0, ServletResponse arg1)
				throws IOException, ServletException {
			if (NDC.getDepth() > 0) {
				ndc = NDC.peek();
			}
			if (throwExcepton) {
				throw new IOException("Bogus");
			}
		}
	}
	
	private static class ChainRunner {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class); 
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		MyFilterConfig filterConfig = new MyFilterConfig();

		MyChain runRequest(boolean throwException) throws Exception {
			MyChain chain = new MyChain(throwException);
			
			PerfMonNDCFilter filter = new PerfMonNDCFilter();
			filter.init(filterConfig);
			
			try {
				filter.doFilter(request, response, chain);
				if (throwException) {
					fail("Expected exception to be throw");
				}
			} catch (Exception ex) {
				if (!throwException) {
					throw ex;
				}
			}

			return chain;
		}
	}
	
	public void testPerfMon4jNotActive() throws Exception {
		configurePerfMon();

		
		MyChain result = new ChainRunner().runRequest(false);
		assertNull("Should not put anything on NDC by default", result.ndc);
	}

	public void testPushCookiesWildCard() throws Exception {
		configurePerfMon();
		
		Cookie siteCookie = new Cookie("Site", "100");
		Cookie userCookie = new Cookie("User", "101");
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_COOKIES, "*");
		Mockito.when(runner.request.getCookies()).thenReturn(new Cookie[]{siteCookie, 
			userCookie});
		
		MyChain result = runner.runRequest(false);
		assertEquals("Cookies should be on the request", 
				"Site:100 User:101", result.ndc);
	}	

	public void testPushCookiesWildCardNoCookiesAvailable() throws Exception {
		configurePerfMon();
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_COOKIES, "*");
		Mockito.when(runner.request.getCookies()).thenReturn(null);
		
		MyChain result = runner.runRequest(false);
		assertNull("Should not have an NDC.. No Cookies", result.ndc);
	}	

	public void testPushCookiesSpecificCookies() throws Exception {
		configurePerfMon();
		
		Cookie siteCookie = new Cookie("Site", "100");
		Cookie userCookie = new Cookie("User", "101");
		
		ChainRunner runner = new ChainRunner();
		// Push specific cookie names!
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_COOKIES, 
				"Site, User");
		Mockito.when(runner.request.getCookies()).thenReturn(new Cookie[]{siteCookie, 
			userCookie});
		
		MyChain result = runner.runRequest(false);
		assertEquals("Cookies should be on the request", 
				"Site:100 User:101", result.ndc);
	}	
	
	public void testPushCookiesSpecificCookiesNotAvailable() throws Exception {
		configurePerfMon();
		
		ChainRunner runner = new ChainRunner();
		// Push specific cookie names!
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_COOKIES, 
				"Site, User");
		Mockito.when(runner.request.getCookies()).thenReturn(null);
		
		MyChain result = runner.runRequest(false);
		assertEquals("Cookies should be on the request", 
				"Site:null User:null", result.ndc);
	}	
	
	private static Enumeration asEnumeration(String values[]) {
		List<String> l = new Vector<String>();
		for (int i = 0; i < values.length; i++) {
			l.add(values[i]);
		}
		return Collections.enumeration(l);
	}
	
	public void testPushSessionAttributesWildCard() throws Exception {
		configurePerfMon();
		
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute("Site")).thenReturn("100");
		Mockito.when(session.getAttribute("User")).thenReturn("101");
		Mockito.when(session.getAttributeNames()).thenReturn(
				asEnumeration(new String[]{"Site", "User"}));
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_SESSION_ATTRIBUTES, 
				"*");
		Mockito.when(runner.request.getSession(false)).thenReturn(session);
		
		MyChain result = runner.runRequest(false);
		assertEquals("Session Attributes should be on the request", 
				"Site:100 User:101", result.ndc);
	}	
	
	public void testPushSessionAttributesWildCardNOSession() throws Exception {
		configurePerfMon();
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_SESSION_ATTRIBUTES, 
				"*");
		Mockito.when(runner.request.getSession(false)).thenReturn(null);
		
		MyChain result = runner.runRequest(false);
		assertNull("No session exist, should return null", result.ndc);
	}	

	public void testPushSessionAttributesWildCardNOAttributes() throws Exception {
		configurePerfMon();
		
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttributeNames()).thenReturn(
				asEnumeration(new String[]{}));
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_SESSION_ATTRIBUTES, 
				"*");
		Mockito.when(runner.request.getSession(false)).thenReturn(session);
		
		MyChain result = runner.runRequest(false);
		assertNull("No session exist, should return null", result.ndc);
	}	
	
	public void testPushSpecificSessionAttributes() throws Exception {
		configurePerfMon();
		
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute("Site")).thenReturn("100");
		Mockito.when(session.getAttribute("User")).thenReturn("101");
		Mockito.when(session.getAttributeNames()).thenReturn(
				asEnumeration(new String[]{"Site", "User"}));
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_SESSION_ATTRIBUTES, 
				"Site, User");
		Mockito.when(runner.request.getSession(false)).thenReturn(session);
		
		MyChain result = runner.runRequest(false);
		assertEquals("Session Attributes should be on the request", 
				"Site:100 User:101", result.ndc);
	}		

	public void testPushSpecificSessionAttributesNOSession() throws Exception {
		configurePerfMon();
		
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute("Site")).thenReturn("100");
		Mockito.when(session.getAttribute("User")).thenReturn("101");
		Mockito.when(session.getAttributeNames()).thenReturn(
				asEnumeration(new String[]{"Site", "User"}));
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_SESSION_ATTRIBUTES, 
				"Site, User");
		Mockito.when(runner.request.getSession(false)).thenReturn(null);
		
		MyChain result = runner.runRequest(false);
		assertEquals("Session Attributes should be on the request", 
				"Site:null User:null", result.ndc);
	}		
	
	
	public void testPushClientInfo() throws Exception {
		configurePerfMon();
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_CLIENT_INFO, 
				"true");
		Mockito.when(runner.request.getRemoteAddr()).thenReturn("192.1.1.1");
		
		MyChain result = runner.runRequest(false);
		assertEquals("Should return client ip", "192.1.1.1", result.ndc);
	}	

	
	public void testPushClientInfoIncludeProxyForwardedInfo() throws Exception {
		configurePerfMon();
		
		ChainRunner runner = new ChainRunner();
		runner.filterConfig.config.put(PerfMonNDCFilter.PROPERTY_PUSH_CLIENT_INFO, 
				"true");
		Mockito.when(runner.request.getRemoteAddr()).thenReturn("192.1.1.1");
		Mockito.when(runner.request.getHeader("X-Forwarded-For")).thenReturn("200.2.2.2");
		
		MyChain result = runner.runRequest(false);
		assertEquals("Should return client ip including forwarded header", "192.1.1.1[200.2.2.2]", result.ndc);
	}	
	
	
	private void configurePerfMon() throws Exception {
		PerfMonConfiguration config = new PerfMonConfiguration();
		PerfMon.configure(config);
	}
	
	private static class MyFilterConfig implements FilterConfig {
		Map<String, String> config = new HashMap<String, String>();
		
		public String getFilterName() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getInitParameter(String key) {
			return config.get(key);
		}

		public Enumeration getInitParameterNames() {
			return null;
		}

		public ServletContext getServletContext() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	
	public void testNoNDC() throws Exception {
//		FilterConfig config = new FilterConfig();
		
		
		
	}

	
}

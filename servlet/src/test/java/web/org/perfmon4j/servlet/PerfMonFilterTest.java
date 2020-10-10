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

package web.org.perfmon4j.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mockito.Mockito;
import org.perfmon4j.Appender;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.TextAppender;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceData;

import junit.framework.TestCase;

public class PerfMonFilterTest extends TestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		PerfMon.deInitAndCleanMonitors_TESTONLY();
		super.tearDown();
	}

	private static class MyChain implements FilterChain {
		ThreadTraceConfig.TriggerValidator validators[] = null;
		private final boolean throwExcepton;
		
		MyChain(boolean throwException) {
			this.throwExcepton = throwException;
		}
		
		
		public void doFilter(ServletRequest arg0, ServletResponse arg1)
				throws IOException, ServletException {
			validators = ThreadTraceConfig.getValidatorsOnThread();
			if (throwExcepton) {
				throw new IOException("Bogus");
			}
		}
	}
	
	private MyChain runRequestThroughChain(boolean throwException) throws Exception {
		TestAppender.threadTraceOutputCount = 0;
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class); 
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		MyChain chain = new MyChain(throwException);
		FilterConfig config = Mockito.mock(FilterConfig.class);
		
		PerfMonFilter filter = new PerfMonFilter();
		filter.init(config);
		
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
	
	public static class TestAppender extends Appender {
		static int threadTraceOutputCount = 0;

		public TestAppender(AppenderID id) {
			super(id);
		}
		
		public void outputData(PerfMonData data) {
			if (data instanceof ThreadTraceData) {
				threadTraceOutputCount++;
				System.out.println("Ouput: " + threadTraceOutputCount  + data.toAppenderString());
			}
		}
	}
	
	
	public void testPerfMon4jNotActive() throws Exception {
		PerfMon.deInit();
		
		MyChain chain = runRequestThroughChain(false);
		assertEquals("Should not put any validators on the thread when we do not have active thread traces", 0, chain.validators.length);
	}

	
	private void configurePerfMon(ThreadTraceConfig traceConfig) throws Exception {
		PerfMonConfiguration config = new PerfMonConfiguration();
		config.defineAppender("Basic", TestAppender.class.getName(), "1 second");
		
		traceConfig.addAppender(config.getAppenderForName("Basic"));
		config.addThreadTraceConfig("WebRequest", traceConfig);
		
		PerfMon.configure(config);
	}
	
	public void testPerfMonActiveButNoThreadTraceWithTriggers() throws Exception {
		configurePerfMon(new ThreadTraceConfig());
		MyChain chain = runRequestThroughChain(false);
		
		Appender.flushAllAppenders();
		
		assertEquals("Request should have created stack trace", 1, TestAppender.threadTraceOutputCount);
		assertEquals("Should not put any validators on the thread when we do not have active thread traces", 0, chain.validators.length);
	}

	
	public void testHttpRequestTriggerIsActive() throws Exception {
		ThreadTraceConfig traceConfig = new ThreadTraceConfig();
		ThreadTraceConfig.HTTPRequestTrigger t = new ThreadTraceConfig.HTTPRequestTrigger("bibID", "100");
		traceConfig.setTriggers(new ThreadTraceConfig.Trigger[]{t});
		configurePerfMon(traceConfig);
		
		MyChain chain = runRequestThroughChain(false);
		
		assertEquals("Request should NOT have created stack trace request DOES not match our trigger", 
				0, TestAppender.threadTraceOutputCount);
		assertEquals("Filter should have inserted validator on stack", 1, chain.validators.length);
		assertTrue("Should be request validator", chain.validators[0] instanceof PerfMonFilter.HttpRequestValidator);

		assertEquals("Filter must not leave validators on the thread", 0, ThreadTraceConfig.getValidatorsOnThread().length);
	}
	
	public void testHttpRequestTriggerIsValid() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getParameterValues("bibID")).thenReturn(new String[]{"100", "101", "102"});
		PerfMonFilter.HttpRequestValidator v = new PerfMonFilter.HttpRequestValidator(request);
		
		ThreadTraceConfig.HTTPRequestTrigger tMatch = new ThreadTraceConfig.HTTPRequestTrigger("bibID", "101");
		assertTrue("expected match for bibID=100",  v.isValid(tMatch));
		
		ThreadTraceConfig.HTTPRequestTrigger tNoMatch = new ThreadTraceConfig.HTTPRequestTrigger("bibID", "201");
		assertFalse("expected match for bibID=201",  v.isValid(tNoMatch));
	}

	public void testHttpSessionTriggerIsValid() {
		HttpSession session = Mockito.mock(HttpSession.class);
		Mockito.when(session.getAttribute("userID")).thenReturn("200");
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getSession(false)).thenReturn(session);
		
		PerfMonFilter.HttpSessionValidator v = new PerfMonFilter.HttpSessionValidator(request);
		
		ThreadTraceConfig.HTTPSessionTrigger tMatch = new ThreadTraceConfig.HTTPSessionTrigger("userID", "200");
		assertTrue("expected match for userID=200",  v.isValid(tMatch));
		
		ThreadTraceConfig.HTTPSessionTrigger tNoMatch = new ThreadTraceConfig.HTTPSessionTrigger("userID", "201");
		assertFalse("expected match for userID=201",  v.isValid(tNoMatch));
	}

	public void testHttpSessionTriggerIsActive() throws Exception {
		ThreadTraceConfig traceConfig = new ThreadTraceConfig();
		ThreadTraceConfig.HTTPSessionTrigger t1 = new ThreadTraceConfig.HTTPSessionTrigger("userID", "200");
		
		traceConfig.setTriggers(new ThreadTraceConfig.Trigger[]{t1});
		configurePerfMon(traceConfig);
		
		MyChain chain = runRequestThroughChain(false);
		
		assertEquals("Filter should have inserted validator on stack", 1, chain.validators.length);
		assertEquals("Filter must not leave validators on the thread", 0, ThreadTraceConfig.getValidatorsOnThread().length);
	}

	public void testHttpCookieTriggerIsValid() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("JSESSIONID", "700")});
		
		PerfMonFilter.HttpCookieValidator v = new PerfMonFilter.HttpCookieValidator(request);
		
		ThreadTraceConfig.HTTPCookieTrigger tMatch = new ThreadTraceConfig.HTTPCookieTrigger("JSESSIONID", "700");
		assertTrue("expected match for JSESSIONID=700",  v.isValid(tMatch));
		
		ThreadTraceConfig.HTTPCookieTrigger tNoMatch = new ThreadTraceConfig.HTTPCookieTrigger("JSESSIONID", "701");
		assertFalse("expected match for JSESSIONID=701",  v.isValid(tNoMatch));
	}
	
	public void testHttpCookieTriggerIsActive() throws Exception {
		ThreadTraceConfig traceConfig = new ThreadTraceConfig();
		ThreadTraceConfig.HTTPCookieTrigger t1 = new ThreadTraceConfig.HTTPCookieTrigger("JSESSIONID", "700");
		
		traceConfig.setTriggers(new ThreadTraceConfig.Trigger[]{t1});
		configurePerfMon(traceConfig);
		
		MyChain chain = runRequestThroughChain(false);
		
		assertEquals("Filter should have inserted validator on stack", 1, chain.validators.length);
		assertEquals("Filter must not leave validators on the thread", 0, ThreadTraceConfig.getValidatorsOnThread().length);
	}
	
	
	public void testTriggersAreCleanedUpOnException() throws Exception {
		ThreadTraceConfig traceConfig = new ThreadTraceConfig();
		ThreadTraceConfig.HTTPRequestTrigger t1 = new ThreadTraceConfig.HTTPRequestTrigger("bibID", "100");
		ThreadTraceConfig.HTTPSessionTrigger t2 = new ThreadTraceConfig.HTTPSessionTrigger("userID", "200");
		ThreadTraceConfig.HTTPCookieTrigger  t3 = new ThreadTraceConfig.HTTPCookieTrigger("JSEE=SION", "200");
		
		traceConfig.setTriggers(new ThreadTraceConfig.Trigger[]{t1, t2, t3});
		
		configurePerfMon(traceConfig);
		
		MyChain chain = null;
		chain = runRequestThroughChain(true);
		
		assertEquals("Filter should have inserted validators on stack", 3, chain.validators.length);
		assertEquals("Filter must NOT leave validators on the thread", 0, ThreadTraceConfig.getValidatorsOnThread().length);
	}
	
	public void testBuildRequestDescription() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Mockito.when(request.getContextPath()).thenReturn("/default");
		Mockito.when(request.getQueryString()).thenReturn(null);
		Mockito.when(request.getServletPath()).thenReturn("/something.do");
		Mockito.when(request.getMethod()).thenReturn("GET");

		String parameterNames[] = new String[] {
				"site", "user"
		};
		Mockito.when(request.getParameterNames()).thenReturn(Collections.enumeration(
				Arrays.asList(parameterNames)));
		Mockito.when(request.getParameterValues("site")).thenReturn(new String[]{"100", "200"});
		Mockito.when(request.getParameterValues("user")).thenReturn(new String[]{"300"});
		
		String result = PerfMonFilter.buildRequestDescription(request);
		assertEquals("", "GET /default/something.do?site=100&site=200&user=300", result);
	}
	
	public void testBuildRequestDescriptionEncodesParameters() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Mockito.when(request.getContextPath()).thenReturn("/default");
		Mockito.when(request.getQueryString()).thenReturn(null);
		Mockito.when(request.getServletPath()).thenReturn("/something.do");

		String parameterNames[] = new String[] {
				"site"
		};
		Mockito.when(request.getParameterNames()).thenReturn(Collections.enumeration(
				Arrays.asList(parameterNames)));
		Mockito.when(request.getParameterValues("site")).thenReturn(new String[]{"description=this is my site"});
		
		String result = PerfMonFilter.buildRequestDescription(request);
		assertEquals("", "/default/something.do?site=description%3Dthis+is+my+site", result);
	}	
	
	public void testBuildRequestDescriptionIncludesPathInfo() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Mockito.when(request.getContextPath()).thenReturn("/dap");
		Mockito.when(request.getServletPath()).thenReturn("/rest");
		Mockito.when(request.getPathInfo()).thenReturn("/ebook/new");
		
		String result = PerfMonFilter.buildRequestDescription(request);
		assertEquals("", "/dap/rest/ebook/new", result);
	}	
	
	
	public void testValueContainingPasswordIsBlocked() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Mockito.when(request.getContextPath()).thenReturn("/default");
		Mockito.when(request.getQueryString()).thenReturn(null);
		Mockito.when(request.getServletPath()).thenReturn("/something.do");

		String parameterNames[] = new String[] {
				"mypassword"
		};
		Mockito.when(request.getParameterNames()).thenReturn(Collections.enumeration(
				Arrays.asList(parameterNames)));
		Mockito.when(request.getParameterValues("mypassword")).thenReturn(new String[]{"secret"});
		
		String result = PerfMonFilter.buildRequestDescription(request);
		assertEquals("", "/default/something.do?mypassword=*******", result);
	}
	
	
	public void testURLFilterIncludesDOESNotIncluedContextServletFilter() throws Exception {
		PerfMonFilter filter = new PerfMonFilter(false);
		
		FilterConfig config = Mockito.mock(FilterConfig.class);
		Mockito.when(config.getInitParameter(PerfMonFilter.PROPERTY_ABORT_TIMER_ON_URL_PATTERN)).thenReturn("/something\\.do");
		
		filter.init(config);
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Mockito.when(request.getContextPath()).thenReturn("/default");
		Mockito.when(request.getServletPath()).thenReturn("/something.do");

		assertTrue("When the filter is attached to a specific context, the pattern should NOT be evaluated " +
			" with the context name included. "
			,filter.matchesURLPattern(request, filter.getAbortTimerOnURLPattern()));
	}

	
	public void testURLFilterRequiersContextFromValve() throws Exception {
		boolean childOfValve = true;
		PerfMonFilter filter = new PerfMonFilter(childOfValve);
		
		FilterConfig config = Mockito.mock(FilterConfig.class);
		Mockito.when(config.getInitParameter(PerfMonFilter.PROPERTY_ABORT_TIMER_ON_URL_PATTERN)).thenReturn("/default/something\\.do");
		
		filter.init(config);
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		Mockito.when(request.getContextPath()).thenReturn("/default");
		Mockito.when(request.getServletPath()).thenReturn("/something.do");

		assertTrue("When called from valve context path is significant to pattern matching! " 
			,filter.matchesURLPattern(request, filter.getAbortTimerOnURLPattern()));

	
		Mockito.when(request.getContextPath()).thenReturn("/someothercontext");
		assertFalse("Context path does not match should not match pattern" 
				,filter.matchesURLPattern(request, filter.getAbortTimerOnURLPattern()));
	}
	
	
	/**
	 * With the increased usage of RESTFUL frameworks dynamic URL's are
	 * increasing dynamic url paths we should not "create" monitors
	 * dynamically in response to a URL.  We should only create them if
	 * a specific appender is attached to the monitor OR an appender pattern
	 * instructs us to create them.
	 */
	public void testMonitorIsLazilyCreated() throws Exception {
		PerfMonConfiguration config = new PerfMonConfiguration();
		PerfMon.configure(config);
		
		HttpSession session = Mockito.mock(HttpSession.class);
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getSession(false)).thenReturn(session);
		
		Mockito.when(request.getContextPath()).thenReturn("");
		Mockito.when(request.getServletPath()).thenReturn("/circulation/getstat/444");
		
		PerfMonFilter f = new PerfMonFilter();
		f.init(Mockito.mock(FilterConfig.class));
		
		assertNotNull("When the filter is initialized the root monitor for web requests should be created", 
				PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY("WebRequest"));

		
		
		f.doFilter(request, Mockito.mock(HttpServletResponse.class), Mockito.mock(FilterChain.class));
		
		assertNull("Should not create monitor without an attatched appender", 
				PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY("WebRequest.circulation"));
		
		// Now configure a CHILD based appender on WebRequest.  We should now
		// create the WebRequest.circulation monitor.
		config = new PerfMonConfiguration();
		config.defineAppender("DEFAULT", TextAppender.class.getName(), "10 seconds");
		config.defineMonitor("WebRequest");
		config.attachAppenderToMonitor("WebRequest", "DEFAULT", "./*");
		PerfMon.configure(config);
		
		f.doFilter(request, Mockito.mock(HttpServletResponse.class), Mockito.mock(FilterChain.class));
		assertNotNull("Now we should have created the child monitor", 
				PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY("WebRequest.circulation"));
		
		assertNull("Should NOT have created GRAND child monitor", 
				PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY("WebRequest.circulation.getstat"));
		
		// Now configure a CHILD and ALL Descendentsbased appender on WebRequest.  
		// We should now create the WebRequest.circulation monitor.
		config = new PerfMonConfiguration();
		config.defineAppender("DEFAULT", TextAppender.class.getName(), "10 seconds");
		config.defineMonitor("WebRequest");
		config.attachAppenderToMonitor("WebRequest", "DEFAULT", "./**");
		PerfMon.configure(config);
		
		f.doFilter(request, Mockito.mock(HttpServletResponse.class), Mockito.mock(FilterChain.class));
		
		assertNotNull("Should have created GREAT GRAND child monitor", 
				PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY("WebRequest.circulation.getstat"));
		assertNotNull("Should have created GREAT GREAT GRAND child monitor", 
				PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY("WebRequest.circulation.getstat.444"));
	}
}

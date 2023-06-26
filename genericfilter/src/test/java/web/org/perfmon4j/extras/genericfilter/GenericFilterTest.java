package web.org.perfmon4j.extras.genericfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.mockito.Mockito;
import org.perfmon4j.Appender;
import org.perfmon4j.BootConfiguration;
import org.perfmon4j.BootConfiguration.ServletValveConfig;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.ThreadTraceData;

import junit.framework.TestCase;

public class GenericFilterTest extends TestCase {
	public GenericFilterTest(String name) {
		super(name);
	}

	private PerfMonConfiguration config;
	protected void setUp() throws Exception {
		super.setUp();
		setUpSimpleAppender("WebRequest");
	}
	
	private void setUpSimpleAppender(String monitorName) throws Exception {
		config = new PerfMonConfiguration();
		config.defineAppender("SIMPLE", SimpleAppender.class.getName(), "200 ms");
		config.defineMonitor(monitorName);
		config.attachAppenderToMonitor(monitorName, "SIMPLE", ".");
		
		PerfMon.configure(config);
	}
	
	protected void tearDown() throws Exception {
		PerfMon.configure();
		
		super.tearDown();
	}
	
	public void testMaskPasswordInQueryString() {
		assertEquals("Password only parameter", "?password=*******", GenericFilter.maskPassword("?password=dave"));
		assertEquals("Password second parameter", "?user=dave&password=*******", GenericFilter.maskPassword("?user=dave&password=dave"));
		assertEquals("Password middle parameter", "?user=dave&password=*******&time=now", GenericFilter.maskPassword("?user=dave&password=dave&time=now"));
		assertEquals("Multiple password parameters", "?password=*******&password=*******&password=*******&password=*******", GenericFilter.maskPassword("?password=dave&password=this is a test&password=&password=t"));
		assertEquals("Password in value is ignored", "?word=password", GenericFilter.maskPassword("?word=password"));
		assertEquals("Work with ; separator", "?password=*******;x=y", GenericFilter.maskPassword("?password=dave;x=y"));
	}
	
	public static class SimpleAppender extends Appender {
		private static SimpleAppender appender = null; 
		
		static SimpleAppender getSingleton() {
			return appender;
		}
		
		private final AtomicReference<CountDownLatch> latchSemaphore = new AtomicReference<>(null);
		private final AtomicReference<Class<? extends PerfMonData>> typeToCapture = new AtomicReference<>(null); 
		private final AtomicReference<PerfMonData> nextData = new AtomicReference<>(null);
		
		private final Object captureThreadTracesLockToken = new Object();
		private List<ThreadTraceData> captureThreadTraces = null;

		public SimpleAppender(AppenderID id) { 
			super(id, false);
			appender = this;
		}

		@SuppressWarnings("unchecked")
		public <T extends PerfMonData > T getNextOuput(Class<? extends PerfMonData> dataType) {
			nextData.set(null);
			typeToCapture.set(dataType);
			latchSemaphore.set(new CountDownLatch(1));
			
			try {
				latchSemaphore.get().await(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return null;
			}
			
			return (T)nextData.getAndSet(null);//
		}
		
		// Since the data will be collected often.  Do this to ensure
		// that your observations are not collected across two time intervals.
		public void waitForNewCollectionIntervalToStart(Class<? extends PerfMonData> dataType)  {
			getNextOuput(IntervalData.class);
		}
		
		public IntervalData runAndGetIntervalOutput(RunnableWithException runnable) throws Exception {
			waitForNewCollectionIntervalToStart(IntervalData.class);
			
			runnable.run();
			IntervalData result = getNextOuput(IntervalData.class);
//System.out.println(result.toAppenderString());			
			assertNotNull("Did not recieve expected appender data outout of type: " + IntervalData.class, result);
			
			return result;
		}

		public ThreadTraceData[] runAndGetThreadTraceOutput(RunnableWithException runnable) throws Exception {
			ThreadTraceData[] result = null;
			
			synchronized (captureThreadTracesLockToken) {
				captureThreadTraces = new ArrayList<>();
			}
			
			runnable.run();
			
			synchronized (captureThreadTracesLockToken) {
				result = captureThreadTraces.toArray(new ThreadTraceData[] {});
				captureThreadTraces = null;
			}
			
			return result;
		}
		
		
		@FunctionalInterface
		public static interface RunnableWithException {
			public void run() throws Exception;
		}
		
		
		@Override
		public void outputData(PerfMonData data) {
			if (ThreadTraceData.class.equals(data.getClass())) {
				synchronized(captureThreadTracesLockToken) {
					if (captureThreadTraces != null) {
						captureThreadTraces.add((ThreadTraceData)data);
					}
				}
			}
			CountDownLatch latch = this.latchSemaphore.get();
			if (latch != null) {
				if (data.getClass().equals(typeToCapture.get())) {
					nextData.set(data);
					latchSemaphore.set(null);
					latch.countDown();
				}
			}
		}
	}

	public void testDefaultFilterParmas() {
		FilterParams params = FilterParams.getDefault();
		assertEquals("baseFilterCategory", "WebRequest", params.getBaseFilterCategory());
		assertNull("abortTimerOnURLPattern", params.getAbortTimerOnURLPattern());
		assertNull("skipTimerOnURLPattern", params.getSkipTimerOnURLPattern());
		assertNull("servletPathTransformationPattern", params.getServletPathTransformationPattern());
		
		assertTrue("abortTimerOnImageResponse", params.isAbortTimerOnImageResponse());
		assertTrue("abortTimerOnRedirect", params.isAbortTimerOnRedirect());
		assertTrue("outputRequestAndDuration", params.isOutputRequestAndDuration());
	}
	
	public void testParseFilterParmas() {
		ServletValveConfig svConfig = new ServletValveConfig();
		BootConfiguration config = BootConfiguration.getDefault();
		config.setServletValveConfig(svConfig);

		svConfig.setBaseFilterCategory("RestRequest");
		svConfig.setAbortTimerOnURLPattern("/dont-time");
		svConfig.setSkipTimerOnURLPattern("/skip");
		svConfig.setServletPathTransformationPattern("/rest/context/*/ => /");
		svConfig.setAbortTimerOnImageResponse(false);
		svConfig.setAbortTimerOnRedirect(false);
		svConfig.setOutputRequestAndDuration(false);
		
		FilterParams params = FilterParams.fromProperties(config.exportAsProperties());
		assertEquals("baseFilterCategory", "RestRequest", params.getBaseFilterCategory());
		assertEquals("abortTimerOnURLPattern", "/dont-time", params.getAbortTimerOnURLPattern());
		assertEquals("skipTimerOnURLPattern", "/skip", params.getSkipTimerOnURLPattern());
		assertEquals("servletPathTransformationPattern", "/rest/context/*/ => /", params.getServletPathTransformationPattern());
		
		assertFalse("abortTimerOnImageResponse", params.isAbortTimerOnImageResponse());
		assertFalse("abortTimerOnRedirect", params.isAbortTimerOnRedirect());
		assertFalse("outputRequestAndDuration", params.isOutputRequestAndDuration());
	}
	
	public void testSimpleMonitor() throws Exception {
		GenericFilterTestImpl genericFilter = new GenericFilterTestImpl();
		HttpRequest request = buildMockRequest();
		HttpResponse response = buildMockResponse();
		HttpRequestChain chain = buildMockChain();
		
		IntervalData data =  SimpleAppender.getSingleton().runAndGetIntervalOutput(() -> { 
			genericFilter.handleRequest(request, response, chain);
		});
		
		assertEquals(1, data.getTotalCompletions());
		assertEquals("Should of had 1 hit", 1, data.getTotalHits());		
	}
	
	private HttpRequest buildMockRequest() {
		HttpRequest result = Mockito.mock(HttpRequest.class);
		Mockito.when(result.getMethod()).thenReturn("GET");
		Mockito.when(result.getServletPath()).thenReturn("/");
		Mockito.when(result.getQueryString()).thenReturn("?x=y");
		return result;
	}
	
	private HttpResponse buildMockResponse() {
		return Mockito.mock(HttpResponse.class);
	}
	
	private HttpRequestChain buildMockChain() {
		return Mockito.mock(HttpRequestChain.class);
	}
	
	public void testAbortTimerOnImageResponse() throws Exception {
		FilterParamsVO params = new FilterParamsVO();
		params.setAbortTimerOnImageResponse(true);
		
		GenericFilterTestImpl genericFilter = new GenericFilterTestImpl(params);
		HttpRequest request = buildMockRequest();
		HttpResponse response = buildMockResponse();
		HttpRequestChain chain = buildMockChain();
		
		Mockito.when(response.getHeader("Content-Type")).thenReturn("image/jpeg");
		IntervalData data =  SimpleAppender.getSingleton().runAndGetIntervalOutput(() -> { 
			genericFilter.handleRequest(request, response, chain);
		});
		
		assertEquals("Should of had 1 hit", 1, data.getTotalHits());
		assertEquals("Completion should have been aborted", 0, data.getTotalCompletions());
	}
	
	public void testAbortTimerOnRedirect() throws Exception {
		FilterParamsVO params = new FilterParamsVO();
		params.setAbortTimerOnRedirect(true);
		
		GenericFilterTestImpl genericFilter = new GenericFilterTestImpl(params);
		HttpRequest request = buildMockRequest();
		HttpResponse response = buildMockResponse();
		HttpRequestChain chain = buildMockChain();
		
		Mockito.when(response.getStatus()).thenReturn(Integer.valueOf(303));
		IntervalData data =  SimpleAppender.getSingleton().runAndGetIntervalOutput(() -> { 
			genericFilter.handleRequest(request, response, chain);
		});
		
		assertEquals("Should of had 1 hit", 1, data.getTotalHits());
		assertEquals("Completion should have been aborted", 0, data.getTotalCompletions());
	}

	public void testAbortTimerOnURLPattern() throws Exception {
		FilterParamsVO params = new FilterParamsVO();
		params.setAbortTimerOnURLPattern("/dave");
		
		GenericFilterTestImpl genericFilter = new GenericFilterTestImpl(params);
		HttpRequest request = buildMockRequest();
		HttpResponse response = buildMockResponse();
		HttpRequestChain chain = buildMockChain();
		
		IntervalData data =  SimpleAppender.getSingleton().runAndGetIntervalOutput(() -> { 
			Mockito.when(request.getServletPath()).thenReturn("/dave");
			genericFilter.handleRequest(request, response, chain);
			
			Mockito.when(request.getServletPath()).thenReturn("/notdave");
			genericFilter.handleRequest(request, response, chain);
		});

		assertEquals("Both should have hit", 2, data.getTotalHits());
		assertEquals("Only the request that did not match pattern should have been flagged complete", 
				1, data.getTotalCompletions());
	}
	
	public void testServletPathTransformationPattern() throws Exception {
		FilterParamsVO params = new FilterParamsVO();
		params.setServletPathTransformationPattern("/context/mycontext/ => /");;
		setUpSimpleAppender("WebRequest.rest");
		
		GenericFilterTestImpl genericFilter = new GenericFilterTestImpl(params);
		HttpRequest request = buildMockRequest();
		HttpResponse response = buildMockResponse();
		HttpRequestChain chain = buildMockChain();
		
		IntervalData data =  SimpleAppender.getSingleton().runAndGetIntervalOutput(() -> { 
			Mockito.when(request.getServletPath()).thenReturn("/context/mycontext/rest");
			genericFilter.handleRequest(request, response, chain);
		});
		
		assertEquals("Should have been captured in WebRequest.rest category", 
				1, data.getTotalCompletions());
	}

	public void testSkipTimerOnURLPattern() throws Exception {
		FilterParamsVO params = new FilterParamsVO();
		params.setSkipTimerOnURLPattern("/dave");
		
		GenericFilterTestImpl genericFilter = new GenericFilterTestImpl(params);
		HttpRequest request = buildMockRequest();
		HttpResponse response = buildMockResponse();
		HttpRequestChain chain = buildMockChain();
		
		IntervalData data =  SimpleAppender.getSingleton().runAndGetIntervalOutput(() -> { 
			Mockito.when(request.getServletPath()).thenReturn("/dave");
			genericFilter.handleRequest(request, response, chain);
			
			Mockito.when(request.getServletPath()).thenReturn("/notdave");
			genericFilter.handleRequest(request, response, chain);
		});
		
		assertEquals("Only ther request that did not match pattern should have been flagged as a hit", 
				1, data.getTotalHits());
		assertEquals("Only the request that did not match pattern should have been flagged complete", 
				1, data.getTotalCompletions());
	}
	
//	public void testThreadTraceHttpRequestTrigger() throws Exception {
//		Trigger trigger = new ThreadTraceConfig.HTTPRequestTrigger("userName", "Dave");
//		addThreadTraceTrigger(trigger);
//		
//		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
//		HttpHandler handler = buildMockHttpHandler();
//		HttpServerExchange exchange = buildMockExchange();
//		
//		HandlerImpl impl = new HandlerImpl(wrapper, handler);
//		ThreadTraceData[] data =  SimpleAppender.getSingleton().runAndGetThreadTraceOutput(() -> { 
//			impl.handleRequest(exchange);
//			
//			// Now add a request parameter that matches the trigger.
//			exchange.addQueryParam("userName", "Dave");
//			impl.handleRequest(exchange);
//		});
//		assertEquals("Should have gotten 1 thread trace", 1, data.length);
//	}
//
//	public void testSkipRequestLogOutput() throws Exception {
//		HttpHandler handler = buildMockHttpHandler();
//		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
//		wrapper.setOutputRequestAndDuration(true);
//		
//		HandlerImpl impl = Mockito.spy(new HandlerImpl(wrapper, handler));
//
//		HttpServerExchange exchange = buildMockExchange();
//		impl.handleRequest(exchange);
//		
//		Mockito.verify(impl, Mockito.times(1)).outputToLog(Mockito.any(), 
//			Mockito.any(), Mockito.any());
//
//		// If this named attribute exists on the request (containing any non-null value)
//		// we will not write the request to the output log.
//		// This allows some WAR application to perform their own logging.
//		RequestSkipLogTracker.getTracker().setAttribute("PERFMON4J_SKIP_LOG_FOR_REQUEST", "anything");
//		
//		impl = Mockito.spy(new HandlerImpl(wrapper, handler));
//		impl.handleRequest(exchange);
//		
//		Mockito.verify(impl, Mockito.times(0)).outputToLog(Mockito.any(), 
//			Mockito.any(), Mockito.any());
//	}
//	
//	private ServletRequestContext getOrCreateServletContext(HttpServerExchange exchange) {
//		ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
//		if (context == null) {
//			context = Mockito.mock(ServletRequestContext.class);
//			exchange.putAttachment(ServletRequestContext.ATTACHMENT_KEY, context);
//		}
//		return context;
//	}
//	
//	private void addSessionAttribute(HttpServerExchange exchange, String name, String value) {
//		ServletRequestContext context = getOrCreateServletContext(exchange);
//		HttpSessionImpl session = context.getSession(); 
//		if (session == null) {
//			session = Mockito.mock(HttpSessionImpl.class);
//			Mockito.when(context.getSession()).thenReturn(session);
//		}
//		Mockito.when(session.getAttribute(name)).thenReturn(value);
//	}
//
//	private void addRequestAttribute(HttpServerExchange exchange, String name, String value) {
//		ServletRequestContext context = getOrCreateServletContext(exchange);
//		ServletRequest servletRequest  = context.getServletRequest();
//		if (servletRequest  == null) {
//			servletRequest = Mockito.mock(ServletRequest.class);
//			Mockito.when(context.getServletRequest()).thenReturn(servletRequest);
//		}
//		Mockito.when(servletRequest.getAttribute(name)).thenReturn(value);
//	}
//
//	public void testThreadTraceHttpSessionTrigger() throws Exception {
//		Trigger trigger = new ThreadTraceConfig.HTTPSessionTrigger("userName", "Dave");
//		addThreadTraceTrigger(trigger);
//		
//		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
//		HttpHandler handler = buildMockHttpHandler();
//		HttpServerExchange exchange = buildMockExchange();
//	
//		HandlerImpl impl = new HandlerImpl(wrapper, handler);
//		ThreadTraceData[] data =  SimpleAppender.getSingleton().runAndGetThreadTraceOutput(() -> { 
//			impl.handleRequest(exchange);
//			
//			// Now add Session attribute that matches the trigger.
//			addSessionAttribute(exchange, "userName", "Dave");
//			impl.handleRequest(exchange);
//		});	
//		
//		assertEquals("Should have gotten 1 thread trace", 1, data.length);
//	}
//
//	private void addCookie(HttpServerExchange exchange, String name, String value) throws Exception {
//		Cookie cookie = Mockito.mock(Cookie.class);
//		Mockito.when(cookie.getValue()).thenReturn(value);
//		
//		exchange.getRequestCookies().put(name, cookie);
//		
//	}
//	
//	public void testThreadTraceHttpCookieTrigger() throws Exception {
//		Trigger trigger = new ThreadTraceConfig.HTTPCookieTrigger("userName", "Dave");
//		addThreadTraceTrigger(trigger);
//		
//		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
//		HttpHandler handler = buildMockHttpHandler();
//		HttpServerExchange exchange = buildMockExchange();
//	
//		HandlerImpl impl = new HandlerImpl(wrapper, handler);
//		ThreadTraceData[] data =  SimpleAppender.getSingleton().runAndGetThreadTraceOutput(() -> { 
//			impl.handleRequest(exchange);
//			
//			// Now add Session attribute that matches the trigger.
//			addCookie(exchange, "userName", "Dave");
//			impl.handleRequest(exchange);
//		});	
//		
//		assertEquals("Should have gotten 1 thread trace", 1, data.length);
//	}
//
//	public void testBuildNDC() throws Exception {
//		HttpServerExchange exchange = buildMockExchange("/mycontext/myservlet");
//		exchange.setQueryString("userName=dave&password=frog");
//		InetSocketAddress source = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);
//		exchange.setSourceAddress(source);
//		exchange.getRequestHeaders().add(new HttpString("X-Forwarded-For"), "172.0.0.1");
//		addCookie(exchange, "cookieA","cA");
//		addCookie(exchange, "cookieB","cB");
//		addCookie(exchange, "cookieC","cC");
//		addSessionAttribute(exchange, "sessA", "sA");
//		addSessionAttribute(exchange, "sessB", "sB");
//		addSessionAttribute(exchange, "sessC", "sC");
//		
//		String[] cookiesToDisplay = new String[]{"cookieA", "cookieC"};
//		String[] sessionsToDisplay = new String[]{"sessA", "sessC"};
//		
//		String ndc = HandlerImpl.buildNDC(exchange, true, false, null, null);
//		assertEquals("ndc with url only", "/mycontext/myservlet?userName=dave&password=*******", ndc);
//		
//		ndc = HandlerImpl.buildNDC(exchange, false, true, null, null);
//		assertEquals("ndc with client info only", "localhost/127.0.0.1[172.0.0.1]", ndc);
//
//		ndc = HandlerImpl.buildNDC(exchange, false, false, cookiesToDisplay, null);
//		assertEquals("ndc with cookies only", "cookieA:cA cookieC:cC", ndc);
//
//		ndc = HandlerImpl.buildNDC(exchange, false, false, null, sessionsToDisplay);
//		assertEquals("ndc with session attributes only", "sessA:sA sessC:sC", ndc);
//
//		ndc = HandlerImpl.buildNDC(exchange, true, true, cookiesToDisplay, sessionsToDisplay);
//		assertEquals("ndc with all options", "/mycontext/myservlet?userName=dave&password=******* localhost/127.0.0.1[172.0.0.1] cookieA:cA cookieC:cC sessA:sA sessC:sC", ndc);
//	}
//
//	private void addThreadTraceTrigger(Trigger trigger) throws Exception {
//		ThreadTraceConfig ttConfig = new ThreadTraceConfig();
//		if (trigger != null) {
//			ttConfig.setTriggers(new Trigger[]{trigger});
//		}
//		ttConfig.addAppender(config.getAppenderForName("SIMPLE"));
//		config.addThreadTraceConfig("WebRequest", ttConfig);
//		
//		PerfMon.configure(config);
//	}
}

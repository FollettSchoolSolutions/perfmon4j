package web.org.perfmon4j.extras.wildfly8;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.mockito.Mockito;
import org.perfmon4j.Appender;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceConfig.Trigger;
import org.xnio.OptionMap;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.Cookie;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.util.HttpString;
import junit.framework.TestCase;

public class HandlerImplTest extends TestCase {

	public HandlerImplTest(String name) {
		super(name);
	}

	private PerfMonConfiguration config;
	
	protected void setUp() throws Exception {
		super.setUp();
		setUpSimpleAppender("WebRequest");
	}
	
	private void setUpSimpleAppender(String monitorName) throws Exception {
		config = new PerfMonConfiguration();
		config.defineAppender("SIMPLE", SimpleAppender.class.getName(), "100 ms");
		config.defineMonitor(monitorName);
		config.attachAppenderToMonitor(monitorName, "SIMPLE", ".");
		
		SimpleAppender.reset();
		PerfMon.configure(config);
		Thread.sleep(100);
	}
	
	protected void tearDown() throws Exception {
		PerfMon.configure();
		
		super.tearDown();
		Thread.sleep(100);
	}
	
	public void testMaskPasswordInQueryString() {
		assertEquals("Password only parameter", "?password=*******", HandlerImpl.maskPassword("?password=dave"));
		assertEquals("Password second parameter", "?user=dave&password=*******", HandlerImpl.maskPassword("?user=dave&password=dave"));
		assertEquals("Password middle parameter", "?user=dave&password=*******&time=now", HandlerImpl.maskPassword("?user=dave&password=dave&time=now"));
		assertEquals("Multiple password parameters", "?password=*******&password=*******&password=*******&password=*******", HandlerImpl.maskPassword("?password=dave&password=this is a test&password=&password=t"));
		assertEquals("Password in value is ignored", "?word=password", HandlerImpl.maskPassword("?word=password"));
		assertEquals("Work with ; separator", "?password=*******;x=y", HandlerImpl.maskPassword("?password=dave;x=y"));
	}
	
	public static class SimpleAppender extends Appender {
		static SimpleAppender appender = null; 
		
		public SimpleAppender(AppenderID id) {
			super(id);
			appender = this;
		}
	
		static void reset() {
			completions = 0;
			hits = 0;
			threadTraces = 0;
			
		}
		
		static void flushOutput() throws Exception {
			if (appender != null) {
				Thread.sleep(100);
				appender.flush();
				Thread.sleep(100);
			}
		}

		static int completions = 0;
		static int hits = 0;
		static int threadTraces = 0;
		
		@Override
		public void outputData(PerfMonData data) {
			if (data instanceof IntervalData) {
				IntervalData d = (IntervalData)data;
				completions += d.getTotalCompletions();
				hits += d.getTotalHits();
			} else {
				threadTraces++;
			}
			System.out.println(data.toAppenderString());			
		}
	}
	
	public void testSimpleMonitor() throws Exception {
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		HttpHandler handler = buildMockHttpHandler();
		HttpServerExchange exchange = buildMockExchange();
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(exchange);
		SimpleAppender.flushOutput();
		
		assertEquals(1, SimpleAppender.completions);
		assertEquals("Should of had 1 hit", 1, SimpleAppender.hits);		
	}
	
	public void testAbortTimerOnImageResponse() throws Exception {
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		wrapper.setAbortTimerOnImageResponse(true);
		
		HttpHandler handler = buildMockHttpHandler();
		HttpServerExchange exchange = buildMockExchange();
		exchange.getResponseHeaders().add(new HttpString("Content-Type"), "image/jpeg");
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(exchange);
		
		SimpleAppender.flushOutput();
		
		assertEquals("Should of had 1 hit", 1, SimpleAppender.hits);
		assertEquals("Completion should have been aborted", 0, SimpleAppender.completions);
	}
	
	public void testAbortTimerOnRedirect() throws Exception {
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		wrapper.setAbortTimerOnRedirect(true);
		
		HttpHandler handler = buildMockHttpHandler();
		HttpServerExchange exchange = buildMockExchange();
		exchange.setResponseCode(303);
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(exchange);
		
		SimpleAppender.flushOutput();
		
		assertEquals("Should of had 1 hit", 1, SimpleAppender.hits);
		assertEquals("Completion should have been aborted", 0, SimpleAppender.completions);
	}

	public void testAbortTimerOnURLPattern() throws Exception {
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		wrapper.setAbortTimerOnURLPattern("/dave");
		
		HttpHandler handler = buildMockHttpHandler();
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(buildMockExchange("/dave"));
		impl.handleRequest(buildMockExchange("/notdave"));
		
		SimpleAppender.flushOutput();
		
		assertEquals("Both should have hit", 2, SimpleAppender.hits);
		assertEquals("Only the request that did not match pattern should have been flagged complete", 
				1, SimpleAppender.completions);
	}
	
	
	public void testServletPathTransformationPattern() throws Exception {
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		wrapper.setServletPathTransformationPattern("/context/mycontext/ => /");
		setUpSimpleAppender("WebRequest.rest");
		
		HttpHandler handler = buildMockHttpHandler();
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(buildMockExchange("/context/mycontext/rest"));
		
		SimpleAppender.flushOutput();
		
		assertEquals("Should have been captured in WebRequest.rest category", 
				1, SimpleAppender.completions);
	}
	
	
	public void testSkipTimerOnURLPattern() throws Exception {
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		wrapper.setSkipTimerOnURLPattern("/dave");
		
		HttpHandler handler = buildMockHttpHandler();
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(buildMockExchange("/dave"));
		impl.handleRequest(buildMockExchange("/notdave"));
		
		SimpleAppender.flushOutput();
		
		assertEquals("Only ther request that did not match pattern should have been flagged as a hit", 
				1, SimpleAppender.hits);
		assertEquals("Only the request that did not match pattern should have been flagged complete", 
				1, SimpleAppender.completions);
	}
	
	public void testThreadTraceHttpRequestTrigger() throws Exception {
		Trigger trigger = new ThreadTraceConfig.HTTPRequestTrigger("userName", "Dave");
		addThreadTraceTrigger(trigger);
		
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		HttpHandler handler = buildMockHttpHandler();
		HttpServerExchange exchange = buildMockExchange();
		
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		
		impl.handleRequest(exchange);
		
		// Now add a request parameter that matches the trigger.
		exchange.addQueryParam("userName", "Dave");
		impl.handleRequest(exchange);
		
		SimpleAppender.flushOutput();
		
		assertEquals("Should have gotten 1 thread trace", 1, SimpleAppender.threadTraces);
	}
	
	private void addSessionAttribute(HttpServerExchange exchange, String name, String value) {
		ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
		if (context == null) {
			context = Mockito.mock(ServletRequestContext.class);
		
			HttpSessionImpl session = Mockito.mock(HttpSessionImpl.class);
			Mockito.when(context.getSession()).thenReturn(session);
			exchange.putAttachment(ServletRequestContext.ATTACHMENT_KEY, context);
		}
		Mockito.when(context.getSession().getAttribute(name)).thenReturn(value);
	}
	
	public void testThreadTraceHttpSessionTrigger() throws Exception {
		Trigger trigger = new ThreadTraceConfig.HTTPSessionTrigger("userName", "Dave");
		addThreadTraceTrigger(trigger);
		
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		HttpHandler handler = buildMockHttpHandler();
		HttpServerExchange exchange = buildMockExchange();
		
	
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(exchange);
		
		// Now add Session attribute that matches the trigger.
		addSessionAttribute(exchange, "userName", "Dave");
		impl.handleRequest(exchange);
		
		SimpleAppender.flushOutput();
		
		assertEquals("Should have gotten 1 thread trace", 1, SimpleAppender.threadTraces);
	}
	

	private void addCookie(HttpServerExchange exchange, String name, String value) throws Exception {
		Cookie cookie = Mockito.mock(Cookie.class);
		Mockito.when(cookie.getValue()).thenReturn(value);
		
		exchange.getRequestCookies().put(name, cookie);
		
	}
	
	public void testThreadTraceHttpCookieTrigger() throws Exception {
		Trigger trigger = new ThreadTraceConfig.HTTPCookieTrigger("userName", "Dave");
		addThreadTraceTrigger(trigger);
		
		PerfmonHandlerWrapper wrapper = new PerfmonHandlerWrapper();
		HttpHandler handler = buildMockHttpHandler();
		HttpServerExchange exchange = buildMockExchange();
	
		HandlerImpl impl = new HandlerImpl(wrapper, handler);
		impl.handleRequest(exchange);
		
		// Now add Session attribute that matches the trigger.
		addCookie(exchange, "userName", "Dave");
		impl.handleRequest(exchange);
		
		SimpleAppender.flushOutput();
		
		assertEquals("Should have gotten 1 thread trace", 1, SimpleAppender.threadTraces);
	}

	public void testBuildNDC() throws Exception {
		HttpServerExchange exchange = buildMockExchange("/mycontext/myservlet");
		exchange.setQueryString("userName=dave&password=frog");
		InetSocketAddress source = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);
		exchange.setSourceAddress(source);
		exchange.getRequestHeaders().add(new HttpString("X-Forwarded-For"), "172.0.0.1");
		addCookie(exchange, "cookieA","cA");
		addCookie(exchange, "cookieB","cB");
		addCookie(exchange, "cookieC","cC");
		addSessionAttribute(exchange, "sessA", "sA");
		addSessionAttribute(exchange, "sessB", "sB");
		addSessionAttribute(exchange, "sessC", "sC");
		
		String[] cookiesToDisplay = new String[]{"cookieA", "cookieC"};
		String[] sessionsToDisplay = new String[]{"sessA", "sessC"};
		
		String ndc = HandlerImpl.buildNDC(exchange, true, false, null, null);
		assertEquals("ndc with url only", "/mycontext/myservlet?userName=dave&password=*******", ndc);
		
		ndc = HandlerImpl.buildNDC(exchange, false, true, null, null);
		assertEquals("ndc with client info only", "localhost/127.0.0.1[172.0.0.1]", ndc);

		ndc = HandlerImpl.buildNDC(exchange, false, false, cookiesToDisplay, null);
		assertEquals("ndc with cookies only", "cookieA:cA cookieC:cC", ndc);

		ndc = HandlerImpl.buildNDC(exchange, false, false, null, sessionsToDisplay);
		assertEquals("ndc with session attributes only", "sessA:sA sessC:sC", ndc);

		ndc = HandlerImpl.buildNDC(exchange, true, true, cookiesToDisplay, sessionsToDisplay);
		assertEquals("ndc with all options", "/mycontext/myservlet?userName=dave&password=******* localhost/127.0.0.1[172.0.0.1] cookieA:cA cookieC:cC sessA:sA sessC:sC", ndc);
	}
	
	
	private HttpHandler buildMockHttpHandler() {
		HttpHandler handler = Mockito.mock(HttpHandler.class);
		
		return handler;
	}
	
	private HttpServerExchange buildMockExchange() {
		return buildMockExchange("/test");
	}
	
	private HttpServerExchange buildMockExchange(String requestPath) {
		ServerConnection conn = Mockito.mock(ServerConnection.class);
		OptionMap map = OptionMap.builder().getMap();
		Mockito.when(conn.getUndertowOptions()).thenReturn(map);
		
		
		HttpServerExchange result = new HttpServerExchange(conn);
		result.setRequestPath(requestPath);
		
		return result;
	}

	private void addThreadTraceTrigger(Trigger trigger) throws Exception {
		ThreadTraceConfig ttConfig = new ThreadTraceConfig();
		if (trigger != null) {
			ttConfig.setTriggers(new Trigger[]{trigger});
		}
		ttConfig.addAppender(config.getAppenderForName("SIMPLE"));
		config.addThreadTraceConfig("WebRequest", ttConfig);
		
		PerfMon.configure(config);
		Thread.sleep(100);
	}
}

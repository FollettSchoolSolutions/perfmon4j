package web.org.perfmon4j.extras.wildfly8;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HttpString;
import junit.framework.TestCase;

import org.mockito.Mockito;
import org.perfmon4j.Appender;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.ThreadTraceConfig;
import org.perfmon4j.ThreadTraceConfig.Trigger;

public class HandlerImplTest extends TestCase {

	public HandlerImplTest(String name) {
		super(name);
	}

	private PerfMonConfiguration config;
	
	protected void setUp() throws Exception {
		super.setUp();
		config = new PerfMonConfiguration();
		config.defineAppender("SIMPLE", SimpleAppender.class.getName(), "100 ms");
		config.defineMonitor("WebRequest");
		config.attachAppenderToMonitor("WebRequest", "SIMPLE", ".");
		
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
	
	private HttpHandler buildMockHttpHandler() {
		HttpHandler handler = Mockito.mock(HttpHandler.class);
		
		return handler;
	}
	
	private HttpServerExchange buildMockExchange() {
		return buildMockExchange("/test");
	}
	
	private HttpServerExchange buildMockExchange(String requestPath) {
		ServerConnection conn = Mockito.mock(ServerConnection.class); 
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

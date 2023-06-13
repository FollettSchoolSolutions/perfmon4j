package web.org.perfmon4j.extras.quarkus.filter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.genericfilter.FilterParamsVO;
import web.org.perfmon4j.extras.genericfilter.GenericFilter;

public class PerfmonHandler extends GenericFilter implements Handler<RoutingContext>{
	private final Logger logger = LoggerFactory.getLogger(PerfmonHandler.class);

	public PerfmonHandler() {
		super(new FilterParamsVO());
	}

	@Override
	public void handle(RoutingContext event) {
		Request request = new Request(event);
		Response response = new Response(event);
		
		PerfmonRequestContextWrapper wrapper = new PerfmonRequestContextWrapper(request, response);
		event.addBodyEndHandler(wrapper::end);
		
//		ServerTracingFilter
		
		RequestChain chain = new RequestChain(event);
//		Request request = new Request(event);
//		Response response = new Response(event);
		
		try {
			this.handleRequest(request, response, chain);
		} catch (Exception ex) {
			logger.error("Unexpected exception handling request", ex);
		}
	}

	@Override
	protected void logInfo(String value) {
		logger.info(value);
	}

	@Override
	protected void logInfo(String value, Exception ex) {
		logger.info(value, ex);
	}
	
	public class PerfmonRequestContextWrapper {
		private final Request request;
		private final Response response;
		private long startTime;
		
		PerfmonRequestContextWrapper(Request request, Response response) {
			this.request = request;
			this.response = response;
			this.startTime = System.currentTimeMillis();
		}
		
		public void start() {
		}
		
		public void end(Object obj) {
			System.out.println("DONE!!!! - " + (System.currentTimeMillis() - startTime));
		}
	}
}

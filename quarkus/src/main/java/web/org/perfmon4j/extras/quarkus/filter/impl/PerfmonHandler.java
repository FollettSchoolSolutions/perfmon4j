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
		
		AsyncFinishRequestCallback callback = this.startAsyncRequest(request, response);
		if (callback != null) {
			event.addBodyEndHandler(callback::finishRequest);
		}
		event.next();
	}

	@Override
	protected void logInfo(String value) {
		logger.info(value);
	}

	@Override
	protected void logInfo(String value, Exception ex) {
		logger.info(value, ex);
	}
}

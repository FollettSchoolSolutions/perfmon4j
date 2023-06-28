package web.org.perfmon4j.extras.quarkus.filter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.genericfilter.FilterParamsVO;
import web.org.perfmon4j.extras.genericfilter.GenericFilter;
import web.org.perfmon4j.extras.quarkus.filter.impl.vertx.Request;
import web.org.perfmon4j.extras.quarkus.filter.impl.vertx.Response;


public class PerfmonHandler extends GenericFilter implements Handler<RoutingContext>{
	private final Logger logger = LoggerFactory.getLogger(PerfmonHandler.class);

	
	/**
	 * Going to need to keep working on this.  I would prefer to intercept at the
	 * vertx handler layer.  However it does not appear to work properly
	 * for SYNC requests.  
	 * 
	 * It seems the ContextPropegation is not fully formed at this point.
	 */
	private final boolean enabled = false;
	
	public PerfmonHandler() {
		super(new FilterParamsVO());
	}

	@Override
	public void handle(RoutingContext event) {
		if (enabled) {
			Request request = new Request(event);
			Response response = new Response(event);
			
			final String requestContext = TracingContextProvider.REQUEST_CONTEXT.get().initContext();
			AsyncFinishRequestCallback callback = this.startAsyncRequest(request, requestContext);
			if (callback != null) {
				event.addEndHandler()
					.andThen((E) -> {
						callback.finishRequest(response);
						TracingContextProvider.REQUEST_CONTEXT.get().clearContext();
					});
			}
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

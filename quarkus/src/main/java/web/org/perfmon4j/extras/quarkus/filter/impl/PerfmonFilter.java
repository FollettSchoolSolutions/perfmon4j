package web.org.perfmon4j.extras.quarkus.filter.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import web.org.perfmon4j.extras.genericfilter.FilterParamsVO;
import web.org.perfmon4j.extras.genericfilter.GenericFilter;
import web.org.perfmon4j.extras.quarkus.filter.impl.container.Request;
import web.org.perfmon4j.extras.quarkus.filter.impl.container.Response;


@Provider
//@PreMatching
//@Priority(Priorities.USER)
public class PerfmonFilter extends GenericFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private final Logger logger = LoggerFactory.getLogger(PerfmonFilter.class);

	public PerfmonFilter() {
		super(new FilterParamsVO());
	}

	@Override
	protected void logInfo(String value) {
		logger.info(value);
	}

	@Override
	protected void logInfo(String value, Exception ex) {
		logger.info(value, ex);
	}

	private static final String ASYNC_CALLBACK_PROPERTY = "PerfMon4j.AsyncCallback";
	
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Request request = new Request(requestContext);
		final String reactiveContext = TracingContextProvider.REQUEST_CONTEXT.get().initContext();
	

		AsyncFinishRequestCallback callback = this.startAsyncRequest(request, reactiveContext);
		requestContext.setProperty(ASYNC_CALLBACK_PROPERTY, callback);
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		
		Response response = new Response(responseContext);
		
		AsyncFinishRequestCallback callback = (AsyncFinishRequestCallback)requestContext.getProperty(ASYNC_CALLBACK_PROPERTY);
		callback.finishRequest(response);
	}
}

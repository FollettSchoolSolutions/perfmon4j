package web.org.perfmon4j.extras.quarkus.filter.impl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import web.org.perfmon4j.extras.genericfilter.GenericFilter;
import web.org.perfmon4j.extras.genericfilter.GenericFilter.AsyncFinishRequestCallback;
import web.org.perfmon4j.extras.quarkus.filter.impl.container.Request;
import web.org.perfmon4j.extras.quarkus.filter.impl.container.Response;

@Provider
//@PreMatching
//@Priority(Priorities.USER)
public class PerfmonFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private static final Logger logger = LoggerFactory.getLogger(PerfmonFilter.class);
	private final QuarkusFilterLoader loader;

	public PerfmonFilter() {
		loader = new QuarkusFilterLoader();
		loader.scheduleLoad();
	}
	
	private static final String ASYNC_CALLBACK_PROPERTY = "PerfMon4j.AsyncCallback";
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Request request = new Request(requestContext);
		GenericFilter filter = loader.getGenericFilterIfInitialized();
		if (filter != null) {
			final String reactiveContext = TracingContextProvider.REQUEST_CONTEXT.get().initContext();

			GenericFilter.AsyncFinishRequestCallback callback = filter.startAsyncRequest(request, reactiveContext);
			requestContext.setProperty(ASYNC_CALLBACK_PROPERTY, callback);
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		
		if (logger.isDebugEnabled()) {
			logger.debug("responseContext.class=" + responseContext.getClass());
		}
		
		GenericFilter.AsyncFinishRequestCallback callback = (AsyncFinishRequestCallback)requestContext.getProperty(ASYNC_CALLBACK_PROPERTY);
		if (callback != null) {
			Response response = new Response(responseContext);
			callback.finishRequest(response);
		}
	}
}

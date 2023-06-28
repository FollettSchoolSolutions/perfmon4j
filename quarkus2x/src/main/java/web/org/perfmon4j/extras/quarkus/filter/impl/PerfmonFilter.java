package web.org.perfmon4j.extras.quarkus.filter.impl;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import web.org.perfmon4j.extras.genericfilter.GenericFilter;
import web.org.perfmon4j.extras.genericfilter.GenericFilter.AsyncFinishRequestCallback;
import web.org.perfmon4j.extras.quarkus.filter.impl.container.Request;
import web.org.perfmon4j.extras.quarkus.filter.impl.container.Response;

@Provider
//@PreMatching
//@Priority(Priorities.USER)
public class PerfmonFilter implements ContainerRequestFilter, ContainerResponseFilter {
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
		GenericFilter.AsyncFinishRequestCallback callback = (AsyncFinishRequestCallback)requestContext.getProperty(ASYNC_CALLBACK_PROPERTY);
		if (callback != null) {
			Response response = new Response(responseContext);
			callback.finishRequest(response);
		}
	}
}

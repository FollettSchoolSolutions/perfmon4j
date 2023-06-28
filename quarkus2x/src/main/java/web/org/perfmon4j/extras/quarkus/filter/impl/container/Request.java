package web.org.perfmon4j.extras.quarkus.filter.impl.container;

import javax.ws.rs.container.ContainerRequestContext;

import web.org.perfmon4j.extras.genericfilter.HttpRequest;

public class Request implements HttpRequest {
	private final ContainerRequestContext request;
	
	public Request(ContainerRequestContext request) {
		this.request = request;
	}
	
	@Override
	public String getServletPath() {
		return request.getUriInfo().getPath();
	}

	@Override
	public String getMethod() {
		return request.getMethod();
	}

	@Override
	public String getQueryString() {
		return request.getUriInfo().getRequestUri().getQuery();
	}
}

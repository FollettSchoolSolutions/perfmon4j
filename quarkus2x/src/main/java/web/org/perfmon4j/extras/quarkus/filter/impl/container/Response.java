package web.org.perfmon4j.extras.quarkus.filter.impl.container;

import javax.ws.rs.container.ContainerResponseContext;

import web.org.perfmon4j.extras.genericfilter.HttpResponse;

public class Response implements HttpResponse {
	private final ContainerResponseContext response;

	public Response(ContainerResponseContext response) {
		this.response = response;
	}
	
	@Override
	public int getStatus() {
		return response.getStatus();
	}

	@Override
	public String getHeader(String name) {
		return response.getHeaderString(name);
	}
}

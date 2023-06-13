package web.org.perfmon4j.extras.quarkus.filter.impl;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.genericfilter.HttpResponse;




class Response implements HttpResponse {
	private final HttpServerResponse response;

	Response(RoutingContext event) {
		this.response = event.response();
	}

	@Override
	public int getStatus() {
		return response.getStatusCode();
	}

	@Override
	public String getHeader(String name) {
		return response.headers().get(name);
	}
}

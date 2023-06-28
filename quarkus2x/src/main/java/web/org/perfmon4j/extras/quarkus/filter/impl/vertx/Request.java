package web.org.perfmon4j.extras.quarkus.filter.impl.vertx;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.genericfilter.HttpRequest;

public class Request implements HttpRequest {
	private final HttpServerRequest request;
	
	public Request(RoutingContext event) {
		this.request = event.request();
	}
	
	@Override
	public String getServletPath() {
		return request.path();
	}

	@Override
	public String getMethod() {
		return request.method().toString();
	}

	@Override
	public String getQueryString() {
		return request.query();
	}
}

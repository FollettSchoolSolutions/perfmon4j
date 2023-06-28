package web.org.perfmon4j.extras.quarkus.filter.impl.vertx;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.genericfilter.HttpResponse;




public class Response implements HttpResponse {
	private final HttpServerResponse response;

	public Response(RoutingContext event) {
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

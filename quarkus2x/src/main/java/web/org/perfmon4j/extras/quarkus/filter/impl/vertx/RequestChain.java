package web.org.perfmon4j.extras.quarkus.filter.impl.vertx;

import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.genericfilter.HttpRequest;
import web.org.perfmon4j.extras.genericfilter.HttpRequestChain;
import web.org.perfmon4j.extras.genericfilter.HttpResponse;

public class RequestChain implements HttpRequestChain {
	private final RoutingContext event;
	
	public RequestChain(RoutingContext event) {
		this.event = event;
	}
	
	@Override
	public void next(HttpRequest request, HttpResponse response, HttpRequestChain chain) {
		event.next();
	}
}

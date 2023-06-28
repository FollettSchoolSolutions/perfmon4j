package web.org.perfmon4j.extras.quarkus.filter;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import web.org.perfmon4j.extras.quarkus.filter.impl.PerfmonHandler;



@Singleton
public class PerfmonRouteFilter {
	
	public void init(@Observes Filters filters) {
		filters.register(new PerfmonRouteFilter().getHandler(), 0);
	}

	public Handler<RoutingContext> getHandler() {
		return new PerfmonHandler();
	}
}
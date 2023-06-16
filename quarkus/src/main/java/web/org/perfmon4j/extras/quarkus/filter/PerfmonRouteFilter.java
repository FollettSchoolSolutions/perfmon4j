package web.org.perfmon4j.extras.quarkus.filter;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
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
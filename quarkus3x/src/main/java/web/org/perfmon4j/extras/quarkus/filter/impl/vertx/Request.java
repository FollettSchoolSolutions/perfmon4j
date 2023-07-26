package web.org.perfmon4j.extras.quarkus.filter.impl.vertx;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.http.Cookie;
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

	@Override
	public List<String> getQueryParameter(String name) {
		List<String> result = null;
		
		String value = request.getParam(name);
		if (value != null) {
			result = new ArrayList<>();
			result.add(value);
		}
		return result;
	}

	@Override
	public Object getSessionAttribute(String name) {
		return null;
	}

	@Override
	public String getCookieValue(String name) {
		Cookie cookie = request.getCookie(name);
		return cookie != null ? cookie.getValue() : null;
	}
}

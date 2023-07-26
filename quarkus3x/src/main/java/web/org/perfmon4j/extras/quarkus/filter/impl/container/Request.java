package web.org.perfmon4j.extras.quarkus.filter.impl.container;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedMap;
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
	
	@Override
	public List<String> getQueryParameter(String name) {
		List<String> result = null;
		
		MultivaluedMap<String, String> parametersMap = request.getUriInfo().getQueryParameters();
		result = parametersMap.get(name); 
		
		return result;	
	}
	
	@Override
	public Object getSessionAttribute(String name) {
		return null;	
	}
	
	@Override
	public String getCookieValue(String name) {
		String result = null;
		
		Map<String, Cookie> cookiesMap = request.getCookies();
		
		if (cookiesMap != null) {
			Cookie cookie = cookiesMap.get(name);
			if (cookie != null) {
				result = cookie.getValue();
			}
		}
		return result;
	}
}

package web.org.perfmon4j.extras.quarkus.filter.impl.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerResponseContext;
import web.org.perfmon4j.extras.genericfilter.HttpResponse;

public class Response implements HttpResponse {
	private static final Logger logger = LoggerFactory.getLogger(Response.class); 
	private final ContainerResponseContext response;

	public Response(ContainerResponseContext response) {
		this.response = response;
	}
	
	@Override
	public int getStatus() {
		try {
			return response.getStatus();
		} catch (ClassCastException ex) {
			logger.warn("Unable to get http status code from response -- returning -1.  Exception:  " + ex.getMessage());
			return -1;
		}
	}

	@Override
	public String getHeader(String name) {
		try {
			return response.getHeaderString(name);
		} catch (ClassCastException ex) {
			logger.warn("Unable to retrieve header \"" + name + "\" from response -- returing null.  Exception: " + ex.getMessage());
			return null;
		}
	}
}

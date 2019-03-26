package demo;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException>{

	@Context
	private HttpHeaders headers;
	
	@Override
	public Response toResponse(RuntimeException ex) {
		return Response.status(Status.BAD_REQUEST)
			.entity(ex.getMessage())
			.type(headers.getMediaType())
			.build();
	}
}

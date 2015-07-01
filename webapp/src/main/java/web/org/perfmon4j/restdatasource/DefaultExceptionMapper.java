package web.org.perfmon4j.restdatasource;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {
	
	@Override
	public Response toResponse(Throwable exception) {
		// In response to an exception, return JSON message.
		ExceptionValueObject vo = new ExceptionValueObject(exception);
		return Response.status(vo.getStatusCode()).entity(vo).build();
	}
	
	public static final class ExceptionValueObject {
		private String status;
		private int statusCode;
		private String message;

		public ExceptionValueObject() {
		}
		
		@SuppressWarnings("deprecation")
		private ExceptionValueObject(Throwable th) {
			if (th instanceof WebApplicationException) {
				Response r = ((WebApplicationException)th).getResponse();
				status = r.getStatusInfo().toString();
				statusCode = r.getStatus();
			} else if (th instanceof NotFoundException) {
				Status s = Status.NOT_FOUND;
				status = s.toString();
				statusCode = s.getStatusCode();
			} else {
				Status s = Status.INTERNAL_SERVER_ERROR;
				status = s.toString();
				statusCode = s.getStatusCode();
			}
			message = th.getMessage();
		}
		
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public int getStatusCode() {
			return statusCode;
		}
		public void setStatusCode(int statusCode) {
			this.statusCode = statusCode;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
	}
}

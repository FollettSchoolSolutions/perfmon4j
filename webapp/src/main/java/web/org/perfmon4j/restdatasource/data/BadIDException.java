package web.org.perfmon4j.restdatasource.data;

import javax.ws.rs.BadRequestException;

public class BadIDException extends BadRequestException {
	private static final long serialVersionUID = 1L;
	
	public BadIDException(String message) {
		super(message);
	}
}

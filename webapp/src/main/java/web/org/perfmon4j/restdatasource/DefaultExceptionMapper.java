/*
 *	Copyright 2015 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

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

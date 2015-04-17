package org.perfmon4j.restdatasource;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.perfmon4j.restdatasource.data.Category;
import org.perfmon4j.restdatasource.data.Database;
import org.perfmon4j.restdatasource.data.Field;
import org.perfmon4j.restdatasource.data.MonitoredSystem;

@Path("/message")
public class RestImpl {
	@GET
	@Path("/x/{param}")
	public Response printMessage(@PathParam("param") String msg) {
 
		String result = "Restful example : " + msg;
		
		return Response.status(200).entity(result).build();
	}
	
	@GET
	@Path("/databases")
	@Produces(MediaType.APPLICATION_JSON)
	public Database[] getDatabases() {
		return new Database[]{new Database("production", true), new Database("integration", false), new Database("uat", false)};
	}

	@GET
	@Path("/systems")
	@Produces(MediaType.APPLICATION_JSON)
	public MonitoredSystem[] getSystems() {
		return new MonitoredSystem[]{new MonitoredSystem("DAP-341234", 101), new MonitoredSystem("SHELF-72131", 102), new MonitoredSystem("UD-ADS-21323", 200)};
	}

	@GET
	@Path("/categories")
	@Produces(MediaType.APPLICATION_JSON)
	public Category[] getCategories() {
		List<Field> fields = new ArrayList<Field>();
		String[] aggregationTypes = new String[] {"SUM", "AVERAGE", "MAX", "MIN"};
		
		fields.add(new Field("Throughput", aggregationTypes));
		fields.add(new Field("Average", aggregationTypes));
		fields.add(new Field("Median", aggregationTypes));
		fields.add(new Field("MaxDuration", aggregationTypes));
		fields.add(new Field("MinDuration", aggregationTypes));
		fields.add(new Field("SQLAverage", aggregationTypes));
		fields.add(new Field("SQLMax", aggregationTypes));
		
		Field[] f = fields.toArray(new Field[]{});
		
		return new Category[]{new Category("Interval:WebRequest", f), new Category("Interval:WebRequest.search", f) };
	}

}

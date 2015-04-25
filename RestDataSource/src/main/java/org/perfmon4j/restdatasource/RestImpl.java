package org.perfmon4j.restdatasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.perfmon4j.restdatasource.data.Category;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.Database;
import org.perfmon4j.restdatasource.data.Field;
import org.perfmon4j.restdatasource.data.IntervalTemplate;
import org.perfmon4j.restdatasource.data.MonitoredSystem;
import org.perfmon4j.restdatasource.data.query.advanced.Series;
import org.perfmon4j.restdatasource.data.query.category.IntervalQueryResultElement;
import org.perfmon4j.restdatasource.data.query.category.Result;
import org.perfmon4j.restdatasource.data.query.category.ResultElement;
import org.perfmon4j.restdatasource.data.query.category.SystemResult;

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
		return new Database[]{new Database("production", true, "GRDW-KWST"), 
				new Database("integration", false, "TRXS-GSMR"), 
				new Database("uat", false, "DSTT-WRVS")};
	}

	@GET
	@Path("/systems")
	@Produces(MediaType.APPLICATION_JSON)
	public MonitoredSystem[] getSystems() {
		return new MonitoredSystem[]{new MonitoredSystem("DAP-341234", "GRDW-KWST.101"), 
				new MonitoredSystem("SHELF-72131", "GRDW-KWST.102"), 
				new MonitoredSystem("UD-ADS-21323", "GRDW-KWST.200")};
	}

	@GET
	@Path("/categories")
	@Produces(MediaType.APPLICATION_JSON)
	public Category[] getCategories() {
		List<Field> fields = new ArrayList<Field>();
		
		Field[] f = fields.toArray(new Field[]{});
		
		return new Category[]{new Category("Interval:WebRequest", "Interval"), 
			new Category("Interval:WebRequest.search", "Interval"), 
			new Category("Snapshot:Cache:SearchResults", "Cache"),
			new Category("Snapshot:JVM", "JVM"),
			new Category("Snapshot:GarbageCollection:ConcurrentMarkSweep", "GarbageCollection"),
			
		};
	}

	@GET
	@Path("/categories/template")
	@Produces(MediaType.APPLICATION_JSON)
	public CategoryTemplate[] getCategoryTemplate() {
		return new CategoryTemplate[] {new IntervalTemplate()};
	}
	
	@GET
	@Path("/categories/result")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getCategoryResults() {
		Result result = new Result();
		
		List<ResultElement> elementsA = new ArrayList<ResultElement>();
		List<ResultElement> elementsB = new ArrayList<ResultElement>();
		
		for (int i = 0; i < 3; i++) {
			String dateTime = "20150421_090" + i;
			elementsA.add(buildRandomIntervalElement(dateTime, i));
			elementsB.add(buildRandomIntervalElement(dateTime, i + 10000));
		}

		SystemResult systemA = new SystemResult(); 
		SystemResult systemB = new SystemResult(); 
		
		systemA.setSystemID("HRGW-KVCE.101");
		systemA.setElements(elementsA.toArray(new ResultElement[]{}));
		
		systemB.setSystemID("HRGW-KVCE.200");
		systemB.setElements(elementsB.toArray(new ResultElement[]{}));
		
		result.setCategory("Interval.WebRequest.search");
		result.setSystemResults(new SystemResult[]{systemA, systemB});
		
		return result;
	}

	
	@GET
	@Path("/query/observations")
	@Produces(MediaType.APPLICATION_JSON)
	public org.perfmon4j.restdatasource.data.query.advanced.Result getQueryObservations() {
		org.perfmon4j.restdatasource.data.query.advanced.Result result = new org.perfmon4j.restdatasource.data.query.advanced.Result();
		
		Series seriesA = new Series();
		Series seriesB = new Series();
		Series seriesC = new Series();
		
		seriesA.setAlias("DAP.MaxThreads");
		seriesA.setCategory("Interval.WebRequest.search");
		seriesA.setFieldName("maxActiveThreads");
		seriesA.setSystemID("HRGW-KVCE.101_HRGW-KVCE.429");
		seriesA.setAggregationMethod("SUM");
		
		seriesB.setAlias("DAP.AverageDuration");
		seriesB.setCategory("Interval.WebRequest.search");
		seriesB.setFieldName("avgDuration");
		seriesB.setSystemID("HRGW-KVCE.101_HRGW-KVCE.429");
		seriesB.setAggregationMethod("NATURAL");

		seriesC.setAlias("SHELF.AverageDuration");
		seriesC.setCategory("Interval.WebRequest.search");
		seriesC.setFieldName("avgDuration");
		seriesC.setSystemID("HRGW-KVCE.200");
		
		
		List<String> dateTimes = new ArrayList<String>();
		List<Number> valuesA = new ArrayList<Number>();
		List<Number> valuesB = new ArrayList<Number>();
		List<Number> valuesC = new ArrayList<Number>();
		
		Random randA = new Random(1);
		Random randB = new Random(2);
		Random randC = new Random(3);
		
		for (int i = 0; i < 10; i++) {
			dateTimes.add("20150421_090" + i);
			valuesA.add(Integer.valueOf(randA.nextInt(50)));
			valuesB.add(roundOff((randB.nextDouble() + 0.5) * (randB.nextInt(10) + 1)));
			valuesC.add(roundOff((randC.nextDouble() + 0.5) * (randC.nextInt(10) + 1)));
		}
		valuesC.set(2, null); // Mock series not recording an observation in a period.

		
		seriesA.setValues(valuesA.toArray(new Number[]{}));
		seriesB.setValues(valuesB.toArray(new Number[]{}));
		seriesC.setValues(valuesC.toArray(new Number[]{}));

		result.setDateTime(dateTimes.toArray(new String[]{}));
		result.setSeries(new Series[]{seriesA, seriesB, seriesC});
		
		return result;
	}
	
	
	private ResultElement buildRandomIntervalElement(String dateTime, long seed) {
		IntervalQueryResultElement result = new IntervalQueryResultElement();
		result.setDateTime(dateTime);
		
		Random random = new Random(seed);
		
		result.setAverageDuration(roundOff((random.nextDouble() + 0.5) * random.nextInt(10)));
		result.setMaxActiveThreads(Integer.valueOf(random.nextInt(25) + 2));
		result.setMaxDuration(Integer.valueOf(random.nextInt(50000)));
		result.setMedianDuration(roundOff(random.nextDouble() * random.nextInt(10)));
		result.setMinDuration(Integer.valueOf(random.nextInt(3)));
		result.setSqlAverageDuration(roundOff(random.nextDouble() * random.nextInt(5)));
		result.setSqlLMinDuration(Integer.valueOf(random.nextInt(2)));
		result.setSqlStandardDeviation(roundOff(random.nextDouble() * random.nextInt(3)));
		result.setStandardDeviation(roundOff(random.nextDouble() * random.nextInt(4)));
		result.setThroughputPerMinute(roundOff(random.nextDouble() * result.getMaxActiveThreads() * 10));
		
		return result;
	}
	
	private Double roundOff(double value) {
		return Double.valueOf(Math.round(value * 100)/100.00);
	}
}

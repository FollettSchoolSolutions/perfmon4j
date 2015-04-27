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

package org.perfmon4j.restdatasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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

@Path("/datasource")
public class RestImpl {
	
	@GET
	@Path("/databases")
	@Produces(MediaType.APPLICATION_JSON)
	public Database[] getDatabases() {
		return new Database[]{new Database("production", true, "GRDW-KWST", 5.0), 
				new Database("integration", false, "TRXS-GSMR", 5.0), 
				new Database("uat", false, "DSTT-WRVS", 5.0)};
	}

		
	@GET
	@Path("/databases/{databaseID}/systems")
	@Produces(MediaType.APPLICATION_JSON)
	public MonitoredSystem[] getSystems(@PathParam("databaseID") String databaseID, 
			@QueryParam("timeStart") @DefaultValue("now-480") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now") String timeEnd) {
		return new MonitoredSystem[]{new MonitoredSystem("DAP-341234", "GRDW-KWST.101"), 
				new MonitoredSystem("SHELF-72131", "GRDW-KWST.102"), 
				new MonitoredSystem("UD-ADS-21323", "GRDW-KWST.200")};
	}

///http://127.0.0.1/perfmon4j/datasource/databases/databaseID/categories?systemID=systemID&timeStart=timeStart&timeEnd=timeEnd	
	
	@GET
	@Path("/databases/{databaseID}/categories")
	@Produces(MediaType.APPLICATION_JSON)
	public Category[] getCategories(@PathParam("databaseID") String databaseID, 
			@QueryParam("systemID") String systemID, 
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now") String timeEnd) {
		List<Field> fields = new ArrayList<Field>();
		
		Field[] f = fields.toArray(new Field[]{});
		
		return new Category[]{new Category("Interval:WebRequest", "Interval"), 
			new Category("Interval:WebRequest.search", "Interval"), 
			new Category("Snapshot:Cache:SearchResults", "Cache"),
			new Category("Snapshot:JVM", "JVM"),
			new Category("Snapshot:GarbageCollection:ConcurrentMarkSweep", "GarbageCollection"),
			
		};
	}
	
//	http://127.0.0.1/perfmon4j/datasource/databases/databaseID/categories/templates/template

	@GET
	@Path("/databases/{databaseID}/categories/templates/{template}")
	@Produces(MediaType.APPLICATION_JSON)
	public CategoryTemplate[] getCategoryTemplate(@PathParam("databaseID") String databaseID, 
			@PathParam("template") String template) {
		return new CategoryTemplate[] {new IntervalTemplate()};
	}
	
//	http://127.0.0.1/perfmon4j/datasoure/databases/databaseID/categories/category/observations?systemId=systemId&timeStart=`now-480'&timeEnd=timeEnd&maxObservations=1440
	
	@GET
	@Path("/databases/{databaseID}/categories/{category}/observations")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getCategoryResults(@PathParam("databaseID") String databaseID, 
			@PathParam("category") String category, 
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now")  String timeEnd, 
			@QueryParam("maxObservations")@DefaultValue("1440")  int maxObservations) {
		Result result = new Result();
		
		List<ResultElement> elementsA = new ArrayList<ResultElement>();
		List<ResultElement> elementsB = new ArrayList<ResultElement>();
		
		for (int i = 0; i < 3; i++) {
			String dateTime = "2015-04-21T09:0" + i;
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

//	http://127.0.0.1/perfmon4j/datasource/databases/databaseID/observations?
//		seriesDefinition=seriesDefinition&timeStart=now-480&timeEnd=now&maxObservations=1440&seriesAlias=seriesAlias

	@GET
	@Path("/databases/{databaseID}/observations")
	@Produces(MediaType.APPLICATION_JSON)
	public org.perfmon4j.restdatasource.data.query.advanced.Result getQueryObservations(@PathParam("databaseID") String databaseID, 
			@QueryParam("seriesDefinition") String seriesDefinition,
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now")  String timeEnd, 
			@QueryParam("seriesAlias") @DefaultValue("")  String seriesAlias) {
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
			dateTimes.add("2015-04-21T09:0" + i);
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

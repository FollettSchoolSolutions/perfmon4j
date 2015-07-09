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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Database;
import web.org.perfmon4j.restdatasource.data.MonitoredSystem;
import web.org.perfmon4j.restdatasource.data.query.advanced.AdvancedQueryResult;
import web.org.perfmon4j.restdatasource.data.query.advanced.C3DataResult;
import web.org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import web.org.perfmon4j.restdatasource.data.query.advanced.Series;
import web.org.perfmon4j.restdatasource.data.query.category.IntervalQueryResultElement;
import web.org.perfmon4j.restdatasource.data.query.category.Result;
import web.org.perfmon4j.restdatasource.data.query.category.ResultElement;
import web.org.perfmon4j.restdatasource.dataproviders.GarbageCollectionDataProvider;
import web.org.perfmon4j.restdatasource.dataproviders.IntervalDataProvider;
import web.org.perfmon4j.restdatasource.dataproviders.JVMDataProvider;
import web.org.perfmon4j.restdatasource.util.DataProviderRegistry;
import web.org.perfmon4j.restdatasource.util.DateTimeHelper;
import web.org.perfmon4j.restdatasource.util.ParsedSeriesDefinition;
import web.org.perfmon4j.restdatasource.util.SeriesField;

@Path("/datasource")
public class DataSourceRestImpl {
	private static final Logger logger = LoggerFactory.initLogger(DataSourceRestImpl.class);
	private final DateTimeHelper helper = new DateTimeHelper();
	private static final DataProviderRegistry registry = new DataProviderRegistry();
	
	
	static {
		registry.registerDataProvider(new IntervalDataProvider());
		registry.registerDataProvider(new JVMDataProvider());
		registry.registerDataProvider(new GarbageCollectionDataProvider());
	}

	@GET
	@Path("/databases")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=60)
	public Database[] getDatabases() {
		List<Database> result = new ArrayList<Database>();
		for (RegisteredDatabaseConnections.Database db : RegisteredDatabaseConnections.getAllDatabases()) {
			Database element = new Database();
			
			element.setDatabaseVersion(db.getDatabaseVersion());
			element.setDefault(db.isDefault());
			element.setID(db.getID());
			element.setName(db.getName());
			result.add(element);
		}
		
		return result.toArray(new Database[]{});
	}
		
	@GET
	@Path("/databases/{databaseID}/systems")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=60)
	public MonitoredSystem[] getSystems(@PathParam("databaseID") String databaseID, 
		@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
		@QueryParam("timeEnd") @DefaultValue("now") String timeEnd) {

		Set<MonitoredSystem> result = new HashSet<MonitoredSystem>();
		RegisteredDatabaseConnections.Database db = getDatabase(databaseID);
 		
		Connection conn = null;
		try {
			conn = db.openConnection();
			long start = helper.parseDateTime(timeStart).getTimeForStart();
			long end = helper.parseDateTime(timeEnd).getTimeForEnd();
			for (DataProvider provider : registry.getDataProviders()) {
				Set<MonitoredSystem> r = provider.lookupMonitoredSystems(conn, db, start, end);
				result.addAll(r);
	 		}
		} catch (SQLException e) {
			logger.logDebug("getSystems", e);
			throw new InternalServerErrorException(e);
		} finally {
			JDBCHelper.closeNoThrow(conn);
		}
		
		return result.toArray(new MonitoredSystem[]{});
	}

///http://127.0.0.1/perfmon4j/datasource/databases/databaseID/categories?systemID=systemID&timeStart=timeStart&timeEnd=timeEnd	
	
	@GET
	@Path("/databases/{databaseID}/categories")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=60)
	public Category[] getCategories(@PathParam("databaseID") String databaseID, 
			@QueryParam("systemID") String systemID, 
			@QueryParam("timeStart") @DefaultValue("now-480") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now") String timeEnd) {
		
		
		Set<Category> result = new HashSet<Category>();
		RegisteredDatabaseConnections.Database db = getDatabase(databaseID);
		SystemID ids[] = SystemID.parse(systemID, db.getID());
		Connection conn = null;
		try {
			conn = db.openConnection();
			long start = helper.parseDateTime(timeStart).getTimeForStart();
			long end = helper.parseDateTime(timeEnd).getTimeForEnd();
			for (DataProvider provider : registry.getDataProviders()) {
				Set<Category> r = provider.lookupMonitoredCategories(conn, db, ids, start, end);
				result.addAll(r);
	 		}
		} catch (SQLException e) {
			logger.logDebug("getCategories", e);
			throw new InternalServerErrorException(e);
		} finally {
			JDBCHelper.closeNoThrow(conn);
		}
		
		return result.toArray(new Category[]{});
	}
	
//	http://127.0.0.1/perfmon4j/datasource/databases/databaseID/categories/templates/template

	@GET
	@Path("/databases/{databaseID}/categories/templates/{template}")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=300)
	public CategoryTemplate[] getCategoryTemplate(@PathParam("databaseID") String databaseID, 
			@PathParam("template") String template) {
		
		DataProvider provider = registry.getDataProvider(template);
		if (provider == null) {
			throw new NotFoundException("Category template not found: " + template);
		}
		return new CategoryTemplate[] {provider.getCategoryTemplate()};
	}
	
//	http://127.0.0.1/perfmon4j/datasoure/databases/databaseID/categories/category/observations?systemId=systemId&timeStart=`now-480'&timeEnd=timeEnd&maxObservations=1440
	
	@GET
	@Path("/databases/{databaseID}/categories/{category}/observations")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=60)
	public Result getCategoryResults(@PathParam("databaseID") String databaseID, 
			@PathParam("category") String category, 
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now")  String timeEnd, 
			@QueryParam("maxObservations")@DefaultValue("1440")  int maxObservations) {
		throw new NotImplementedYetException();
		
//		Result result = new Result();
//		
//		List<ResultElement> elementsA = new ArrayList<ResultElement>();
//		List<ResultElement> elementsB = new ArrayList<ResultElement>();
//		
//		for (int i = 0; i < 3; i++) {
//			String dateTime = "2015-04-21T09:0" + i;
//			elementsA.add(buildRandomIntervalElement(dateTime, i));
//			elementsB.add(buildRandomIntervalElement(dateTime, i + 10000));
//		}
//
//		SystemResult systemA = new SystemResult(); 
//		SystemResult systemB = new SystemResult(); 
//		
//		systemA.setSystemID("HRGW-KVCE.101");
//		systemA.setElements(elementsA.toArray(new ResultElement[]{}));
//		
//		systemB.setSystemID("HRGW-KVCE.200");
//		systemB.setElements(elementsB.toArray(new ResultElement[]{}));
//		
//		result.setCategory("Interval.WebRequest.search");
//		result.setSystemResults(new SystemResult[]{systemA, systemB});
//		
//		return result;
	}
	
	
//	http://127.0.0.1/perfmon4j/datasource/databases/databaseID/observations?
//		seriesDefinition=seriesDefinition&timeStart=now-480&timeEnd=now&maxObservations=1440&seriesAlias=seriesAlias

	@GET
	@Path("/databases/{databaseID}/observations")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=60)
	public web.org.perfmon4j.restdatasource.data.query.advanced.AdvancedQueryResult getQueryObservations(@PathParam("databaseID") String databaseID, 
			@QueryParam("seriesDefinition") String seriesDefinition,
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now")  String timeEnd, 
			@QueryParam("seriesAlias") @DefaultValue("")  String seriesAlias) {

		AdvancedQueryResult result = null;
		RegisteredDatabaseConnections.Database db = getDatabase(databaseID);
		databaseID = db.getID(); // Make sure we are using the actual databaseID in case the "default" keyword was used.
		ParsedSeriesDefinition series[] = ParsedSeriesDefinition.parse(seriesDefinition, databaseID);
		
		String aliasNames[] = new String[]{};
		if (seriesAlias != null && !"".equals(seriesAlias)) {
			aliasNames = seriesAlias.split("_");
		}

		// Just work with one of the Series for now....
		Map<String, List<SeriesField>> seriesToProcess = groupFieldsByTemplate(series, aliasNames);

		Connection conn = null;;
		try {
			long start = helper.parseDateTime(timeStart).getTimeForStart();
			long end = helper.parseDateTime(timeEnd).getTimeForEnd();
			
			conn = db.openConnection();
			
			ResultAccumulator accumulator = new ResultAccumulator();
			for (String templateName : seriesToProcess.keySet()) {
				// Accumulate data for each DataProvider template
				DataProvider provider =  registry.getDataProvider(templateName);
				provider.processResults(conn, db, accumulator, seriesToProcess.get(templateName).toArray(new SeriesField[]{}), start, end);
			}
			result = accumulator.buildResults();
		} catch (SQLException ex) {
			throw new InternalServerErrorException(ex);
		} finally {
			JDBCHelper.closeNoThrow(conn);
		}
		
		return result;
	}

	
	@GET
	@Path("/databases/{databaseID}/observations.c3")
	@Produces(MediaType.APPLICATION_JSON)
	@Cache(maxAge=60)
	public C3DataResult getQueryObservationsInC3Format(@PathParam("databaseID") String databaseID, 
			@QueryParam("seriesDefinition") String seriesDefinition,
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now")  String timeEnd, 
			@QueryParam("seriesAlias") @DefaultValue("")  String seriesAlias) {

		AdvancedQueryResult qr =  getQueryObservations(databaseID, seriesDefinition, timeStart, timeEnd, seriesAlias);
		List<Object[]> columns = new ArrayList<Object[]>(); 
		
		
		List<Object> dateTime = new ArrayList<Object>();
		dateTime.addAll(Arrays.asList(qr.getDateTime()));
		dateTime.add(0, "dateTime");
		columns.add(dateTime.toArray());

		for (Series s : qr.getSeries()) {
			List<Object> sList = new ArrayList<Object>();
			sList.addAll(Arrays.asList(s.getValues()));
			sList.add(0, s.getAlias());

			columns.add(sList.toArray());
		}
		C3DataResult result = new C3DataResult();
		result.setColumns(columns.toArray(new Object[][]{}));
		
		return result;
	}
	

	/**
	 * This method will group all of the data series into their respective 
	 * provider templates.
	 * @param series
	 * @return
	 */
	private Map<String, List<SeriesField>> groupFieldsByTemplate(ParsedSeriesDefinition series[], String aliasOverride[]) {
		Map<String, List<SeriesField>> result = new HashMap<String, List<SeriesField>>();
		
	
		int seriesOffset = 0;
		
		
		for (ParsedSeriesDefinition def : series) {
			String seriesAlias = "Series " + Integer.toString(++seriesOffset);
			if (aliasOverride.length >= seriesOffset) {
				seriesAlias = aliasOverride[seriesOffset - 1];
			}
			
			SeriesField field = registry.resolveField(def, seriesAlias);
			String templateName = field.getCategory().getTemplateName();
			
			List<SeriesField> seriesForTemplate = result.get(templateName);
			if (seriesForTemplate == null) {
				seriesForTemplate = new ArrayList<SeriesField>();
				result.put(templateName, seriesForTemplate);
			}
			seriesForTemplate.add(field);
		}
		return result;
	}
	
	private RegisteredDatabaseConnections.Database getDatabase(String databaseID) {
		RegisteredDatabaseConnections.Database db = null;
		if ("default".equals(databaseID)) {
			db = RegisteredDatabaseConnections.getDefaultDatabase();
		} else {
			db = RegisteredDatabaseConnections.getDatabaseByID(databaseID);
		}
		
		if (db == null) {
			throw new NotFoundException("Database not found.  databaseID: " + databaseID);
		}
		return db;
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
	
	public static final class SystemID {
		private static final Pattern pattern = Pattern.compile("(\\w{4}\\-\\w{4})\\.(\\d+)"); 
		private final String databaseID;
		private final long ID;
		
		SystemID(String systemID, String expectedDatabaseID) throws BadRequestException {
			Matcher matcher = pattern.matcher(systemID);
			if (matcher.matches()) {
				databaseID = matcher.group(1);
				ID = Long.parseLong(matcher.group(2));
				if (!expectedDatabaseID.equals(databaseID)) {
					throw new BadRequestException("SystemID must match the specified database(" + expectedDatabaseID + "): " + systemID);
				}
			} else {
				throw new BadRequestException("Invalid SystemID: " + systemID);
			}
		}

		public static SystemID[] parse(String systemIDs[], String expectedDatabaseID) {
			List<SystemID> result = new ArrayList<DataSourceRestImpl.SystemID>();
			
			for (String s : systemIDs) {
				result.add(new SystemID(s, expectedDatabaseID));
			}
			
			return result.toArray(new SystemID[]{});
		}

		/**
		 * Parses a ~ separated list of SystemIDs.
		 * @param systemID
		 * @param expectedDatabaseID
		 * @return
		 */
		public static SystemID[] parse(String systemID, String expectedDatabaseID) {
			List<SystemID> result = new ArrayList<DataSourceRestImpl.SystemID>();
			
			String[] ids = systemID.split("~");
			for (String id: ids) {
				result.add(new SystemID(id, expectedDatabaseID));
			}
			
			return result.toArray(new SystemID[]{});
		}
		
		public static SystemID manualConstructor_TESTONLY(String databaseID, long systemID) {
			return new SystemID(databaseID + "." + systemID, databaseID);
		}
		
		public static String toString(SystemID systems[]) {
			String result = "";
			for (SystemID s : systems) {
				if (!result.isEmpty()) {
					result += "~";
				}
				result += s.toString();
			}
			return result;
		}
		
		
		public String toString() {
			return getDatabaseID() + "." + getID();
		}
		
		public String getDatabaseID() {
			return databaseID;
		}

		public long getID() {
			return ID;
		}
	}
}

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.InternalServerErrorException;
import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.restdatasource.data.Category;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.Database;
import org.perfmon4j.restdatasource.data.IntervalTemplate;
import org.perfmon4j.restdatasource.data.MonitoredSystem;
import org.perfmon4j.restdatasource.data.query.advanced.AdvancedQueryResult;
import org.perfmon4j.restdatasource.data.query.category.IntervalQueryResultElement;
import org.perfmon4j.restdatasource.data.query.category.Result;
import org.perfmon4j.restdatasource.data.query.category.ResultElement;
import org.perfmon4j.restdatasource.data.query.category.SystemResult;
import org.perfmon4j.restdatasource.dataproviders.IntervalDataProvider;
import org.perfmon4j.restdatasource.util.DataProviderRegistry;
import org.perfmon4j.restdatasource.util.DateTimeHelper;
import org.perfmon4j.restdatasource.util.ParsedSeriesDefinition;
import org.perfmon4j.restdatasource.util.ProcessArgsException;
import org.perfmon4j.restdatasource.util.SeriesField;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

@Path("/datasource")
public class RestImpl {
	private static final Logger logger = LoggerFactory.initLogger(RestImpl.class);
	private final DateTimeHelper helper = new DateTimeHelper();
	private static final DataProviderRegistry registry = new DataProviderRegistry();

	static {
		registry.registerDataProvider(new IntervalDataProvider());
	}
	

	@GET
	@Path("/databases")
	@Produces(MediaType.APPLICATION_JSON)
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
	public MonitoredSystem[] getSystems(@PathParam("databaseID") String databaseID, 
		@QueryParam("timeStart") @DefaultValue("now-480") String timeStart,
		@QueryParam("timeEnd") @DefaultValue("now") String timeEnd) {

		RegisteredDatabaseConnections.Database db = getDatabase(databaseID);
		return lookupMonitoredSystems(db, timeStart, timeEnd);
	}
	

///http://127.0.0.1/perfmon4j/datasource/databases/databaseID/categories?systemID=systemID&timeStart=timeStart&timeEnd=timeEnd	
	
	@GET
	@Path("/databases/{databaseID}/categories")
	@Produces(MediaType.APPLICATION_JSON)
	public Category[] getCategories(@PathParam("databaseID") String databaseID, 
			@QueryParam("systemID") String systemID, 
			@QueryParam("timeStart") @DefaultValue("now-480") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now") String timeEnd) {
		
		
		RegisteredDatabaseConnections.Database db = getDatabase(databaseID);
		SystemID ids[] = SystemID.parse(systemID, db.getID());
		
		return lookupMonitoredCategories(db, ids, timeStart, timeEnd);
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
	public org.perfmon4j.restdatasource.data.query.advanced.AdvancedQueryResult getQueryObservations(@PathParam("databaseID") String databaseID, 
			@QueryParam("seriesDefinition") String seriesDefinition,
			@QueryParam("timeStart") @DefaultValue("now-8H") String timeStart,
			@QueryParam("timeEnd") @DefaultValue("now")  String timeEnd, 
			@QueryParam("seriesAlias") @DefaultValue("")  String seriesAlias) {

		RegisteredDatabaseConnections.Database db = getDatabase(databaseID);
		ParsedSeriesDefinition series[] = ParsedSeriesDefinition.parse(seriesDefinition, databaseID);

		AdvancedQueryResult result = new AdvancedQueryResult();

		// Just work with one of the Series for now....
		SeriesField field = registry.resolveField(series[0], "Series 1");
		
		
		
		
				
//		
//		Series seriesA = new Series();
//		Series seriesB = new Series();
//		Series seriesC = new Series();
//		
//		seriesA.setAlias("DAP.MaxThreads");
//		seriesA.setCategory("Interval.WebRequest.search");
//		seriesA.setFieldName("maxActiveThreads");
//		seriesA.setSystemID("HRGW-KVCE.101_HRGW-KVCE.429");
//		seriesA.setAggregationMethod("SUM");
//		
//		seriesB.setAlias("DAP.AverageDuration");
//		seriesB.setCategory("Interval.WebRequest.search");
//		seriesB.setFieldName("avgDuration");
//		seriesB.setSystemID("HRGW-KVCE.101_HRGW-KVCE.429");
//		seriesB.setAggregationMethod("NATURAL");
//
//		seriesC.setAlias("SHELF.AverageDuration");
//		seriesC.setCategory("Interval.WebRequest.search");
//		seriesC.setFieldName("avgDuration");
//		seriesC.setSystemID("HRGW-KVCE.200");
//		
//		
//		List<String> dateTimes = new ArrayList<String>();
//		List<Number> valuesA = new ArrayList<Number>();
//		List<Number> valuesB = new ArrayList<Number>();
//		List<Number> valuesC = new ArrayList<Number>();
//		
//		Random randA = new Random(1);
//		Random randB = new Random(2);
//		Random randC = new Random(3);
//		
//		for (int i = 0; i < 10; i++) {
//			dateTimes.add("2015-04-21T09:0" + i);
//			valuesA.add(Integer.valueOf(randA.nextInt(50)));
//			valuesB.add(roundOff((randB.nextDouble() + 0.5) * (randB.nextInt(10) + 1)));
//			valuesC.add(roundOff((randC.nextDouble() + 0.5) * (randC.nextInt(10) + 1)));
//		}
//		valuesC.set(2, null); // Mock series not recording an observation in a period.
//
//		
//		seriesA.setValues(valuesA.toArray(new Number[]{}));
//		seriesB.setValues(valuesB.toArray(new Number[]{}));
//		seriesC.setValues(valuesC.toArray(new Number[]{}));
//
//		result.setDateTime(dateTimes.toArray(new String[]{}));
//		result.setSeries(new Series[]{seriesA, seriesB, seriesC});
		
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
	
	private String fixupSchema(String schema) {
		return schema == null ? "" : schema + ".";
	}
	
	private MonitoredSystem[] lookupMonitoredSystems(RegisteredDatabaseConnections.Database db, String timeStart, String timeEnd) {
		List<MonitoredSystem> result = new ArrayList<MonitoredSystem>(); 
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String schema = fixupSchema(db.getSchema());
			
			long start = helper.parseDateTime(timeStart).getTimeForStart();
			long end = helper.parseDateTime(timeEnd).getTimeForEnd();
			
			String SQL = "SELECT SystemID, SystemName "
				+ " FROM " + schema + "P4JSystem s "
				+ " WHERE EXISTS (SELECT IntervalID " 
				+ " FROM " + schema + "P4JIntervalData pid WHERE pid.SystemID = s.SystemID "
				+ "	AND pid.EndTime >= ? AND pid.EndTime <= ?)";
			if (logger.isDebugEnabled()) {
				logger.logDebug("getSystems SQL: " + SQL);
			}
			
			conn = db.openConnection();
			stmt = conn.prepareStatement(SQL);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				MonitoredSystem ms = new MonitoredSystem(rs.getString("SystemName"), db.getID() + "." + rs.getLong("SystemID"));
				result.add(ms);
			}
		} catch (SQLException se) {
			logger.logDebug("getSystems", se);
			throw new InternalServerErrorException(se);
		} catch (ProcessArgsException e) {
			logger.logDebug("getSystems", e);
			throw new BadRequestException(e);
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
			JDBCHelper.closeNoThrow(conn);
		}
		
		return result.toArray(new MonitoredSystem[]{});
	}
	

	private String buildInArrayForSystems(SystemID systems[]) {
		StringBuilder builder = new StringBuilder();
		builder.append("( ");

		for (SystemID id: systems) {
			if (builder.length() > 2) {
				builder.append(", ");
			}
			builder.append(id.getID());	
		}
		builder.append(" )");
		
		return builder.toString();
	}
	
	private Category[] lookupMonitoredCategories(RegisteredDatabaseConnections.Database db, SystemID systems[], String timeStart, String timeEnd) {
		List<Category> result = new ArrayList<Category>(); 
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String schema = fixupSchema(db.getSchema());
			
			long start = helper.parseDateTime(timeStart).getTimeForStart();
			long end = helper.parseDateTime(timeEnd).getTimeForEnd();
			
			String SQL = "SELECT CategoryName"
				+ " FROM " + schema + "P4JCategory cat "
				+ " WHERE EXISTS (SELECT IntervalID " 
				+ " FROM " + schema + "P4JIntervalData pid WHERE pid.categoryId = cat.categoryID "
				+ " AND pid.systemID IN " + buildInArrayForSystems(systems)
				+ "	AND pid.EndTime >= ? AND pid.EndTime <= ?)";
			
			if (logger.isDebugEnabled()) {
				logger.logDebug("getIntervalCategories SQL: " + SQL);
			}
			
			conn = db.openConnection();
			stmt = conn.prepareStatement(SQL);
			stmt.setTimestamp(1, new Timestamp(start));
			stmt.setTimestamp(2, new Timestamp(end));
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				Category cat = new Category("Interval." + rs.getString("CategoryName"), "Interval");
				result.add(cat);
			}
		} catch (SQLException se) {
			logger.logDebug("getIntervalCategories", se);
			throw new InternalServerErrorException(se);
		} catch (ProcessArgsException e) {
			logger.logDebug("getIntervalCategories", e);
			throw new BadRequestException(e);
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmt);
			JDBCHelper.closeNoThrow(conn);
		}
		
		return result.toArray(new Category[]{});
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
			List<SystemID> result = new ArrayList<RestImpl.SystemID>();
			
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
			List<SystemID> result = new ArrayList<RestImpl.SystemID>();
			
			String[] ids = systemID.split("~");
			for (String id: ids) {
				result.add(new SystemID(id, expectedDatabaseID));
			}
			
			return result.toArray(new SystemID[]{});
		}
		
		public static String toString(SystemID systems[]) {
			String result = "";
			for (SystemID s : systems) {
				if (result.isEmpty()) {
					result += "~";
				}
				result += s.getDatabaseID() + "." + s.getID();
			}
			return result;
		}
		
		public String getDatabaseID() {
			return databaseID;
		}

		public long getID() {
			return ID;
		}
	}
}

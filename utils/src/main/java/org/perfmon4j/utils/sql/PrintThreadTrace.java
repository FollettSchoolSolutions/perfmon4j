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
package org.perfmon4j.utils.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.perfmon4j.util.JDBCHelper;

public class PrintThreadTrace {
	private static DateFormat ROOT_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:sss:SSS");
	private static DateFormat CHILD_FORMAT = new SimpleDateFormat("HH:mm:sss:SSS");
	
	public static void main(String[] args) {
		if (args.length < 6) {
			printUsage();
		} else {
			String jarFileName = args[0];
			String jdbcURL = args[1];
			String driverClassName = args[2];
			String userName = args[3];
			String password = args[4];
			long targetTraceID = Integer.parseInt(args[5]);
			List<TraceRow> rootList = new ArrayList<TraceRow>();
			Map<Long, TraceRow> directory = new HashMap<Long, TraceRow>(); 
			
			targetTraceID = 356043;
			
			Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;
			try {
				conn = JDBCHelper.createJDBCConnection(new JDBCHelper.DriverCache(), driverClassName, jarFileName, jdbcURL, userName, password);
				
				
				
				String sql = "SELECT "
						+ "	s.SystemName, "
						+ "	c.CategoryName, "
						+ "	tt.TraceRowID, "
						+ "	tt.ParentRowID, "
						+ "	tt.Duration, "
						+ "	tt.SQLDuration, "
						+ "	tt.startTime, "
						+ " tt.endTime "
						+ "FROM P4JThreadTrace tt "
						+ "JOIN P4JSystem s ON s.SystemID = tt.SystemID "
						+ "JOIN P4JCategory c ON c.CategoryID = tt.CategoryID "
						+ "WHERE tt.TraceRowID >= " + targetTraceID + " AND tt.TraceRowID < " + (targetTraceID + 1000) + " "  
						+ "ORDER BY TraceRowID ASC";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					TraceRow row = TraceRow.fromResultSet(rs);
					Long rowID = row.getRowID();
					Long parentRowID = row.getParentRowID();
					
					directory.put(rowID, row);
					if (parentRowID == null) {
						if (rowID.longValue() == targetTraceID) {
							rootList.add(row);
						}
					} else {
						TraceRow parent = directory.get(parentRowID);
						if (parent != null) {
							parent.getChildren().add(row);
						}
					}
				}

				for (TraceRow r : rootList) {
					dumpTrace(r, "");
//					break;
				}
				
				
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				JDBCHelper.closeNoThrow(conn);
			}
		}
	}
	
	
	

	private static void dumpTrace(TraceRow trace, String indention) {
		String start;
		String end;
		boolean isRoot = trace.getParentRowID() == null;
		
		if (isRoot) {
			start = ROOT_FORMAT.format(new Date(trace.getStartTime()));
			end = ROOT_FORMAT.format(new Date(trace.getEndTime()));
		} else {
			start = CHILD_FORMAT.format(new Date(trace.getStartTime()));
			end = CHILD_FORMAT.format(new Date(trace.getEndTime()));
		}
		
		System.out.println(indention + "+" + start + " (" + trace.getDuration() + ") " + trace.getCategoryName());
		for (TraceRow c : trace.getChildren()) {
			dumpTrace(c, indention + "|   ");
		}
		System.out.println(indention + "+" +  end + " " + trace.getCategoryName());
	}
	
	
	public static void printUsage() {
		System.out.println("Usage: [optionalParameters]");
		System.out.println("\tPrintThreadTrace: driverJarFile driverClassName jdbcURL userName password [schemaName=] [systemName=]");
	}
	
}

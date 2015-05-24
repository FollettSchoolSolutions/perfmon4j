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
import java.sql.SQLException;
import java.util.Set;

import org.perfmon4j.RegisteredDatabaseConnections;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.MonitoredSystem;
import org.perfmon4j.restdatasource.data.query.advanced.ResultAccumulator;
import org.perfmon4j.restdatasource.util.SeriesField;

public abstract class DataProvider {
	private final String templateName;
	
	protected DataProvider(String templateName) {
		this.templateName = templateName;
	}
	
	public abstract Set<MonitoredSystem> lookupMonitoredSystems(Connection conn, RegisteredDatabaseConnections.Database database, 
			long startTime, long endTime) throws SQLException;	
	public abstract void processResults(ResultAccumulator accumulator, SeriesField[] fields, long startTime, 
			long endTime) throws SQLException;
	
	public abstract CategoryTemplate getCategoryTemplate();

	public String getTemplateName() {
		return templateName;
	}
	
	protected String fixupSchema(String schema) {
		return schema == null ? "" : schema + ".";
	}	
}

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TraceRow {
	private final String systemName;
	private final String categoryName;
	private final Long rowID;
	private final Long parentRowID;
	private final long duration;
	private final long sqlDuration;
	private final long startTime;
	private final long endTime;
	private final List<TraceRow> children = new ArrayList<TraceRow>(); 
	
	public TraceRow(String systemName, String categoryName, Long rowID,
			Long parentRowID, long duration, long sqlDuration, long startTime, long endTime) {
		super();
		this.systemName = systemName;
		this.categoryName = categoryName;
		this.rowID = rowID;
		this.parentRowID = parentRowID;
		this.duration = duration;
		this.sqlDuration = sqlDuration;
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public static TraceRow fromResultSet(ResultSet rs) throws SQLException {
		Object r = rs.getObject("TraceRowID");
		Object pr = rs.getObject("ParentRowID");
		
		Long rowID = Long.parseLong(r.toString());
		Long parentRowID = pr != null ? Long.parseLong(pr.toString()) : null;
		return new TraceRow(		
				rs.getString("SystemName").trim(),
				rs.getString("CategoryName").trim(),
				rowID,
				parentRowID,
				rs.getLong("Duration"),
				rs.getLong("SQLDuration"),
				rs.getTimestamp("StartTime").getTime(),
				rs.getTimestamp("EndTime").getTime());
	}
	

	public String getSystemName() {
		return systemName;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public Long getRowID() {
		return rowID;
	}

	public Long getParentRowID() {
		return parentRowID;
	}

	public long getDuration() {
		return duration;
	}

	public long getSqlDuration() {
		return sqlDuration;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}
	
	public List<TraceRow> getChildren() {
		return children;
	}

	@Override
	public String toString() {
		return "TraceRow [systemName=" + systemName + ", categoryName="
				+ categoryName + ", rowID=" + rowID + ", parentRowID="
				+ parentRowID + ", duration=" + duration + ", sqlDurations="
				+ sqlDuration + ", startTime=" + startTime + ", endTime="
				+ endTime + "]";
	}
}
/*
 *	Copyright 2008-2014 Follett Software Company 
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
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j.instrument.snapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.util.JDBCHelper;

public class CacheSnapShotSQLWriter implements SnapShotSQLWriter {
	public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
		throws SQLException {
		writeToSQL(conn, schema, (CacheSnapShotData)data, systemID);
	}
	
	public void writeToSQL(Connection conn, String schema, CacheSnapShotData data, long systemID)
		throws SQLException {
		schema = (schema == null) ? "" : (schema + ".");
		
		final String SQL = "INSERT INTO " + schema + "P4JCache " +
			"(SystemID, CacheType, InstanceName, StartTime, EndTime, Duration, HitRatio, HitCount, " +
			" MissCount, PutCount) " +
			"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(SQL);
			int index = 1;
			
			stmt.setLong(index++, systemID);
			stmt.setString(index++, data.getCacheType());
			stmt.setString(index++, data.getInstanceName());
			stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
			stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
			stmt.setLong(index++, data.getDuration());
			stmt.setDouble(index++, data.getHitRatio().getRatio());
			stmt.setLong(index++, data.getHitCount().getDelta());
			stmt.setLong(index++, data.getMissCount().getDelta());
			stmt.setLong(index++, data.getPutCount().getDelta());
			
			int count = stmt.executeUpdate();
			if (count != 1) {
				throw new SQLException("GarbageCollectorSnapShot failed to insert row");
			}
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}
}

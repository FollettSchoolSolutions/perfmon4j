/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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

package org.perfmon4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.MiscHelper;


public class ThreadTraceData implements PerfMonData, SQLWriteable {
    private ThreadTraceData parent;
    private final String name;
    private final List<ThreadTraceData> children = new Vector<ThreadTraceData>();
    private final int depth;
    private final long startTime;
    private long endTime = -1;
    private final long sqlStartTime;
    private long sqlEndTime = -1;
    
    ThreadTraceData(String name, long startTime) {
        this(name, null, startTime);
    }

    ThreadTraceData(String name, ThreadTraceData parent, long startTime) {
        this.name = name;
        this.parent = parent;
        this.startTime = startTime;
        this.sqlStartTime = SQLTime.getSQLTime();
        if (parent != null) {
            parent.children.add(this);
            depth = parent.depth + 1;
        } else {
            depth = 0;
        }
    }
    
    public ThreadTraceData[] getChildren() {
        return children.toArray(new ThreadTraceData[]{});
    }
    

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    void stop() {
        endTime = MiscHelper.currentTimeWithMilliResolution();
        sqlEndTime = SQLTime.getSQLTime();
    }
    
    ThreadTraceData getParent() {
        return parent;
    }

    public String toAppenderString() {
        String result = String.format(
            "\r\n********************************************************************************\r\n" +
            "%s" +
            "********************************************************************************",
            buildAppenderStringBody(""));
        return result;
    }
    
    
    public String buildAppenderStringBody(String indent) {
        StringBuilder childAppenderString = new StringBuilder();
        ThreadTraceData children[] = getChildren();
        String childIndent = indent + "|\t";
        for (int i = 0; i < children.length; i++) {
            childAppenderString.append(children[i].buildAppenderStringBody(childIndent));
        }
        String sqlTime = "";
        if (SQLTime.isEnabled() && (sqlEndTime - sqlStartTime) > 0) {
        	sqlTime = "(SQL:" + (sqlEndTime - sqlStartTime) + ")"; 
        }
        String result = String.format(
            "%s+-%s (%d)%s %s\r\n" +
            "%s" +
            "%s+-%s %s\r\n",
            indent,
            MiscHelper.formatTimeAsString(getStartTime()),
            getEndTime() - getStartTime(),
            sqlTime,
            name,
            childAppenderString.toString(),
            indent,
            MiscHelper.formatTimeAsString(getEndTime()),
            name);
        return result;
    }
        

    public String getName() {
        return name;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getDepth() {
        return depth;
    }

    void seperateFromParent() {
        if (parent != null) {
            parent.children.remove(this);
            parent = null;
        }
    }

	private void writeToSQL(Long parentRowID, ThreadTraceData data, 
			Connection conn, String schema,
			Map categoryNameCache) throws SQLException {
		Long myRowID = null;
		String s = (schema == null) ? "" : (schema + ".");
		final boolean oracleConnection = JDBCHelper.isOracleConnection(conn);

		String categoryName = data.getName();
		Long categoryID = (Long)categoryNameCache.get(categoryName);
		if (categoryID == null) {
			categoryID = JDBCHelper.simpleGetOrCreate(conn, s + "P4JCategory", "CategoryID", 
					"CategoryName", categoryName);
			categoryNameCache.put(categoryID, categoryName);
		}
		
		PreparedStatement stmtInsert = null;
		ResultSet rs = null;
		try {
			final String sql = "INSERT INTO " + s + "P4JThreadTrace\r\n" +
				"	(ParentRowID, CategoryID, StartTime, EndTime, Duration)\r\n" +
				"	VALUES(?, ?, ?, ?, ?)";
			
			if (oracleConnection) {
				stmtInsert = conn.prepareStatement(sql, new int[]{1});
			} else {
				stmtInsert = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			}
			stmtInsert.setObject(1, parentRowID, Types.INTEGER);
			stmtInsert.setLong(2, categoryID.longValue());
			stmtInsert.setTimestamp(3, new Timestamp(data.getStartTime()));
			stmtInsert.setTimestamp(4, new Timestamp(data.getEndTime()));
			stmtInsert.setLong(5, data.getEndTime() - data.getStartTime());

			stmtInsert.execute();
			rs = stmtInsert.getGeneratedKeys();
			rs.next();
			myRowID = new Long(rs.getLong(1));
		} finally {
			JDBCHelper.closeNoThrow(rs);
			JDBCHelper.closeNoThrow(stmtInsert);
		}
		
		ThreadTraceData children[] = data.getChildren();
		for (int i = 0; i < children.length; i++) {
			writeToSQL(myRowID, children[i], conn, schema, categoryNameCache);
		}
	}    
    
	public void writeToSQL(Connection conn, String dbSchema)
		throws SQLException {

		boolean originalAutoCommit = conn.getAutoCommit();
		boolean success = false;

		try {
			conn.setAutoCommit(false);
			try {
				writeToSQL(null, this, conn, dbSchema, new HashMap());
				success = true;
			} finally {
				if (!success) {
					JDBCHelper.rollbackNoThrow(conn);
				} else {
					conn.commit();
				}
			}
		} finally {
			conn.setAutoCommit(originalAutoCommit);
		}
	}
}

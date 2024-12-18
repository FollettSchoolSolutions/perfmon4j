/*
 *	Copyright 2008-2011 Follett Software Company 
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

import org.perfmon4j.ThreadTracesBase.UniqueThreadTraceTimerKey;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.MiscHelper;


public class ThreadTraceData implements PerfMonData, SQLWriteable {
    private ThreadTraceData parent;
    private final UniqueThreadTraceTimerKey key;
    private final String name;
    private final List<ThreadTraceData> children = new Vector<ThreadTraceData>();
    private int depth;
    private final long startTime;
    private long endTime = -1;
    private final long sqlStartTime;
    private long sqlEndTime = -1;
    private boolean overflow;
    
    ThreadTraceData(UniqueThreadTraceTimerKey key, long startTime, long sqlStartTime) {
        this(key, null, startTime, sqlStartTime);
    }

    ThreadTraceData(UniqueThreadTraceTimerKey key, ThreadTraceData parent, long startTime, long sqlStartTime) {
        this.key = key;
        String nameToUse = key.getMonitorName();
        
        // If this is the outermost monitor.  We want to use the fully qualified monitor for
        // the display name.  For instance - We could of configured thread traces to
        // capture "WebRequest" but we were actually started by "WebRequest.rest.circulation".  
        // In that case we want to display the fully qualified name.
        if (parent == null) {
        	// PerfMonTimer stores the last PerfMonTimer.start(String key) key name
        	// on the thread.
        	String lastNameOnThread = PerfMonTimer.getLastFullyQualifiedStartNameForThread();
        	if (lastNameOnThread != null) {
        		nameToUse = lastNameOnThread;
        	}
        }
        this.name = nameToUse;
        
        this.parent = parent;
        this.startTime = startTime;
        this.sqlStartTime = sqlStartTime;
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
    
    public long getDuration() {
    	long duration = getEndTime() - getStartTime();
    	return duration>=0 ? duration : 0;
    }
    
    void stop(long stopTime, long sqlStopTime) {
        endTime = stopTime;
        sqlEndTime = sqlStopTime;
    }
    
    ThreadTraceData getParent() {
        return parent;
    }

    public String toAppenderString() {
        String result = String.format(
            "\r\n********************************************************************************\r\n" +
            "%s" +
            "%s" +
            "********************************************************************************",
            overflow ? "Thread Trace Limit Exceeded -- Data truncated\r\n" : "", 
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
            getDuration(),
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
    
    public UniqueThreadTraceTimerKey getKey() {
    	return key;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getDepth() {
        return depth;
    }

    void seperateFromParent() {
    	seperateFromParent(false);
    }

    void seperateFromParent(boolean relocateChildren) {
        if (parent != null) {
            parent.children.remove(this);
            if (relocateChildren) {
            	for (ThreadTraceData child : children) {
            		child.parent = parent;
            		reduceDepth(child);
            		parent.children.add(child);
            	}
            }
            parent = null;
        }
    }
    
    private void reduceDepth(ThreadTraceData data) {
    	data.depth--;
    	for (ThreadTraceData child : data.children) {
    		reduceDepth(child);
    	}
    }
    
    
	private void writeToSQL(Long parentRowID, ThreadTraceData data, 
			Connection conn, String schema,
			Map categoryNameCache, long systemID) throws SQLException {
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
				"	(SystemID, ParentRowID, CategoryID, StartTime, EndTime, Duration, SQLDuration)\r\n" +
				"	VALUES(?, ?, ?, ?, ?, ?, ?)";
			
			if (oracleConnection) {
				stmtInsert = conn.prepareStatement(sql, new int[]{1});
			} else {
				stmtInsert = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			}
			
			int index = 1;
			stmtInsert.setLong(index++, systemID);
			stmtInsert.setObject(index++, parentRowID, Types.INTEGER);
			stmtInsert.setLong(index++, categoryID.longValue());
			stmtInsert.setTimestamp(index++, new Timestamp(data.getStartTime()));
			stmtInsert.setTimestamp(index++, new Timestamp(data.getEndTime()));
			stmtInsert.setLong(index++, data.getEndTime() - data.getStartTime());
			Long sqlTimeVal = null;
			if (SQLTime.isEnabled()) {
				sqlTimeVal = new Long(Math.max(0, sqlEndTime - sqlStartTime));
			}
			stmtInsert.setObject(index++, sqlTimeVal, Types.INTEGER);

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
			writeToSQL(myRowID, children[i], conn, schema, categoryNameCache, systemID);
		}
	}    
    
	void setOverflow(boolean overflow) {
		this.overflow = overflow;
	}
	
	public void writeToSQL(Connection conn, String dbSchema, long systemID)
		throws SQLException {

		boolean originalAutoCommit = conn.getAutoCommit();
		boolean success = false;

		try {
			conn.setAutoCommit(false);
			try {
				writeToSQL(null, this, conn, dbSchema, new HashMap(), systemID);
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

    public Map<FieldKey, Object> getFieldData(FieldKey[] fields) {
    	Map<FieldKey, Object> result = new HashMap<FieldKey, Object>();

    	
    	return result;
    }
}

package org.perfmon4j.extras.hystrix;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.util.JDBCHelper;

public class SQLWriter implements SnapShotSQLWriter {
	private final double REQUIRED_DATABASE_VERSION = 7.0;

	public void writeToSQL(Connection conn, String schema, SnapShotData data, long systemID)
		throws SQLException {
		writeToSQL(conn, schema, (HystrixBaseData)data, systemID);
	}
	
	
	/** Package level for Testing **/
	void writeToSQL(Connection conn, String schema, HystrixBaseData data, long systemID)
		throws SQLException {

		boolean isSupporedtData = (data instanceof HystrixCommandData) 
				|| (data instanceof HystrixThreadPoolData);
		
		if (!isSupporedtData) {
			throw new SQLException("Unsupported SnapShotData type for: " + this.getClass().getName());
		}
		
		double version = JDBCHelper.getDatabaseVersion(conn, schema);
		if (version < REQUIRED_DATABASE_VERSION) {
			throw new SQLException("Hystrix SnapShot requires P4J Database version >= " + REQUIRED_DATABASE_VERSION);
		}

		// Fixup schema if needed
		schema = (schema == null) ? "" : (schema + ".");
		String keyName = ((HystrixBaseData)data).getInstanceName();
		
		long hystrixKeyID = getOrCreateHystrixKeyID(conn, schema, keyName);
		
		if (data instanceof HystrixCommandData) {
			writeToSQL(conn, schema, (HystrixCommandData)data, systemID, hystrixKeyID);
		} else {
			writeToSQL(conn, schema, (HystrixThreadPoolData)data, systemID, hystrixKeyID);
		}
	}
	
	public long getOrCreateHystrixKeyID(Connection conn, String schema, String keyName) throws SQLException {
		return JDBCHelper.simpleGetOrCreate(conn, schema + "P4JHystrixKey", "KeyID", "KeyName", keyName);
	}
	
	public void writeToSQL(Connection conn, String schema, HystrixCommandData data, long systemID, 
			long hystrixKeyID)
		throws SQLException {

		final String SQL = "INSERT INTO " + schema + "P4JHystrixCommand " +
			"(SystemID, KeyID, StartTime, EndTime, Duration,  " +
			"SuccessCount, FailureCount, TimeoutCount, ShortCircuitedCount, ThreadPoolRejectedCount, SemaphoreRejectedCount) " +
			"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(SQL);
			
			int index = 1;
			stmt.setLong(index++, systemID);
			stmt.setLong(index++, hystrixKeyID);
			stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
			stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
			stmt.setLong(index++, data.getDuration());
			
			stmt.setLong(index++, data.getSuccessCount());
			stmt.setLong(index++, data.getFailureCount());
			stmt.setLong(index++, data.getTimeoutCount());
			stmt.setLong(index++, data.getShortCircuitedCount());
			stmt.setLong(index++, data.getThreadPoolRejectedCount());
			stmt.setLong(index++, data.getSemaphoreRejectedCount());
			
			int count = stmt.executeUpdate();
			if (count != 1) {
				throw new SQLException("P4JHystrixCommand failed to insert row");
			}
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}
	
	public void writeToSQL(Connection conn, String schema, HystrixThreadPoolData data, long systemID, 
			long hystrixKeyID)
		throws SQLException {
		schema = (schema == null) ? "" : (schema + ".");

		final String SQL = "INSERT INTO " + schema + "P4JHystrixThreadPool " +
			"(SystemID, KeyID, StartTime, EndTime, Duration,  " +
			"ExecutedThreadCount, RejectedThreadCount, CompletedThreadCount, ScheduledTaskCount,"
			+ "MaxActiveThreads, CurrentQueueSize, CurrentPoolSize) " +
			"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(SQL);
			
			int index = 1;
			stmt.setLong(index++, systemID);
			stmt.setLong(index++, hystrixKeyID);
			stmt.setTimestamp(index++, new Timestamp(data.getStartTime()));
			stmt.setTimestamp(index++, new Timestamp(data.getEndTime()));
			stmt.setLong(index++, data.getDuration());
			
			stmt.setLong(index++, data.getExecutedThreadCount());
			stmt.setLong(index++, data.getRejectedThreadCount());
			stmt.setLong(index++, data.getCompletedTaskCount());
			stmt.setLong(index++, data.getScheduledTaskCount());
			stmt.setLong(index++, data.getMaxActiveThreads());
			stmt.setLong(index++, data.getCurrentQueueSize());
			stmt.setLong(index++, data.getCurrentPoolSize());
			
			int count = stmt.executeUpdate();
			if (count != 1) {
				throw new SQLException("P4JHystrixThreadPool failed to insert row");
			}
		} finally {
			JDBCHelper.closeNoThrow(stmt);
		}
	}
}

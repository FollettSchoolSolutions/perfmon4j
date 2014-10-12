package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLWriteableWithDatabaseVersion {
	public void writeToSQL(Connection conn, String dbSchema, long systemID, double databaseVersion) throws SQLException;
}

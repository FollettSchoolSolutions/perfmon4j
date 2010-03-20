package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLWriteable {
	public void writeToSQL(Connection conn, String dbSchema) throws SQLException;
}

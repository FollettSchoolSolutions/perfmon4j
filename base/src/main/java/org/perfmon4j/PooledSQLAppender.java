package org.perfmon4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class PooledSQLAppender extends SQLAppender {
	private final Logger logger = LoggerFactory.initLogger(PooledSQLAppender.class);
	private  DataSource dataSource = null;
	private String poolName = null;
	
	public PooledSQLAppender(AppenderID id) {
		super(id);
	}
	
	public PooledSQLAppender(long intervalMillis) {
		this(getAppenderID(intervalMillis));
	}
	
    public static AppenderID getAppenderID(long intervalMillis) {
        return Appender.getAppenderID(PooledSQLAppender.class.getName(), intervalMillis);
    }

	@Override
	protected synchronized Connection getConnection() throws SQLException {
    	Connection result = null;
    	
    	if (dataSource == null) {
    		try {
				dataSource = (DataSource)InitialContext.doLookup(poolName);
			} catch (NamingException e) {
				throw new SQLException("Unabled find datasource: " + poolName, e);
			}
    	}
    	result = dataSource.getConnection();
    	
    	return result;	
	}

	@Override
	protected void releaseConnection(Connection conn) {
		JDBCHelper.closeNoThrow(conn);
	}

	@Override
	protected void resetConnection() {
		// Nothing todo here....
	}	
    
   
//    private synchronized Connection getConnection() throws Exception {
//    	Connection result = null;
//    	
//    	if (dataSource == null) {
//    		dataSource = (DataSource)InitialContext.doLookup(poolName);
//    	}
//    	result = dataSource.getConnection();
//    	return result;
//    }
	
}

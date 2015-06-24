package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class PooledSQLAppender extends SQLAppender {
	private final Logger logger = LoggerFactory.initLogger(PooledSQLAppender.class);
	private  DataSource dataSource = null;
	private String poolName = null;
	private String contextFactory = null;
	private String urlPkgs = null;
	
	public PooledSQLAppender(AppenderID id) {
		super(id);
	}
    
	@Override
	protected synchronized Connection getConnection() throws SQLException {
    	Connection result = null;
    	
    	if (dataSource == null) {
    		if (poolName == null) {
    			throw new SQLException("poolName must not be NULL");
    		}
    		dataSource = JDBCHelper.lookupDataSource(poolName, contextFactory, urlPkgs);
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
	
	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public String getContextFactory() {
		return contextFactory;
	}

	public void setContextFactory(String contextFactory) {
		this.contextFactory = contextFactory;
	}

	public String getUrlPkgs() {
		return urlPkgs;
	}

	public void setUrlPkgs(String urlPkgs) {
		this.urlPkgs = urlPkgs;
	}
	
	public static boolean testIfDataSourceIsAvailable(Properties props) {
		boolean result = false;
		
		try {
			JDBCHelper.lookupDataSource(props.getProperty("poolName"), props.getProperty("contextFactory"), 
					props.getProperty("urlPkgs"));
			result = true;
		} catch (SQLException e) {
			// Ignore... We will just assume datasource is not available.
		}
		
		return result;
	}
	
}

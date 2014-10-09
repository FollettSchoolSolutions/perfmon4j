package org.perfmon4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
    		try {
                Properties props = new Properties();
                if (contextFactory != null) {
                	props.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
                }
                if (urlPkgs != null) {
                	props.put("java.naming.factory.url.pkgs", urlPkgs);
                }
                InitialContext initialContext = new InitialContext(props);
				dataSource = (DataSource)initialContext.lookup(poolName);
			} catch (NamingException e) {
				throw new SQLException("Unabled find datasource: " + poolName + ": " + e.getMessage());
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

	@Override
	protected void logNullConnectionWarning() {
		logger.logWarn("Failed to obtain JDBCConnection for PoolName: " + poolName);
	}
}

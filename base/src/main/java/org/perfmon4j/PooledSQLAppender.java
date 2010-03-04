package org.perfmon4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
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
	private String initialContextFactory = null;
	private String urlPkgs = null;
	
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
    			Properties props = new Properties();
    			if (initialContextFactory != null) {
	    			props.setProperty(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
    			}
    			if (urlPkgs != null) {
        			props.setProperty("java.naming.factory.url.pkgs", urlPkgs);
    			}
    			InitialContext context = new InitialContext(props);
				dataSource = (DataSource)context.lookup(poolName);
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
	
	public String getPoolName() {
		return poolName;
	}

	public void setPoolName(String poolName) {
		this.poolName = poolName;
	}

	public String getInitialContextFactory() {
		return initialContextFactory;
	}

	public void setInitialContextFactory(String initialContextFactory) {
		this.initialContextFactory = initialContextFactory;
	}

	public String getUrlPkgs() {
		return urlPkgs;
	}

	public void setUrlPkgs(String urlPkgs) {
		this.urlPkgs = urlPkgs;
	}
}

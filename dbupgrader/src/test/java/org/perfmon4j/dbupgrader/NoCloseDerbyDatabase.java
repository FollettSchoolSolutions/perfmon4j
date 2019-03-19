package org.perfmon4j.dbupgrader;

import liquibase.database.DatabaseFactory;
import liquibase.database.core.DerbyDatabase;
import liquibase.exception.DatabaseException;

import org.slf4j.LoggerFactory;


/**
 * Liquibase closes the DERBY embedded database
 * when they close their connection.  While this
 * is fine for production (although is it really?)
 * it does not work for tests where you want to 
 * examine the database after liquibase is done.
 * 
 * See liquibase.database.core.DerbyDatabase.close() for details.
 * 
 * @author perfmon
 *
 */
public class NoCloseDerbyDatabase extends DerbyDatabase {
	private boolean inclose = false;
	
	public static void initLiquibaseNoCloseDerbyDatabase() {
		// Quiet down Liquibase
    	ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("liquibase");
    	logger.setLevel(ch.qos.logback.classic.Level.WARN);		

    	logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger("org.perfmon4j.dbupgrader.NoCloseDerbyDatabase");
    	logger.setLevel(ch.qos.logback.classic.Level.WARN);		
		
		DatabaseFactory factory = DatabaseFactory.getInstance();
		factory.clearRegistry();
		factory.register(new NoCloseDerbyDatabase());
	}
	
	public static void deInitLiquibaseNoCloseDerbyDatabase() {
		DatabaseFactory.reset();
	}
	
	@Override
	public String getDefaultDriver(String url) {
		if (inclose) {
			return null; // Returning null from this method will keep Liquibase from closing the embeddedDatabase
						 // in the close method;
		} else {
			return super.getDefaultDriver(url);
		}
	}


	@Override
	public void close() throws DatabaseException {
		try {
			inclose = true;
			super.close();
		} finally {
			inclose = false;
		}
	}
}

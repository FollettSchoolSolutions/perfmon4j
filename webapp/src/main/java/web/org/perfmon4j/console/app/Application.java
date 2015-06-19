package web.org.perfmon4j.console.app;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.internal.EntityManagerImpl;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.JDBCHelper.DriverCache;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppCleanup;
import org.zkoss.zk.ui.util.WebAppInit;

import web.org.perfmon4j.console.app.data.EMProvider;

public class Application implements WebAppInit, WebAppCleanup{

	@Override
	public void init(WebApp wapp) throws Exception {
		// Initialize the EntityManager
		EMProvider.getEM();
	}

	@Override
	public void cleanup(WebApp wapp) throws Exception {
		// Close the EntityManager
		EntityManager mgr = EMProvider.getEM();
		
		// Check to see if wee need to drop the database;
		SessionFactoryImplementor impl  = (SessionFactoryImplementor)((EntityManagerImpl)mgr).getSession().getSessionFactory();
		@SuppressWarnings("deprecation")
		DatabaseMetaData md = impl.getConnectionProvider().getConnection().getMetaData();
		String url = md.getURL();
		String driverName = md.getDriverName();
		mgr.close();

		if(url.startsWith("jdbc:derby:") && driverName.contains("Embedded")) {
			try {
				JDBCHelper.createJDBCConnection(DriverCache.DEFAULT, "org.apache.derby.jdbc.EmbeddedDriver", null, url + ";shutdown=true", null, null);
			} catch (SQLException sn) {
			}
		}
	}
}

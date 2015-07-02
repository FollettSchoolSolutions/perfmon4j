/*
 *	Copyright 2015 Follett School Solutions 
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
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

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

import web.org.perfmon4j.console.app.data.AppConfigService;
import web.org.perfmon4j.console.app.data.EMProvider;

public class Application implements WebAppInit, WebAppCleanup{
	
	@Override
	public void init(WebApp wapp) throws Exception {
		// Initialize the EntityManager
		EMProvider.getEM();
		(new AppConfigService()).refreshDataSourceSecurity();
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

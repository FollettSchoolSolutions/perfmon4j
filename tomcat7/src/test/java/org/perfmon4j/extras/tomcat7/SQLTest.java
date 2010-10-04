/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j.extras.tomcat7;

import org.perfmon4j.JDBCSQLAppender;
import org.perfmon4j.Appender.AppenderID;

import junit.framework.TestCase;


public abstract class SQLTest extends TestCase {
	protected JDBCSQLAppender appender = null;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		appender = new JDBCSQLAppender(AppenderID.getAppenderID(JDBCSQLAppender.class.getName()));
		appender.setDbSchema("mydb");
		appender.setDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
		appender.setJdbcURL("jdbc:derby:memory:derbyDB;create=true");
	}

	protected void tearDown() throws Exception {
		appender.deInit();
		appender = null;
		
		super.tearDown();
	}
}

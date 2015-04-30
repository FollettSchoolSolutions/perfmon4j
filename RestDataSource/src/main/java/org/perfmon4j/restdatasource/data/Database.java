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

package org.perfmon4j.restdatasource.data;


public class Database {
	private boolean def;
	private String name;
	private String id;
	private double databaseVersion;

	public Database() {
	}

	public Database(String name, boolean def, String id, double databaseVersion) {
		this.name = name;
		this.def = def;
		this.id = id;
		this.databaseVersion = databaseVersion;
	}
	
	public boolean isDefault() {
		return def;
	}
	public void setDefault(boolean def) {
		this.def = def;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public double getDatabaseVersion() {
		return databaseVersion;
	}

	public void setDatabaseVersion(double databaseVersion) {
		this.databaseVersion = databaseVersion;
	}

	@Override
	public String toString() {
		return "Database [default=" + def + ", name=" + name + ", ID=" + id
				+ ", databaseVersion=" + databaseVersion + "]";
	}
}

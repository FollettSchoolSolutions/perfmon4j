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

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CategoryTemplate {
	private String name;
	private Field[] fields;
	private String databaseTableName;

	public CategoryTemplate() {
	}

	public CategoryTemplate(String name, Field[] fields) {
		super();
		this.name = name;
		this.fields = fields;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Field[] getFields() {
		return fields;
	}
	
	public void setFields(Field[] fields) {
		this.fields = fields;
	}
	
	@JsonIgnore
	public String getDatabaseTableName() {
		return databaseTableName;
	}

	public void setDatabaseTableName(String databaseTableName) {
		this.databaseTableName = databaseTableName;
	}

	@Override
	public String toString() {
		return "CategoryTemplate [name=" + name + ", fields="
				+ Arrays.toString(fields) + ", databaseTableName="
				+ databaseTableName + "]";
	}
}

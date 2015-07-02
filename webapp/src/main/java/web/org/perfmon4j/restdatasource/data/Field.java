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

package web.org.perfmon4j.restdatasource.data;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Field {
	private String name;
	private AggregationMethod[] aggregationMethods;
	private AggregationMethod defaultAggregationMethod;
	
	public Field() {
		super();
	}

	public Field(String name) {
		super();
		this.name = name;
	}
	
	public Field(String name, AggregationMethod[] aggregationMethod) {
		super();
		this.name = name;
		this.aggregationMethods = aggregationMethod;
		this.defaultAggregationMethod = AggregationMethod.NATURAL;
	}

	public Field(String name, AggregationMethod[] aggregationMethods, AggregationMethod defaultAggregationMethod) {
		super();
		this.name = name;
		this.aggregationMethods = aggregationMethods;
		this.defaultAggregationMethod = defaultAggregationMethod;
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AggregationMethod[] getAggregationMethods() {
		return aggregationMethods;
	}
	public void setAggregationMethods(AggregationMethod[] aggregationMethods) {
		this.aggregationMethods = aggregationMethods;
	}
	
	public AggregationMethod getDefaultAggregationMethod() {
		return defaultAggregationMethod;
	}

	public void setDefaultAggregationMethod(AggregationMethod defaultAggregationMethod) {
		this.defaultAggregationMethod = defaultAggregationMethod;
	}

	@Override
	public String toString() {
		return "Field [name=" + name + ", aggregationMethods="
				+ Arrays.toString(aggregationMethods)
				+ ", defaultAggregationMethod=" + defaultAggregationMethod
				+ "]";
	}
}

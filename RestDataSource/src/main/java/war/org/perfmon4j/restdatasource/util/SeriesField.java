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

package war.org.perfmon4j.restdatasource.util;

import war.org.perfmon4j.restdatasource.DataProvider;
import war.org.perfmon4j.restdatasource.RestImpl.SystemID;
import war.org.perfmon4j.restdatasource.data.AggregationMethod;
import war.org.perfmon4j.restdatasource.data.Category;
import war.org.perfmon4j.restdatasource.data.Field;
import war.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;

public class SeriesField {
	private final String alias;
	private final DataProvider provider;
	private final Category category;
	private final Field field;
	private final SystemID systems[];
	private final AggregationMethod aggregationMethod;
	
	// This will be set by the dataProvider.
	private AggregatorFactory factory = null;
	
	public SeriesField(String alias, DataProvider provider, SystemID systems[], Category category, Field field, AggregationMethod aggregationMethod) {
		super();
		this.alias = alias;
		this.provider = provider;
		this.systems = systems;
		this.category = category;
		this.field = field;
		this.aggregationMethod = aggregationMethod;
	}

	public Category getCategory() {
		return category;
	}

	public Field getField() {
		return field;
	}

	public AggregationMethod getAggregationMethod() {
		return aggregationMethod;
	}

	public String getAlias() {
		return alias;
	}

	public DataProvider getProvider() {
		return provider;
	}

	public SystemID[] getSystems() {
		return systems;
	}

	public AggregatorFactory getFactory() {
		return factory;
	}

	public void setFactory(AggregatorFactory factory) {
		this.factory = factory;
	}
}


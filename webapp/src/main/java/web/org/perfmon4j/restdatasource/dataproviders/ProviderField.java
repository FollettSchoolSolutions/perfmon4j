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

package web.org.perfmon4j.restdatasource.dataproviders;


import javax.ws.rs.BadRequestException;

import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.data.Field;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.AverageAggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.MaxAggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.MinAggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.SumAggregatorFactory;


public class ProviderField extends Field {
	protected boolean floatingPoint = false;
	protected String databaseColumn = null;
	
	public ProviderField(String name, AggregationMethod[] aggregationMethods,
			AggregationMethod defaultAggregationMethod, String databaseColumn, boolean floatingPoint) {
		super(name, aggregationMethods, defaultAggregationMethod);
		this.databaseColumn = databaseColumn;
		this.floatingPoint = floatingPoint;
	}

	public AggregatorFactory buildFactory(AggregationMethod method) {
		AggregatorFactory factory = null;
		
		if (method.equals(AggregationMethod.SUM)) {
			factory = new SumAggregatorFactory(databaseColumn, floatingPoint);
		} else if (method.equals(AggregationMethod.MAX)) {
			factory = new MaxAggregatorFactory(databaseColumn, floatingPoint);
		} else if (method.equals(AggregationMethod.MIN)) {
			factory = new MinAggregatorFactory(databaseColumn, floatingPoint);
		} else if (method.equals(AggregationMethod.AVERAGE)) {
			factory = new AverageAggregatorFactory(databaseColumn, floatingPoint);
		} else {
			throw new BadRequestException("Aggregation method not supported for field");	
		}

		return factory;
	}
}

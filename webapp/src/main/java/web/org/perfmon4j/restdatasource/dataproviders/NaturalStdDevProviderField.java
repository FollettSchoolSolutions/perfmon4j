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

import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.util.aggregators.AggregatorFactory;
import web.org.perfmon4j.restdatasource.util.aggregators.NaturalStdDevAggregatorFactory;

public class NaturalStdDevProviderField extends ProviderField {
	protected String numeratorColumn;
	protected String sumOfSquaresColumn;
	protected String denominatorColumn;

	public NaturalStdDevProviderField(String name,
			AggregationMethod[] aggregationMethods,
			String databaseColumn, 
			String numeratorColumn,
			String sumOfSquaresColumn,
			String denominatorColumn,
			boolean floatingPoint) {
		super(name, aggregationMethods, AggregationMethod.NATURAL, databaseColumn,
				floatingPoint);
		this.numeratorColumn = numeratorColumn;
		this.sumOfSquaresColumn = sumOfSquaresColumn;
		this.denominatorColumn = denominatorColumn;
	}

	@Override
	public AggregatorFactory buildFactory(AggregationMethod method) {
		if (method.equals(AggregationMethod.NATURAL)) {
			return new NaturalStdDevAggregatorFactory(numeratorColumn, sumOfSquaresColumn, denominatorColumn);
		} else {
			return super.buildFactory(method);
		}
	}
}

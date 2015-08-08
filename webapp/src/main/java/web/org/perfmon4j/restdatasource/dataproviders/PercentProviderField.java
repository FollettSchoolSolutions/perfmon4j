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
import web.org.perfmon4j.restdatasource.util.aggregators.PercentAggregatorFactory;

public class PercentProviderField extends ProviderField {
	protected String systemIDColumn;
	protected String numeratorColumn;
	protected String denominatorColumn;

	public PercentProviderField(String name,
			String systemIDColumn,
			String numeratorColumn,
			String denominatorColumn) {
		super(name, new AggregationMethod[]{AggregationMethod.NATURAL, AggregationMethod.MAX, AggregationMethod.MIN}, AggregationMethod.NATURAL, null,
				true);
		this.systemIDColumn = systemIDColumn;
		this.numeratorColumn = numeratorColumn;
		this.denominatorColumn = denominatorColumn;
	}

	@Override
	public AggregatorFactory buildFactory(AggregationMethod method) {
		return new PercentAggregatorFactory(systemIDColumn, numeratorColumn, denominatorColumn, method);
	}
}

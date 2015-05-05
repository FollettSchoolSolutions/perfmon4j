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

import java.util.ArrayList;
import java.util.List;

public class IntervalTemplate extends CategoryTemplate {

	public IntervalTemplate() {
		super("Interval", buildFields());
	}
	
	private static final Field[] buildFields() {
		List<Field> fields = new ArrayList<Field>();
		
		fields.add(new Field("maxActiveThreads", AggregationMethod.DEFAULT, AggregationMethod.SUM));
		fields.add(new Field("maxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX));
		fields.add(new Field("minDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN));
		fields.add(new Field("throughputPerMinute", AggregationMethod.DEFAULT_WITH_NATURAL));
		fields.add(new Field("averageDuration", AggregationMethod.DEFAULT_WITH_NATURAL));
		fields.add(new Field("medianDuration", AggregationMethod.DEFAULT, AggregationMethod.AVERAGE));
		fields.add(new Field("standardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL));
		fields.add(new Field("sqlMaxDuration", AggregationMethod.DEFAULT, AggregationMethod.MAX));
		fields.add(new Field("sqlLMinDuration", AggregationMethod.DEFAULT, AggregationMethod.MIN));
		fields.add(new Field("sqlAverageDuration", AggregationMethod.DEFAULT_WITH_NATURAL));
		fields.add(new Field("sqlStandardDeviation", AggregationMethod.DEFAULT_WITH_NATURAL));
		
		return fields.toArray(new Field[]{});
	}
}

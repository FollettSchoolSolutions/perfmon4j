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

import java.util.ArrayList;
import java.util.List;

import org.jboss.resteasy.spi.BadRequestException;

import war.org.perfmon4j.restdatasource.RestImpl;
import war.org.perfmon4j.restdatasource.data.AggregationMethod;

public class ParsedSeriesDefinition {
	private final AggregationMethod aggregationMethod;
	private final RestImpl.SystemID[] systems;
	private final String categoryName;
	private final String fieldName;
	
	private ParsedSeriesDefinition(AggregationMethod aggregationMethod, String[] systems,
			String categoryName, String fieldName, String expectedDatabaseID) {
		super();
		this.aggregationMethod = aggregationMethod;
		this.systems = RestImpl.SystemID.parse(systems, expectedDatabaseID);
		this.categoryName = categoryName;
		this.fieldName = fieldName;
	}

	public AggregationMethod getAggregationMethod() {
		return aggregationMethod;
	}

	public RestImpl.SystemID[] getSystems() {
		return systems;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public String getFieldName() {
		return fieldName;
	}
	
	private static ParsedSeriesDefinition parseSingleSeries(String definition, String expectedDatabaseID) {
		if (definition == null || "".equals(definition.trim())) {
			throw new BadRequestException("You must provide a series definition");
		}	
		
		List<String> systems = new ArrayList<String>();
		String split[] = definition.split("~");
		AggregationMethod aggregationMethod = null;
		
		int offset = 0;
		if (!(split[0].contains("-") || split[0].contains("."))) {
			// All system identifiers contain a "-" or a "."
			// If it does not we assume it is the optional aggregationMethod.
			String tmp = split[offset++];
			aggregationMethod = AggregationMethod.fromString(tmp);
			
			if (aggregationMethod == null) {
				throw new BadRequestException("Invalid aggregation method: " + tmp);
			}
		}
		if ((split.length - offset) < 3) {
			throw new BadRequestException("Insufficent fields in series definition");
		}
		
		int endOfSystems = split.length - 2; 
		
		for (;offset < endOfSystems; offset++) {
			systems.add(split[offset]);
		}
		String categoryName = split[offset++];
		String fieldName = split[offset];
		
		return new ParsedSeriesDefinition(aggregationMethod, systems.toArray(new String[]{}), categoryName, fieldName, expectedDatabaseID);
	}


	public static ParsedSeriesDefinition[] parse(String definition, String expectedDatabaseID) {
		if (definition == null || "".equals(definition.trim())) {
			throw new BadRequestException("You must provide a series definition");
		}
		
		List<ParsedSeriesDefinition> result = new ArrayList<ParsedSeriesDefinition>();
		
		String split[] = definition.split("_");
		if (split.length == 0) {
			throw new BadRequestException("You must provide a series definition");
		}		
		for (String s : split) {
			result.add(parseSingleSeries(s, expectedDatabaseID));	
		}
		
		return result.toArray(new ParsedSeriesDefinition[]{});
	}
}

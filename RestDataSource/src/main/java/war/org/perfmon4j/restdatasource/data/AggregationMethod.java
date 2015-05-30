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

package war.org.perfmon4j.restdatasource.data;


public enum AggregationMethod {
	NATURAL("NATURAL"),
	MAX("MAX"),
	MIN("MIN"),
	SUM("SUM"),
	AVERAGE("AVERAGE");

	private final String description;
	
	AggregationMethod(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}
	
	public static AggregationMethod fromString(String value) {
		if (NATURAL.getDescription().equals(value)) {
			return NATURAL;
		} else if (MAX.getDescription().equals(value)) {
			return MAX;
		} else if (MIN.getDescription().equals(value)) {
			return MIN;
		} else if (SUM.getDescription().equals(value)) {
			return SUM;
		} else if (AVERAGE.getDescription().equals(value)) {
			return AVERAGE;
		} 
		
		return null;
	}
	
	public static final AggregationMethod[] DEFAULT = {MAX, MIN, SUM, AVERAGE};
	public static final AggregationMethod[] DEFAULT_WITH_NATURAL = {NATURAL, MAX, MIN, SUM, AVERAGE};
	public static final AggregationMethod[] NONE = {};
}

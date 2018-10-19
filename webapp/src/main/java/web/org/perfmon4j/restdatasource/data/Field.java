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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Field implements Comparable<Field>{
	private String name;
	private AggregationMethod[] aggregationMethods;
	private AggregationMethod defaultAggregationMethod;
	
	@JsonIgnore
	private String requiredChangSet = null;

	@JsonIgnore
	private boolean primary = false;
	
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
				+ ", primary=" + primary + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(aggregationMethods);
		result = prime
				* result
				+ ((defaultAggregationMethod == null) ? 0
						: defaultAggregationMethod.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (primary ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Field other = (Field) obj;
		if (!Arrays.equals(aggregationMethods, other.aggregationMethods))
			return false;
		if (defaultAggregationMethod != other.defaultAggregationMethod)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (primary != other.primary)
			return false;
		return true;
	}

	@Override
	public int compareTo(Field o) {
		String myName = (primary ? "1" : "2") + name;
		String otherName = (o.primary ? "1" : "2") + o.name;
		
		return myName.compareTo(otherName);
	}
	
	public String getRequiredChangSet() {
		return requiredChangSet;
	}

	/* *
	 * Primary indicates fields that are more commonly used.  These will be sorted to the top of the list.
	 * @return
	 */
	public Field makePrimary() {
		primary = true;
		return this;
	}

	/* *
	 * This is used if a field is added to an existing category once created and deployed.
	 * The dataprovider will filter fields to only include them if the specified Liguibase changeset has been applied
	 * to the database.
	 * @param changeSet
	 * @return
	 */
	public Field requiresChangeSet(String changeSet) {
		this.requiredChangSet = changeSet;
		return this;
	}


}

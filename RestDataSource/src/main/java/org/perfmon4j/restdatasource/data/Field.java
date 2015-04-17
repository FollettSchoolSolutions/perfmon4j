package org.perfmon4j.restdatasource.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Field {
	private String name;
	private AggregationType[] aggregationTypes;
	private AggregationType defaultAggregationType;

	public Field(String name) {
		super();
		this.name = name;
	}
	
	public Field(String name, AggregationType[] aggregationTypes) {
		super();
		this.name = name;
		this.aggregationTypes = aggregationTypes;
		this.defaultAggregationType = null;
	}

	public Field(String name, AggregationType[] aggregationTypes, AggregationType defaultAggregationType) {
		super();
		this.name = name;
		this.aggregationTypes = aggregationTypes;
		this.defaultAggregationType = defaultAggregationType;
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AggregationType[] getAggregationTypes() {
		return aggregationTypes;
	}
	public void setAggregationTypes(AggregationType[] aggregationTypes) {
		this.aggregationTypes = aggregationTypes;
	}

	@JsonInclude(Include.NON_NULL)
	public AggregationType getDefaultAggregationType() {
		return defaultAggregationType;
	}

	public void setDefaultAggregationType(AggregationType defaultAggregationType) {
		this.defaultAggregationType = defaultAggregationType;
	}
}

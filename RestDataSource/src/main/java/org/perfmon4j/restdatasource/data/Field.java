package org.perfmon4j.restdatasource.data;

public class Field {
	private String name;
	private String[] aggregationTypes = new String[]{};

	public Field(String name) {
		super();
		this.name = name;
	}
	
	public Field(String name, String[] aggregationTypes) {
		super();
		this.name = name;
		this.aggregationTypes = aggregationTypes;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String[] getAggregationTypes() {
		return aggregationTypes;
	}
	public void setAggregationTypes(String[] aggregationTypes) {
		this.aggregationTypes = aggregationTypes;
	}
}

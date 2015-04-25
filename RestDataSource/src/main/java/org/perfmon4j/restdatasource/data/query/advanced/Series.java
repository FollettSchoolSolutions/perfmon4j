package org.perfmon4j.restdatasource.data.query.advanced;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Series {
	private String alias;
	private String systemID;
	private String category;
	private String fieldName;
	private String aggregationMethod;
	private Number values[];
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getSystemID() {
		return systemID;
	}
	
	public void setSystemID(String systemID) {
		this.systemID = systemID;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	
	public String getAggregationMethod() {
		return aggregationMethod;
	}
	
	public void setAggregationMethod(String aggregationMethod) {
		this.aggregationMethod = aggregationMethod;
	}
	
	public Number[] getValues() {
		return values;
	}
	
	public void setValues(Number[] values) {
		this.values = values;
	}
}

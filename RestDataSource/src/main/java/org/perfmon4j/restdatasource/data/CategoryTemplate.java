package org.perfmon4j.restdatasource.data;

public class CategoryTemplate {
	private String name;
	private Field[] fields;
	
	public CategoryTemplate(String name, Field[] fields) {
		super();
		this.name = name;
		this.fields = fields;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Field[] getFields() {
		return fields;
	}
	public void setFields(Field[] fields) {
		this.fields = fields;
	}
}

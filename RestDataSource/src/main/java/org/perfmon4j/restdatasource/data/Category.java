package org.perfmon4j.restdatasource.data;

public class Category {
	private String name;
	private String templateName;
	
	public Category(String name, String templateName) {
		super();
		this.name = name;
		this.templateName = templateName;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
}

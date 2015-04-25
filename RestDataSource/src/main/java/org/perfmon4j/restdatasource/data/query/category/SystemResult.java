package org.perfmon4j.restdatasource.data.query.category;

public class SystemResult {
	private String systemId;
	private ResultElement[] elements;
	
	public String getSystemId() {
		return systemId;
	}
	public void setSystemID(String systemId) {
		this.systemId = systemId;
	}
	public ResultElement[] getElements() {
		return elements;
	}
	public void setElements(ResultElement[] elements) {
		this.elements = elements;
	}
}

package org.perfmon4j.restdatasource.data.query.category;

public class Result {
	private String category;
	private SystemResult[] systemResults;
	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public SystemResult[] getSystemResults() {
		return systemResults;
	}
	public void setSystemResults(SystemResult[] systemResults) {
		this.systemResults = systemResults;
	}
}

package org.perfmon4j.restdatasource.data.query.advanced;

public class Result {
	private String[] dateTime;
	private Series[] series;
	
	public String[] getDateTime() {
		return dateTime;
	}
	public void setDateTime(String[] dateTime) {
		this.dateTime = dateTime;
	}
	public Series[] getSeries() {
		return series;
	}
	public void setSeries(Series[] series) {
		this.series = series;
	}
}

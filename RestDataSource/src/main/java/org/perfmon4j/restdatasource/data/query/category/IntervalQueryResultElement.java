package org.perfmon4j.restdatasource.data.query.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class IntervalQueryResultElement extends ResultElement {
	private Integer maxActiveThreads;
	private Integer maxDuration;
	private Integer minDuration;
	private Double throughputPerMinute;
	private Double averageDuration;
	private Double medianDuration;
	private Double standardDeviation;
	private Integer sqlMaxDuration;
	private Integer sqlLMinDuration;
	private Double sqlAverageDuration;
	private Double sqlStandardDeviation;
	
	public Integer getMaxActiveThreads() {
		return maxActiveThreads;
	}
	public void setMaxActiveThreads(Integer maxActiveThreads) {
		this.maxActiveThreads = maxActiveThreads;
	}
	public Integer getMaxDuration() {
		return maxDuration;
	}
	public void setMaxDuration(Integer maxDuration) {
		this.maxDuration = maxDuration;
	}
	public Integer getMinDuration() {
		return minDuration;
	}
	public void setMinDuration(Integer minDuration) {
		this.minDuration = minDuration;
	}
	public Double getThroughputPerMinute() {
		return throughputPerMinute;
	}
	public void setThroughputPerMinute(Double throughputPerMinute) {
		this.throughputPerMinute = throughputPerMinute;
	}
	public Double getAverageDuration() {
		return averageDuration;
	}
	public void setAverageDuration(Double averageDuration) {
		this.averageDuration = averageDuration;
	}
	public Double getMedianDuration() {
		return medianDuration;
	}
	public void setMedianDuration(Double medianDuration) {
		this.medianDuration = medianDuration;
	}
	public Double getStandardDeviation() {
		return standardDeviation;
	}
	public void setStandardDeviation(Double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}
	public Integer getSqlMaxDuration() {
		return sqlMaxDuration;
	}
	public void setSqlMaxDuration(Integer sqlMaxDuration) {
		this.sqlMaxDuration = sqlMaxDuration;
	}
	public Integer getSqlLMinDuration() {
		return sqlLMinDuration;
	}
	public void setSqlLMinDuration(Integer sqlLMinDuration) {
		this.sqlLMinDuration = sqlLMinDuration;
	}
	public Double getSqlAverageDuration() {
		return sqlAverageDuration;
	}
	public void setSqlAverageDuration(Double sqlAverageDuration) {
		this.sqlAverageDuration = sqlAverageDuration;
	}
	public Double getSqlStandardDeviation() {
		return sqlStandardDeviation;
	}
	public void setSqlStandardDeviation(Double sqlStandardDeviation) {
		this.sqlStandardDeviation = sqlStandardDeviation;
	}	
}

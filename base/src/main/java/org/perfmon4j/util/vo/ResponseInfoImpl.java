package org.perfmon4j.util.vo;

import java.sql.Timestamp;

public class ResponseInfoImpl extends ResponseInfo {
	private long endTime;
	private long maxDuration;
	private long maxThreads;
	private long minDuration;
	private String monitorName;
	private long startTime;
	private long sum;
	private long sumOfSquares;
	private double throughput;
	private long totalCompletions;
	private long totalHits;
	
	public ResponseInfoImpl() {
	}
	
	@Override
	public long getEndTime() {
		return endTime;
	}

	@Override
	public long getMaxDuration() {
		return maxDuration;
	}

	@Override
	public long getMaxThreads() {
		return maxThreads;
	}

	@Override
	public long getMinDuration() {
		return minDuration;
	}

	@Override
	public String getMonitorName() {
		return monitorName;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public long getSum() {
		return sum;
	}

	@Override
	public long getSumOfSquares() {
		return sumOfSquares;
	}

	@Override
	public double getThroughput() {
		return throughput;
	}

	@Override
	public long getTotalCompletions() {
		return totalCompletions;
	}

	@Override
	public long getTotalHits() {
		return totalHits;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime.getTime();
	}

	public void setMaxDuration(long maxDuration) {
		this.maxDuration = maxDuration;
	}

	public void setMaxThreads(long maxThreads) {
		this.maxThreads = maxThreads;
	}

	public void setMinDuration(long minDuration) {
		this.minDuration = minDuration;
	}

	public void setMonitorName(String monitorName) {
		this.monitorName = monitorName;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime.getTime();
	}

	public void setSum(long sum) {
		this.sum = sum;
	}

	public void setSumOfSquares(long sumOfSquares) {
		this.sumOfSquares = sumOfSquares;
	}

	public void setThroughput(double throughput) {
		this.throughput = throughput;
	}

	public void setTotalCompletions(long totalCompletions) {
		this.totalCompletions = totalCompletions;
	}

	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}

}

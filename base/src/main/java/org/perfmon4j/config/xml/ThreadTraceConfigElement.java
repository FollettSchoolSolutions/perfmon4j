package org.perfmon4j.config.xml;

import java.util.ArrayList;
import java.util.List;

public class ThreadTraceConfigElement {
	private String monitorName = null;
	private String maxDepth = null;
	private String minDurationToCapture = null;
	private String randomSamplingFactor = null;
	private final List<AppenderMappingElement> appenders = new ArrayList<AppenderMappingElement>();
	private final List<TriggerConfigElement> triggers = new ArrayList<TriggerConfigElement>();

	public List<AppenderMappingElement> getAppenders() {
		return appenders;
	}
	public List<TriggerConfigElement> getTriggers() {
		return triggers;
	}
	public String getMonitorName() {
		return monitorName;
	}
	public void setMonitorName(String monitorName) {
		this.monitorName = monitorName;
	}
	public String getMaxDepth() {
		return maxDepth;
	}
	public void setMaxDepth(String maxDepth) {
		this.maxDepth = maxDepth == null ? "0" : maxDepth;
	}
	public String getMinDurationToCapture() {
		return minDurationToCapture;
	}
	public void setMinDurationToCapture(String minDurationToCapture) {
		this.minDurationToCapture = minDurationToCapture == null ? "0" : minDurationToCapture;
	}
	public String getRandomSamplingFactor() {
		return randomSamplingFactor;
	}
	public void setRandomSamplingFactor(String randomSamplingFactor) {
		this.randomSamplingFactor = randomSamplingFactor == null ? "0" : randomSamplingFactor;
	}
	
}

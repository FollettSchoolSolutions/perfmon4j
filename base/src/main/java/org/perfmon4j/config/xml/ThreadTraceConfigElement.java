/*
 *	Copyright 2014 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j.config.xml;

import java.util.ArrayList;
import java.util.List;

public class ThreadTraceConfigElement {
	/**
	 * IMPORTANT IF YOU ADD ATTRIBUTES:  Make sure you update the copy constructor/clone!
	 */
	private String monitorName = null;
	private String maxDepth = null;
	private String minDurationToCapture = null;
	private String randomSamplingFactor = null;
	private final List<AppenderMappingElement> appenders = new ArrayList<AppenderMappingElement>();
	private final List<TriggerConfigElement> triggers = new ArrayList<TriggerConfigElement>();
	private boolean enabled = true;

	public ThreadTraceConfigElement() {
	}
	
	public ThreadTraceConfigElement(ThreadTraceConfigElement elementToCopy) {
		this.enabled = elementToCopy.enabled; 
		this.maxDepth = elementToCopy.maxDepth;
		this.minDurationToCapture = elementToCopy.minDurationToCapture;
		this.monitorName = elementToCopy.monitorName;
		this.randomSamplingFactor = elementToCopy.randomSamplingFactor;

		for (AppenderMappingElement mapping : elementToCopy.appenders) {
			this.appenders.add(mapping.clone());
		}
		
		for (TriggerConfigElement trigger : elementToCopy.triggers) {
			this.triggers.add(trigger.clone());
		}
	}
	
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
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public ThreadTraceConfigElement clone() {
		return new ThreadTraceConfigElement(this);
	}
}

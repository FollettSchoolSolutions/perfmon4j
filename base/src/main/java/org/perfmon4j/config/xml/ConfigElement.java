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

public class ConfigElement {
	/**
	 * IMPORTANT IF YOU ADD ATTRIBUTES:  Make sure you update the copy constructor/clone!
	 */	
	private boolean enabled = true;
	private final List<AppenderConfigElement> appenders = new ArrayList<AppenderConfigElement>();
	private final List<MonitorConfigElement> monitors = new ArrayList<MonitorConfigElement>();
	private final List<SnapShotConfigElement> snapShots = new ArrayList<SnapShotConfigElement>();
	private final List<ThreadTraceConfigElement> threadTraces = new ArrayList<ThreadTraceConfigElement>();
	
	public ConfigElement() {
	}
	
	private ConfigElement(ConfigElement elementToCopy) {
		this.enabled = elementToCopy.enabled;
		for (AppenderConfigElement appenderElement : elementToCopy.appenders) {
			this.appenders.add(appenderElement.clone());
		}

		for (MonitorConfigElement monitorElement : elementToCopy.monitors) {
			this.monitors.add(monitorElement.clone());
		}
		
		for (SnapShotConfigElement snapShotElement : elementToCopy.snapShots) {
			this.snapShots.add(snapShotElement.clone());
		}
		
		for (ThreadTraceConfigElement threadTraceElement : elementToCopy.threadTraces) {
			this.threadTraces.add(threadTraceElement.clone());
		}
	}
	
	public List<AppenderConfigElement> getAppenders() {
		return appenders;
	}
	public List<MonitorConfigElement> getMonitors() {
		return monitors;
	}
	public List<SnapShotConfigElement> getSnapShots() {
		return snapShots;
	}
	public List<ThreadTraceConfigElement> getThreadTraces() {
		return threadTraces;
	}

	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	  ///*----------------------------------------------------------------------------*/
  	public AppenderConfigElement getAppender(String name) {
  		AppenderConfigElement result = null;
  		
  		for (AppenderConfigElement a : getAppenders()) {
  			if (name.equals(a.getName())) {
  				result = a;
  				break;
  			}
  		}
  		return result;
  	}

    ///*----------------------------------------------------------------------------*/
  	public MonitorConfigElement getMonitor(String name) {
  		MonitorConfigElement result = null;
  		
  		for (MonitorConfigElement a : getMonitors()) {
  			if (name.equals(a.getName())) {
  				result = a;
  				break;
  			}
  		}
  		return result;
  	}

    ///*----------------------------------------------------------------------------*/
  	public SnapShotConfigElement getSnapShot(String name) {
  		SnapShotConfigElement result = null;
  		
  		for (SnapShotConfigElement a : getSnapShots()) {
  			if (name.equals(a.getName())) {
  				result = a;
  				break;
  			}
  		}
  		return result;
  	}

    ///*----------------------------------------------------------------------------*/
  	public ThreadTraceConfigElement getThreadTrace(String name) {
  		ThreadTraceConfigElement result = null;
  		
  		for (ThreadTraceConfigElement a : getThreadTraces()) {
  			if (name.equals(a.getMonitorName())) {
  				result = a;
  				break;
  			}
  		}
  		return result;
  	}
  	
  	public ConfigElement clone() {
  		return new ConfigElement(this);
  	}
 	
//	public static ConfigElement mergeIntoRootConfig(ConfigElement rootConfig, ConfigElement config) {
//		return rootConfig;
//	}

  	
  	/**
  	 * This method performs something similar to an "UNION" logical operation
  	 * on configurations.  
  	 * 
  	 * In terms of deduping matching elements (Appenders, Monitors, SnapShots, ThreadTraces)
  	 * the config elements later in the list have precedence over the elements earlier
  	 * int the list. 
  	 * 
  	 * A clone operation is used so the configElement parameters are NOT modified.
  	 */
	public static ConfigElement mergeConfigs(ConfigElement...configElements) {
		ConfigElement result = configElements[0];
		
		for (int i = 1; i < configElements.length; i++) {
			result = mergeConfig(result, configElements[1]);
		}
		
		return result;
	}
	
	private static ConfigElement mergeConfig(ConfigElement configA, ConfigElement configB) {
		ConfigElement result = configA.clone();
		
		for (AppenderConfigElement a : configB.appenders) {
			AppenderConfigElement found = null;
			if ((found = result.getAppender(a.getName())) != null) {
				result.appenders.remove(found);
			}
			result.appenders.add(a.clone());
		}

		for (MonitorConfigElement a : configB.monitors) {
			MonitorConfigElement found = null;
			if ((found = result.getMonitor(a.getName())) != null) {
				result.monitors.remove(found);
			}
			result.monitors.add(a.clone());
		}

		for (SnapShotConfigElement a : configB.snapShots) {
			SnapShotConfigElement found = null;
			if ((found = result.getSnapShot(a.getName())) != null) {
				result.snapShots.remove(found);
			}
			result.snapShots.add(a.clone());
		}
		
		for (ThreadTraceConfigElement a : configB.threadTraces) {
			ThreadTraceConfigElement found = null;
			if ((found = result.getThreadTrace(a.getMonitorName())) != null) {
				result.threadTraces.remove(found);
			}
			result.threadTraces.add(a.clone());
		}
		
		return result;
	}
}

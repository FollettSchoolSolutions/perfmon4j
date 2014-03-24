package org.perfmon4j.config.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigElement {
	private boolean enabled = true;
	private final List<AppenderConfigElement> appenders = new ArrayList<AppenderConfigElement>();
	private final List<MonitorConfigElement> monitors = new ArrayList<MonitorConfigElement>();
	private final List<SnapShotConfigElement> snapShots = new ArrayList<SnapShotConfigElement>();
	private final List<ThreadTraceConfigElement> threadTraces = new ArrayList<ThreadTraceConfigElement>();
	private final Map<String, Boolean> systemPropertyState = new HashMap<String, Boolean>();
	
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

	public Map<String, Boolean> getSystemPropertyState() {
		return systemPropertyState;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}

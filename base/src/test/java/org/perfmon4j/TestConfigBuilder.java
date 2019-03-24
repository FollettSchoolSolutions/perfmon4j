package org.perfmon4j;

import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.Appender.AppenderID;

public class TestConfigBuilder {
	private final Map<String, String> map = new HashMap<String, String>();
	
	public TestConfigBuilder() {
	}

	public TestConfigBuilder defineRootMonitor(String pattern) {
		map.put(PerfMon.ROOT_MONITOR_NAME, pattern);
		return this;
	}
	
	public TestConfigBuilder defineMonitor(String monitorName, String pattern) {
		map.put(monitorName, pattern);
		return this;
	}

	public TestConfigBuilder clearMonitors() {
		map.clear();
		
		return this;
	}
	
	public PerfMonConfiguration build() {
		return build((AppenderID[])null);
	}
	
	public PerfMonConfiguration build(AppenderID... appenders) {
		PerfMonConfiguration config = new PerfMonConfiguration();
		if (appenders != null) {
			int counter = 0;
			for (AppenderID appenderID : appenders) {
				String appenderName = "default" + (counter++);
				config.defineAppender(appenderName, appenderID);
				
				for (Map.Entry<String, String> entry : map.entrySet()) {
					config.defineMonitor(entry.getKey());
					try {
						config.attachAppenderToMonitor(entry.getKey(), appenderName, entry.getValue());
					} catch (InvalidConfigException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		return config;
	}
}

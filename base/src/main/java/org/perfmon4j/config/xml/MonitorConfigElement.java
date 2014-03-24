package org.perfmon4j.config.xml;

import java.util.ArrayList;
import java.util.List;

public class MonitorConfigElement {
	private String name;
	private final List<AppenderMappingElement> appenders = new ArrayList<AppenderMappingElement>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<AppenderMappingElement> getAppenders() {
		return appenders;
	}
}

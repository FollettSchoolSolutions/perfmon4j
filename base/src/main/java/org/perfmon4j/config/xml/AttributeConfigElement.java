package org.perfmon4j.config.xml;

import java.util.Properties;

public abstract class AttributeConfigElement {
	private final Properties attributes = new Properties();
	private String key;
	
	
	public Properties getAttributes() {
		return attributes;
	}
	
	void pushKey(String key) {
		this.key = key;
	}
	
	void pushValue(String value) {
		attributes.put(key, value == null ? "" : value);
		key = null;
	}
}

package org.perfmon4j.remotemanagement.intf;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MonitorDataTransport implements Serializable {
	private static final long serialVersionUID = ManagementVersion.MAJOR_VERSION;
	
	private final MonitorDefinition definition;
	private final Map<String, Object> map = new HashMap<String, Object>(); 
	
	protected MonitorDataTransport(MonitorDefinition def) {
		this.definition = def;
	}
	
	protected void addElement(String key, Object value) {
		map.put(key, value);
	}
	
	public Object getElement(String key) {
		return map.get(key);
	}

	public MonitorDefinition getDefinition() {
		return definition;
	}
}

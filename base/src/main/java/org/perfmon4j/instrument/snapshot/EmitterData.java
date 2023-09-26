package org.perfmon4j.instrument.snapshot;

public class EmitterData {
	private final String category;
	private final String instanceName;
	private final long timestamp;
	
	EmitterData(String category, String instanceName, long timestamp) {
		super();
		this.category = category;
		this.instanceName = instanceName;
		this.timestamp = timestamp;
	}

	long getTimestamp() {
		return timestamp;
	}

	String getCategory() {
		return category;
	}

	String getInstanceName() {
		return instanceName;
	}
}

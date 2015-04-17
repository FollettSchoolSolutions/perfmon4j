package org.perfmon4j.restdatasource.data;

public enum AggregationType {
	NATURAL("NATURAL"),
	MAX("MAX"),
	MIN("MIN"),
	SUM("SUM"),
	AVERAGE("AVERAGE");

	private final String description;
	
	AggregationType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}
	
	public static final AggregationType[] DEFAULT = {MAX, MIN, SUM, AVERAGE};
	public static final AggregationType[] DEFAULT_WITH_NATURAL = {NATURAL, MAX, MIN, SUM, AVERAGE};
	public static final AggregationType[] NONE = {};
}

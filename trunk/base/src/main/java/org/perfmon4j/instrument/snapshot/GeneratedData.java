package org.perfmon4j.instrument.snapshot;

public interface GeneratedData {
    public String getName();
	public long getStartTime();
	public long getEndTime();
	public long getDuration();
	public String toAppenderString();
}

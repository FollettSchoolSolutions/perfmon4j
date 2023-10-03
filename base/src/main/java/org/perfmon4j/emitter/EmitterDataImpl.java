package org.perfmon4j.emitter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.MiscHelper;

public class EmitterDataImpl implements EmitterData, PerfMonObservableData {
	private final String name;
	private final long timestamp;
	private final String instanceName;
	private final Set<PerfMonObservableDatum<?>> observations = new HashSet<PerfMonObservableDatum<?>>();
	
	EmitterDataImpl(String name, String instanceName, long timestamp) {
		this.name = name;
		this.timestamp = timestamp;
		this.instanceName = instanceName;
		if (instanceName != null) {
			observations.add(PerfMonObservableDatum.newDatum("instanceName", instanceName));
		}
	}
  
	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	@Override
	public void addData(String fieldName, long value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	@Override
	public void addData(String fieldName, int value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	@Override
	public void addData(String fieldName, double value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	@Override
	public void addData(String fieldName, float value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}
	
	@Override
	public void addData(String fieldName, boolean value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}
	
	@Override
	public void addData(String fieldName, String value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	private void addData(PerfMonObservableDatum<?> datum) {
		observations.add(datum);
	}
	
	@Override
	public String toAppenderString() {
		SnapShotStringFormatter formatter = new SnapShotStringFormatter();
		
		final String sep = "********************************************************************************"; 
		
		String result = "\r\n" + sep;
		result += "\r\n" + getDataCategory();
		result += "\r\n" + MiscHelper.formatDateTimeAsString(getTimestamp(), true);
		
		for (PerfMonObservableDatum<?> datum : getObservations()) {
			result += "\r\n"+ formatter.format(25, datum.getDefaultDisplayName(), datum.isNumeric() ? datum.getValue() : datum.getComplexObject());
		}
		result += sep;
		
		
		return result;
	}

	@Override
	public Map<FieldKey, Object> getFieldData(FieldKey[] fields) {
		// Unused.. This is for external appenders that
		// poll for data.  This is not supported by emitters.
		return null;
	}

	@Override
	public Set<PerfMonObservableDatum<?>> getObservations() {
		return Collections.unmodifiableSet(observations);
	}

	@Override
	public String getDataCategory() {
		return name;
	}

	@Override
	public long getDurationMillis() {
		// Unused...
		return -1;
	}

	@Override
	public String getInstanceName() {
		return instanceName;
	}
	
	
}

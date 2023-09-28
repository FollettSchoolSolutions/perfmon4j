package org.perfmon4j.snapshot.emitter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.MiscHelper;

public class EmitterData implements PerfMonObservableData {
	private final String dataCategory;
	private final long timestamp;
	private final Set<PerfMonObservableDatum<?>> observations = new HashSet<PerfMonObservableDatum<?>>();
	
	EmitterData(String dataCategory, String instanceName, long timestamp) {
		super();
		this.dataCategory = dataCategory;
		this.timestamp = timestamp;
		if (instanceName != null) {
			observations.add(PerfMonObservableDatum.newDatum("instanceName", instanceName));
		}
	}
  
	@Override
	public long getTimestamp() {
		return timestamp;
	}
	
	public void addData(String fieldName, long value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	public void addData(String fieldName, int value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	public void addData(String fieldName, double value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}

	public void addData(String fieldName, float value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}
	
	public void addData(String fieldName, boolean value) {
		addData(PerfMonObservableDatum.newDatum(fieldName, value));
	}
	
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
		return dataCategory;
	}

	@Override
	public long getDurationMillis() {
		// Unused...
		return -1;
	}
}

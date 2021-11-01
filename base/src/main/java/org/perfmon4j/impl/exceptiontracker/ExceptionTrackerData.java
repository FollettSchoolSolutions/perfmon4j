package org.perfmon4j.impl.exceptiontracker;

import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.remotemanagement.intf.FieldKey;

public class ExceptionTrackerData extends SnapShotData implements PerfMonObservableData {
	private final Map<String, MeasurementElement> start;
	private final long startTimeMillis;
	private long endTimeMillis = PerfMon.NOT_SET;
	private Set<DeltaElement> dataSet = null;
	
	public ExceptionTrackerData(Map<String, MeasurementElement> start, long startTimeMillis) {
		this.start = start;
		this.startTimeMillis = startTimeMillis;
	}
	
	public ExceptionTrackerData stop(Map<String, MeasurementElement> end, long endTimeMillis) {
		this.endTimeMillis = endTimeMillis;
		this.dataSet = DeltaElement.createDeltaSet(start, end, Math.max(0L, endTimeMillis - startTimeMillis));
		
		return this;
	}
	
	@Override
	public String toAppenderString() {
		String result = "";
		if (dataSet != null) {
			for (DeltaElement element : dataSet) {
				result += element.getFieldName() + "=" + element.getCount().getDeltaPerMinute() + "\r\n";
			}
		} else {
			result = "SNAPSHOT NOT CREATED";
		}
			
		return result;
	}

	@Override
	public Map<FieldKey, Object> getFieldData(FieldKey[] fields) {
		return null;
	}

	@Override
	public Set<PerfMonObservableDatum<?>> getObservations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getTimestamp() {
		return endTimeMillis;
	}

	@Override
	public long getDurationMillis() {
		return endTimeMillis == PerfMon.NOT_SET ? PerfMon.NOT_SET : (endTimeMillis - startTimeMillis);
	}

	@Override
	public String getDataCategory() {
		return "ExceptionTracker";
	}
}

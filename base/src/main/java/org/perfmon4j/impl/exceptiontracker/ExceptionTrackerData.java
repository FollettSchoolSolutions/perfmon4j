/*
 *	Copyright 2021 Follett School Solutions, LLC 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	ddeucher@follett.com
 * 	David Deuchert
 * 	Follett School Solutions, LLC
 * 	1340 Ridgeview Dr
 * 	McHenry, IL 60050
*/
package org.perfmon4j.impl.exceptiontracker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.MiscHelper;

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
		String dataSetResult = "";
		if (dataSet != null) {
			for (DeltaElement element : dataSet) {
				dataSetResult += String.format(" %s",
					MiscHelper.formatTextDataLine(40, element.getFieldName(), 
					(float)element.getCount().getDeltaPerMinute(), " per minute", 2));
			}
		} 
		
		return String.format(
            "\r\n********************************************************************************\r\n" +
            "%s\r\n" +
            "%s -> %s\r\n" + 
            "%s" +
            "********************************************************************************",
            this.getName(),
            MiscHelper.formatTimeAsString(startTimeMillis),
            MiscHelper.formatTimeAsString(endTimeMillis),
            dataSetResult);
	}

	@Override
	public Map<FieldKey, Object> getFieldData(FieldKey[] fields) {
		return null;
	}

	@Override
	public Set<PerfMonObservableDatum<?>> getObservations() {
		Set<PerfMonObservableDatum<?>> result = new HashSet<PerfMonObservableDatum<?>>();
		
		
		addIfNotNull(result, PerfMonObservableDatum.newDateTimeDatumIfSet("timeStart", startTimeMillis));
		addIfNotNull(result, PerfMonObservableDatum.newDateTimeDatumIfSet("timeStop", endTimeMillis));
		if (dataSet != null) {
			for (DeltaElement element : dataSet) {
				addIfNotNull(result, PerfMonObservableDatum.newDatum(element.getFieldName(), element.getCount()));
				addIfNotNull(result, PerfMonObservableDatum.newDatum(element.getFieldName() + " SQL", element.getCount()));
			}
		} 

		return result;
	}

	@Override
	public long getTimestamp() {
		return endTimeMillis;
	}

	@Override
	public long getDurationMillis() {
		return endTimeMillis == PerfMon.NOT_SET ? PerfMon.NOT_SET : (endTimeMillis - startTimeMillis);
	}
	
	
    private void addIfNotNull(Set<PerfMonObservableDatum<?>> result, PerfMonObservableDatum<?> datum) {
    	if (datum != null) {
    		result.add(datum);
    	}
    }
	
	@Override
	public String getDataCategory() {
		return "ExceptionTracker";
	}
}

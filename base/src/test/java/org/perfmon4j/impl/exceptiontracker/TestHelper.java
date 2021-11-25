package org.perfmon4j.impl.exceptiontracker;

import org.mockito.Mockito;
import org.perfmon4j.PerfMon;
import org.perfmon4j.instrument.snapshot.Delta;

public class TestHelper extends org.perfmon4j.TestHelper {
	
	public static final Counter getMockCounter(long count) {
		return getMockCounter(count, PerfMon.NOT_SET);
	}
	
	@SuppressWarnings("boxing")
	public static final Counter getMockCounter(long count, long sqlCount) {
		Counter result = Mockito.mock(Counter.class);
		
		Mockito.when(result.getCount()).thenReturn(Long.valueOf(count));
		Mockito.when(result.getSQLCount()).thenReturn(Long.valueOf(sqlCount));
		
		return result;
	}
	
	public static final MeasurementElement getMockMeasurementElement(String fieldName, long count) {
		return getMockMeasurementElement(fieldName, count, PerfMon.NOT_SET);
	}
	
	public static final MeasurementElement getMockMeasurementElement(String fieldName, long count, long sqlCount) {
		return new MeasurementElement(fieldName, getMockCounter(count, sqlCount), true);
	}
	
	@SuppressWarnings("boxing")
	public static final Delta getMockDelta(String fieldName, double perMinute) {
		Delta result = Mockito.mock(Delta.class);
		Mockito.when(result.getDeltaPerMinute()).thenReturn(Double.valueOf(perMinute));
		
		return result;
	}
	
	public static final DeltaElement getMockDeltaElement(String fieldName, double perMinute) {
		return getMockDeltaElement(fieldName, perMinute, (double)PerfMon.NOT_SET);
	}
	
	public static final DeltaElement getMockDeltaElement(String fieldName, double perMinute, double perMinuteSQL) {
		DeltaElement result =  Mockito.mock(DeltaElement.class);
		Delta mockDelta = getMockDelta(fieldName, perMinute); 
		Mockito.when(result.getFieldName()).thenReturn(fieldName);
		Mockito.when(result.getCount()).thenReturn(mockDelta);
		
		if (perMinuteSQL >= 0) {
			Delta mockSQLDelta = getMockDelta(fieldName, perMinuteSQL); 
			Mockito.when(result.getSqlCount()).thenReturn(mockSQLDelta);
		}
	
		return result;
	}
}

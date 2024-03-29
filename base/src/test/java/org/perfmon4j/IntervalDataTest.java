/*
 *	Copyright 2008-2011 Follett Software Company 
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
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import java.util.Map;
import java.util.Set;

import org.mockito.Mockito;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.ThresholdCalculator;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class IntervalDataTest extends PerfMonTestCase {
    public static final String TEST_ALL_TEST_TYPE = "UNIT";

/*----------------------------------------------------------------------------*/
    public IntervalDataTest(String name) {
        super(name);
    }

/*----------------------------------------------------------------------------*/
    public void testCalcThroughput() throws Exception {
        // Perfmon with 5 minute duration....
        long now = System.currentTimeMillis();
        long fiveMinutesAgo = now - (5 * 60 * 1000);
    
        IntervalData data = new IntervalData(null, fiveMinutesAgo, null, null, now);
        data.setTotalCompletions(5*60);
        
        assertEquals("throughPutPerMinute", new Double(60.00), new Double(data.getThroughputPerMinute()));
    }

    
     public void testToString() throws Exception {
        final long NOW = System.currentTimeMillis();
        final long FIVE_MINUTES = (5 * 60 * 1000);
        final long FIVE_MINUTES_AGO = NOW - FIVE_MINUTES;
        
        IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), FIVE_MINUTES_AGO, null, null, NOW);
        data.setTotalCompletions(5*60);
        data.stop(FIVE_MINUTES, FIVE_MINUTES * FIVE_MINUTES, NOW, 0, 0);
        
        System.err.println(data.toString());
    }
   
     public void testGetFields() throws Exception {
    	 FieldKey[] fields = IntervalData.getFields(MonitorKey.newIntervalKey("a.b.c")).getFields();
    	 assertTrue("Should have at least 1 field...", fields.length > 0);

    	 Set<FieldKey> set = FieldKey.toSet(fields);
    	 
    	 FieldKey fieldAverage = FieldKey.parse("INTERVAL(name=a.b.c):FIELD(name=AverageDuration;type=LONG)");
    	 assertTrue("Should have average duration field", set.contains(fieldAverage));
     }

     
     public void testGetFieldData() throws Exception {
         final long NOW = System.currentTimeMillis();
         final long FIVE_MINUTES = (5 * 60 * 1000);
         final long FIVE_MINUTES_AGO = NOW - FIVE_MINUTES;
         
    	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), FIVE_MINUTES_AGO, null, null, NOW);
    	 data.setTotalHits(5);
    	 
    	 FieldKey[] fields = IntervalData.getFields(MonitorKey.newIntervalKey("a.b.c")).getFields();
    	 Map<FieldKey, Object> d = data.getFieldData(fields);
    	 
    	 FieldKey fieldTotalHits = FieldKey.parse("INTERVAL(name=a.b.c):FIELD(name=TotalHits;type=INTEGER)");
    	 Integer totalHits = (Integer)d.get(fieldTotalHits);
    	 
    	 assertNotNull("average", totalHits);
    	 assertEquals("totalHits", 5, totalHits.longValue());
     }

     public void testGetTimestampAndDuration() throws Exception {
         final long NOW = System.currentTimeMillis();
       	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-100, null, null, NOW);
       	 
       	 // Timestamp is always the end of the monitoring period.
       	 assertEquals("The end of the monitoring period", NOW, data.getTimestamp());
       	 assertEquals("The number of milliseconds this monitor was collecting data", 100, data.getDurationMillis());
     }
     
     public void testGetDataCategory() throws Exception {
         final long NOW = System.currentTimeMillis();
       	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-100, null, null, NOW);
       	 
       	 assertEquals("Interval data measurements are always prefixed with \"Interval.\"", 
       			 "Interval.a.b.c", data.getDataCategory());
     }
     
     @SuppressWarnings("boxing")
	public void testGetObservations() throws Exception {
         final long NOW = System.currentTimeMillis();
       	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-1, null, null, NOW);
       	 
       	 data = Mockito.spy(data);
       	 Mockito.when(data.getTotalHits()).thenReturn(Integer.valueOf(1));
       	 Mockito.when(data.getTotalCompletions()).thenReturn(Integer.valueOf(2));
       	 Mockito.when(data.getTotalDuration()).thenReturn(Long.valueOf(3));
       	 Mockito.when(data.getSumOfSquares()).thenReturn(Long.valueOf(4));
       	 Mockito.when(data.getTotalSQLDuration()).thenReturn(Long.valueOf(5));
       	 Mockito.when(data.getSumOfSQLSquares()).thenReturn(Long.valueOf(6));
       	 Mockito.when(data.getAverageDuration()).thenReturn(Long.valueOf(7));
       	 Mockito.when(data.getMaxDuration()).thenReturn(Long.valueOf(8));
       	 Mockito.when(data.getMinDuration()).thenReturn(Long.valueOf(9));
       	 Mockito.when(data.getStdDeviation()).thenReturn(Double.valueOf(10));
       	 Mockito.when(data.getAverageSQLDuration()).thenReturn(Long.valueOf(11));
       	 Mockito.when(data.getMaxSQLDuration()).thenReturn(Long.valueOf(12));
       	 Mockito.when(data.getMinSQLDuration()).thenReturn(Long.valueOf(13));
       	 Mockito.when(data.getSQLStdDeviation()).thenReturn(Double.valueOf(14));
       	 Mockito.when(data.getMaxActiveThreadCount()).thenReturn(Integer.valueOf(15));
       	 Mockito.when(data.getThroughputPerSecond()).thenReturn(Double.valueOf(16));
       	 Mockito.when(data.getOldestActiveThread()).thenReturn("MyThread");
       	 Mockito.when(data.getOldestActiveThreadDuration()).thenReturn(Long.valueOf(17));
       	 
       	 Set<PerfMonObservableDatum<?>> observations = data.getObservations();
       	 assertNotNull(observations);
       	 assertFalse(observations.isEmpty());

       	 validateObservation(observations, "totalHits", "1");
       	 validateObservation(observations, "totalCompletions", "2");
       	 validateObservation(observations, "durationSum", "3");
       	 validateObservation(observations, "durationSumOfSquares", "4");
       	 validateObservation(observations, "sqlDuration", "5");
       	 validateObservation(observations, "sqlDurationSumOfSquares", "6");
       	 validateObservation(observations, "averageDuration", "7");
       	 validateObservation(observations, "maxDuration", "8");
       	 validateObservation(observations, "minDuration", "9");
       	 validateObservation(observations, "stdDeviation", "10.000");
       	 validateObservation(observations, "averageSQLDuration", "11");
       	 validateObservation(observations, "maxSQLDuration", "12");
       	 validateObservation(observations, "minSQLDuration", "13");
       	 validateObservation(observations, "sqlStdDeviation", "14.000");
       	 validateObservation(observations, "maxActiveThreadCount", "15");
       	 validateObservation(observations, "throughputPerSecond", "16.000");
       	 validateObservation(observations, "oldestActiveThread", "MyThread");
       	 validateObservation(observations, "oldestActiveThreadDuration", "17");
     }

     @SuppressWarnings("boxing")
 	public void testGetDateTimeObservations() throws Exception {
        final long NOW = System.currentTimeMillis();
      	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-1, null, null, NOW);
      	 
      	 data = Mockito.spy(data);
      	 Mockito.when(data.getTimeMaxActiveThreadCountSet()).thenReturn(Long.valueOf(10000));
      	 Mockito.when(data.getTimeMaxDurationSet()).thenReturn(Long.valueOf(20000));
      	 Mockito.when(data.getTimeMinDurationSet()).thenReturn(Long.valueOf(30000));
      	 Mockito.when(data.getTimeMaxSQLDurationSet()).thenReturn(Long.valueOf(40000));
      	 Mockito.when(data.getTimeMinSQLDurationSet()).thenReturn(Long.valueOf(50000));
      	 Mockito.when(data.getTimeStart()).thenReturn(Long.valueOf(60000));
      	 Mockito.when(data.getTimeStop()).thenReturn(Long.valueOf(70000));
      	
      	 Set<PerfMonObservableDatum<?>> observations = data.getObservations();
      	 assertNotNull(observations);
      	 assertFalse(observations.isEmpty());

      	 validateObservation(observations, "maxActiveThreadCountSet", MiscHelper.formatTimeAsISO8601(10000));
      	 validateObservation(observations, "maxDurationSet", MiscHelper.formatTimeAsISO8601(20000));
      	 validateObservation(observations, "minDurationSet", MiscHelper.formatTimeAsISO8601(30000));
      	 validateObservation(observations, "maxSQLDurationSet", MiscHelper.formatTimeAsISO8601(40000));
      	 validateObservation(observations, "minSQLDurationSet", MiscHelper.formatTimeAsISO8601(50000));
      	 validateObservation(observations, "timeStart", MiscHelper.formatTimeAsISO8601(60000));
      	 validateObservation(observations, "timeStop", MiscHelper.formatTimeAsISO8601(70000));
    }
     
     @SuppressWarnings("boxing")
 	public void testGetDateTimeObservationsNotIncludedWhenNOT_SET() throws Exception {
        final long NOW = System.currentTimeMillis();
      	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-1, null, null, NOW);
      	 
      	 data = Mockito.spy(data);
      	 Mockito.when(data.getTimeMaxActiveThreadCountSet()).thenReturn(Long.valueOf(PerfMon.NOT_SET));
      	 Mockito.when(data.getTimeMaxDurationSet()).thenReturn(Long.valueOf(PerfMon.NOT_SET));
      	 Mockito.when(data.getTimeMinDurationSet()).thenReturn(Long.valueOf(PerfMon.NOT_SET));
      	 Mockito.when(data.getTimeMaxSQLDurationSet()).thenReturn(Long.valueOf(PerfMon.NOT_SET));
      	 Mockito.when(data.getTimeMinSQLDurationSet()).thenReturn(Long.valueOf(PerfMon.NOT_SET));
      	
      	 Set<PerfMonObservableDatum<?>> observations = data.getObservations();
      	 assertNull(PerfMonObservableDatum.findObservationByFieldName("maxActiveThreadCountSet", observations));
      	 assertNull(PerfMonObservableDatum.findObservationByFieldName("maxDurationSet", observations));
      	 assertNull(PerfMonObservableDatum.findObservationByFieldName("minDurationSet", observations));
      	 assertNull(PerfMonObservableDatum.findObservationByFieldName("maxSQLDurationSet", observations));
      	 assertNull(PerfMonObservableDatum.findObservationByFieldName("minSQLDurationSet", observations));
    }
     
 	public void testGetOptionalMedianObservations() throws Exception {
        final long NOW = System.currentTimeMillis();
      	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-1, null, null, NOW);
      	 
      	 // First try with no median calculator present.
      	 Set<PerfMonObservableDatum<?>> observations = data.getObservations();
      	 assertNull("Should not include a medianDuration", PerfMonObservableDatum.findObservationByFieldName("medianDuration", observations));

      	 
      	 // Now add a median calculator...should return median
      	 MedianCalculator calc = new MedianCalculator(10, 1);
      	 calc.putValue(1);
      	 data = Mockito.spy(data);
       	 Mockito.when(data.getMedianCalculator()).thenReturn(calc);

      	 observations = data.getObservations();
      	 validateObservation(observations, "medianDuration", "1.000");
    }
     

 	public void testGetOptionalThresholdObservation() throws Exception {
        final long NOW = System.currentTimeMillis();
      	 IntervalData data = new IntervalData(PerfMon.getMonitor("a.b.c"), NOW-1, null, null, NOW);
      	 
      	 // First try with no threshold calculator present
      	 Set<PerfMonObservableDatum<?>> observations = data.getObservations();
      	 assertNull("Should not include a percentOver_1_Second", PerfMonObservableDatum.findObservationByFieldName("percentOver_1_second", observations));

      	 // Now add a threshold calculator...should return percent
      	 ThresholdCalculator calc = new ThresholdCalculator("1 second");
      	 calc.putValue(1001);
      	 data = Mockito.spy(data);
       	 Mockito.when(data.getThresholdCalculator()).thenReturn(calc);

      	 observations = data.getObservations();
      	 validateObservation(observations, "percentOver_1_second", "100.000");
    }
 	
 	void validateObservation(Set<PerfMonObservableDatum<?>> observations, String fieldName, String expectedValue) {
 		PerfMonObservableDatumTest.validateObservation(observations, fieldName, expectedValue);
     }
     
   
/*----------------------------------------------------------------------------*/    
    public static void main(String[] args) {
        String[] testCaseName = {IntervalDataTest.class.getName()};

        TestRunner.main(testCaseName);
    }

/*----------------------------------------------------------------------------*/
    public static junit.framework.Test suite() {
        String testType = System.getProperty("UNIT");
        TestSuite newSuite = new TestSuite();

        // Here is where you can specify a list of specific tests to run.
        // If there are no tests specified, the entire suite will be set in the if
        // statement below.
//        newSuite.addTest(new IntervalDataTest("testGetFieldData"));

        // Here we test if we are running testunit or testacceptance (testType will
        // be set) or if no test cases were added to the test suite above, then
        // we run the full suite of tests.
        if (testType != null || newSuite == null || (newSuite.countTestCases() < 1)) {
            newSuite = new TestSuite(IntervalDataTest.class);
        }

        return( newSuite);
    }
}

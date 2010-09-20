/*
 *	Copyright 2008 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j;

import org.perfmon4j.util.MedianCalculator;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.ThresholdCalculator;
import org.perfmon4j.util.ThresholdCalculator.ThresholdResult;


public class IntervalData implements PerfMonData {
    private final PerfMon owner;
    private final long timeStart;
    private long timeStop = PerfMon.NOT_SET;
    private final MedianCalculator medianCalculator;
    private final ThresholdCalculator thresholdCalculator;
    private int totalHits = 0;
    
    private int maxActiveThreadCount = 0;
    private long timeMaxActiveThreadCountSet = PerfMon.NOT_SET;
    
    private int totalCompletions = 0;
    
    private long maxDuration = 0;
    private long timeMaxDurationSet = PerfMon.NOT_SET;
    
    private long minDuration = PerfMon.NOT_SET;
    private long timeMinDurationSet = PerfMon.NOT_SET;
    
    private long totalDuration = 0;
    private long sumOfSquares = 0;


    /** SQL Durations...  NOTE SQLDuration are only valid if SQLTime.isEnabled() **/
    private long maxSQLDuration = 0;
    private long timeMaxSQLDurationSet = PerfMon.NOT_SET;
    
    private long minSQLDuration = PerfMon.NOT_SET;
    private long timeMinSQLDurationSet = PerfMon.NOT_SET;
    
    private long totalSQLDuration = 0;
    private long sumOfSQLSquares = 0;

//    private long lifetimeMaxSQLDuration = 0;
//    private long timeLifetimeMaxSQLDurationSet = PerfMon.NOT_SET;
//    
//    private long lifetimeMinSQLDuration = PerfMon.NOT_SET;
//    private long timeLifetimeMinSQLDurationSet = PerfMon.NOT_SET;
//
//    private long lifetimeTotalSQLDuration = 0;
//    private long lifetimeSumOfSQLSquares = 0;
    
    /** SQL Durations END **/

    
    private long lifetimeStartTime = 0;
    private long lifetimeMaxDuration = 0;
    private long timeLifetimeMaxDurationSet = PerfMon.NOT_SET;
    
    private long lifetimeMinDuration = PerfMon.NOT_SET;
    private long timeLifetimeMinDurationSet = PerfMon.NOT_SET;
    
    private int lifetimeTotalCompletions = 0;
    private long lifetimeTotalDuration = 0;
    private long lifetimeSumOfSquares = 0;
    
    private int lifetimeMaxActiveThreadCount = 0;
    private long timeLifetimeMaxActiveThreadCountSet = PerfMon.NOT_SET;
    
    private MaxThroughput lifetimeMaxThroughputPerMinute = null;
    private boolean haveLifetimeStats = false;
    
    
    private long getLifetimeMaxDuration() {
        if (haveLifetimeStats) {
            return lifetimeMaxDuration;
        } else {
            return maxDuration;
        }
    }
    
    public int getLifetimeMaxThreadCount() {
        return lifetimeMaxActiveThreadCount;
    }

    public long getLifetimeMinDuration() {
        long result = minDuration;
        if (haveLifetimeStats) {
            result = lifetimeMinDuration;
        } 
        return Math.max(result, 0); // Mask PerfMon.NOT_SET
    }

    public long getLifetimeAverageDuration() {
        if (haveLifetimeStats) {
            long result = 0;
            if (lifetimeTotalCompletions > 0) {
                result = lifetimeTotalDuration / lifetimeTotalCompletions;
            }
            return result;
        } else {
            return getAverageDuration();
        }
    }
    
    private static long getPreciseMillis() {
        return System.nanoTime()/1000000;
    }
    
    
/*----------------------------------------------------------------------------*/    
    protected IntervalData(PerfMon owner) {
        this(owner, getPreciseMillis());
    }
    
/*----------------------------------------------------------------------------*/    
    protected IntervalData(PerfMon owner, long timeStart) {
        this(owner, timeStart, null);
    }

/*----------------------------------------------------------------------------*/    
    protected IntervalData(PerfMon owner, long timeStart, MedianCalculator medianCalculator) {
        this(owner, timeStart, medianCalculator, null, PerfMon.NOT_SET);
    }

/*----------------------------------------------------------------------------*/    
    protected IntervalData(PerfMon owner, long timeStart, MedianCalculator medianCalculator, 
        ThresholdCalculator thresholdCalculator) {
        this(owner, timeStart, medianCalculator, thresholdCalculator, PerfMon.NOT_SET);
    }
    
/*----------------------------------------------------------------------------*/    
    public IntervalData(PerfMon owner, long timeStart, MedianCalculator medianCalculator, 
        ThresholdCalculator thresholdCalculator, long timeStop) {
        this.owner = owner;
        this.timeStart = timeStart;
        this.timeStop = timeStop;
        this.medianCalculator = medianCalculator;
        this.thresholdCalculator = thresholdCalculator;
        
        if (owner != null) {
            haveLifetimeStats = true;
            
            lifetimeStartTime = owner.getStartTime();
            lifetimeMaxDuration = owner.getMaxDuration();
            timeLifetimeMaxDurationSet = owner.getTimeMaxDurationSet();
            
            // Go directly to the data member to 
            // avoid conversion of PerfMon.NOT_SET to 0;
            lifetimeMinDuration = owner.getMinDuration_NO_FIXUP();
            timeLifetimeMinDurationSet = owner.getTimeMinDurationSet();
            
            lifetimeTotalCompletions = owner.getTotalCompletions();
            lifetimeTotalDuration = owner.getTotalDuration();
            lifetimeSumOfSquares = owner.getSumOfSquares();
            
            lifetimeMaxActiveThreadCount = owner.getMaxActiveThreadCount();
            timeLifetimeMaxActiveThreadCountSet = owner.getTimeMaxActiveThreadCountSet();
        }
    }    
    
    
/*----------------------------------------------------------------------------*/
    /**
     * This constructor is for testing only, DO NOT USE for any other purposes!!!
     */    
    private IntervalData(long startTime, long endTime, PerfMon owner, long durationSumOfSquare,
        long maxDuration, int maxActiveThreads, int totalHits, int totalCompletions, MedianCalculator median, 
        ThresholdCalculator thresholdCalculator, long durationSum, long minDuration){

        this.owner = owner;
        this.timeStart = startTime;
        this.timeStop = endTime;
        this.medianCalculator = median;
        this.thresholdCalculator = thresholdCalculator;
        this.sumOfSquares = durationSumOfSquare;
        this.maxActiveThreadCount = maxActiveThreads;
        this.totalHits = totalHits;
        this.totalCompletions = totalCompletions;
        this.maxDuration = maxDuration;
        this.totalDuration = durationSum;
        this.minDuration = minDuration;                
    }    
    
    
    public static IntervalData newIntervalData_TEST_ONLY(long startTime, long endTime, String perfMonOwner, long durationSumOfSquare,
        long maxDuration, int maxActiveThreads, int totalHits, int totalCompletions, MedianCalculator median, long durationSum, long minDuration) {
        
        return new IntervalData(startTime, endTime, PerfMon.getMonitor(perfMonOwner), durationSumOfSquare,
            maxDuration, maxActiveThreads, totalHits, totalCompletions, median, null, durationSum, minDuration);
    }

    public static IntervalData newIntervalData_TEST_ONLY(long startTime, long endTime, String perfMonOwner, long durationSumOfSquare,
        long maxDuration, int maxActiveThreads, int totalHits, int totalCompletions, MedianCalculator median, ThresholdCalculator threshold, long durationSum, long minDuration) {
        
        return new IntervalData(startTime, endTime, PerfMon.getMonitor(perfMonOwner), durationSumOfSquare,
            maxDuration, maxActiveThreads, totalHits, totalCompletions, median, threshold, durationSum, minDuration);
    }
    
    
/*----------------------------------------------------------------------------*/    
    public int getMaxActiveThreadCount() {
        return maxActiveThreadCount;
    }
    
/*----------------------------------------------------------------------------*/    
    public long getMaxDuration() {
        return maxDuration;
    }

/*----------------------------------------------------------------------------*/    
    public long getMaxSQLDuration() {
        return maxSQLDuration;
    }
    
/*----------------------------------------------------------------------------*/    
    public long getMinDuration() {
        return Math.max(minDuration, 0); // Mask PerfMon.NOT_SET
    }

/*----------------------------------------------------------------------------*/    
    public long getMinSQLDuration() {
        return Math.max(minSQLDuration, 0); // Mask PerfMon.NOT_SET
    }
    
/*----------------------------------------------------------------------------*/    
    public int getTotalCompletions() {
        return totalCompletions;
    }
    
/*----------------------------------------------------------------------------*/    
    public long getTotalDuration() {
        return totalDuration;
    }

/*----------------------------------------------------------------------------*/    
    public int getTotalHits() {
        return totalHits;
    }
    
/*----------------------------------------------------------------------------*/    
    void start(int activeCount, long systemTime) {
        if (activeCount >= maxActiveThreadCount) {
            maxActiveThreadCount = activeCount;
            timeMaxActiveThreadCountSet = systemTime;
        }
        if (haveLifetimeStats && (activeCount >= lifetimeMaxActiveThreadCount)) {
            lifetimeMaxActiveThreadCount = activeCount;
            timeLifetimeMaxActiveThreadCountSet = systemTime;
        }

        totalHits++;
    }
    
    
/*----------------------------------------------------------------------------*/    
    void stop(long duration, long durationSquared, long systemTime, long sqlDuration, long sqlDurationSquared) {
        totalCompletions++;
        if (SQLTime.isEnabled()) {
            if (sqlDuration >= maxSQLDuration) {
                maxSQLDuration = sqlDuration;
                timeMaxSQLDurationSet = systemTime;
            }
            if ((sqlDuration <= minSQLDuration) || minSQLDuration == PerfMon.NOT_SET) {
                minSQLDuration = sqlDuration;
                timeMinSQLDurationSet = systemTime;
            }
            totalSQLDuration += sqlDuration;
            sumOfSQLSquares += sqlDurationSquared;
        }
        
        if (duration >= maxDuration) {
            maxDuration = duration;
            timeMaxDurationSet = systemTime;
        }
        if ((duration <= minDuration) || minDuration == PerfMon.NOT_SET) {
            minDuration = duration;
            timeMinDurationSet = systemTime;
        }
        
        if (medianCalculator != null) {
            medianCalculator.putValue(duration);
        }
        
        if (thresholdCalculator != null) {
            thresholdCalculator.putValue(duration);
        }
        
        totalDuration += duration;
        sumOfSquares += durationSquared;
        
        if (haveLifetimeStats) {
            lifetimeTotalCompletions++;
            if (duration >= lifetimeMaxDuration) {
                lifetimeMaxDuration = duration;
                timeLifetimeMaxDurationSet = systemTime;
            }
            if ((duration <= lifetimeMinDuration) || lifetimeMinDuration == PerfMon.NOT_SET) {
                lifetimeMinDuration = duration;
                timeLifetimeMinDurationSet = systemTime;
            }
            lifetimeTotalDuration += duration;
            lifetimeSumOfSquares += durationSquared;
        }
    }
    
/*----------------------------------------------------------------------------*/    
    void setTimeStop(long timeStop) {
        this.timeStop = timeStop;
    }
    
/*----------------------------------------------------------------------------*/    
    public PerfMon getOwner() {
        return owner;
    }
    
/*----------------------------------------------------------------------------*/    
    public long getTimeStart() {
        return timeStart;
    }
    
/*----------------------------------------------------------------------------*/    
    public long getTimeStop() {
        return timeStop;
    }

/*----------------------------------------------------------------------------*/    
    void setMaxActiveThreadCount(int maxActiveCount) {
        this.maxActiveThreadCount = maxActiveCount;
    }

/*----------------------------------------------------------------------------*/    
    void setMaxDuration(long maxDuration) {
        this.maxDuration = maxDuration;
    }

/*----------------------------------------------------------------------------*/    
    void setMinDuration(long minDuration) {
        this.minDuration = minDuration;
    }

/*----------------------------------------------------------------------------*/    
    void setTotalCompletions(int totalCompletions) {
        this.totalCompletions = totalCompletions;
    }

/*----------------------------------------------------------------------------*/    
    void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

/*----------------------------------------------------------------------------*/    
    void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

/*----------------------------------------------------------------------------*/    
    public double getThroughputPerMinute() {
        double result = 0.0;
        // Use current time if the data has not been stopped yet...
        long timeMillis = (timeStop == PerfMon.NOT_SET ? MiscHelper.currentTimeWithMilliResolution() : timeStop) - timeStart;
        if (timeMillis > 0) {
            result = ((double)totalCompletions / timeMillis) * 1000 * 60;
        }
        return result;
    }

/*----------------------------------------------------------------------------*/    
    public long getAverageDuration() {
        long result = 0;
        if (totalCompletions > 0) {
            result = totalDuration / totalCompletions;
        }
        return result;
    }
    

    /*----------------------------------------------------------------------------*/    
    public long getAverageSQLDuration() {
        long result = 0;
        if (totalCompletions > 0) {
            result = totalSQLDuration / totalCompletions;
        }
        return result;
    }
    
    
    private static String formatTimeDataSet(long time) {
        String result = "";
        
        if (time != PerfMon.NOT_SET) {
            result = "(" + MiscHelper.formatDateTimeAsString(time, true) + ")";
        }
        
        return result;
    }

    private String buildMedianString() {
        String result = "";
        
        if (medianCalculator != null) {
            result = " Median Duration.... " + medianCalculator.getMedianAsString() + "\r\n";
        }
        
        return result;
    }

    private String buildThresholdString() {
        String result = "";
        
        if (thresholdCalculator != null) {
            long trArray[] = thresholdCalculator.getThresholdMillis();
            for (int i = 0; i < trArray.length; i++) {
                ThresholdResult t = thresholdCalculator.getResult(trArray[i]);
                result += String.format(" %19.19s %s\r\n",
                    t.getDescription() + "........................",
                    t.getPercentOverThresholdAsString());
            }
        }
        
        return result;
    }
    
    
    
    public String toAppenderString() {
        String name = "Unknown";
        if (owner != null) {
            name = owner.getName();
        }

        String sqlDurationInfo = "";
        if (SQLTime.isEnabled()) {
        	sqlDurationInfo = String.format(
	                " (SQL)Avg. Duration. %.2f\r\n" +
	                " (SQL)Std. Dev...... %.2f\r\n" +
	                " (SQL)Max Duration.. %d %s\r\n" +
	                " (SQL)Min Duration.. %d %s\r\n",
	                new Double(getAverageSQLDuration()),
	                new Double(getStdDeviationSQL()),
	                new Long(getMaxSQLDuration()), 
	                formatTimeDataSet(timeMaxSQLDurationSet),
	                new Long(getMinSQLDuration()),
	                formatTimeDataSet(timeMinSQLDurationSet)
	            );
        }
        
        String result = String.format(
            "\r\n********************************************************************************\r\n" +
            "%s\r\n" +
            "%s -> %s\r\n" +   
            " Max Active Threads. %d %s\r\n" + 
            " Throughput......... %.2f per minute\r\n" +
            " Average Duration... %.2f\r\n" +
            "%s" +
            "%s" +
            " Standard Deviation. %.2f\r\n" +
            " Max Duration....... %d %s\r\n" +
            " Min Duration....... %d %s\r\n" +
            " Total Hits......... %d\r\n" +
            " Total Completions.. %d\r\n" +
            "%s",
            name,
            MiscHelper.formatTimeAsString(getTimeStart()),
            MiscHelper.formatTimeAsString(getTimeStop()),
            new Integer(getMaxActiveThreadCount()),
            formatTimeDataSet(timeMaxActiveThreadCountSet),
            new Double(getThroughputPerMinute()),
            new Double(getAverageDuration()),
            buildMedianString(),
            buildThresholdString(),
            new Double(getStdDeviation()),
            new Long(getMaxDuration()), 
            formatTimeDataSet(timeMaxDurationSet),
            new Long(getMinDuration()),
            formatTimeDataSet(timeMaxDurationSet),
            new Long(getTotalHits()), 
            new Long(getTotalCompletions()),
            sqlDurationInfo
        );
        if (haveLifetimeStats) {
            result += String.format(
                "Lifetime (%s):\r\n" +   
                " Max Active Threads. %d %s\r\n" + 
                " Max Throughput..... %s\r\n" +
                " Average Duration... %.2f\r\n" +
                " Standard Deviation. %.2f\r\n" +
                " Max Duration....... %d %s\r\n" +
                " Min Duration....... %d %s\r\n",
                MiscHelper.formatDateTimeAsString(lifetimeStartTime),
                new Integer(getLifetimeMaxThreadCount()),
                formatTimeDataSet(timeLifetimeMaxActiveThreadCountSet),
                lifetimeMaxThroughputPerMinute == null ? "" : lifetimeMaxThroughputPerMinute,
                new Double(getLifetimeAverageDuration()),
                new Double(getLifetimeStdDeviation()),
                new Long(getLifetimeMaxDuration()),
                formatTimeDataSet(timeLifetimeMaxDurationSet),
                new Long(getLifetimeMinDuration()),
                formatTimeDataSet(timeLifetimeMinDurationSet)
            );
        }
        
        result += "********************************************************************************";
        
        return result;
    }

    MaxThroughput refreshMonitorsMaxThroughputPerMinute(MaxThroughput max) {
        double throughput = getThroughputPerMinute();
        if (max == null || (throughput > max.getThroughputPerMinute())) {
            max = new MaxThroughput(this.timeStart, this.timeStop, throughput);
        }
        this.lifetimeMaxThroughputPerMinute = max;
        return max;
    }

    public long getTimeLifetimeMaxActiveThreadCountSet() {
        return timeLifetimeMaxActiveThreadCountSet;
    }

    public long getTimeLifetimeMaxDurationSet() {
        return timeLifetimeMaxDurationSet;
    }

    public long getTimeLifetimeMinDurationSet() {
        return timeLifetimeMinDurationSet;
    }

    public long getTimeMaxActiveThreadCountSet() {
        return timeMaxActiveThreadCountSet;
    }

    public long getTimeMaxDurationSet() {
        return timeMaxDurationSet;
    }

    public long getTimeMinDurationSet() {
        return timeMinDurationSet;
    }

    public long getLifetimeStartTime() {
        return lifetimeStartTime;
    }
    
    public double getStdDeviation() {
        return MiscHelper.calcStdDeviation(totalCompletions, totalDuration, sumOfSquares);
    }

    public double getStdDeviationSQL() {
        return MiscHelper.calcStdDeviation(totalCompletions, totalSQLDuration, sumOfSQLSquares);
    }
    
    public long getSumOfSquares() {
        return this.sumOfSquares;
    }
    
    public double getLifetimeStdDeviation() {
        return MiscHelper.calcStdDeviation(lifetimeTotalCompletions, lifetimeTotalDuration, lifetimeSumOfSquares);
    }
 
    public MedianCalculator getMedianCalculator(){
        return this.medianCalculator;
    }

    public ThresholdCalculator getThresholdCalculator(){
        return this.thresholdCalculator;
    }
    
    public String toString() {
        return "PerfMonData(owner=" + owner +
            " timeStart=" + MiscHelper.formatDateTimeAsString(timeStart) +
            " timeStop=" + (timeStop == -1 ? "running" : MiscHelper.formatDateTimeAsString(timeStop)) +
            ")";
    }

//    public static void main(String args[]) {
//        String sqlDurationInfo = String.format(
//	                " (SQL)Avg. Duration. %.2f \r\n" +
//	                " (SQL)Std. Dev...... %.2f \r\n" +
//	                " (SQL)Max Duration.. %d %s\r\n" +
//	                " (SQL)Min Duration.. %d %s\r\n", 
//	                new Double(1),
//	                new Double(1),
//	                new Long(1), 
//	                formatTimeDataSet(0),
//	                new Long(1),
//	                formatTimeDataSet(0)
//	            );
//    }	
}
    

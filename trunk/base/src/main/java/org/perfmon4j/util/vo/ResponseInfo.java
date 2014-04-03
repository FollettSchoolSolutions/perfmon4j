package org.perfmon4j.util.vo;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.perfmon4j.util.MiscHelper;


public abstract class ResponseInfo {
    public abstract long getStartTime();
    public abstract long getEndTime();
    public abstract String getMonitorName();
    public abstract long getMaxThreads();
    public abstract double getThroughput();
    public abstract long getTotalHits();
    public abstract long getMaxDuration();
    public abstract long getMinDuration();
    public abstract long getSum();
    public abstract long getSumOfSquares();
    public abstract long getTotalCompletions();
    
    public double getAverageDuration() {
        return MiscHelper.safeDivide(getSum(), getTotalCompletions());
    }

    public int getHour() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(getStartTime());
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public int getMinute() {
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(getStartTime());
        return cal.get(Calendar.MINUTE);
    }

    public boolean isActive() {
        return (getSum() > 0) || (getTotalCompletions() > 0) || (getTotalHits() > 0) || (getMaxThreads() > 0);
    }
}




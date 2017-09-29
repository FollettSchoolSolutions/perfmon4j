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

package org.perfmon4j.util;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


/**
 * The purpose of this class is to calculate
 * the median of a set of n results while restricting
 * the total number of elements stored.
 * 
 * Based on the number of elements allowed (maxElements),
 * the rounding factor used (factor) and the bell shaped 
 * distribution an accurate median should be returned.
 * 
 * If the elements allowed is insufficient to determine an
 * exact median you will receive an estimated median indicating the
 * actual result is greater than or equal to the value returned or less than or equal to
 * the value returned.
 */
public class MedianCalculator {
    private final Object lockToken = new Object();
    private final int maxElements;
    private final int factor;
    private long totalValuesCount = 0;
    private TreeMap<Long, OccuranceCount> valueMap = new TreeMap();
    
    public static final int OVERFLOW_NONE = 0;
    public static final int OVERFLOW_HIGH = 1;
    public static final int OVERFLOW_LOW = -1;
    
    /**
     * For purposes of median calculations the default
     * will round the medians to the nearest 100 milliseconds
     * and the midpoint of any set to move +- 25 seconds without
     * requiring an estimate.
     * 
     * This setting should be acceptable for tracking medians on a
     * web request.  For more grainular timings you would 
     * probably want to decrease the factor to 10 (since the java timer
     * is around a 10ms window) and perhaps increase the max elements.
     */
    public static final int DEFAULT_MAX_ELEMENTS = 500;
    public static final int DEFAULT_FACTOR = 100;
    

/*----------------------------------------------------------------------------*/    
    public MedianCalculator() {
        this(DEFAULT_MAX_ELEMENTS);
    }
    
/*----------------------------------------------------------------------------*/    
    public MedianCalculator(int maxElements) {
        this(maxElements, DEFAULT_FACTOR);
    }
    
/*----------------------------------------------------------------------------*/    
    public MedianCalculator(int maxElements, int factor) {
        this.maxElements = maxElements;
        this.factor = factor;
    }

/*----------------------------------------------------------------------------*/    
    public MedianCalculator(String params) {
        Integer maxElementsParsed = MiscHelper.parseInteger("maxElements", params);
        Integer factorParsed = MiscHelper.parseInteger("factor", params);
        
        
        this.maxElements = maxElementsParsed != null ? maxElementsParsed.intValue() : DEFAULT_MAX_ELEMENTS;
        this.factor = factorParsed != null ? factorParsed.intValue() : DEFAULT_FACTOR;
    }
    

/*----------------------------------------------------------------------------*/    
    public MedianCalculator clone() {
        return new MedianCalculator(this.maxElements, this.factor);
    }
    
/*----------------------------------------------------------------------------*/    
    /** todo For performance reasons we may need to create a queue
     * for storing values when we need to insert beyound this  **/
    public void putValue(long value) {
        if (factor > 1) {
            value = Math.round((double)value / factor);
        }
        
        final Long key = new Long(value);

        synchronized(lockToken) {
            OccuranceCount count = valueMap.get(key);
            totalValuesCount++;
            if (count != null) {
                count.incCount();
            } else {
                if (valueMap.size() >= maxElements) {
                    /** todo For performance reasons we may need to create a queue
                     * storing values outside the normal range...  
                     **/
                    long lowValue = valueMap.firstKey().longValue();
                    long highValue = valueMap.lastKey().longValue();
                    
                    boolean insertHigh = 
                        (value > lowValue) &&
                        ((value > highValue) || 
                        ((value - lowValue) <  (highValue - value)));
                    
                    if (insertHigh) {
                        Long lastValue = valueMap.lastKey();
                        count = valueMap.get(lastValue);
                        
                        if (lastValue.longValue() < value) {
                            count.incCount();
                            count.setOverflowTop(true);
                        } else {
                            long overflowCount = count.getCount();
                            valueMap.remove(lastValue);
    
                            count = new OccuranceCount();
                            valueMap.put(key, count);
    
                            count = valueMap.get(valueMap.lastKey());
                            count.setOverflowTop(true);
                            count.incCount(overflowCount);
                        }
                    } else { // !insertHigh 
                        Long firstValue = valueMap.firstKey();
                        count = valueMap.get(firstValue);
                        
                        if (firstValue.longValue() > value) {
                            count.incCount();
                            count.setOverflowBottom(true);
                        } else {
                            long overflowCount = count.getCount();
                            valueMap.remove(firstValue);
    
                            count = new OccuranceCount();
                            valueMap.put(key, count);
    
                            count = valueMap.get(valueMap.firstKey());
                            count.setOverflowBottom(true);
                            count.incCount(overflowCount);
                        }
                    }
                } else {
                    count = new OccuranceCount();
                    valueMap.put(key, count);
                }
            }
        }
    }

/*----------------------------------------------------------------------------*/    
    public MedianResult getMedian() {
        MedianResult result = new MedianResult();
        
        if (totalValuesCount > 0) {
            Long resultA = null;
            Long resultB = null;
            boolean need2Values =  (totalValuesCount % 2) == 0;
            long totalInstances = totalValuesCount / 2;
            if (need2Values) {
                totalInstances--;
            }
            synchronized(lockToken) {
                Iterator<Map.Entry<Long, OccuranceCount>> entryItr = valueMap.entrySet().iterator();
                while (totalInstances >= 0 ) {
                    Map.Entry<Long, OccuranceCount> entry = entryItr.next();
                    OccuranceCount counter = entry.getValue();
                    resultA = entry.getKey();
                    resultB = entry.getKey();
                    totalInstances -= counter.getCount();
                    result.setOverflowFlag(counter.isOverflowTop(), counter.isOverflowBottom());
                    if (need2Values && totalInstances == -1) {
                        entry = entryItr.next();
                        resultB = entry.getKey();
                        // If we have to average 2 seperate values we are never overflow..
                        result.setOverflowFlag(false, false);
                    }
                }
            }
            result.result = new Double((resultA.doubleValue() + resultB.doubleValue()) / 2 * factor);
        }
        return result;
    }
    
/*----------------------------------------------------------------------------*/    
    public static class MedianResult {
        private Double result;
        private int overflowFlag = OVERFLOW_NONE;
        
        private MedianResult() {
        }
        
        private void setOverflowFlag(boolean overflowHigh, boolean overflowLow) {
            if (overflowHigh) {
                overflowFlag = OVERFLOW_HIGH; 
            } else if (overflowLow) {
                overflowFlag = OVERFLOW_LOW;
            } else {
                overflowFlag = OVERFLOW_NONE;
            }
        }
        
        public Double getResult() {
            return result;
        }
        
        public int getOverflowFlag() {
            return overflowFlag;
        }
    }
    
    
/*----------------------------------------------------------------------------*/    
    private static class OccuranceCount {
        private long count;
        private boolean overflowTop;
        private boolean overflowBottom;

        private OccuranceCount() {
            count = 1;
        }
        
        private OccuranceCount(long count) {
            this.count = count;
        }
        
        private void incCount() {
            count++;
        }

        private void incCount(long value) {
            count += value;
        }
        
        private long getCount() {
            return count;
        }
        
        private void setOverflowTop(boolean overflowTop) {
            this.overflowTop = overflowTop;
        }
        
        private boolean isOverflowTop() {
            return overflowTop;
        }

        private void setOverflowBottom(boolean overflowBottom) {
            this.overflowBottom = overflowBottom;
        }
        
        private boolean isOverflowBottom() {
            return overflowBottom;
        }
    
    }

/*----------------------------------------------------------------------------*/    
    public String getMedianAsString() {
        String result = "";
        MedianResult r = getMedian();
        if (r.result != null) {
            if (r.getOverflowFlag() == OVERFLOW_HIGH) {
                result = ">= ";
            } else if (r.getOverflowFlag() == OVERFLOW_LOW) {
                result = "<= ";
            }
            result += r.result.toString();
        } else {
            result = "NA";
        }
        
        return result;
    }

    public int getFactor() {
        return factor;
    }

    public int getMaxElements() {
        return maxElements;
    }
    
}

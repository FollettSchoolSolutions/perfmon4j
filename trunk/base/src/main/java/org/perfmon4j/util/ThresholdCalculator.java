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

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;


public class ThresholdCalculator {
    private final ThresholdResult[] resultArray;
    private final long thresholdMillis[];
    private int totalCount;
    
/*----------------------------------------------------------------------------*/    
    public ThresholdCalculator(long[] values) {
        // Don't modify the array passed in...
        thresholdMillis = new long[values.length];
        System.arraycopy(values, 0, thresholdMillis, 0, values.length);
        Arrays.sort(thresholdMillis);
        
        resultArray = new ThresholdResult[thresholdMillis.length];
        for (int i = 0; i < thresholdMillis.length; i++) {
            resultArray[i] = new ThresholdResult(thresholdMillis[i]);
        }
        
    }
    
/*----------------------------------------------------------------------------*/    
    public ThresholdCalculator(String values) {
        this(convertStringToMillis(values));
    }
    
    
    private static long[] convertStringToMillis(String values) {
        Vector<Long> list = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(values, ",");
        while (tokenizer.hasMoreTokens()) {
            String nextToken = tokenizer.nextToken();
            long converted = MiscHelper.convertIntervalStringToMillis(nextToken, -1);
            list.add(new Long(converted));
        }

        long result[] = new long[list.size()];
        int index = 0;
        for (Long value : list) {
            result[index++] = value.longValue();
        }
        
        return result;
    }
/*----------------------------------------------------------------------------*/    
    /**
     * IMPORTANT!!! clone does NOT copy current counts...
     * This just creates an empty shell of the ThresholdCalculator Object
     */
    public ThresholdCalculator clone() {
        return new ThresholdCalculator(thresholdMillis);
    }
    
/*----------------------------------------------------------------------------*/    
    public void putValue(long millis) {
        boolean incremented = true;
        for (int i = 0; i < resultArray.length && incremented; i++) {
            incremented = resultArray[i].incIfOver(millis);
        }
        totalCount++;
    }
    
/*----------------------------------------------------------------------------*/    
    public long[] getThresholdMillis() {
        return thresholdMillis;
    }
    
/*----------------------------------------------------------------------------*/    
    public static class ThresholdResult {
        final long threshold;
        int totalCount = 0;
        int countOverThreshold = 0;
        
        ThresholdResult(long threshold) {
            this.threshold = threshold;
        }
        
        public long getThreshold() {
            return threshold;
        }
        
        public int getCountOverThreshold() {
            return countOverThreshold;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public float getPercentOverThreshold() {
            return totalCount > 0 ? ((float)countOverThreshold / (float)totalCount) * 100 : 0;
        }
        
        private void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }
        
        private boolean incIfOver(long millis) {
            boolean result = false;
            if (millis > threshold) {
                result = true;
                countOverThreshold++;
            }
            return result;
        }
        
        public String getDescription() {
            return "> " + MiscHelper.getMillisDisplayable(threshold);
        }
        
        public String getPercentOverThresholdAsString() {
            return String.format(
                "%.2f%s",
                getPercentOverThreshold(),
                "%");
        }
        
        public String toString() {
            return getPercentOverThresholdAsString() 
                + " " 
                + getDescription();
        }
    }

    public ThresholdResult getResult(long millis) {
        ThresholdResult result = null;
        int offset = Arrays.binarySearch(thresholdMillis, millis);
        if (offset > -1) {
            result = resultArray[offset];
            /** @todo Might be nice to clone this **/
            result.setTotalCount(totalCount);
        }
        
        return result;
    }
    
    
}

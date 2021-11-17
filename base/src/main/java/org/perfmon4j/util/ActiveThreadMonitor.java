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
package org.perfmon4j.util;

import java.util.Arrays;

/**
 * This class can be added to an IntervalData method.  
 * When it is found the interval data will included detailed
 * observations for active threads that have exceeded the
 * threshold. 
 * 
 * Unlike the ThresholdCalculator this object does NO calculations
 * it is simply provided to indicate which fields should be
 * added to the output based on the thresholds provided.
 * 
 * If not provided IntervalData will simply return the thread name
 * and current duration of the oldest active thread.
 * @author ddeucher
 */
public class ActiveThreadMonitor {
    private final long activeThresholdMillis[];
    
/*----------------------------------------------------------------------------*/    
    public ActiveThreadMonitor(long[] values) {
        // Don't modify the array passed in...
    	activeThresholdMillis = new long[values.length];
        System.arraycopy(values, 0, activeThresholdMillis, 0, values.length);
        Arrays.sort(activeThresholdMillis);
    }
    
/*----------------------------------------------------------------------------*/    
    public ActiveThreadMonitor(String values) {
        this(ThresholdCalculator.convertStringToMillis(values));
    }

/*----------------------------------------------------------------------------*/    
	public long[] getActiveThresholdMillis() {
		return activeThresholdMillis;
	}
}

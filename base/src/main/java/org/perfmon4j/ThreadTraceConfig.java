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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.perfmon4j.Appender.AppenderID;


public class ThreadTraceConfig {
    private int maxDepth = 0;
    private int minDurationToCapture = 0;
    private int randomSamplingFactor = 0;
    private final Set<AppenderID> appenders = new HashSet<AppenderID>();
    private final Random random = new Random();
    
    public ThreadTraceConfig() {
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMinDurationToCapture() {
        return minDurationToCapture;
    }

    public void setMinDurationToCapture(int minDurationToCapture) {
        this.minDurationToCapture = minDurationToCapture;
    }
    
    public void addAppender(AppenderID appenderID) {
        appenders.add(appenderID);
    }
    
    public void removeAppender(AppenderID appenderID) {
        appenders.remove(appenderID);
    }
    
    public AppenderID[] getAppenders() {
        return appenders.toArray(new AppenderID[]{});
    }

    public void setRandomSamplingFactor(int randomSamplingFactor) {
        this.randomSamplingFactor = randomSamplingFactor;
    }
    
    public int getRandomSamplingFactor() {
        return randomSamplingFactor;
    }
    
    public boolean shouldTraceBasedOnRandomSamplingFactor() {
        boolean result = true;
        
        if (randomSamplingFactor > 1) {
            result = random.nextInt(randomSamplingFactor-1) == 0;
        }
        return result;
    }
}

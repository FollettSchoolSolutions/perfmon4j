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

package org.perfmon4j.demo;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;

/**
 * This is an example of a very simple SnapShotMonitor...  
 * It collects two attributes, the totalMemory and freeMemory
 * based on the JVM.
 * @author dave
 *
 */
@SnapShotProvider(type = SnapShotProvider.Type.STATIC)
public class JVMMemory {
   
	@SnapShotGauge
	public static long getFreeMemory() {
		return Runtime.getRuntime().freeMemory();
	}
	
	@SnapShotGauge
	public static long getTotalMemory() {
		return Runtime.getRuntime().totalMemory();
	}
    
    public static void main(String args[]) throws Exception {
    	System.setProperty("PERFMON_APPENDER_ASYNC_TIMER_MILLIS", "500");
    	
    	BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.perfmon4j").setLevel(Level.DEBUG);
   	
    	
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("SimpleAppender", TextAppender.class.getName(), "1 second");
        config.defineSnapShotMonitor("JVM Memory", JVMMemory.class.getName());
        config.attachAppenderToSnapShotMonitor("JVM Memory", "SimpleAppender");
        
        
        PerfMon.configure(config);
        System.out.println("Sleeping for 5 seconds -- Will take a JVM SnapShot every 1 second");
        Thread.sleep(5000);
    }
}

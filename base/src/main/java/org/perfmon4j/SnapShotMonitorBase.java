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
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j;


import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.FailSafeTimerTask;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

abstract class SnapShotMonitorBase<T extends Object> implements SnapShotMonitorLifecycle {
    final private static Logger logger = LoggerFactory.initLogger(SnapShotMonitorBase.class);
    
    final private String name;
    final private Object lockToken = new Object();
    private boolean usePriorityTimer = false;
    final private Map<AppenderID, MonitorTimerTask<T>> map = new HashMap();
    
    private boolean active = true; /** Active goes to false when deInit is invoked **/
    

/*----------------------------------------------------------------------------*/    
    protected SnapShotMonitorBase(String name) {
        this(name, false);
    }
    
/*----------------------------------------------------------------------------*/    
    protected SnapShotMonitorBase(String name, boolean usePriorityTimer) {
        this.name = name;
        this.usePriorityTimer = usePriorityTimer;
    }
    
/*----------------------------------------------------------------------------*/    
    public String getName() {
        return name;
    }

    public T initSnapShot(long currentTimeMillis) {
        return null;
    }
    
    public boolean isActive() {
    	return active;
    }
    
    public void deInit() {
    	active = false;
    }
    
/*----------------------------------------------------------------------------*/    
    /**
     * @param data - Returns SnapShotData returned by initSnapShot() or
     * null if initSnapShot() did not return a SnapShotData.
     * @param currentTimeMillis - Current System Time
     * @return - Return a SnapShotData instance that will be passed to the appender.
     */
    public abstract T takeSnapShot(T data, long currentTimeMillis);

/*----------------------------------------------------------------------------*/    
    public void addAppender(AppenderID appenderID) throws InvalidConfigException {
        synchronized(lockToken) {
            if (!map.containsKey(appenderID)) {
                Appender.getOrCreateAppender(appenderID); // Create appender if it does not exist
                map.put(appenderID, new MonitorTimerTask<T>(this, appenderID, usePriorityTimer));
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static SnapShotMonitorID getSnapShotMonitorID(String className, String name) {
        return new SnapShotMonitorID(className, name, null);
    }
    
/*----------------------------------------------------------------------------*/    
    public static SnapShotMonitorID getSnapShotMonitorID(String className, 
        String name, Properties attributes) {
        return new SnapShotMonitorID(className, name, attributes);
    }

/*----------------------------------------------------------------------------*/    
     public boolean isUsePriorityTimer() {
        return usePriorityTimer;
    }

/*----------------------------------------------------------------------------*/    
    public void setUsePriorityTimer(boolean usePriorityScheduler) {
        this.usePriorityTimer = usePriorityScheduler;
    }
    
    protected abstract void appendData(Appender appender, T data);
   
/*----------------------------------------------------------------------------*/    
    private static class MonitorTimerTask<T extends Object> extends FailSafeTimerTask {
        private final WeakReference<SnapShotMonitorBase<T>> monitorReference;
        private final AppenderID appenderID;
        private final long intervalMillis;
        private T dataElements = null;
        private final boolean usePriorityTimer;
        
        MonitorTimerTask(SnapShotMonitorBase<T> monitor, AppenderID appenderID, boolean usePriorityTimer) throws InvalidConfigException {
            this.monitorReference = new WeakReference<SnapShotMonitorBase<T>>(monitor);
            this.appenderID = appenderID;
            this.usePriorityTimer = usePriorityTimer;
            intervalMillis = appenderID.getIntervalMillis();
            Timer timerToUse = usePriorityTimer ? PerfMon.getPriorityTimer() : PerfMon.getUtilityTimer();
            long now = MiscHelper.currentTimeWithMilliResolution();
            timerToUse.schedule(this, PerfMon.roundInterval(now, intervalMillis));
            dataElements = monitor.initSnapShot(now);
        }
        
        public void failSafeRun() {
            Appender appender = Appender.getAppender(appenderID);
            SnapShotMonitorBase<T> monitor = monitorReference.get();

            boolean cancelTask = true;
            if (monitor != null && monitor.isActive() && PerfMon.isConfigured()) {
                try {
                    dataElements = monitor.takeSnapShot(dataElements, MiscHelper.currentTimeWithMilliResolution());
                } catch (Exception ex) {
                    dataElements = null;
                    logger.logError("Error taking snap shot for monitor: " + monitor.getName(), ex);
                }
                if (appender != null) {
                    cancelTask = false;
                    try {
                        monitor.appendData(appender, dataElements);
                    } catch (Exception ex) {
                        logger.logError("Error appending snapshot data for monitor: " + monitor.getName(), ex);
                    }
                    try {
                    	// Reschedule task
                        new MonitorTimerTask<T>(monitor, appenderID,  usePriorityTimer);
                    } catch (Exception ex) {
                        dataElements = null;
                        logger.logError("Error in initSnapShot for monitor: " + monitor.getName(), ex);
                    }
                }
            } 
            
            if (cancelTask) {
                if (monitor != null) {
                    synchronized(monitor.lockToken) {
                        monitor.map.remove(appenderID);
                    }
                }
            }
        }
    }
    
/*----------------------------------------------------------------------------*/    
    public static class SnapShotMonitorID extends ObjectID {
        final private String name;
        
        protected SnapShotMonitorID(String className, String name, Properties attributes) {
            super(className, attributes);
            this.name = name;
        }
        
        public String getName() {
            return name;
        }

        public String buildAttributeString() {
            return super.buildAttributeString() + "-Name=" + name;
        }
    }
}

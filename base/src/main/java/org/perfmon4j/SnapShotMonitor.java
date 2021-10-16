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

public abstract class SnapShotMonitor {
    final private static Logger logger = LoggerFactory.initLogger(SnapShotMonitor.class);
    
    final private String name;
    final private Object lockToken = new Object();
    private boolean usePriorityTimer = false;
    final private Map<AppenderID, MonitorTimerTask> map = new HashMap();
    
    private boolean active = true; /** Active goes to false when deInit is invoked **/
    

/*----------------------------------------------------------------------------*/    
    protected SnapShotMonitor(String name) {
        this(name, false);
    }
    
/*----------------------------------------------------------------------------*/    
    protected SnapShotMonitor(String name, boolean usePriorityTimer) {
        this.name = name;
        this.usePriorityTimer = usePriorityTimer;
    }
    
/*----------------------------------------------------------------------------*/    
    public String getName() {
        return name;
    }

    public SnapShotData initSnapShot(long currentTimeMillis) {
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
    public abstract SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis);

/*----------------------------------------------------------------------------*/    
    public void addAppender(AppenderID appenderID) throws InvalidConfigException {
        synchronized(lockToken) {
            if (!map.containsKey(appenderID)) {
                Appender.getOrCreateAppender(appenderID); // Create appender if it does not exist
                map.put(appenderID, new MonitorTimerTask(this, appenderID, usePriorityTimer));
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
   
/*----------------------------------------------------------------------------*/    
    private static class MonitorTimerTask extends FailSafeTimerTask {
        private final WeakReference monitorReference;
        private final AppenderID appenderID;
        private final long intervalMillis;
        private SnapShotData data = null;
        private final boolean usePriorityTimer;
        
        MonitorTimerTask(SnapShotMonitor monitor, AppenderID appenderID, boolean usePriorityTimer) throws InvalidConfigException {
            this.monitorReference = new WeakReference(monitor);
            this.appenderID = appenderID;
            this.usePriorityTimer = usePriorityTimer;
            intervalMillis = appenderID.getIntervalMillis();
            Timer timerToUse = usePriorityTimer ? PerfMon.getPriorityTimer() : PerfMon.getUtilityTimer();
            long now = MiscHelper.currentTimeWithMilliResolution();
            timerToUse.schedule(this, PerfMon.roundInterval(now, intervalMillis));
            data = monitor.initSnapShot(now);
        }
        
        public void failSafeRun() {
            Appender appender = Appender.getAppender(appenderID);
            SnapShotMonitor monitor = (SnapShotMonitor)monitorReference.get();

            boolean cancelTask = true;
            if (monitor != null && monitor.isActive() && PerfMon.isConfigured()) {
                try {
                    data = monitor.takeSnapShot(data, MiscHelper.currentTimeWithMilliResolution());
                } catch (Exception ex) {
                    data = null;
                    logger.logError("Error taking snap shot for monitor: " + monitor.getName(), ex);
                }
                if (appender != null) {
                    cancelTask = false;
                    if (data != null) {
                        data.setName(monitor.getName());
                        try {
                            appender.appendData(data);
                        } catch (Exception ex) {
                            logger.logError("Error appending snapshot data for monitor: " + monitor.getName(), ex);
                        }
                    }
                    try {
                    	// Reschedule task
                        new MonitorTimerTask(monitor, appenderID,  usePriorityTimer);
                    } catch (Exception ex) {
                        data = null;
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

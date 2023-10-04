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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonConfiguration.SnapShotMonitorConfig;
import org.perfmon4j.SnapShotMonitorBase.SnapShotMonitorID;
import org.perfmon4j.emitter.Emitter;
import org.perfmon4j.emitter.EmitterMonitor;
import org.perfmon4j.instrument.PerfMonTimerTransformer;
import org.perfmon4j.instrument.jmx.JMXSnapShotProxyFactory;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.remotemanagement.ExternalAppender;
import org.perfmon4j.util.BeanHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public class SnapShotManager {
	public static final String INSTANCE_NAME_PROPERTY = "instanceName";
	public static final String JMX_XML_PROPERTY = "jmxXML";
	
    // Dont use log4j here... The class may not have been loaded
    private static final Logger logger = LoggerFactory.initLogger(SnapShotManager.class);
    
    private static final Map<SnapShotMonitorID, SnapShotMonitorLifecycle> monitorMap = new HashMap<SnapShotMonitorID, SnapShotMonitorLifecycle>();

    private SnapShotManager() {
    }
    
    public static int getMonitorCount() {
        return monitorMap.size();
    }
    
    public static synchronized void deInit() {
        Iterator<SnapShotMonitorLifecycle> itr = monitorMap.values().iterator();
        while (itr.hasNext()) {
            itr.next().deInit();
        }
        monitorMap.clear();
    }
    
    public static synchronized SnapShotMonitorLifecycle getMonitor(SnapShotMonitorID monitorID) {
        return monitorMap.get(monitorID);
    }
    
    public static synchronized SnapShotMonitorLifecycle getOrCreateMonitor(SnapShotMonitorID monitorID) throws ClassNotFoundException {
    	SnapShotMonitorLifecycle result = monitorMap.get(monitorID);
        if (result == null) {
            try {
                Class<?> clazz = null;
                if (PerfMon.getClassLoader() != null) {
                    clazz = PerfMon.getClassLoader().loadClass(monitorID.getClassName());
                } else {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(monitorID.getClassName());
                }
                Properties attr = monitorID.getAttributes();
                if (clazz.getName().equals(JMXSnapShotProxyFactory.class.getName())) {
                	String XML = attr.getProperty(JMX_XML_PROPERTY);
                	result = PerfMonTimerTransformer.jmxSnapShotProxyFactory.getnerateSnapShotWrapper(monitorID.getName(), XML);
                } else if (SnapShotMonitor.class.isAssignableFrom(clazz)) {
	                Constructor constructor = clazz.getConstructor(new Class[]{String.class});
	                result = (SnapShotMonitor)constructor.newInstance(new Object[]{monitorID.getName()});
	                if (attr != null) {
	                    Iterator itr = attr.entrySet().iterator();
	                    while (itr.hasNext()) {
	                        Map.Entry entry = (Map.Entry)itr.next();
	                        try {
	                            BeanHelper.setValue(result, (String)entry.getKey(), (String)entry.getValue());
	                        } catch (BeanHelper.UnableToSetAttributeException ex) {
	                            if (logger.isDebugEnabled()) {
	                                // Only show stack trace if debug is enabled...
	                                logger.logError(ex.getMessage(), ex);
	                            } else {
	                                logger.logError(ex.getMessage());
	                            }
	                        }
	                    }
	                }                
                } else {
                	try {
                		if (Emitter.class.isAssignableFrom(clazz)) {
                			result = new EmitterMonitor(clazz.getName(), monitorID.getName());
	                    	logger.logDebug("Found SnapShotEmitter for class: " + clazz.getName());
                		} else {
	                		// First see if this is a new style POJO Monitor.
	                		SnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundleForPOJO(clazz);
	                    	result = new POJOSnapShotMonitor(monitorID.getName(), bundle.isUsePriorityTimer(), clazz.getName(), POJOSnapShotRegistry.getSingleton());
	                    	logger.logDebug("Found POJO based SnapShotMonitor for class: " + clazz.getName());
                		}
                    	// Try to initialize the class
                		Class.forName(clazz.getName(), true, clazz.getClassLoader());
                	} catch(GenerateSnapShotException ex) {
                		// Try legacy monitor.
                    	SnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(clazz, attr.getProperty(INSTANCE_NAME_PROPERTY));
                    	result = new SnapShotProviderWrapper(monitorID.getName(), bundle);
                    	logger.logDebug("Found Legacy based SnapShotMonitor for class: " + clazz.getName());
                	}
                	ExternalAppender.registerSnapShotClass(clazz.getName());
                }
                monitorMap.put(monitorID, result);
            } catch (ClassNotFoundException nfe) {
            	throw nfe;
        	} catch (Exception ex) {
                logger.logError("Error creating snapshot manager", ex);
            }
        }
        
        return result;
    }

    private static boolean monitorIsInConfig(SnapShotMonitorID id, SnapShotMonitorConfig monitors[]) {
        boolean result = false;
        
        for (int i = 0; (i < monitors.length) && !result; i++) {
            result = id.equals(monitors[i].getMonitorID());
        }
        
        return result;
    }
    
    public synchronized static void applyConfig(PerfMonConfiguration config) throws InvalidConfigException {
         SnapShotMonitorConfig snapShotMonitors[] = config.getSnapShotMonitorArray();
         // First walk through and remove any monitors that are no-longer in
         // the configuration
         SnapShotMonitorID currentIDs[] = monitorMap.keySet().toArray(new SnapShotMonitorID[]{});
         for (int i = 0; i < currentIDs.length; i++) {
             SnapShotMonitorID id = currentIDs[i];
             if (!monitorIsInConfig(id, snapShotMonitors)) {
            	 SnapShotMonitorLifecycle m = monitorMap.get(id);
                 if (m != null) {
                     m.deInit();
                     monitorMap.remove(id);
                 }
             }
         }
        
        for (int i = 0; i < snapShotMonitors.length; i++) {
            SnapShotMonitorConfig cfg = snapShotMonitors[i];
            SnapShotMonitorLifecycle monitor = null;
            try {
            	monitor = SnapShotManager.getOrCreateMonitor(cfg.getMonitorID());
            } catch (NoClassDefFoundError nfe) {
            	config.getClassNotFoundInfo().add("SnapShotMonitor: " + cfg.getMonitorID().getClassName()
            		+ " - Dependent class not found");
            } catch (ClassNotFoundException nfe) {
            	config.getClassNotFoundInfo().add("SnapShotMonitor: " + cfg.getMonitorID().getClassName());
            }
            
            if (monitor != null) {
	            AppenderID appenderIDs[] = cfg.getAppenders();
	            for (int j = 0; j < appenderIDs.length; j++) {
	            	try {
	            		Appender.getOrCreateAppender(appenderIDs[j]);
		                monitor.addAppender(appenderIDs[j]);
	            	} catch (InvalidConfigException ice) {
	            		String name = (appenderIDs[j] == null ? "Unknown" : appenderIDs[j].getClassName()); 
	            		config.getClassNotFoundInfo().add("Appender: " + name);
	            	}
	            }
            }
        }
        
    }
}

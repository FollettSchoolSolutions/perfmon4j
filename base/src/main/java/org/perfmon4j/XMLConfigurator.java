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

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;

import org.perfmon4j.util.FailSafeTimerTask;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public class XMLConfigurator {
    private static final Logger logger = LoggerFactory.initLogger(XMLConfigurator.class);
    private static TimerTaskImpl timerTask = null;
    private static final Object LOCK_TOKEN = new Object();
    
    private XMLConfigurator() {
    }
    
    public static void configure(File xmlFile) {
        configure(xmlFile, -1);
    }
    
    public static void configure(File xmlFile, long reloadSeconds) {
        synchronized (LOCK_TOKEN) {
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
            TimerTaskImpl task = new TimerTaskImpl(xmlFile, reloadSeconds);
            if (reloadSeconds > 0) {
                timerTask = task;
            }
        }
    }
    
    private static class TimerTaskImpl extends FailSafeTimerTask {
        private final File configFile;
        private final long reloadSeconds;
        private long lastModifiedTime = -1;
        
        TimerTaskImpl(File configFile, long reloadSeconds) {
            this.configFile = configFile;
            this.reloadSeconds = reloadSeconds;
            long initialDelay = 500;
            
            // Having problems with JBoss 7.x when loading the perfmon4j configuration, particularly
            // when loading the Microsoft JDBCDriver.  When the driver is instantiated, it 
            // attempts to log output and the jboss logger is not yet initialized.  This
            // causes JBoss to throw an exception, and then subsequently fails to start.
            // To mitigate this issue we will delay 5 seconds before initial configuration
            // of perfmon4j to give jboss time to initialize its log manager.
            //
            if ("org.jboss.logmanager.LogManager".equals(System.getProperty("java.util.logging.manager"))) {
            	System.err.println("org.jboss.logmanager.LogManager found.  Will delay initial load of perfmon4j config for 5 seconds to allow JBoss time to load the LogManager");
            	initialDelay = Integer.getInteger("Perfmon4j.configDelayMillisForJBossLogManager", 5000).longValue();
            }
            if (reloadSeconds > 0) {
                PerfMon.getUtilityTimer().schedule(this, initialDelay, reloadSeconds * 1000);
            } else {
                PerfMon.getUtilityTimer().schedule(this, initialDelay);
            }
        }

        public void failSafeRun() {
            synchronized (LOCK_TOKEN) {
                boolean hadFile = lastModifiedTime > 0;
                
                if (configFile.exists()) {
                    long modifiedTime = configFile.lastModified();
                    if (modifiedTime != lastModifiedTime) {
                        logger.logInfo("Loading configuration from: " + configFile.getName());
                        try {
                            XMLPerfMonConfiguration config = XMLConfigurationParser.parseXML(new FileReader(configFile));
                            if (!config.isEnabled()) {
                                // Enabled flag was set to false...
                                // disable monitoring...
                                if (PerfMon.configured) {
                                    PerfMon.deInit();
                                }
                            } else {
                                PerfMon.configure(config);
                                if (config.isPartialLoad()) {
                                	String warning = "PerfMon4j could not load the following resources: ";
                                	Iterator<String> itr = config.getClassNotFoundInfo().iterator();
                                	boolean addComma = false;
                                	while (itr.hasNext()) {
                                		if (addComma) {
                                			warning += ", ";
                                		}
                                		addComma = true;
                                		warning += "(" + itr.next() + ")";
                                	}
                                	warning += ". Will try again in " + reloadSeconds + " seconds.";
                                	logger.logWarn(warning);
                                	modifiedTime = -1;
                                }
                            }
                        } catch (Throwable ex) {
                        	modifiedTime = -1;
                            logger.logError("Unable to load configuration from file: " + configFile, ex);
                        }
                    }
                    lastModifiedTime = modifiedTime;
                } else if (hadFile || (lastModifiedTime == -1)) {
                    logger.logInfo("Configuration file not found: " + configFile.getName() +
                        " turning off PerfMon");
                    if (PerfMon.configured) {
                        PerfMon.deInit();
                    }
                    lastModifiedTime = 0;
                }
            }
        }
    }
}

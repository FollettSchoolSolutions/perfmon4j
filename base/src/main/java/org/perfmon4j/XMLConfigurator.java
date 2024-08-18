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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.perfmon4j.instrument.TransformerParams;
import org.perfmon4j.util.FailSafeTimerTask;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class XMLConfigurator implements Closeable {
    private static final Logger logger = LoggerFactory.initLogger(XMLConfigurator.class);
    private static final AtomicLong NEXT_XML_CONFIGURATOR_ID = new AtomicLong(0);
    private PerfmonConfigLoaderRunnable scheduledLoader = null;
    private final Object LOCK_TOKEN = new Object();
    private final long id = NEXT_XML_CONFIGURATOR_ID.incrementAndGet();
    private final File xmlFile;
    private final String configFromClassloaderName;
    private final int reloadSeconds;
    private final LoadMode loadMode;
    
    private static enum LoadMode {
    	LOAD_FROM_CLASSLOADER,
    	LOAD_FROM_FILE,
    	LOAD_FROM_CLASSLOADER_OR_FILE,
    	LOAD_NONE;
    	
    	boolean hasOptionToLoadFromFile() {
    		return this.equals(LOAD_FROM_FILE) || this.equals(LOAD_FROM_CLASSLOADER_OR_FILE);
    	}
    	
    	boolean hasOptionToLoadFromClassloader() {
    		return this.equals(LOAD_FROM_CLASSLOADER) || this.equals(LOAD_FROM_CLASSLOADER_OR_FILE);
    	}
    	
    	public static LoadMode getLoadMode(File configFile, String configFromClassloaderName) {
    		LoadMode result = LoadMode.LOAD_NONE;
            if (configFile != null && configFromClassloaderName != null) {
            	result = LoadMode.LOAD_FROM_CLASSLOADER_OR_FILE;
            } else if (configFile != null) {
            	result = LoadMode.LOAD_FROM_FILE;
            } else if (configFromClassloaderName != null){
            	result = LoadMode.LOAD_FROM_CLASSLOADER;
            }
            return result;
    	}
    }    
    
    public XMLConfigurator(TransformerParams params) {
    	this(fileOrNull(params.getXmlFileToConfig()), params.getConfigFromClassloaderName(), params.getReloadConfigSeconds());
    }
    
    private static File fileOrNull(String fileName) {
    	return fileName != null ? new File(fileName) : null;
    }
   
    public XMLConfigurator(File xmlFile, String configFromClassloaderName, int reloadSeconds) {
    	this.xmlFile = xmlFile;
    	this.configFromClassloaderName = configFromClassloaderName;
    	this.reloadSeconds = reloadSeconds;
    	this.loadMode = LoadMode.getLoadMode(xmlFile, configFromClassloaderName);
    }
    
    public BootConfiguration loadBootConfiguartion() {
    	BootConfiguration bootConfiguration = null;
    	
    	if (loadMode.hasOptionToLoadFromFile()) {
			String fileDisplayName = MiscHelper.getDisplayablePath(xmlFile);
    		if (xmlFile.exists()) {
    			try (Reader reader = new FileReader(xmlFile, StandardCharsets.UTF_8)) {
        			bootConfiguration = XMLBootParser.parseXML(reader);
        			logger.logInfo("Loaded perfmon4j boot configuration from: " + fileDisplayName);
    			} catch (IOException e) {
    				logger.logInfo("Error loading perfmon4j boot configuration from file: " + fileDisplayName);
				}
    		} else {
    			logger.logInfo("Unable to load perfmon4j boot configuration from (file not found): " + fileDisplayName);
    		}
    	}

    	if (bootConfiguration == null && loadMode.hasOptionToLoadFromClassloader()) {
        	InputStream resource = loadResource(configFromClassloaderName);
        	if (resource != null) {
        		try (Reader reader =  new InputStreamReader(resource, StandardCharsets.UTF_8)) {
        			bootConfiguration = XMLBootParser.parseXML(reader);
        			logger.logInfo("Loaded perfmon4j boot configuration from classloader. Resource name: " 
        				+ configFromClassloaderName);
        		} catch (IOException ex) {
        			logger.logInfo("Unable to load pefmon4j boot configuration from classloader. Resource name: " 
        				+ configFromClassloaderName, ex);
        		}
        	} 
    	}

    	if (bootConfiguration == null) {
    		logger.logInfo("Perfmon4j using default boot configuration");
    		bootConfiguration = BootConfiguration.getDefault();
    	}
    	
    	return bootConfiguration;
    }
    
    /**
     * Starts the configuration process.   Depending on the reloadSeconds setting
     * this will schedule an async timer task that will monitor for changes
     * until close() is called.
     */
    public void start() {
        synchronized (LOCK_TOKEN) {
        	close();
            scheduledLoader = new PerfmonConfigLoaderRunnable(this, xmlFile, configFromClassloaderName, reloadSeconds);
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
            scheduleForRun(scheduledLoader, initialDelay);
            
            logger.logDebug(this + " started.");
        }
    }
    
    public void close() {
        synchronized (LOCK_TOKEN) {
            if (scheduledLoader != null) {
                scheduledLoader.cancel();
                scheduledLoader = null;
                logger.logDebug(this + " has been closed.");
            }
        }
    }
    
    protected boolean isPerfMonConfigured() {
    	return PerfMon.isConfigured();
    }
    
    protected void deInitPerfMon() {
    	PerfMon.deInit();
		logger.logDebug(this + " de-initializing perfmon4j");
    }
    
    protected void configurePerfMon(XMLPerfMonConfiguration config) throws InvalidConfigException {
		PerfMon.configure(config);
		logger.logDebug(this + " configuring perfmon4j");
    }
    
    private InputStream loadResource(String resourceFileName) {
    	InputStream resource = PerfMon.getClassLoader().getResourceAsStream(resourceFileName);
    	if (resource != null) {
    		logger.logDebug(this + " found resource (" + resourceFileName +  ") using the PerfMon.globalClassLoader");
    	}
    	
    	if (resource == null) {
    		resource = this.getClass().getClassLoader().getResourceAsStream(resourceFileName);
    		if (resource != null) {
    			logger.logDebug(this + " found resource (" + resourceFileName +  ") using the " +
    				this.getClass().getName() + " class loader");
    		}
    	}
    	
    	if (resource == null) {
    		resource =  Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFileName);
    		if (resource != null) {
    			logger.logDebug(this + " found resource (" + resourceFileName +  ") using the Thread Context class loader");
    		}
    	}
    	
    	if (resource == null && MiscHelper.isRunningInQuarkusDevTestMode()) {
    		File file = new File("./target/classes/", resourceFileName);
    		if (file.exists()) {
    			try {
					resource = new FileInputStream(file);
	    			logger.logDebug(this + "- Running in quarkus dev/test mode and found resource (" + resourceFileName +  ") in local maven classpath: " 
	    					+ MiscHelper.getDisplayablePath(file));
				} catch (FileNotFoundException e) {
					// Ignore.
				}
    		}
    	}
    	
    	if (resource == null) {
    		logger.logDebug(this + " Unable to find resource (" + resourceFileName +  ") using classloaders");
    	}
    	
    	return resource;
    }
    
	protected XMLPerfMonConfiguration load(String resourceFileName)  {
    	XMLPerfMonConfiguration result = null;
    	InputStream resource = loadResource(resourceFileName);
    	
    	if (resource != null) {
    		try (Reader reader =  new InputStreamReader(resource, StandardCharsets.UTF_8)) {
    			result = XMLConfigurationParser.parseXML(reader);
    			logger.logDebug(this + " loaded configuration from classloader");
    		} catch (IOException | InvalidConfigException ex) {
    			logger.logWarn("Unable to read Perfmon configuration from classloader: " + resourceFileName, ex);
    		}
    	} else {
    		logger.logWarn("Could not locate Perfmon configuration from classloader: " + resourceFileName);
    	}
    	return result;
    }

    protected XMLPerfMonConfiguration load(File configFile)  {
    	XMLPerfMonConfiguration result = null;
		try (Reader reader =  new FileReader(configFile, StandardCharsets.UTF_8)) {
			result = XMLConfigurationParser.parseXML(reader);
			logger.logDebug(this + " loaded configuration from file");
		} catch (IOException | InvalidConfigException ex) {
			logger.logWarn("Unable to read Perfmon configuration from: " + MiscHelper.getDisplayablePath(configFile), ex);
		}
    	return result;
    }
    
    
    private void scheduleForRun(final PerfmonConfigLoaderRunnable loader, long initialDelay) {
    	PerfMon.getUtilityTimer().schedule(new FailSafeTimerTask() {
			@Override
			public void failSafeRun() {
				synchronized (LOCK_TOKEN) {
					if (!loader.isCancelled()) {
				    	logger.logDebug("Run being invoked on: " + loader);
						loader.run();
					} else {
				    	logger.logDebug("Skipping run being on: " + loader + " loader has been cancelled");
					}
				}
			}
		}, initialDelay);
    	logger.logDebug(loader + " has been scheduled to run in " + initialDelay + " millis");
    }
    
    @Override
	public String toString() {
		return "XMLConfigurator [id=" + id + ", xmlFile=" + xmlFile + ", configFromClassloaderName="
				+ configFromClassloaderName + ", reloadSeconds=" + reloadSeconds + ", scheduledLoader="
				+ scheduledLoader + "]";
	}

	private static class PerfmonConfigLoaderRunnable implements Runnable {
        private static final AtomicLong NEXT_LOADER_ID = new AtomicLong(0);
        private final long id = NEXT_LOADER_ID.incrementAndGet();
        private final XMLConfigurator configurator;
        private final File configFile;
        private final String configFromClassloaderName;
        private final long reloadSeconds;
        private final LoadMode loadMode;
        private boolean loadedFromClasspath = false;
        private boolean cancelled = false;
        
        private long lastConfigFileModifiedTime = -1;
        
        PerfmonConfigLoaderRunnable(XMLConfigurator configurator, File configFile, String configFromClassloaderName, long reloadSeconds) {
        	this.configurator = configurator;
            this.configFile = configFile;
            this.configFromClassloaderName = configFromClassloaderName;
            this.reloadSeconds = reloadSeconds;
            
            loadMode = LoadMode.getLoadMode(configFile, configFromClassloaderName);
        }
        
        @Override
        public void run() {
    		XMLPerfMonConfiguration config = null;            		
    		boolean reschedule = loadMode.hasOptionToLoadFromFile();
    		boolean attemptLoadFromClassloader = loadMode.hasOptionToLoadFromClassloader();
    		boolean doConfigure = false;

    		if (loadMode.hasOptionToLoadFromFile()) {
    			if (configFile.exists()) {
    				if (loadedFromClasspath || (configFile.lastModified() != lastConfigFileModifiedTime)) {
    					lastConfigFileModifiedTime = configFile.lastModified();
    					config = configurator.load(configFile);
    					doConfigure = true;
    		    		logger.logInfo("Perfmon4j configuration retrieved from file: " 
    		    			+ MiscHelper.getDisplayablePath(configFile));
    				} 
					loadedFromClasspath = false;
    				attemptLoadFromClassloader = false;
    			} else {
    				lastConfigFileModifiedTime = -1;
    			}
    		}
    		
    		if (attemptLoadFromClassloader && !loadedFromClasspath) {
				config = configurator.load(configFromClassloaderName);
				loadedFromClasspath = true;
				doConfigure = true;
				if (config != null) {
					logger.logInfo("Perfmon4j configuration retrieved from classloader. Resource name: " 
							+ configFromClassloaderName);
				}
				reschedule = reschedule || (config == null); // If we were unable to load from the classloader try again...
    		}
    		
    		if (doConfigure) {
        		if (config == null || !config.isEnabled()) {
                    if (configurator.isPerfMonConfigured()) {
                        configurator.deInitPerfMon();
        	    		logger.logInfo("Perfmon4j configuration not found or disabled. Perfmon4j has been deinitialized.");
                    }
        		} else {
      	            try {
      	            	configurator.configurePerfMon(config);
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
        	            	loadedFromClasspath = true;
        	            	reschedule = true;
        	            }
					} catch (InvalidConfigException e) {
						logger.logWarn("Error confguring perfmon4j", e);
                        if (PerfMon.configured) {
                            PerfMon.deInit();
                        }
					}
        		}
    		}
    		
    		if (reschedule && reloadSeconds > 0) {
    			configurator.scheduleForRun(this, reloadSeconds * 1000);
    		}
        }
        
        private void cancel() {
        	logger.logDebug(this + " has been cancelled");
        	cancelled = true;
        }

        private boolean isCancelled() {
			return cancelled;
		}

		@Override
		public String toString() {
			return "PerfmonConfigLoaderRunnable [id=" + id + ", configurator.ID=" + configurator.id + ", loadMode=" + loadMode
					+ ", cancelled=" + cancelled + ", lastConfigFileModifiedTime=" + lastConfigFileModifiedTime + "]";
		}
    }
}

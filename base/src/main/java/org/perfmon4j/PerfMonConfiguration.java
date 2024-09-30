/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.SnapShotMonitorBase.SnapShotMonitorID;
import org.perfmon4j.util.ConfigurationProperties;
import org.perfmon4j.util.EnhancedAppenderPatternHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.mbean.MBeanInstance;
import org.perfmon4j.util.mbean.MBeanQuery;


public class PerfMonConfiguration {
    private static final Logger logger = LoggerFactory.initLogger(PerfMonConfiguration.class);
    
    public final static long DEFAULT_APPENDER_INTERVAL = 60 * 60 * 1000; // 
    public final static long ONE_MINUTE_INTERVAL = 60 * 1000; // 
    
    private final Map<String, MonitorConfig> monitorMap = new HashMap<>();
    private final Map<String, Appender.AppenderID> appenderMap = new HashMap<>();
    private final Map<String, SnapShotMonitorConfig> snapShotMonitors = new HashMap<>();
    private final Set<MBeanQuery> mBeanQuerys = new HashSet<MBeanQuery>();
    private final Map<String, ThreadTraceConfig> threadTraceConfigs = new HashMap<>();
    private final Set<String> disabledAppenders = new HashSet<String>();
    private final ConfigurationProperties configurationProperties;
    
    // This list will be filled with the name of any classes that could not be found
    // while processing the config.
    // The format will be: "<PerfMonElement>: <className>"
    private Set<String> classNotFoundInfo = new HashSet<String>();
    
    public static final String DEFAULT_APPENDER_NAME = "Perfmon4jDefaultAppender";
    
    
    public PerfMonConfiguration() {
    	this(new ConfigurationProperties());
    }
    
    
    protected PerfMonConfiguration(ConfigurationProperties configurationProperties) {
    	this.configurationProperties = configurationProperties;
    }

    
    public void addDisabledAppender(String name) {
    	disabledAppenders.add(name);
    }
    
/*----------------------------------------------------------------------------*/
    public void defineAppender(String name, String className, 
        String interval) {
        defineAppender(name, className, interval, null);
    }

/*----------------------------------------------------------------------------*/
    public void defineAppender(String name, AppenderID appenderID) {
        defineAppender(name, appenderID.getClassName(), appenderID.getIntervalMillis() + " ms", 
        	appenderID.getAttributes());
    }
    
/*----------------------------------------------------------------------------*/
    public void defineAppender(String name, String className, 
        String interval, Properties attributes) {
        if (appenderMap.get(name) == null) {
        	AppenderID id = AppenderID.getAppenderID(className, convertIntervalStringToMillis(interval), attributes);
        	boolean isAvailable = true;
        	if (PooledSQLAppender.class.getName().equals(className)) {
        		isAvailable = PooledSQLAppender.testIfDataSourceIsAvailable(attributes);
        		if (!isAvailable) {
        			String dataSourceName = attributes.getProperty("poolName");
        			if (dataSourceName == null) {
        				dataSourceName = "Unknown Datasource";
        			}
        			classNotFoundInfo.add("PooledSQLAppender (jndiDataSource): " + dataSourceName);
        		}
        	}
        	if (isAvailable) {
        		appenderMap.put(name, id);
        	}
        }
    }

	/*----------------------------------------------------------------------------*/
    public String[] getAppenderNames() {
    	return appenderMap.keySet().toArray(new String[]{});
    }
    
/*----------------------------------------------------------------------------*/
    public MonitorConfig defineMonitor(String monitorName) {
    	String key = monitorName;
        if (PerfMon.ROOT_MONITOR_NAME.equalsIgnoreCase(monitorName)) {
            key = ""; // Make sure root always sorts to the top..
        }
        MonitorConfig result = monitorMap.get(key); 
        if (result == null) {
        	result = new MonitorConfig(monitorName);
            monitorMap.put(key, result);
        }
        
        return result;
    }
    
    
    /*----------------------------------------------------------------------------*/
    public MonitorConfig[] getMonitorConfigArray() {
    	List<MonitorConfig> result = new ArrayList<MonitorConfig>();
        String keys[] = monitorMap.keySet().toArray(new String[]{});
        Arrays.sort(keys);
        
        for (String key : keys) {
        	result.add(monitorMap.get(key));
        }
        
        return result.toArray(new MonitorConfig[]{});
    }

/*----------------------------------------------------------------------------*/
    @Deprecated
    /**
     * Should replace with calls to getMonitorConfigArray()
     * @return
     */
    public String[] getMonitorArray() {
        String result[] = monitorMap.keySet().toArray(new String[]{});
        Arrays.sort(result);
        
        if (result.length > 0 && result[0].equals("")) {
            result[0] = PerfMon.ROOT_MONITOR_NAME;
        }
        
        return result;
    }
    
    public Appender.AppenderID getAppenderForName(String appenderName) {
        return appenderMap.get(appenderName);
    }
    
/*----------------------------------------------------------------------------*/
    public void attachAppenderToMonitor(String monitorName, String appenderName) throws InvalidConfigException {
        attachAppenderToMonitor(monitorName, appenderName, PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
    }
    
    
    boolean isDisabledAppender(String appenderName) {
    	return disabledAppenders.contains(appenderName);
    }
    
/*----------------------------------------------------------------------------*/
    public void attachAppenderToMonitor(String monitorName, String appenderName, String appenderPattern) throws InvalidConfigException {
        String nameToSearch = monitorName;
        if (PerfMon.ROOT_MONITOR_NAME.equalsIgnoreCase(monitorName)) {
            nameToSearch = ""; 
        }
        MonitorConfig config = monitorMap.get(nameToSearch);
        if (config == null) {
            throw new InvalidConfigException("Monitor: \"" + monitorName + "\" not defined.");
        }
    
        if (!isDisabledAppender(appenderName)) {
	        Appender.AppenderID appenderID = appenderMap.get(appenderName);
	        if (appenderID == null) {
	        	logger.logError("Appender: \"" + appenderName + "\" not defined. Attaching monitor \"" 
	        			+ monitorName + "\" to the default text appender with a one minute polling interval." );
	        	// Get or create a default text appender...
	        	appenderID = getOrCreateDefaultAppender();
	        }
	        config.addAppender(appenderID, appenderPattern);
        } else {
        	config.setContainsDisabledAppenders(true);
        }
    }

/*----------------------------------------------------------------------------*/
    public void attachAppenderToSnapShotMonitor(String monitorName, String appenderName) throws InvalidConfigException {
        SnapShotMonitorConfig config = snapShotMonitors.get(monitorName);
        if (config == null) {
            throw new InvalidConfigException("SnapShotMonitor not defined. monitorName=" + monitorName);
        }
        
        if (!isDisabledAppender(appenderName)) {
	        Appender.AppenderID appenderID = appenderMap.get(appenderName);
	        if (appenderID == null) {
	        	logger.logError("Appender: \"" + appenderName + "\" not defined. Attaching SnapShotMonitor \"" 
	        			+ monitorName + "\" to the default text appender with a one minute polling interval" );
	        	// Get or create a default text appender...
	        	appenderID = getOrCreateDefaultAppender();
	        }
	        config.addAppender(appenderID);
        } else {
        	config.setContainsDisabledAppenders(true);
        }
    }
    
    public Map<String, ThreadTraceConfig> getThreadTraceConfigMap() {
        return threadTraceConfigs;
    }
    
/*----------------------------------------------------------------------------*/
    public SnapShotMonitorID defineSnapShotMonitor(String name, String className) {
        return defineSnapShotMonitor(name, className, null);
    }

/*----------------------------------------------------------------------------*/
    public SnapShotMonitorID defineSnapShotMonitor(String name, String className, Properties attributes) {
        SnapShotMonitorID result = null;
        
        if (snapShotMonitors.containsKey(name)) {
            logger.logWarn("Duplicate snapShotMonitor name found name=" + name);
        }
        
        result = SnapShotMonitor.getSnapShotMonitorID(className, name, attributes);
        snapShotMonitors.put(name, new SnapShotMonitorConfig(result));
        
        return result;
    }

    
    /*----------------------------------------------------------------------------*/
    /**
     * This method actually defines two elements:
     * 	1) The MBeanQuery that will be used to generate a SnapShotPOJO that will be used
     * 		to extract data from the JMX MBean.  This generated POJO will be registered
     * 		with the SnapShotPOJORegistry.
     * 	2) A SnapShotMonitor that will be created to pull measurements from the SnapShotPOJO
     * 		and write them to one or more appenders.
     * @param query
     * @return
     */
    public SnapShotMonitorID defineMBeanSnapShotMonitor(MBeanQuery query) {
    	String name = query.getDisplayName();
    	SnapShotMonitorID result = null;
        
        if (snapShotMonitors.containsKey(name)) {
            logger.logWarn("Duplicate snapShotMonitor name found name=" + name);
        }
        
        result = SnapShotMonitor.getSnapShotMonitorID(MBeanInstance.buildEffectiveClassName(query), name);
        snapShotMonitors.put(name, new SnapShotMonitorConfig(result));
        
        mBeanQuerys.add(query);
        
        return result;
    }
    
    
	/*----------------------------------------------------------------------------*/  
    public ConfigurationProperties getConfigurationProperties() {
		return configurationProperties;
	}

	/*----------------------------------------------------------------------------*/  
    public SnapShotMonitorConfig[] getSnapShotMonitorArray() {
        return snapShotMonitors.values().toArray(new SnapShotMonitorConfig[]{});
    }
    
    public Set<MBeanQuery> getMBeanQueryArray() {
    	return new HashSet<MBeanQuery>(mBeanQuerys);
    }
    
    public Appender.AppenderID[] getAllDefinedAppenders() {
        return appenderMap.values().toArray(new Appender.AppenderID[]{});
    }
    
    private static String cleanAppenderPattern(String pattern) {
    	String result = PerfMon.APPENDER_PATTERN_PARENT_ONLY;
    	
    	if (PerfMon.APPENDER_PATTERN_PARENT_ONLY.equals(pattern) ||
    			PerfMon.APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY.equals(pattern) ||
    			PerfMon.APPENDER_PATTERN_CHILDREN_ONLY.equals(pattern) ||
    			PerfMon.APPENDER_PATTERN_ALL_DESCENDENTS.equals(pattern) ||
    			PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS.equals(pattern)) {
    		result = pattern;
    	} else {
    		if (".".equals(pattern)) {
    			// Don't warn if we are simply replacing '.' with './', '.' can be considered a synonym for './' 
    		} else if (EnhancedAppenderPatternHelper.validateAppenderPattern(pattern)) {
    			result = pattern;
    		} else {
       			logger.logWarn("Invalid appender pattern found: '" + pattern + "' Replacing with default: '" + result + "'");
    		}
    	}
    	
    	return result;
    }
    
    
/*----------------------------------------------------------------------------*/
    public static class MonitorConfig {
        private final Set<Appender.AppenderID> appenderSet = new HashSet<Appender.AppenderID>();
        private final Map<Appender.AppenderID, String> patternMap = new HashMap<Appender.AppenderID, String>();
        private final Properties properties = new Properties();
        private final String monitorName;
        private boolean containsDisabledAppenders = false;
        private boolean flaggedAsDisabled = false;
        
        private MonitorConfig(String monitorName) {
        	this.monitorName = monitorName;
        }
        
        private void addAppender(Appender.AppenderID appenderID, String appenderPattern) {
            appenderSet.add(appenderID);
            patternMap.put(appenderID, cleanAppenderPattern(appenderPattern));
        }
        
        void setProperty(String key, String value) {
            properties.setProperty(key, value);
        }
        
		boolean isContainsDisabledAppenders() {
			return containsDisabledAppenders;
		}

		void setContainsDisabledAppenders(boolean containsDisabledAppenders) {
			this.containsDisabledAppenders = containsDisabledAppenders;
		}
		
		boolean isFlaggedAsDisabled() {
			return flaggedAsDisabled;
		}

		void setFlaggedAsDisabled(boolean flaggedAsDisabled) {
			this.flaggedAsDisabled = flaggedAsDisabled;
		}

		String getProperty(String key) {
			return properties.getProperty(key);
		}

		public String getMonitorName() {
			return monitorName;
		}
    }

/*----------------------------------------------------------------------------*/
    public static class SnapShotMonitorConfig {
        private final SnapShotMonitorID monitorID;
        private final Set<Appender.AppenderID> appenderSet = new HashSet<Appender.AppenderID>();
        private boolean containsDisabledAppenders = false;
        
		boolean isContainsDisabledAppenders() {
			return containsDisabledAppenders;
		}

		void setContainsDisabledAppenders(boolean containsDisabledAppenders) {
			this.containsDisabledAppenders = containsDisabledAppenders;
		}
        
        private SnapShotMonitorConfig(SnapShotMonitorID monitorID) {
            this.monitorID = monitorID;
        }
        
        private void addAppender(Appender.AppenderID appenderID) {
            appenderSet.add(appenderID);
        }
        
        public SnapShotMonitorID getMonitorID() {
            return monitorID;
        }
        
        public Appender.AppenderID[] getAppenders() {
            return appenderSet.toArray(new Appender.AppenderID[]{});
        }
    }
    
    
/*----------------------------------------------------------------------------*/
    public static final class AppenderAndPattern {
        final private Appender appender;
        final private String appenderPattern;
        final private Appender.AppenderID appenderID;
        
        AppenderAndPattern(Appender.AppenderID appenderID, String appenderPattern) throws InvalidConfigException {
        	this.appenderID = appenderID;
            this.appender = Appender.getOrCreateAppender(appenderID);
            this.appenderPattern = (appenderPattern == null) ? PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS :
                appenderPattern;
        }

        public Appender getAppender() {
            return appender;
        }

        public String getAppenderPattern() {
            return appenderPattern;
        }

		public Appender.AppenderID getAppenderID() {
			return appenderID;
		}
    }
    
/*----------------------------------------------------------------------------*/
    static long convertIntervalStringToMillis(String interval) {
        return MiscHelper.convertIntervalStringToMillis(interval, DEFAULT_APPENDER_INTERVAL);
    }
    
    public AppenderAndPattern[] getAppendersForMonitor(String monitorName) throws InvalidConfigException {
    	return getAppendersForMonitor(monitorName, null);
    }
    

    
/*----------------------------------------------------------------------------*/    
    public AppenderAndPattern[] getAppendersForMonitor(String monitorName, PerfMonConfiguration perfMonConfig) throws InvalidConfigException {
        String nameToSearch = monitorName;
        if (PerfMon.ROOT_MONITOR_NAME.equalsIgnoreCase(monitorName)) {
            nameToSearch = ""; 
        }
        MonitorConfig config = monitorMap.get(nameToSearch);
        if (config == null) {
            throw new InvalidConfigException("Monitor: \"" + monitorName + "\" not defined.");
        }
        
        List<AppenderAndPattern> appenders = new Vector<AppenderAndPattern>();
        Iterator<Appender.AppenderID> itr = config.appenderSet.iterator();
        while (itr.hasNext()) {
            Appender.AppenderID id = itr.next();
            try {
            	appenders.add(new AppenderAndPattern(id, config.patternMap.get(id)));
            } catch (InvalidConfigException ie) {
            	if (perfMonConfig != null) {
            		perfMonConfig.getClassNotFoundInfo().add("Appender: " + id.getClassName());
            	} else {
            		logger.logWarn("Unable to load appender: " + id.getClassName());
            	}
            }
        }
        
        return appenders.toArray(new AppenderAndPattern[]{});
    }

    public void addThreadTraceConfig(String monitorKey, ThreadTraceConfig threadTraceConfig) {
        threadTraceConfigs.put(monitorKey, threadTraceConfig);
    }
    
    public boolean isPartialLoad() {
    	return !classNotFoundInfo.isEmpty();
    }
    
    public Set<String> getClassNotFoundInfo() {
    	return classNotFoundInfo;
    }

    public AppenderID getOrCreateDefaultAppender() {
    	AppenderID appenderID = appenderMap.get(DEFAULT_APPENDER_NAME);
		if (appenderID == null) {
        	// Get or create a default text appender...
        	if (appenderID == null) {
        		appenderID = AppenderID.getAppenderID(TextAppender.class.getName(), ONE_MINUTE_INTERVAL);
        		appenderMap.put(DEFAULT_APPENDER_NAME, appenderID);
        	}
		}
        return appenderID;
    }
    
    /**
     * This method will find all monitors that are not currently attached
     * to an appender, and attach them to the default appender.
     */
    public void cleanupElementsPostConfig() {
    	// First look for interval monitors that are missing appenders...
    	for (String monitorName : monitorMap.keySet().toArray(new String[]{})) {
    		MonitorConfig config = monitorMap.get(monitorName);
    		if (config.isFlaggedAsDisabled()) {
    			monitorMap.remove(monitorName);
    		} else if (config.appenderSet.isEmpty()) {
    			if (config.isContainsDisabledAppenders()) {
    				monitorMap.remove(monitorName);
    			} else {
		        	logger.logInfo("No appenders defined for monitor \"" 
		        			+  monitorName + "\" attaching to the default text appender" );
		        	AppenderID appenderID = getOrCreateDefaultAppender();
	    			config.addAppender(appenderID, PerfMon.APPENDER_PATTERN_PARENT_ONLY);
    			}
    		}
    	}
    	
    	// Next look for snapshot monitors missing appenders
    	for (String monitorName : snapShotMonitors.keySet().toArray(new String[] {})) {
    		SnapShotMonitorConfig config = snapShotMonitors.get(monitorName);
    		if (config.appenderSet.isEmpty()) {
    			if (config.isContainsDisabledAppenders()) {
    				snapShotMonitors.remove(monitorName);
    			} else {
		        	logger.logInfo("No appenders defined for SnapShotMonitor \"" 
		        			+  monitorName + "\" attaching to the default text appender with a 1 minute polling interval." );
		        	AppenderID appenderID = getOrCreateDefaultAppender();
	    			config.addAppender(appenderID);
    			}
    		}
    	}
    	
    	// Finally look for ThreadTrace monitors missing appenders
    	for (String monitorName : threadTraceConfigs.keySet().toArray(new String[] {})) {
    		ThreadTraceConfig config = threadTraceConfigs.get(monitorName);
    		if (config.isFlaggedAsDisabled()) {
				threadTraceConfigs.remove(monitorName);
    		} else if (config.getAppenders().length == 0) {
    			if (config.isContainsDisabledAppenders()) {
    				threadTraceConfigs.remove(monitorName);
    			} else {
		        	logger.logInfo("No appenders defined for ThreadTraceMonitor \"" 
		        			+  monitorName + "\" attaching to the default text appender." );
		        	AppenderID appenderID = getOrCreateDefaultAppender();
	    			config.addAppender(appenderID);
    			}
    		}
		}
    }
}

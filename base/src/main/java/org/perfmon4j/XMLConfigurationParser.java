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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.PerfMonConfiguration.MonitorConfig;
import org.perfmon4j.util.ConfigurationProperties;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.PropertyStringFilter;
import org.perfmon4j.util.mbean.MBeanQuery;
import org.perfmon4j.util.mbean.MBeanQueryBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


// Only package level
public class XMLConfigurationParser extends DefaultHandler {
    private static final Logger logger = LoggerFactory.initLogger(XMLConfigurationParser.class);
    
    private final XMLPerfMonConfiguration config;

    XMLConfigurationParser() {
    	this(new ConfigurationProperties());
    }

    XMLConfigurationParser(ConfigurationProperties configurationProperties) {
    	config = new XMLPerfMonConfiguration(configurationProperties);
    	filter = new PropertyStringFilter(configurationProperties, true);
    }
    
    private static XMLPerfMonConfiguration getDefaultConfiguration()
        throws InvalidConfigException {
        XMLPerfMonConfiguration result = new XMLPerfMonConfiguration();
        
        final String APPENDER_NAME = "DEFAULT_APPENDER";
        result.defineAppender(APPENDER_NAME, TextAppender.class.getName(),
            Appender.DEFAULT_INTERVAL_MILLIS + " ms");
        result.defineMonitor(PerfMon.ROOT_MONITOR_NAME);
        result.attachAppenderToMonitor(PerfMon.ROOT_MONITOR_NAME, APPENDER_NAME, PerfMon.APPENDER_PATTERN_CHILDREN_ONLY);
        return result;
    }

    public static final String DEFAULT_XML_READER_CLASS = "com.sun.org.apache.xerces.internal.parsers.SAXParser";
    public static String lastKnownGoodXMLReaderClass = null;
    
    public static XMLPerfMonConfiguration parseXML(Reader reader)throws InvalidConfigException {
    	return parseXML(reader, new ConfigurationProperties());
    }
    
    public static XMLPerfMonConfiguration parseXML(Reader reader, ConfigurationProperties configurationProperties)
        throws InvalidConfigException {
        XMLPerfMonConfiguration result = null;
        try {
            XMLConfigurationParser handler = new XMLConfigurationParser(configurationProperties);
            XMLReader xr = null;
            
            try {
                xr = XMLReaderFactory.createXMLReader();
                lastKnownGoodXMLReaderClass = xr.getClass().getName();
            } catch (SAXException ex) {
            	String clazzToUse = lastKnownGoodXMLReaderClass != null 
            		? lastKnownGoodXMLReaderClass :
            		DEFAULT_XML_READER_CLASS;
            	xr = XMLReaderFactory.createXMLReader(clazzToUse);
            }
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(new InputSource(reader));
            result = handler.config;
        } catch (SAXException ex) {
            logger.logError("SAXException parsing configuration... Returning default configuration", ex);
            result = getDefaultConfiguration();
        } catch (IOException ex) {
            logger.logError("IOException parsing configuration... Returning default configuration", ex);
            result = getDefaultConfiguration();
        }
        
        result.cleanupElementsPostConfig();
        
        return result;
    }
    
    private final static String ROOT_ELEMENT_NAME = "Perfmon4JConfig";
    private final static String APPENDER_NAME = "appender";
    private final static String MONITOR_NAME = "monitor";
    private final static String ALIAS_NAME = "alias";
    private final static String ATTRIBUTE_NAME = "attribute";
    private final static String SNAP_SHOT_MONITOR_NAME = "snapShotMonitor";
    private final static String MBEAN_SNAP_SHOT_MONITOR_NAME = "mBeanSnapshotMonitor";
    private final static String PROPERTIES_NAME = "properties";
    private final static String PROPERTY_NAME = "property";
    private final static String ACTIVATION_NAME = "activation";
    private final static String EMITTER_MONITOR_NAME = "emitterMonitor";
    private final static String THREAD_TRACE_NAME = "threadTrace";
    private final static String THREAD_TRACE_TRIGGERS_NAME = "Triggers";
    private final static String HTTP_REQUEST_TRIGGER_NAME = "HttpRequestTrigger";
    private final static String HTTP_SESSION_TRIGGER_NAME = "HTTPSessionTrigger";
    private final static String HTTP_COOKIE_TRIGGER_NAME = "HTTPCookieTrigger";
    private final static String THREAD_NAME_TRIGGER_NAME = "ThreadNameTrigger";
    private final static String THREAD_PROPERTY_TRIGGER_NAME = "ThreadPropertyTrigger";
    private final static String BOOT_NAME = "boot";
    
    private final int STATE_UNDEFINED                           = 0;
    private final int STATE_IN_ROOT                             = 1;
    private final int STATE_IN_APPENDER                         = 2;
    private final int STATE_IN_APPENDER_ATTRIBUTE               = 3;
    private final int STATE_IN_MONITOR                          = 4;
    private final int STATE_IN_SNAP_SHOT_MONITOR                = 5;
    private final int STATE_IN_SNAP_SHOT_ATTRIBUTE              = 6;
    private final int STATE_IN_THREAD_TRACE                     = 7;
    private final int STATE_IN_THREAD_TRACE_TRIGGERS          	= 8;
    private final int STATE_DONE                                = 9;
    private final int STATE_IN_BOOT /* Ignored */				= 10;
    private final int STATE_IN_MONITOR_ATTRIBUTE               	= 11;
    private final int STATE_IN_PROPERTIES 						= 12;
    private final int STATE_IN_PROPERTIES_PROPERTY 				= 13;
    private final int STATE_IN_ACTIVATION 						= 14;
    private final int STATE_IN_ACTIVATION_PROPERTY 				= 15;
    private final int STATE_IN_MBEAN_SNAP_SHOT_MONITOR         	= 16;
    
    
    private int currentState = STATE_UNDEFINED;
    private MonitorConfig currentMonitorConfig = null;
    
    private SnapShotMonitorVO currentSnapShotMonitor = null;
    private boolean currentSnapShotEnabled = true;

    private MBeanSnapShotMonitorVO currentMBeanSnapShotMonitor = null;
    
    // Current Appender information
    private String currentAppenderName = null;
    private String currentAppenderClassName = null;
    private String currentAppenderInterval = null;
    private boolean currentAppenderEnabled = true;
    private Properties currentAppenderAttributes = null;
    
    // Current Attribute information
    private String currentAttributeName = null;
    private String currentAttributeData = null;
  
    // Current ThreadTraceConfig
    private String currentThreadTraceMonitorName = null;
    private ThreadTraceConfig currentThreadTraceConfig = null;
    private List<ThreadTraceConfig.Trigger> currentTriggers = null;
    private final PropertyStringFilter filter;
    
    // Current properties
    private Map<String, String> currentProperties = null;
    private String currentPropertyKey = null;
    private Boolean currentPropertiesActivationFlag = null;

    @Override() public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
    	startElementWorker(uri, name, qName, new SystemPropertyAwareAttributes(atts, filter));
    }
    
    private boolean processEnabledAttribute(Attributes atts) {
    	boolean result = true;
        String value = atts.getValue("enabled");
        if (value != null && !Boolean.parseBoolean(value)) {
            result = false;
        }
        return result;
    }
    
    public void startElementWorker(String uri, String name, String qName, Attributes atts) throws SAXException {
        switch (currentState) {
            case STATE_UNDEFINED: 
                if (!ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
                    throw new SAXException("Did not find root element: " + ROOT_ELEMENT_NAME);
                }
                config.setEnabled(processEnabledAttribute(atts));
                currentState = STATE_IN_ROOT;
                break;
                
            case STATE_DONE: 
                throw new SAXException("Unexpected element: " + name);
                
            case STATE_IN_ROOT:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    currentAppenderName = atts.getValue("name");
                    currentAppenderClassName = atts.getValue("className");
                    currentAppenderInterval = atts.getValue("interval");
                    currentAppenderEnabled = processEnabledAttribute(atts);
                    currentAppenderAttributes = null;
                    
                    validateArg(APPENDER_NAME, "name", currentAppenderName);
                    validateArg(APPENDER_NAME, "className", currentAppenderClassName);
                    validateArg(APPENDER_NAME, "interval", currentAppenderInterval);
                    
                    currentState = STATE_IN_APPENDER;
                } else if (MONITOR_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");

                    validateArg(MONITOR_NAME, "name", nameAttr);
                    currentMonitorConfig = config.defineMonitor(nameAttr);
                    currentMonitorConfig.setFlaggedAsDisabled(!processEnabledAttribute(atts));
                    currentState = STATE_IN_MONITOR;
                } else if (ALIAS_NAME.equalsIgnoreCase(name)) {
                    logger.logDebug("Alias names are no longer supported in perfmon4j");
                } else if (SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name) || EMITTER_MONITOR_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    String classNameAttr = atts.getValue("className");

                    validateArg(name, "name", nameAttr);
                    validateArg(name, "className", classNameAttr);
                    currentSnapShotEnabled = processEnabledAttribute(atts);
 
                    currentSnapShotMonitor = new SnapShotMonitorVO(nameAttr, classNameAttr);
                    currentState = STATE_IN_SNAP_SHOT_MONITOR;
                } else if (MBEAN_SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name)) {
                	// Get required attributes
                	String nameAttr = MiscHelper.blankToNull(atts.getValue("name"));
                	String jmxNameAttr = MiscHelper.blankToNull(atts.getValue("jmxName"));
                    
                    validateArg(name, "name", nameAttr);
                    validateArg(name, "jmxNameAttr", jmxNameAttr);
                    
                    // Must supply a counters or gauges attribute
                    String gaugesAttr = atts.getValue("gauges");
                    String countersAttr = atts.getValue("counters");
                    String ratiosAttr = atts.getValue("ratios");
                    validateArg(name, "gauges or counters or ratios", MiscHelper.mergeStrings(gaugesAttr, countersAttr, ratiosAttr));
                    
                    
                    String domainAttr = MiscHelper.blankToNull(atts.getValue("domain"));
                    String instanceKeyAttr = MiscHelper.blankToNull(atts.getValue("instanceKey"));
                    String instanceValueFilter = MiscHelper.blankToNull(atts.getValue("instanceValueFilter"));
                    String attributeValueFilter = MiscHelper.blankToNull(atts.getValue("attributeValueFilter"));
                    

                    boolean enabled = processEnabledAttribute(atts);
                    MBeanQueryBuilder builder = new MBeanQueryBuilder(jmxNameAttr)
                    	.setDomain(domainAttr)
                    	.setDisplayName(nameAttr)
                    	.setInstanceKey(instanceKeyAttr)
                    	.setInstanceValueFilter(instanceValueFilter)
                    	.setAttributeValueFilter(attributeValueFilter)
                    	.setCounters(countersAttr)
                    	.setGauges(gaugesAttr)
                    	.setRatios(ratiosAttr);
                    try {
	                    currentMBeanSnapShotMonitor = new MBeanSnapShotMonitorVO(builder.build(), enabled);
	                    currentState = STATE_IN_MBEAN_SNAP_SHOT_MONITOR;
					} catch (Exception e) {
						throw new SAXException("Error processing: " + name, e);
					}
                } else if (THREAD_TRACE_NAME.equalsIgnoreCase(name)  ){
                    currentThreadTraceConfig = new ThreadTraceConfig();

                    currentThreadTraceMonitorName = atts.getValue("monitorName");
                    String maxDepth = atts.getValue("maxDepth");
                    String minDurationToCapture = atts.getValue("minDurationToCapture");
                    String randomSamplingFactor = atts.getValue("randomSamplingFactor");
                    
                    currentThreadTraceConfig.setFlaggedAsDisabled(!processEnabledAttribute(atts));
                    
                    validateArg(THREAD_TRACE_NAME,"monitorName", currentThreadTraceMonitorName);
                    
                    if (maxDepth != null) {
                        currentThreadTraceConfig.setMaxDepth(Integer.parseInt(maxDepth));
                    }
                    if (minDurationToCapture != null) {
                        currentThreadTraceConfig.setMinDurationToCapture((int)MiscHelper.convertIntervalStringToMillis(minDurationToCapture, 0, "millis"));
                    }
                    if (randomSamplingFactor != null) {
                        currentThreadTraceConfig.setRandomSamplingFactor(Integer.parseInt(randomSamplingFactor));
                    }

                    config.addThreadTraceConfig(currentThreadTraceMonitorName, currentThreadTraceConfig);
                    currentState = STATE_IN_THREAD_TRACE;
                } else if (BOOT_NAME.equalsIgnoreCase(name)) {
                	currentState = STATE_IN_BOOT;
                } else if (PROPERTIES_NAME.equalsIgnoreCase(name)) {
                	currentState = STATE_IN_PROPERTIES;
                	currentProperties = new HashMap<String, String>();
                	currentPropertyKey = null;
                	currentPropertiesActivationFlag = null;
        		} else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
              
            case STATE_IN_MONITOR:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    String patternAttr = atts.getValue("pattern");
                    
                    validateArg(MONITOR_NAME + "." + APPENDER_NAME, "name", nameAttr);
                    if (patternAttr == null) {
                        patternAttr = PerfMon.APPENDER_PATTERN_PARENT_ONLY;
                    }
                    try {
                        config.attachAppenderToMonitor(currentMonitorConfig.getMonitorName(), nameAttr, patternAttr);
                    } catch (InvalidConfigException ex) {
                        throw new SAXException("Invalid configuration", ex);
                    }
                } else if (ATTRIBUTE_NAME.equalsIgnoreCase(name)) {
                    currentAttributeName = atts.getValue("name");
                    currentAttributeData = null;
                    validateArg(MONITOR_NAME + "." + ATTRIBUTE_NAME, "name", currentAttributeName);
                    currentState = STATE_IN_MONITOR_ATTRIBUTE;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;

            case STATE_IN_APPENDER:
                if (ATTRIBUTE_NAME.equalsIgnoreCase(name)) {
                    currentAttributeName = atts.getValue("name");
                    currentAttributeData = null;
                    validateArg(APPENDER_NAME + "." + ATTRIBUTE_NAME, "name", currentAttributeName);
                    currentState = STATE_IN_APPENDER_ATTRIBUTE;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
                
            case STATE_IN_SNAP_SHOT_MONITOR:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    
                    validateArg(SNAP_SHOT_MONITOR_NAME + "." + APPENDER_NAME, "name", nameAttr);
                    currentSnapShotMonitor.appenders.add(nameAttr);
                } else if (ATTRIBUTE_NAME.equalsIgnoreCase(name)) {
                    currentAttributeName = atts.getValue("name");
                    currentAttributeData = null;
                    validateArg(APPENDER_NAME + "." + ATTRIBUTE_NAME, "name", currentAttributeName);
                    currentState = STATE_IN_SNAP_SHOT_ATTRIBUTE;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
                
            case STATE_IN_MBEAN_SNAP_SHOT_MONITOR:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    
                    validateArg(SNAP_SHOT_MONITOR_NAME + "." + APPENDER_NAME, "name", nameAttr);
                    currentMBeanSnapShotMonitor.appenders.add(nameAttr);
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;                
                
            case STATE_IN_THREAD_TRACE:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    
                    validateArg(THREAD_TRACE_NAME + "." + APPENDER_NAME, "name", nameAttr);
                    boolean isDisabledAppender = config.isDisabledAppender(nameAttr);

                    if (!isDisabledAppender) {
	                    AppenderID appenderID = config.getAppenderForName(nameAttr);
	                    if (appenderID == null) {
	                    	logger.logError("Appender: \"" + nameAttr + "\" not defined. Attaching ThreadTraceMonitor \"" 
	                    			+ currentThreadTraceMonitorName + "\" to the default text appender." );
	
	                    	appenderID = config.getOrCreateDefaultAppender();
	                    }
	                    currentThreadTraceConfig.addAppender(appenderID);
                    } else {
                    	currentThreadTraceConfig.setContainsDisabledAppenders(true);
                    }
                } else if (THREAD_TRACE_TRIGGERS_NAME.equalsIgnoreCase(name)) {
                	currentState = STATE_IN_THREAD_TRACE_TRIGGERS;
                	currentTriggers = new Vector<ThreadTraceConfig.Trigger>();
                }  else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
        
            case STATE_IN_THREAD_TRACE_TRIGGERS:
                if (HTTP_REQUEST_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("name");
                    String valueParam = atts.getValue("value");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + HTTP_REQUEST_TRIGGER_NAME;
                    validateArg(location, "name", nameParam);
                    validateArg(location, "value", valueParam);
                    
                    currentTriggers.add(new ThreadTraceConfig.HTTPRequestTrigger(nameParam, valueParam));
                } else if (HTTP_SESSION_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("attributeName");
                    String valueParam = atts.getValue("attributeValue");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + HTTP_SESSION_TRIGGER_NAME;
                    validateArg(location, "attributeName", nameParam);
                    validateArg(location, "attributeValue", valueParam);
                    
                    currentTriggers.add(new ThreadTraceConfig.HTTPSessionTrigger(nameParam, valueParam));
                } else if (HTTP_COOKIE_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("name");
                    String valueParam = atts.getValue("value");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + HTTP_COOKIE_TRIGGER_NAME;
                    validateArg(location, "name", nameParam);
                    validateArg(location, "value", valueParam);
                    
                    currentTriggers.add(new ThreadTraceConfig.HTTPCookieTrigger(nameParam, valueParam));
                } else if (THREAD_NAME_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String threadNameParam = atts.getValue("threadName");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + THREAD_NAME_TRIGGER_NAME;
                    validateArg(location, "threadName", threadNameParam);
                    
                    currentTriggers.add(new ThreadTraceConfig.ThreadNameTrigger(threadNameParam));
                } else if (THREAD_PROPERTY_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("name");
                    String valueParam = atts.getValue("value");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + THREAD_PROPERTY_TRIGGER_NAME;
                    validateArg(location, "name", nameParam);
                    validateArg(location, "value", valueParam);
                    
                    currentTriggers.add(new ThreadTraceConfig.ThreadPropertytTrigger(nameParam, valueParam));
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
        
		    case STATE_IN_PROPERTIES:
		    	if (PROPERTY_NAME.equalsIgnoreCase(name)) {
                    String propertyKey = atts.getValue("name");

                    String location = PROPERTIES_NAME + "." + PROPERTY_NAME;
                    validateArg(location, "name", propertyKey);
                    
                    currentPropertyKey = propertyKey;
                    currentState = STATE_IN_PROPERTIES_PROPERTY;
                } else if (ACTIVATION_NAME.equalsIgnoreCase(name)) {
                	if (currentPropertiesActivationFlag != null) {
                        throw new SAXException("Multiple activationFlags not allowed in properties" );
                	}
                    currentState = STATE_IN_ACTIVATION;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
		        break;
		        
		    case STATE_IN_ACTIVATION:
		    	if (PROPERTY_NAME.equalsIgnoreCase(name)) {
                    String propertyKey = atts.getValue("name");

                    String location = ACTIVATION_NAME + "." + PROPERTY_NAME;
                    validateArg(location, "name", propertyKey);
                    
                    currentPropertyKey = propertyKey;
                    currentState = STATE_IN_ACTIVATION_PROPERTY;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
		        break;
        	}
    }

    private static void validateArg(String section, String name, String value) throws SAXException {
        if (MiscHelper.isBlankOrNull(value)) {
            throw new SAXException("Attribute: " + name + " required for section: " + section);
        }
    }
    
    public void endElement(@SuppressWarnings("unused") String uri, String name, @SuppressWarnings("unused") String qName) throws SAXException {
        if (ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
            currentState = STATE_DONE;
        } 
        
        if (MONITOR_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_MONITOR) {
            currentState = STATE_IN_ROOT;
        }

        if (APPENDER_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_APPENDER) {
        	if (currentAppenderEnabled) {
	            config.defineAppender(currentAppenderName, currentAppenderClassName, currentAppenderInterval,
	                currentAppenderAttributes);
        	} else {
        		config.addDisabledAppender(currentAppenderName);
        	}
            currentState = STATE_IN_ROOT;
        }

        if (THREAD_TRACE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_THREAD_TRACE) {
            currentThreadTraceConfig = null;
            currentThreadTraceMonitorName = null;
            currentState = STATE_IN_ROOT;
        }

        if (THREAD_TRACE_TRIGGERS_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_THREAD_TRACE_TRIGGERS) {
        	currentThreadTraceConfig.setTriggers(currentTriggers.toArray(new ThreadTraceConfig.Trigger[]{}));
        	currentState = STATE_IN_THREAD_TRACE;
        }
        
        if ((SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name) || EMITTER_MONITOR_NAME.equalsIgnoreCase(name)) && currentState == STATE_IN_SNAP_SHOT_MONITOR) {
        	if (currentSnapShotEnabled) { 
	        	if (currentSnapShotMonitor.attributes.size() > 0) {
	                config.defineSnapShotMonitor(currentSnapShotMonitor.name, currentSnapShotMonitor.className,
	                    currentSnapShotMonitor.attributes);
	            } else {
	                config.defineSnapShotMonitor(currentSnapShotMonitor.name, currentSnapShotMonitor.className);
	            }
	            
	            Iterator itr = currentSnapShotMonitor.appenders.iterator();
	            while (itr.hasNext()) {
	                String snapShotMonitorName = currentSnapShotMonitor.name;
	                String appenderName = (String)itr.next();
	                try {
	                    config.attachAppenderToSnapShotMonitor(snapShotMonitorName, appenderName);
	                } catch (InvalidConfigException ex) {
	                    logger.logWarn("Unable to attach appender to monitor: " + snapShotMonitorName, ex);
	                }
	            }
        	} else {
        		currentSnapShotEnabled = true;
        	}
            currentState = STATE_IN_ROOT;
        }

        if (MBEAN_SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_MBEAN_SNAP_SHOT_MONITOR) {
        	if (currentMBeanSnapShotMonitor.enabled) { 
        		config.defineMBeanSnapShotMonitor(currentMBeanSnapShotMonitor.query);
        		
                String snapShotMonitorName = currentMBeanSnapShotMonitor.query.getDisplayName();
	            Iterator<String> itr = currentMBeanSnapShotMonitor.appenders.iterator();
	            while (itr.hasNext()) {
	                String appenderName = itr.next();
	                try {
	                    config.attachAppenderToSnapShotMonitor(snapShotMonitorName, appenderName);
	                } catch (InvalidConfigException ex) {
	                    logger.logWarn("Unable to attach appender to monitor: " + snapShotMonitorName, ex);
	                }
	            }
        	}
            currentState = STATE_IN_ROOT;
        }        
        
        if (ATTRIBUTE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_MONITOR_ATTRIBUTE) {
        	String attributeData = currentAttributeData != null ? currentAttributeData : "";
        	currentMonitorConfig.setProperty(currentAttributeName, attributeData);
            currentState = STATE_IN_MONITOR;
        }
        
        if (ATTRIBUTE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_APPENDER_ATTRIBUTE) {
            if (currentAppenderAttributes == null) {
                currentAppenderAttributes = new Properties();
            }
            currentAppenderAttributes.setProperty(currentAttributeName, 
                currentAttributeData == null ? "" : currentAttributeData);
            currentState = STATE_IN_APPENDER;
        }
        
        if (ATTRIBUTE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_SNAP_SHOT_ATTRIBUTE) {
            if (currentAttributeData == null) {
                currentAttributeData = "";
            }
            currentSnapShotMonitor.attributes.put(currentAttributeName, currentAttributeData);
            
            currentState = STATE_IN_SNAP_SHOT_MONITOR;
        }

        if (BOOT_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_BOOT) {
            currentState = STATE_IN_ROOT;
        }
    
        if (PROPERTIES_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_PROPERTIES) {
        	if (currentProperties != null && (currentPropertiesActivationFlag == null || currentPropertiesActivationFlag.booleanValue())) {
        		for (Map.Entry<String, String> entry : currentProperties.entrySet()) {
        			config.getConfigurationProperties().setProperty(entry.getKey(), entry.getValue());
        		}
        	}
        	currentProperties = null;
        	currentPropertyKey = null;
        	currentPropertiesActivationFlag = null;
            currentState = STATE_IN_ROOT;
        }
        
        if (PROPERTY_NAME.equalsIgnoreCase(name)) {
            if (currentState == STATE_IN_PROPERTIES_PROPERTY) {
            	if (currentProperties != null) {
            		currentProperties.put(currentPropertyKey, currentAttributeData);
            	}
                currentState = STATE_IN_PROPERTIES;
            } else if (currentState == STATE_IN_ACTIVATION_PROPERTY) {
            	if (currentPropertiesActivationFlag == null || currentPropertiesActivationFlag.booleanValue()) {
            		String propValue = config.getConfigurationProperties().getProperty(currentPropertyKey);
            		if (propValue == null) {
            			currentPropertiesActivationFlag = Boolean.FALSE;
            		} else {
            			if (currentAttributeData == null) {
            				currentPropertiesActivationFlag = Boolean.TRUE;
            			} else {
            				currentPropertiesActivationFlag = Boolean.valueOf(currentAttributeData.equals(propValue));
            			}
            		}
            	}
                currentState = STATE_IN_ACTIVATION;
            }
            currentAttributeData = null;
        }

        if (ACTIVATION_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_ACTIVATION) {
            currentState = STATE_IN_PROPERTIES;
        }
        
    }
    
    public void characters (char ch[], int start, int length) throws SAXException {
        if (currentState == STATE_IN_APPENDER_ATTRIBUTE || currentState == STATE_IN_MONITOR_ATTRIBUTE || currentState == STATE_IN_SNAP_SHOT_ATTRIBUTE
        	|| currentState == STATE_IN_PROPERTIES_PROPERTY || currentState == STATE_IN_ACTIVATION_PROPERTY) {
            if (currentAttributeData == null) {
                currentAttributeData = "";
            }
            currentAttributeData +=  filter.doFilter(new String(ch, start, length));
        }
    }
    
    private static class SnapShotMonitorVO {
        final String name;
        final String className;
        final Properties attributes = new Properties();
        final Set appenders = new HashSet();
        
        SnapShotMonitorVO(String name, String className) {
            this.name = name;
            this.className = className;
        }
    }
    
    private static class MBeanSnapShotMonitorVO {
    	final MBeanQuery query;
    	final boolean enabled;
    	
    	final Set<String> appenders = new HashSet<>();

		public MBeanSnapShotMonitorVO(MBeanQuery query, boolean enabled) {
			super();
			this.query = query;
			this.enabled = enabled;
		}
    }
    
    
    private static class SystemPropertyAwareAttributes implements Attributes {
    	private final PropertyStringFilter filter;
    	private final Attributes wrapped;
    	
    	SystemPropertyAwareAttributes(Attributes wrapped, PropertyStringFilter filter) {
    		this.wrapped = wrapped;
    		this.filter = filter;
    	}

		public int getLength() {
			return wrapped.getLength();
		}

		public String getURI(int index) {
			return wrapped.getURI(index);
		}

		public String getLocalName(int index) {
			return wrapped.getLocalName(index);
		}

		public String getQName(int index) {
			return wrapped.getQName(index);
		}

		public String getType(int index) {
			return wrapped.getType(index);
		}

		public String getValue(int index) {
			return filter.doFilter(wrapped.getValue(index));
		}

		public int getIndex(String uri, String localName) {
			return wrapped.getIndex(uri, localName);
		}

		public int getIndex(String qName) {
			return wrapped.getIndex(qName);
		}

		public String getType(String uri, String localName) {
			return wrapped.getType(uri, localName);
		}

		public String getType(String qName) {
			return wrapped.getType(qName);
		}

		public String getValue(String uri, String localName) {
			return filter.doFilter(wrapped.getValue(uri, localName));
		}

		public String getValue(String qName) {
			return filter.doFilter(wrapped.getValue(qName));
		}
    }
    
}
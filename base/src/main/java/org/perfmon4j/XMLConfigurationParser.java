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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.PropertyStringFilter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


// Only package level
class XMLConfigurationParser extends DefaultHandler {
    private static final Logger logger = LoggerFactory.initLogger(XMLConfigurationParser.class);
    
    private final XMLPerfMonConfiguration config = new XMLPerfMonConfiguration();
    
    private static XMLPerfMonConfiguration getDefaultConfiguration()
        throws InvalidConfigException {
        XMLPerfMonConfiguration result = new XMLPerfMonConfiguration();
        
        final String APPENDER_NAME = "DEFAULT_APPENDER";
        result.defineAppender(APPENDER_NAME, TextAppender.class.getName(),
            Appender.DEFAULT_INTERVAL_MILLIS + " ms");
        result.defineMonitor(PerfMon.ROOT_MONITOR_NAME);
        result
            .attachAppenderToMonitor(PerfMon.ROOT_MONITOR_NAME, APPENDER_NAME);
        return result;
    }
    
    public static XMLPerfMonConfiguration parseXML(Reader reader)
        throws InvalidConfigException {
        XMLPerfMonConfiguration result = null;
        try {
            XMLConfigurationParser handler = new XMLConfigurationParser();
            XMLReader xr = XMLReaderFactory.createXMLReader();
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
        return result;
    }
    
    private final static String ROOT_ELEMENT_NAME = "Perfmon4JConfig";
    private final static String APPENDER_NAME = "appender";
    private final static String MONITOR_NAME = "monitor";
    private final static String ALIAS_NAME = "alias";
    private final static String ATTRIBUTE_NAME = "attribute";
    private final static String SNAP_SHOT_MONITOR_NAME = "snapShotMonitor";
    private final static String THREAD_TRACE_NAME = "threadTrace";
    private final static String THREAD_TRACE_TRIGGERS_NAME = "Triggers";
    private final static String HTTP_REQUEST_TRIGGER_NAME = "HttpRequestTrigger";
    private final static String HTTP_SESSION_TRIGGER_NAME = "HTTPSessionTrigger";
    private final static String HTTP_COOKIE_TRIGGER_NAME = "HTTPCookieTrigger";
    private final static String THREAD_NAME_TRIGGER_NAME = "ThreadNameTrigger";
    private final static String THREAD_PROPERTY_TRIGGER_NAME = "ThreadPropertyTrigger";
    
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
    
    private int currentState = STATE_UNDEFINED;
    private String currentMonitorName = null;
    private SnapShotMonitorVO currentSnapShotMonitor = null;

    // Current Appender information
    private String currentAppenderName = null;
    private String currentAppenderClassName = null;
    private String currentAppenderInterval = null;
    private Properties currentAppenderAttributes = null;
    
    // Current Attribute information
    private String currentAttributeName = null;
    private String currentAttributeData = null;
  
    // Current ThreadTraceConfig
    private ThreadTraceConfig currentThreadTraceConfig = null;
    private List<ThreadTraceConfig.Trigger> currentTriggers = null;
    
    
    @Override() public void startElement(@SuppressWarnings("unused") String uri, String name, @SuppressWarnings("unused") String qName,
        Attributes atts) throws SAXException {
        switch (currentState) {
            case STATE_UNDEFINED: 
                if (!ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
                    throw new SAXException("Did not find root element: " + ROOT_ELEMENT_NAME);
                }
                String value = atts.getValue("enabled");
                if (value != null && !Boolean.parseBoolean(value)) {
                    config.setEnabled(false);
                }
                currentState = STATE_IN_ROOT;
                break;
                
            case STATE_DONE: 
                throw new SAXException("Unexpected element: " + name);
                
            case STATE_IN_ROOT:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    currentAppenderName = atts.getValue("name");
                    currentAppenderClassName = atts.getValue("className");
                    currentAppenderInterval = atts.getValue("interval");
                    currentAppenderAttributes = null;
                    
                    validateArg(APPENDER_NAME, "name", currentAppenderName);
                    validateArg(APPENDER_NAME, "className", currentAppenderClassName);
                    validateArg(APPENDER_NAME, "interval", currentAppenderInterval);
                    
                    currentState = STATE_IN_APPENDER;
                } else if (MONITOR_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");

                    validateArg(MONITOR_NAME, "name", nameAttr);
                    
                    config.defineMonitor(nameAttr);
                    currentMonitorName = nameAttr;
                    currentState = STATE_IN_MONITOR;
                } else if (ALIAS_NAME.equalsIgnoreCase(name)) {
                    logger.logDebug("Alias names are no longer supported in perfmon4j");
                } else if (SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    String classNameAttr = atts.getValue("className");

                    validateArg(SNAP_SHOT_MONITOR_NAME, "name", nameAttr);
                    validateArg(SNAP_SHOT_MONITOR_NAME, "className", classNameAttr);
 
                    currentSnapShotMonitor = new SnapShotMonitorVO(nameAttr, classNameAttr);
                    currentState = STATE_IN_SNAP_SHOT_MONITOR;
                } else if (THREAD_TRACE_NAME.equalsIgnoreCase(name)  ){
                    currentThreadTraceConfig = new ThreadTraceConfig();

                    String monitorNameAttr = atts.getValue("monitorName");
                    String maxDepth = atts.getValue("maxDepth");
                    String minDurationToCapture = atts.getValue("minDurationToCapture");
                    String randomSamplingFactor = atts.getValue("randomSamplingFactor");
                    
                    validateArg(THREAD_TRACE_NAME,"monitorName", monitorNameAttr);
                    
                    if (maxDepth != null) {
                        currentThreadTraceConfig.setMaxDepth(Integer.parseInt(maxDepth));
                    }
                    if (minDurationToCapture != null) {
                        currentThreadTraceConfig.setMinDurationToCapture((int)MiscHelper.convertIntervalStringToMillis(minDurationToCapture, 0, "millis"));
                    }
                    if (randomSamplingFactor != null) {
                        currentThreadTraceConfig.setRandomSamplingFactor(Integer.parseInt(randomSamplingFactor));
                    }

                    config.addThreadTraceConfig(monitorNameAttr, currentThreadTraceConfig);
                    currentState = STATE_IN_THREAD_TRACE;
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
                        patternAttr = PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS;
                    }
                    try {
                        config.attachAppenderToMonitor(currentMonitorName, nameAttr, patternAttr);
                    } catch (InvalidConfigException ex) {
                        throw new SAXException("Invalid configuration", ex);
                    }
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
                
            case STATE_IN_THREAD_TRACE:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String nameAttr = atts.getValue("name");
                    
                    validateArg(THREAD_TRACE_NAME + "." + APPENDER_NAME, "name", nameAttr);
                    currentThreadTraceConfig.addAppender(config.getAppenderForName(nameAttr));
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
        }
    }

    private static void validateArg(String section, String name, String value) throws SAXException {
        if (value == null || "".equals(value)) {
            throw new SAXException("Attribute: " + name + " required for section: " + section);
        }
    }
    
    public void endElement(@SuppressWarnings("unused") String uri, String name, @SuppressWarnings("unused") String qName) {
        if (ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
            currentState = STATE_DONE;
        } 
        
        if (MONITOR_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_MONITOR) {
            currentState = STATE_IN_ROOT;
        }

        if (APPENDER_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_APPENDER) {
            config.defineAppender(currentAppenderName, currentAppenderClassName, currentAppenderInterval,
                currentAppenderAttributes);
            currentState = STATE_IN_ROOT;
        }

        if (THREAD_TRACE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_THREAD_TRACE) {
            currentThreadTraceConfig = null;
            currentState = STATE_IN_ROOT;
        }

        if (THREAD_TRACE_TRIGGERS_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_THREAD_TRACE_TRIGGERS) {
        	currentThreadTraceConfig.setTriggers(currentTriggers.toArray(new ThreadTraceConfig.Trigger[]{}));
        	currentState = STATE_IN_THREAD_TRACE;
        }
        
        if (SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_SNAP_SHOT_MONITOR) {
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
            
            currentState = STATE_IN_ROOT;
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
    }
    
    public void characters (char ch[], int start, int length) throws SAXException {
        if (currentState == STATE_IN_APPENDER_ATTRIBUTE || currentState == STATE_IN_SNAP_SHOT_ATTRIBUTE) {
            if (currentAttributeData == null) {
                currentAttributeData = "";
            }
            currentAttributeData +=  PropertyStringFilter.filter(new String(ch, start, length));
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
}
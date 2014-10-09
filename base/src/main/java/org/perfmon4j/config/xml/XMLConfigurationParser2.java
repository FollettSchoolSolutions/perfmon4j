/*
 *	Copyright 2008-2014 Follett Software Company 
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
package org.perfmon4j.config.xml;

import java.io.IOException;
import java.io.Reader;

import org.perfmon4j.InvalidConfigException;
import org.perfmon4j.PerfMon;
import org.perfmon4j.config.xml.TriggerConfigElement.Type;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.PropertyStringFilter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


// Only package level
class XMLConfigurationParser2 extends DefaultHandler {
    private static final Logger logger = LoggerFactory.initLogger(XMLConfigurationParser2.class);
    
    public static final String DEFAULT_XML_READER_CLASS = "com.sun.org.apache.xerces.internal.parsers.SAXParser";
    public static String lastKnownGoodXMLReaderClass = null;
    
    public static ConfigElement parseXML(Reader reader)
        throws InvalidConfigException {
        ConfigElement result = null;
        try {
            XMLConfigurationParser2 handler = new XMLConfigurationParser2();
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
            result = handler.configElement;
        } catch (SAXException ex) {
            logger.logError("SAXException parsing configuration... Returning default configuration", ex);
        } catch (IOException ex) {
            logger.logError("IOException parsing configuration... Returning default configuration", ex);
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
    
    private int currentState = STATE_UNDEFINED;
    
    private String currentAttributeData = null;
  
    private final ConfigElement configElement = new ConfigElement();
    
    private AppenderConfigElement currentAppenderElement = null;
    private MonitorConfigElement currentMonitorElement = null;
    private SnapShotConfigElement currentSnapShotElement = null;
    private ThreadTraceConfigElement currentThreadTraceElement = null;
    
//    private boolean isActiveBasedOnProperty(Attributes atts) {
//        boolean result = true;
//        
//    	String systemProperty = atts.getValue("ifSystemProperty");
//    	if (systemProperty != null) {
//    		result = Boolean.parseBoolean(systemProperty);
//    		configElement.getSystemPropertyState().put(systemProperty, Boolean.valueOf(result));
//    	}
//    	
//    	return result;
//    }
    
    private boolean isEnabled(Attributes atts) {
    	boolean result = true;
    	
        String value = atts.getValue("enabled");
        if (value != null && !Boolean.parseBoolean(value)) {
            result = false;
        }

        return result;
    }
    
    
    @Override() public void startElement(String uri, String name, String qName,
        Attributes atts) throws SAXException {
        switch (currentState) {
            case STATE_UNDEFINED: 
                if (!ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
                    throw new SAXException("Did not find root element: " + ROOT_ELEMENT_NAME);
                }
                configElement.setEnabled(isEnabled(atts));
                currentState = STATE_IN_ROOT;
                break;
                
            case STATE_DONE: 
                throw new SAXException("Unexpected element: " + name);
                
            case STATE_IN_ROOT:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String attrName = atts.getValue("name");
                    String attrClassName = atts.getValue("className");
                    String attrInterval = atts.getValue("interval");

                    validateArg(APPENDER_NAME, "name", attrName);
                    validateArg(APPENDER_NAME, "className", attrClassName);
                    validateArg(APPENDER_NAME, "interval", attrInterval);
                    
                    currentAppenderElement = new AppenderConfigElement();
                    currentAppenderElement.setName(attrName);
                    currentAppenderElement.setClassName(attrClassName);
                    currentAppenderElement.setInterval(attrInterval);
                    currentAppenderElement.setEnabled(isEnabled(atts));
                    
                    currentState = STATE_IN_APPENDER;
                } else if (MONITOR_NAME.equalsIgnoreCase(name)) {
                    String attrName = atts.getValue("name");

                    validateArg(MONITOR_NAME, "name", attrName);
              
                    currentMonitorElement = new MonitorConfigElement();
                    currentMonitorElement.setName(attrName);
                    currentMonitorElement.setEnabled(isEnabled(atts));
                    
                    currentState = STATE_IN_MONITOR;
                } else if (ALIAS_NAME.equalsIgnoreCase(name)) {
                    logger.logDebug("Alias names are no longer supported in perfmon4j");
                } else if (SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name)) {
                    String attrName = atts.getValue("name");
                    String attrClassName = atts.getValue("className");

                    validateArg(SNAP_SHOT_MONITOR_NAME, "name", attrName);
                    validateArg(SNAP_SHOT_MONITOR_NAME, "className", attrClassName);
 
                    currentSnapShotElement = new SnapShotConfigElement();
                    currentSnapShotElement.setName(attrName);
                    currentSnapShotElement.setClassName(attrClassName);
                    currentSnapShotElement.setEnabled(isEnabled(atts));
                    
                    currentState = STATE_IN_SNAP_SHOT_MONITOR;
                } else if (THREAD_TRACE_NAME.equalsIgnoreCase(name)  ){
                    String attrName = atts.getValue("monitorName");
                    String maxDepth = atts.getValue("maxDepth");
                    String minDurationToCapture = atts.getValue("minDurationToCapture");
                    String randomSamplingFactor = atts.getValue("randomSamplingFactor");
                    
                    validateArg(THREAD_TRACE_NAME,"monitorName", attrName);
                    
                    currentThreadTraceElement = new ThreadTraceConfigElement();
                    currentThreadTraceElement.setMonitorName(attrName);
                    currentThreadTraceElement.setMaxDepth(maxDepth);
                    currentThreadTraceElement.setMinDurationToCapture(minDurationToCapture);
                    currentThreadTraceElement.setRandomSamplingFactor(randomSamplingFactor);
                    currentThreadTraceElement.setEnabled(isEnabled(atts));

                    currentState = STATE_IN_THREAD_TRACE;
                } else if (BOOT_NAME.equalsIgnoreCase(name)) {
                	currentState = STATE_IN_BOOT;
        		} else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
              
            case STATE_IN_MONITOR:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String attrName = atts.getValue("name");
                    String attrPattern = atts.getValue("pattern");
                    
                    validateArg(MONITOR_NAME + "." + APPENDER_NAME, "name", attrName);
                    if (attrPattern == null) {
                        attrPattern = PerfMon.APPENDER_PATTERN_PARENT_ONLY;
                    }
                    
                    AppenderMappingElement mapping = new AppenderMappingElement();
                    mapping.setName(attrName);
                    mapping.setPattern(attrPattern);
                    
                    currentMonitorElement.getAppenders().add(mapping);
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;

            case STATE_IN_APPENDER:
                if (ATTRIBUTE_NAME.equalsIgnoreCase(name)) {
                    String attrKey = atts.getValue("name");
                    validateArg(APPENDER_NAME + "." + ATTRIBUTE_NAME, "name", attrKey);
                    currentAppenderElement.pushKey(attrKey);
                    
                    currentState = STATE_IN_APPENDER_ATTRIBUTE;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
                
            case STATE_IN_SNAP_SHOT_MONITOR:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String attrName = atts.getValue("name");
                    
                    validateArg(SNAP_SHOT_MONITOR_NAME + "." + APPENDER_NAME, "name", attrName);
                    currentSnapShotElement.getAppenders().add(new AppenderMappingElement(attrName));
                } else if (ATTRIBUTE_NAME.equalsIgnoreCase(name)) {
                    String attrKey = atts.getValue("name");
                    validateArg(APPENDER_NAME + "." + ATTRIBUTE_NAME, "name", attrKey);
                    currentSnapShotElement.pushKey(attrKey);
                    currentState = STATE_IN_SNAP_SHOT_ATTRIBUTE;
                } else {
                    throw new SAXException("Unexpected element: " + name);
                }
                break;
                
            case STATE_IN_THREAD_TRACE:
                if (APPENDER_NAME.equalsIgnoreCase(name)) {
                    String attrName = atts.getValue("name");
                    validateArg(THREAD_TRACE_NAME + "." + APPENDER_NAME, "name", attrName);
                    currentThreadTraceElement.getAppenders().add(new AppenderMappingElement(attrName));
                } else if (THREAD_TRACE_TRIGGERS_NAME.equalsIgnoreCase(name)) {
                	currentState = STATE_IN_THREAD_TRACE_TRIGGERS;
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
                    
                    currentThreadTraceElement.getTriggers().add(new TriggerConfigElement(Type.REQUEST_TRIGGER,
                    		nameParam, valueParam));
                } else if (HTTP_SESSION_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("attributeName");
                    String valueParam = atts.getValue("attributeValue");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + HTTP_SESSION_TRIGGER_NAME;
                    validateArg(location, "attributeName", nameParam);
                    validateArg(location, "attributeValue", valueParam);
                    
                    currentThreadTraceElement.getTriggers().add(new TriggerConfigElement(Type.SESSION_TRIGGER,
                    		nameParam, valueParam));
                } else if (HTTP_COOKIE_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("name");
                    String valueParam = atts.getValue("value");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + HTTP_COOKIE_TRIGGER_NAME;
                    validateArg(location, "name", nameParam);
                    validateArg(location, "value", valueParam);
                    
                    currentThreadTraceElement.getTriggers().add(new TriggerConfigElement(Type.COOKIE_TRIGGER,
                    		nameParam, valueParam));
                } else if (THREAD_NAME_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String threadNameParam = atts.getValue("threadName");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + THREAD_NAME_TRIGGER_NAME;
                    validateArg(location, "threadName", threadNameParam);
                    
                    currentThreadTraceElement.getTriggers().add(new TriggerConfigElement(Type.THREAD_TRIGGER,
                    		threadNameParam, null));
                } else if (THREAD_PROPERTY_TRIGGER_NAME.equalsIgnoreCase(name)) {
                    String nameParam = atts.getValue("name");
                    String valueParam = atts.getValue("value");

                    String location = THREAD_TRACE_NAME + "." + THREAD_TRACE_TRIGGERS_NAME + "." + THREAD_PROPERTY_TRIGGER_NAME;
                    validateArg(location, "name", nameParam);
                    validateArg(location, "value", valueParam);

                    currentThreadTraceElement.getTriggers().add(new TriggerConfigElement(Type.THREAD_PROPERTY_TRIGGER,
                    		nameParam, valueParam));
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
    
    public void endElement(String uri, String name, String qName) throws SAXException {
        if (ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
            currentState = STATE_DONE;
        } 
        
        if (MONITOR_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_MONITOR) {
        	configElement.getMonitors().add(currentMonitorElement);
        	currentMonitorElement = null;
        	
            currentState = STATE_IN_ROOT;
        }

        if (APPENDER_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_APPENDER) {
        	configElement.getAppenders().add(currentAppenderElement);
        	currentAppenderElement = null;
        	
            currentState = STATE_IN_ROOT;
        }

        if (THREAD_TRACE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_THREAD_TRACE) {
        	configElement.getThreadTraces().add(currentThreadTraceElement);
        	currentThreadTraceElement = null;
        	
            currentState = STATE_IN_ROOT;
        }

        if (THREAD_TRACE_TRIGGERS_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_THREAD_TRACE_TRIGGERS) {
        	currentState = STATE_IN_THREAD_TRACE;
        }
        
        if (SNAP_SHOT_MONITOR_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_SNAP_SHOT_MONITOR) {
        	configElement.getSnapShots().add(currentSnapShotElement);
        	currentSnapShotElement = null;
        	
            currentState = STATE_IN_ROOT;
        }
        
        if (ATTRIBUTE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_APPENDER_ATTRIBUTE) {
            currentAppenderElement.pushValue(currentAttributeData);
            currentAttributeData = null;
            
            currentState = STATE_IN_APPENDER;
        }
        
        if (ATTRIBUTE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_SNAP_SHOT_ATTRIBUTE) {
            currentSnapShotElement.pushValue(currentAttributeData);
            currentAttributeData = null;
            
            currentState = STATE_IN_SNAP_SHOT_MONITOR;
        }

        if (BOOT_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_BOOT) {
            currentState = STATE_IN_ROOT;
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
}
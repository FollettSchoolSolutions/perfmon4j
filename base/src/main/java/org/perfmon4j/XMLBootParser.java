/*
 *	Copyright 2012 Follett Software Company 
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
import java.io.StringReader;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


public class XMLBootParser extends DefaultHandler {
    private static final Logger logger = LoggerFactory.initLogger(XMLBootParser.class);
    
    private final BootConfiguration config = new BootConfiguration();
    
    private static BootConfiguration getDefaultConfiguration(){
    	
        return new BootConfiguration();
    }

    public static final String DEFAULT_XML_READER_CLASS = "com.sun.org.apache.xerces.internal.parsers.SAXParser";
    public static String lastKnownGoodXMLReaderClass = null;
    
    public static BootConfiguration parseXML(String xml) {
    	return parseXML(new StringReader(xml));
    }
    
    public static BootConfiguration parseXML(Reader reader) {
        BootConfiguration result = null;
        try {
            XMLBootParser handler = new XMLBootParser();
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
        return result;
    }
    
    private final static String ROOT_ELEMENT_NAME = "Perfmon4JConfig";
    private final static String BOOT_NAME = "boot";
    private final static String SERVLET_VALVE_NAME = "servletValve";
    
    private final int STATE_UNDEFINED                           = 0;
    private final int STATE_IN_ROOT                             = 1;
    private final int STATE_IN_BOOT                         	= 2;
    private final int STATE_IN_SERVLET_VALVE               		= 3;
    private final int STATE_IN_IGNORED_ELEMENT					= 4;
    private final int STATE_DONE               					= 5;
    
    private int currentState = STATE_UNDEFINED;
    
    @Override() public void startElement(@SuppressWarnings("unused") String uri, String name, @SuppressWarnings("unused") String qName,
        Attributes atts) throws SAXException {
        switch (currentState) {
            case STATE_UNDEFINED: 
                if (!ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
                    throw new SAXException("Did not find root element: " + ROOT_ELEMENT_NAME);
                }
                currentState = STATE_IN_ROOT;
                break;
                
            case STATE_DONE: 
                throw new SAXException("Unexpected element: " + name);
                
            case STATE_IN_ROOT:
                if (BOOT_NAME.equalsIgnoreCase(name)) {
                    currentState = STATE_IN_BOOT;
                } else {                
                	currentState = STATE_IN_IGNORED_ELEMENT;
                }
                break;
              
            case STATE_IN_BOOT:
                if (SERVLET_VALVE_NAME.equalsIgnoreCase(name)) {
                	currentState = STATE_IN_SERVLET_VALVE;
                	
                	BootConfiguration.ServletValveConfig valveConfig = new BootConfiguration.ServletValveConfig();
                	
                	String baseFilter = atts.getValue("baseFilterCategory");
                	if (baseFilter != null) {
                		valveConfig.setBaseFilterCategory(baseFilter);
                	}
                	String abort = atts.getValue("abortTimerOnRedirect");
                	if (abort != null) {
                		valveConfig.setAbortTimerOnRedirect(Boolean.parseBoolean(abort));
                	}
                	abort = atts.getValue("abortTimerOnImageResponse");
                	if (abort != null) {
                		valveConfig.setAbortTimerOnImageResponse(Boolean.parseBoolean(abort));
                	}
                	String output = atts.getValue("outputRequestAndDuration");
                	if (output != null) {
                		valveConfig.setOutputRequestAndDuration(Boolean.parseBoolean(output));
                	}
                	String push = atts.getValue("pushClientInfoOnNDC");
                	if (push != null) {
                		valveConfig.setPushClientInfoOnNDC(Boolean.parseBoolean(push));
                	}
                	push = atts.getValue("pushSessionAttributesOnNDC");
                	if (push != null) {
                		valveConfig.setPushSessionAttributesOnNDC(push);
                	}
                	push = atts.getValue("pushCookiesOnNDC");
                	if (push != null) {
                		valveConfig.setPushCookiesOnNDC(push);
                	}
                	push = atts.getValue("pushURLOnNDC");
                	if (push != null) {
                		valveConfig.setPushURLOnNDC(Boolean.parseBoolean(push));
                	}
                	abort = atts.getValue("abortTimerOnURLPattern");
                	if (abort != null) {
                		valveConfig.setAbortTimerOnURLPattern(abort);
                	}
                	String skip = atts.getValue("skipTimerOnURLPattern");
                	if (skip != null) {
                		valveConfig.setSkipTimerOnURLPattern(skip);
                	}
                	String servletPattern = atts.getValue("servletPathTransformationPattern");
                	if (servletPattern != null) {
                		valveConfig.setServletPathTransformationPattern(servletPattern);
                	}
                			
                	config.setServletValveConfig(valveConfig);
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
    
    public void endElement(@SuppressWarnings("unused") String uri, String name, @SuppressWarnings("unused") String qName) throws SAXException {
        if (ROOT_ELEMENT_NAME.equalsIgnoreCase(name)) {
            currentState = STATE_DONE;
        } 
        
        if (BOOT_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_BOOT) {
            currentState = STATE_IN_ROOT;
        }

        if (SERVLET_VALVE_NAME.equalsIgnoreCase(name) && currentState == STATE_IN_SERVLET_VALVE) {
            currentState = STATE_IN_BOOT;
        }
    }
    
    public void characters (char ch[], int start, int length) throws SAXException {
//        if (currentState == STATE_IN_APPENDER_ATTRIBUTE || currentState == STATE_IN_SNAP_SHOT_ATTRIBUTE) {
//            if (currentAttributeData == null) {
//                currentAttributeData = "";
//            }
//            currentAttributeData +=  PropertyStringFilter.filter(new String(ch, start, length));
//        }
    }
}
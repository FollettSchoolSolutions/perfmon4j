/*
 *	Copyright 2014 Follett Software Company 
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

import java.util.Map;
import java.util.Properties;

public abstract class AttributeConfigElement {
	/**
	 * IMPORTANT IF YOU ADD ATTRIBUTES:  Make sure you update the copy constructor!
	 */	
	private final Properties attributes = new Properties();
	private String key;
	
	protected AttributeConfigElement() {
	}
	
	protected AttributeConfigElement(AttributeConfigElement elementToCopy) {
		for (Map.Entry<Object, Object> e : elementToCopy.attributes.entrySet()) {
			attributes.setProperty((String)e.getKey(), (String)e.getValue());
		}
		this.key = elementToCopy.key;
	}
	
	public Properties getAttributes() {
		return attributes;
	}
	
	void pushKey(String key) {
		this.key = key;
	}
	
	void pushValue(String value) {
		attributes.put(key, value == null ? "" : value);
		key = null;
	}
}

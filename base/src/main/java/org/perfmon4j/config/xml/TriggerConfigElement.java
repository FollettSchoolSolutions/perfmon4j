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

public class TriggerConfigElement {
	/**
	 * IMPORTANT IF YOU ADD ATTRIBUTES:  Make sure you update the copy constructor/clone!
	 */
	private final Type type;
	private final String name;
	private final String value;
	
	public TriggerConfigElement(Type type, String name) {
		this(type, name, null);
	}
	
	public TriggerConfigElement(Type type, String name, String value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}

	private TriggerConfigElement(TriggerConfigElement elementToCopy) {
		this.type = elementToCopy.type;
		this.name = elementToCopy.name;
		this.value = elementToCopy.value;
	}
	
	public Type getType() {
		return type;
	}
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}

	public static enum Type {
	    REQUEST_TRIGGER,
	    SESSION_TRIGGER,
	    COOKIE_TRIGGER,
	    THREAD_TRIGGER,
	    THREAD_PROPERTY_TRIGGER
	}
	
	public TriggerConfigElement clone() {
		return new TriggerConfigElement(this);
	}
}

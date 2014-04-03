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

public class AppenderConfigElement extends AttributeConfigElement {
	/**
	 * IMPORTANT IF YOU ADD ATTRIBUTES:  Make sure you update the copy constructor/clone!
	 */
	private String name;
	private String className;
	private String interval;
	private boolean enabled;
	
	public AppenderConfigElement() {
	}

	private AppenderConfigElement(AppenderConfigElement elementToCopy) {
		super(elementToCopy);
		this.name = elementToCopy.name;
		this.className = elementToCopy.className;
		this.interval = elementToCopy.interval;
		this.enabled = elementToCopy.enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getInterval() {
		return interval;
	}
	public void setInterval(String interval) {
		this.interval = interval;
	}
	
	public AppenderConfigElement clone() {
		return new AppenderConfigElement(this);
	}
	
}

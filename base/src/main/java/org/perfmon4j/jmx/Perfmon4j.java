/* 
 *	Copyright 2021 Follett School Solutions, LLC  
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
 * 	ddeucher@follett.com 
 * 	David Deuchert 
 * 	Follett School Solutions, LLC 
 * 	1340 Ridgeview Dr 
 * 	McHenry, IL 60050 
*/ 

package org.perfmon4j.jmx;

import javax.management.JMException;

import org.perfmon4j.PerfMon;
import org.perfmon4j.util.JMXServerWrapper;

public class Perfmon4j implements Perfmon4jMBean, NamedObject {
	private static Perfmon4j singleton;
	private final JMXServerWrapper wrapper;
	
	static public Perfmon4j getSignleton() {
		return singleton;
	}
	
	/**
	 * This should only be called from within the Perfmon4j javaagent initialization (or
	 * during unit tests).  If this object is NOT initialized it should be taken
	 * as an indication that the user does not want to use the Perfmon4j JMX interface.
	 * 
	 * @param initialize
	 * @return
	 * @throws JMException
	 */
	static public synchronized Perfmon4j getSingleton(boolean initialize) throws JMException {
		if (singleton == null && initialize) {
			singleton = new Perfmon4j();
		}
		return singleton;
	}
	
	static public boolean isInitialized() {
		return singleton != null;
	}
	
	private Perfmon4j() throws JMException {
		this.wrapper = new JMXServerWrapper();
		wrapper.registerMBean(this);
	}
	
	@Override
	public String getVersion() {
		return PerfMon.version;
	}

	@Override
	public String getCopyright() {
		return PerfMon.copyright;
	}

	@Override
	public String getObjectName() {
		return "org.perfmon4j:type=deployment,name=Perfmon4j";
	}
	
	public void registerMBean(NamedObject namedMBean) {
		wrapper.registerMBean(namedMBean);
	}
}

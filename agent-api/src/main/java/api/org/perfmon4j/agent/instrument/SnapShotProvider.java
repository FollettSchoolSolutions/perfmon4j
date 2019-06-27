/*
 *	Copyright 2019 Follett School Solutions 
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
 * 	ddeuchert@follett.com
 * 	David Deuchert
 * 	Follett School Solutions
*/
package api.org.perfmon4j.agent.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class serves as an alias for the SnapShotProvider Annotation
 * implemented in the perfmon4j agent. 
 * 
 * The alias provides a simplified implementation using only default
 * values for the following attributes:
 * 	dataInterface(default) = void.class
 * 	usePriorityTimer(default) = false 
 *  sqlWriter = null
 * 
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SnapShotProvider {
	public static enum Type {
		INSTANCE_PER_MONITOR,	// A new provider will be instantiated for each monitor (DEFAULT)
		STATIC,		// The SnapShotManager will access static methods on the class.
		FACTORY,	// The SnapShotManager will invoke the static method getInstance() OR getInstance(String instanceName) ;
	}
	Type type() default Type.INSTANCE_PER_MONITOR;
}

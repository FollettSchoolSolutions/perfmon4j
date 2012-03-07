/*
 *	Copyright 2008-2012 Follett Software Company 
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

package org.perfmon4j.instrument.javassist;

import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

/**
 *  Javassist (Copyright (C) 1999-2007 Shigeru Chiba. All Rights Reserved) made a change to the algorithm they
 *  use to calculate the default serialVersionUID.  This change introduced a defect in the when running under the
 *  Sun JVM (version 1.6.0_16) where the serialVersionUID of an instrumented class would not match the serialVersionUID
 *  of it's non-instrumented counterpart.
 *  
 *  This class attempts to calculate the serialVersionUID of a serialized class with both the old (svn revision 382,
 *  and the latest (as of 3/7/2012, svn revision 584).  The algorithm that produce the correct SerialVersionUID is used
 *  by perfmon4j to generate all future serialVersionUID classes when a Serializable class is instrumented (and the class does 
 *  not contain a explicitly defined serialVersionUID.
 *  
 *  If neither algorithm produces the correct serialVersionUID then perfmon4j will refuse to instrument any Serializable classes
 *  without an explicit SerialVersionUID.
 *  
 *  You can also force perfmon4j to skip instrumentation of Serializable classes that do not contain a explicit serialVersionUID
 *  by setting the system property  
 *  
 * 
 * @author ddeucher
 *
 */

public class SerialVersionUIDHelper {
	private final static Logger logger = LoggerFactory.initLogger(SerialVersionUIDHelper.class); 
	private final static Logger verboseLogger = LoggerFactory.getVerboseInstrumentationLogger(); 
	private boolean use382 = false;
	private boolean use584 = false;
	public static final String REQUIRE_EXPLICIT_SERIAL_VERSION_UID = "Perfmon4j.RequireExplicitSerialVersionUID";
	
	public SerialVersionUIDHelper() {
		if (isSkipGenerationOfSerialVersionUID()) {
			logger.logInfo("Found System Property \"Perfmon4j.RequireExplicitSerialVersionUID=TRUE\", " +
					"Perfmon4j will NOT instrument Serializable classes unless they declare " +
					"an explicit SerialVersionUID.");
        } else {
	    	logger.logInfo("Perfmon4j will instrument ALL Serializable classes, including those that do not declare " +
	    			"an explicit SerialVersionUID. To disable add \"-D" + REQUIRE_EXPLICIT_SERIAL_VERSION_UID + "=true\" to your command line.");
        }
		
		try {
	        long id = ObjectStreamClass.lookup(SerialVersionUIDHelper.TestSerializationGenerator.class).getSerialVersionUID();
	        CtClass clazz = ClassPool.getDefault().get(SerialVersionUIDHelper.TestSerializationGenerator.class.getName());
	
	        if (SerialVersionUID382.calculateDefault(clazz) == id) {
	        	use382 = true;
	        	verboseLogger.logInfo("*** Perfmon4j will use SerialVersionUID382 to calculate default serialVersionUID");
	        } else if (SerialVersionUID584.calculateDefault(clazz) == id) {
	        	use584 = true;
	        	verboseLogger.logInfo("*** Perfmon4j will use SerialVersionUID584 to calculate default serialVersionUID");
	        } else {
	        	if (!isSkipGenerationOfSerialVersionUID()) {
					logger.logWarn("Unable to determine algorithm to calculate default serialVersionUID, " +
							"Perfmon4j will NOT instrument Serializable classes unless they declare " +
							"an explicit SerialVersionUID.");
	        	}
	        }
		} catch (Exception ex) {
			if (!isSkipGenerationOfSerialVersionUID()) {
				logger.logError("Unable to determine correct defaultSerialUID algorithim", ex);
			}
		}
	}
	
	public boolean isSkipGenerationOfSerialVersionUID() {
		return Boolean.getBoolean(REQUIRE_EXPLICIT_SERIAL_VERSION_UID);
	}

	public boolean requiresSerialVesionUID(CtClass clazz) throws NotFoundException {
	    // check for pre-existing field.
	    try {
	        clazz.getDeclaredField("serialVersionUID");
	        return false;
	    }
	    catch (NotFoundException e) {}
	    return SerialVersionUID382.isSerializable(clazz);
	}
	
	public boolean setSerialVersionUID(CtClass clazz) throws CannotCompileException {
		Long serialVersionUID = null;
		
		if (use382) {
			serialVersionUID = Long.valueOf(SerialVersionUID382.calculateDefault(clazz));
		} else if (use584) {
			serialVersionUID = Long.valueOf(SerialVersionUID584.calculateDefault(clazz));
		} 
		
		if (serialVersionUID != null) {
			CtField field = new CtField(CtClass.longType, "serialVersionUID", clazz);
			field.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
			clazz.addField(field, serialVersionUID.toString() + "L");			
			return true;
		} else {
			return false;
		}
	}
	
	@SuppressWarnings("serial")
	private static class TestSerializationGenerator implements Serializable {
		@SuppressWarnings("unused")
		private int x;
		protected long y;
		public double z;
		
		public TestSerializationGenerator() {
		}
		
		public void doX() {
			doY();
		}
		
		private void doY() {
		}
	}
}

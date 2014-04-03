/*
 *	Copyright 2008,2009 Follett Software Company 
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

package org.perfmon4j.instrument;

import java.io.IOException;
import java.io.InputStream;

import org.perfmon4j.instrument.PerfMonTimerTransformerTest.GlobalClassLoaderTester;

/**
 * BogusClassLoader used by test PerfMonTimerTransformerTest.testGlobalClassLoaderUsesWeakReferences
 * The only purpose of this classloader is to bypass any parent classloader and instantiate a 
 * class. 
 */
class BogusClassLoader extends ClassLoader {
	private final byte[] classBytes = new byte[1000000];
	private final int numBytes;
	
	public BogusClassLoader() throws IOException {
		super(GlobalClassLoaderTester.class.getClassLoader());
		
		InputStream in = super.getResourceAsStream("org/perfmon4j/instrument/TestClass.class");
		numBytes = in.read(classBytes);
		in.close();
	}
	
	public Class loadClass(String className) throws ClassNotFoundException {
		if (className.endsWith("TestClass")) {
			return super.defineClass(className, classBytes, 0, numBytes);
		} else {
			return super.loadClass(className);
		}
	}
}


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

package org.perfmon4j.util;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * The Global ClassLoader will contain a weak reference
 * to all classloaders found by the PerfMon4j Java Agent.
 * This enables perfmon4j to be initialized by the system
 * boot class loader.  Loading any dependent classes, like 
 * monitors and loggers can be defered until the associated class
 * loader is created...
 * 
 */
public class GlobalClassLoader extends ClassLoader {
    private static final GlobalClassLoader classLoader;
    
    static {
    	classLoader = new GlobalClassLoader();
    }
    
    
    private final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    private long totalClassLoaders = 0; // This is a count of the total number of classloaders we have encountered
    private long loadRequestCount = 0; 	// Total number of classes we where asked to load.
    private long loadAttemptsCount = 0;		// The total number of attempts made to load classes (traversing our nested class loaders)
    
    Map<ClassLoader, Object> loaders = new WeakHashMap<ClassLoader, Object>();
    
    public static GlobalClassLoader getClassLoader() {
    	
    	return classLoader;
    }
    
    private GlobalClassLoader() {
        super();
    	loaders.put(this, this);
    }
    
    private boolean inCoreClassLoader = true;

    public Class<?> loadClass(String className) throws ClassNotFoundException {
    	loadRequestCount++;
    	return super.loadClass(className);
    }

    public Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
    	loadRequestCount++;
    	return super.loadClass(className, resolve);
    }
    
    protected Class<?> findClass(String name) throws ClassNotFoundException {
    	Class<?> result = null;
        try {
            result = super.findClass(name);
        } catch (ClassNotFoundException nfe) {
        	Object a[];
            LOCK.readLock().lock();
            try {
            	Set set = loaders.entrySet();
            	a = set.toArray();
            } finally {
            	LOCK.readLock().unlock();
            }
            for (int i = 0; (i < a.length) && (result == null); i++) {
            	Map.Entry entry = (Map.Entry)a[i];
            	ClassLoader loader = (ClassLoader)entry.getKey();
            	if (this != loader) {
            		result = loadClassNoThrow(loader, name);
            		loadAttemptsCount++;
            	}
			}
            if (result == null) {
                throw nfe;
            }
        }
        return result;
    }

    private static Class loadClassNoThrow(ClassLoader loader, String name) {
        Class result = null;
        if (loader != null) {
            try {
                result = loader.loadClass(name);
            } catch (ClassNotFoundException nfe) {
                // Nothing todo...
            }
        }
        return result;
    }
    
    public void addClassLoader(ClassLoader loader) {
//System.err.println("GlobalClassLoader - Waiting for WriteLock");
    	LOCK.writeLock().lock();
//System.err.println("GlobalClassLoader - Waiting for WriteLock");
        try {
            if (!loaders.containsKey(loader)) {
            	if (inCoreClassLoader) {
                	String className =  loader.getClass().getName();
            		inCoreClassLoader = className.startsWith("sun.misc.Launcher");
            	}
            	totalClassLoaders++;
//            	System.err.println("ClassLoader(" + totalClassLoaders + "): " 
//            			+  loader.getClass().getName()
//            			+ " toString: " + loader.toString()
//            			+ " hashCode: " + loader.hashCode()
//            			+ " inCoreClassLoader: " + inCoreClassLoader);
                loaders.put(loader,null);
            }
        } finally {
            LOCK.writeLock().unlock();
//System.err.println("GlobalClassLoader - Released WriteLock");
        }
    }

    /**
     * Represents the total number of classloaders we have encountered...
     * Since the classLoaders are held this value my be greater than the
     * current number of classloaders.
     * @return
     */
	public long getTotalClassLoaders() {
		return totalClassLoaders;
	}

	public long getCurrentClassLoaders() {
		long result = 0;
		try {
			LOCK.readLock().lock();
			result = loaders.size() - 1; // This list includes the GlobalClassLoader..  Do not include it in the count. 
		} finally {
			LOCK.readLock().unlock();
		}
		return result;
	}
	
	public long getLoadAttemptsCount() {
		return loadAttemptsCount;
	}
	
	public long getLoadRequestCount() {
		return loadRequestCount;
	}
	
	/**
	 * returns true until we have moved beyond one of the following root class loaders:
	 * 		System ClassLoader
	 * 		sun.misc.Launcher$ExtClassLoader
	 * 		sun.misc.Launcher$AppClassLoader	
	 */
	public boolean isInCoreClassLoader() {
		return inCoreClassLoader;
	}
	
}

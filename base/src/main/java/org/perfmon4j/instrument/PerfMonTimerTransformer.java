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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import org.perfmon4j.PerfMon;
import org.perfmon4j.XMLConfigurator;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class PerfMonTimerTransformer implements ClassFileTransformer {
    private final TransformerParams params; 
    private final static Logger logger = LoggerFactory.initLogger(PerfMonTimerTransformer.class);
    
    private PerfMonTimerTransformer(String paramsString) {
        params = new TransformerParams(paramsString);
    }
    
    private static ThreadLocal<RecursionPreventor> recursionPreventor = new ThreadLocal() {
        protected synchronized RecursionPreventor initialValue() {
            return new RecursionPreventor();
        }
    };    
    
    private static class RecursionPreventor {
    	boolean threadInScope = false;
    }
    
    private static boolean allowedClass(String className) {
        boolean result = !className.startsWith("["); // Skip all array classes...
        
        // Perfmon classes should not be instrumented --- except for demo classes and Test Classes
        if (result && className.startsWith("org/perfmon4j")) {
            result = className.startsWith("org/perfmon4j/demo") 
            	|| className.endsWith("Test.class")
            	|| className.endsWith("Test");
        }
        
        logger.logDebug(className + " allowed: " + result);        
        
        return result;
    }
    
        
    public byte[] transform(ClassLoader loader, String className, 
        Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
        byte[] classfileBuffer) {
        byte[] result = null;
        long startMillis = -1;
        
        if (recursionPreventor.get().threadInScope) {
        	InstrumentationMonitor.incRecursionSkipCount();
        } else {
            // !!!! IMPORTANT !!!! DO NOT DO ANYTHING HERE BEFORE BEFORE
        	// YOU set recursionPreventor.get().threadInScope = true
        	// IF YOU DO, YOU ARE RISKING RECURSION!
        	try {
        		recursionPreventor.get().threadInScope = true;
        		
        		if (classBeingRedefined != null && !params.isBootStrapInstrumentationEnabled()) {
        			// Only redefine classes if bootstrap implementation IS enabled....
        			// TODO need an instrumentation count for this!
        			return result;
        		}
        		
        		InstrumentationMonitor.incCurrentInstThreads();
                if (loader != null) {
                    GlobalClassLoader.getClassLoader().addClassLoader(loader);
                }
        		if (allowedClass(className)) {
	                startMillis = MiscHelper.currentTimeWithMilliResolution();
	                logger.logDebug("Loading class: " + className);
	                
	                if (params.getTransformMode(className.replace('/', '.')) != TransformerParams.MODE_NONE) {
	                    ClassPool classPool = null;
	                    if (loader == null) {
	                        classPool = new ClassPool(true);
	                    } else {
	                        classPool = new ClassPool(false);
	                        classPool.appendClassPath(new LoaderClassPath(loader));
	                    }
	                    
	                    ByteArrayInputStream inStream = new ByteArrayInputStream(classfileBuffer);
	                    CtClass clazz = classPool.makeClass(inStream);
	                    if (clazz.isFrozen()) {
	                        clazz.defrost();
	                    }
	                    
	                    int count = RuntimeTimerInjector.injectPerfMonTimers(clazz, classBeingRedefined != null, params);
	                    if (count > 0) {
	                        if (classBeingRedefined != null) {
	                        	InstrumentationMonitor.incBootstrapClassesInst();
	                            logger.logInfo("Inserting timer into bootstrap class: " + className);
	                        }
	                    	InstrumentationMonitor.incClassesInst();
	                    	InstrumentationMonitor.incMethodsInst(count);
	                        result = clazz.toBytecode();
	                        logger.logDebug(count + " timers inserted into class: " + className);
	                    }
	                } // if transformMode != TransformerParams.MODE_NONE 
	        	} // if (allowedClass())
            } catch (Throwable ex) {
            	InstrumentationMonitor.incClassInstFailures();
                final String msg = "Unable to inject PerfMonTimers into class: " + className;
                if (logger.isDebugEnabled()) {
                    logger.logDebug("Unable to inject PerfMonTimers into class: " + className, ex);
                } else {
                    logger.logInfo(msg + " Throwable: " + ex.getMessage());
                }
            } finally {
        		InstrumentationMonitor.decCurrentInstThreads();
            	recursionPreventor.get().threadInScope = false;
            }
            if (startMillis > 0) {
            	InstrumentationMonitor.incInstrumentationMillis(MiscHelper.currentTimeWithMilliResolution() - startMillis);
            }
        }

        return result;
    }
    
    public static void premain(String packageName,  Instrumentation inst)  {
        logger.logInfo("Perfmon4j Instrumentation Agent v." + PerfMonTimerTransformer.class.getPackage().getImplementationVersion() + " installed. (http://perfmon4j.org)");

        String javassistVersion = javassist.CtClass.class.getPackage().getSpecificationVersion();
        if (javassistVersion != null) {
        	logger.logInfo("Perfmon4j found Javassist bytcode instrumentation library version: " + javassistVersion);
        }
        
        PerfMonTimerTransformer t = new PerfMonTimerTransformer(packageName);

        LoggerFactory.setDefaultDebugEnbled(t.params.isDebugEnabled());
        LoggerFactory.setVerboseInstrumentationEnabled(t.params.isVerboseInstrumentationEnabled());
        
        inst.addTransformer(t);
        
        logger.logInfo("Perfmon4j transformer paramsString: " + (packageName == null ? "" : packageName));
        if (t.params.isExtremeInstrumentationEnabled() && !t.params.isVerboseInstrumentationEnabled()) {
        	logger.logInfo("Perfmon4j verbose instrumentation logging disabled.  Add -vtrue to javaAgent parameters to enable.");
        }
        
        // Check for all the preloaded classes and try to instrument any that might
        // match our perfmon4j javaagent configuration
        if (!t.params.isBootStrapInstrumentationEnabled()) {
        	logger.logInfo("Perfmon4j bootstrap implementation disabled.  Add -btrue to javaAgent parameters to enable.");
        } else {
	        Class loadedClasses[] = inst.getAllLoadedClasses();
	        List<ClassDefinition> redefineList = new ArrayList<ClassDefinition>(loadedClasses.length);
	        for (int i = 0; i < loadedClasses.length; i++) {
	            Class clazz = loadedClasses[i];
	            logger.logDebug("Found preloaded class: " + clazz.getName());
	            
	            String resourceName = clazz.getName().replace('.', '/') + ".class";
	            if (allowedClass(resourceName) && t.params.getTransformMode(clazz) != TransformerParams.MODE_NONE) {
	                logger.logInfo("Perfmon4j trying to instrument preloaded class: " + clazz.getName());
	                try {
	                    ClassLoader loader = clazz.getClassLoader();
	                    if (loader == null) {
	                        loader = ClassLoader.getSystemClassLoader();
	                    }
	                    InputStream stream = loader.getResourceAsStream(resourceName);
	                    if (stream == null) {
	                        logger.logError("Unable to load bytes for resourcename: " + resourceName 
	                            + " from loader: " + loader.toString());
	                    } else {
	                        ByteArrayOutputStream o = new ByteArrayOutputStream();
	                        int c = 0;
	                        while ((c = stream.read()) != -1) {
	                            o.write(c);
	                        }
	                        ClassDefinition def = new ClassDefinition(clazz, o.toByteArray()); 
	                        redefineList.add(def);
	                    }
	                } catch (Exception ex) {
	                    String  msg = "Error retrieving class definition for class: " + clazz.getName();
	                    if (logger.isDebugEnabled()) {
	                        logger.logError(msg, ex);
	                    } else {
	                        logger.logError(msg + " Exception: " + ex.getMessage());
	                    }
	                }
	            }
	        }
	        
	        if (redefineList.size() > 0) {
	            int numClasses = redefineList.size();
	            boolean jvmSupports = inst.isRedefineClassesSupported();
	            if (!jvmSupports) {
	                logger.logError("JVM Does not support redefineClass. " +
	                		"Unable to instrument " + numClasses + " preloaded classes");
	            } else {
	                try {
	                    // Reserve space for the PerfMon references for each of the
	                    // preloaded classes...  We need to do this, since we can not add static
	                    // data members for the preloaded classes..
	                    RuntimeTimerInjector.monitorsForRedefinedClasses = new PerfMon[numClasses][];
	                    logger.logInfo("Starting to instrument " + numClasses + " preloaded classes");
	                    inst.redefineClasses(redefineList.toArray(new ClassDefinition[]{}));
	                    logger.logInfo("Instrumented " + numClasses + " preloaded classes");
	                } catch (UnsupportedOperationException ex) {
	                    logger.logWarn("JVM UnsupportedOperactionException in redefineClass. " +
	                            "Unable to instrument " + numClasses + " preloaded classes. Ex: " + ex.getMessage());
	                } catch (Throwable th) {
	                    String msg = "Unexpected error trying to instrument " + numClasses + 
	                            " preloaded classes";
	                    if (logger.isDebugEnabled()) {
	                        logger.logError(msg, th);
	                    } else {
	                        logger.logError(msg + " Throwable: " + th.getMessage());
	                    }
	                }
	            }
	        }
        }
        
        String xmlFileToConfig = t.params.getXmlFileToConfig();
        if (xmlFileToConfig != null) {
        	int reloadConfigSeconds = t.params.getReloadConfigSeconds();
        	
        	File xmlFile = new File(xmlFileToConfig);
        	if (xmlFile.exists()) {
        		logger.logInfo("Loading perfmon configuration from file: " + getDisplayablePath(xmlFile));
        	} else {
        		if (reloadConfigSeconds == 0) {
        			logger.logInfo("Configuration file not found since -r parameter was 0 or less the file will NOT be checked for updates -- file: " + getDisplayablePath(xmlFile));
        		} else {
        			logger.logInfo("Configuration file not found will check again in " + reloadConfigSeconds + " seconds -- file: " + getDisplayablePath(xmlFile));
        		}
        	}
            XMLConfigurator.configure(new File(xmlFileToConfig), reloadConfigSeconds);
        }
    }
    
    private static String getDisplayablePath(File file) {
    	String result = file.getAbsolutePath();
    	
    	try {
			result = file.getCanonicalPath();
		} catch (IOException e) {
			// Nothing todo...  Just return the absolute path
		}
    	
    	return result;
    }
    
}


/*
 *	Copyright 2008-2011 Follett Software Company 
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import org.perfmon4j.BootConfiguration;
import org.perfmon4j.PerfMon;
import org.perfmon4j.SQLTime;
import org.perfmon4j.XMLBootParser;
import org.perfmon4j.XMLConfigurator;
import org.perfmon4j.remotemanagement.RemoteImpl;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class PerfMonTimerTransformer implements ClassFileTransformer {
    private final TransformerParams params; 
    private final static Logger logger = LoggerFactory.initLogger(PerfMonTimerTransformer.class);
	
    private final static String REMOTE_INTERFACE_DELAY_SECONDS_PROPERTY="Perfmon4j.RemoteInterfaceDelaySeconds"; 
	private final static int REMOTE_INTERFACE_DEFAULT_DELAY_SECONDS=30;

    public final static String USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY="Perfmon4j.UseLegacyInstrumentationWrapper"; 
	public final static boolean USE_LEGACY_INSTRUMENTATION_WRAPPER=Boolean.getBoolean(USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY);
	
	/*
	 * This property is NOT used when legacy instrumentation wrapper is used.
	 * 
	 * When using the non-wrapper instrumentation, setting this flag to true will
	 * prevent the instrumentation agent from  generating dynamic classes to store
	 * the PerfMon monitors for each instrumented method.  The dynamic class is only required
	 * when instrumenting a serialized class that does not have a explicit 
	 * serial version ID.
	 * 
	 * If this system property is set to false, the monitors will have to be 
	 * retrieved on each method invocation, resulting in a modest performance penalty.
	 */
    public final static String DONT_CREATE_EXTERNAL_CLASS_ON_INSTRUMENTATION_PROPERTY  ="Perfmon4j.DontCreateExternalClassOnInstrumentation"; 
	public final static boolean DONT_CREATE_EXTERNAL_CLASS_ON_INSTRUMENTATION=Boolean.getBoolean(DONT_CREATE_EXTERNAL_CLASS_ON_INSTRUMENTATION_PROPERTY);

	
	private static ValveHookInserter valveHookInserter = null;
	
    private PerfMonTimerTransformer(String paramsString) {
        params = new TransformerParams(paramsString);
    }
    
    private static ThreadLocal<RecursionPreventor> recursionPreventor = new ThreadLocal<RecursionPreventor>() {
        protected synchronized RecursionPreventor initialValue() {
            return new RecursionPreventor();
        }
    };    
    
    public static boolean isThreadInInstrumentationPhase() {
    	return recursionPreventor.get().threadInScope;
    }
    
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
        		InstrumentationMonitor.incCurrentInstThreads();

        		recursionPreventor.get().threadInScope = true;
        		
        		if (classBeingRedefined != null && !params.isBootStrapInstrumentationEnabled()) {
        			// Only redefine classes if bootstrap implementation IS enabled....
        			// TODO need an instrumentation count for this!
        			return result;
        		}
        		
                if (loader != null) {
                    GlobalClassLoader.getClassLoader().addClassLoader(loader);
                }
        		if (allowedClass(className)) {
	                startMillis = MiscHelper.currentTimeWithMilliResolution();
	                logger.logDebug("Loading class: " + className);
	                
	                if ((params.getTransformMode(className.replace('/', '.')) != TransformerParams.MODE_NONE)  
	                		|| params.isPossibleJDBCDriver(className.replace('/', '.')) ) {
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
	                    
	                    int count = RuntimeTimerInjector.injectPerfMonTimers(clazz, classBeingRedefined != null, params, loader, protectionDomain);
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
                    logger.logInfo("Unable to inject PerfMonTimers into class: " + className, ex);
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

    public static ValveHookInserter getValveHookInserter() {
    	return valveHookInserter;
    }
    
    public static class ValveHookInserter implements ClassFileTransformer {
    	final private String undertowDeployInfoClassName = "io/undertow/servlet/api/DeploymentInfo";
    	final private String engineClassName =  System.getProperty("Perfmon4j.catalinaEngine", "org.apache.catalina.core.StandardEngine").replaceAll("\\.", "/");
    	final private String valveClassName =  System.getProperty("Perfmon4j.webValve");
    	final private BootConfiguration.ServletValveConfig valveConfig;
    	
    	// Undertow is the servlet engine used in JBoss Wildfly 8.x
    	private Object undertowHanlderWrapperSingleton = null;
    	private volatile boolean undertowWrapperInitialized = false;
    	
    	public ValveHookInserter(BootConfiguration.ServletValveConfig valveConfig) {
    		this.valveConfig = valveConfig;
    	}
    	
        public byte[] transform(ClassLoader loader, String className, 
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
                byte[] classfileBuffer) {
            byte[] result = null;
            
            if (engineClassName.equals(className)  || undertowDeployInfoClassName.equals(className)) {
	            try {
	            	ClassPool classPool;
		            
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
		            addSetValveHook(clazz);
		            result = clazz.toBytecode();
	            } catch (Exception ex) {
	            	logger.logError("Unable to insert addValveHook", ex);
	            }
            }
			return result;
		}
        
        
        private Object buildValve(String valveClassName, ClassLoader loader) {
        	Object result = null;

			try {
				Class<?> clazz = Class.forName(valveClassName, true, loader);
	    		result = clazz.newInstance();
			} catch (ClassNotFoundException e) {
				// Nothing todo
			} catch (InstantiationException e) {
				// Nothing todo
			} catch (IllegalAccessException e) {
				// Nothing todo
			}
        	
        	return result;
        }
        
        public void installValve(Object engine) {
        	try {
        		Object valve = null;
        		ClassLoader loader = engine.getClass().getClassLoader();
        		if (valveClassName != null) {
        			valve = buildValve(valveClassName, loader);
        			if (valve == null) {
            			logger.logError("Perfmon4j -- Unable to instantiate valve class: " + valveClassName);
            			return;
        			}
        		} else {
        			valve = buildValve("org.perfmon4j.extras.tomcat55.PerfMonValve", loader);
        			if (valve == null) {
        				valve = buildValve("org.perfmon4j.extras.tomcat7.PerfMonValve", loader);
        			}
        			if (valve == null) {
        				valve = buildValve("web.org.perfmon4j.extras.jbossweb7.PerfMonValve", loader);
        			}
        			if (valve == null) {
            			logger.logError("Perfmon4j -- Unable to instantiate tomcat55, tomcat7 OR jbossweb7 valve class");
            			return;
        			}
        		}

        		if (valveConfig != null) {
        			valveConfig.copyProperties(valve);
        		}
        		
        		Class<?> engineClass = engine.getClass();
        		Method addValve = null;
        		Method[] methods = engineClass.getMethods();
        		for (int i = 0 ; (i < methods.length) && (addValve == null); i++) {
        			if (methods[i].getName().equals("addValve")) {
        				addValve = methods[i];
        			}
        		}
        		if (addValve != null) {
        			addValve.invoke(engine, valve);
        			logger.logInfo("Perfmon4j valve installed in Catalina Engine Class: "  + engine.getClass().getName());
        		} else {
        			logger.logError("Perfmon4j -- Error installing valve in Catalina Engine Class: " + engine.getClass().getName() + " -- addValve method not found.");
        		}
        		
        	} catch (Exception ex) {
    			logger.logError("Perfmon4j -- Error installing valve in Catalina Engine Class: " + engine.getClass().getName(), ex);
        	}
        }
        
        public synchronized Object getUndertowHandlerWrapperSingleton(Object undertowClass) {
        	if (!undertowWrapperInitialized) {
        		try {
        			Class<?> clazz = Class.forName("web.org.perfmon4j.extras.wildfly8.PerfmonHandlerWrapper", true, undertowClass.getClass().getClassLoader());
        			Object wrapper = clazz.newInstance();
	        		if (valveConfig != null) {
	        			valveConfig.copyProperties(wrapper);
	        		}
	        		undertowHanlderWrapperSingleton = wrapper;
        		} catch (Exception ex) {
        			logger.logError("Perfmon4j -- Error installing Undertow Valve", ex);
        		}
        		undertowWrapperInitialized = true;
        	}
        	return undertowHanlderWrapperSingleton;
        }
        
        
        public void addSetValveHook(CtClass clazz) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        	if (clazz.getName().contains("undertow")) {
            	logger.logInfo("Perfmon4j found Undertow DeploymentInfo Class: " + clazz.getName());

            	// Undertow, which includes JBoss Wildfly, no longer has Valves.  We implement similar behavior by
            	// installing a HandlerWrapper into the innerHandlerChain.
            	CtMethod m = clazz.getDeclaredMethod("getInnerHandlerChainWrappers");
            	final String insertBlock = 
            			"synchronized (innerHandlerChainWrappers) {\r\n" +
            			"	io.undertow.server.HandlerWrapper wrapper = (io.undertow.server.HandlerWrapper)" + PerfMonTimerTransformer.class.getName() + ".getValveHookInserter().getUndertowHandlerWrapperSingleton(this);\r\n" +
            			"	if (wrapper != null && !innerHandlerChainWrappers.contains(wrapper)) {\r\n" +
        	    		"		innerHandlerChainWrappers.add(wrapper);\r\n" +
        	    		"	}\r\n" +
            			"}\r\n";            	
    			m.insertBefore(insertBlock);
        	} else {
            	logger.logInfo("Perfmon4j found Catalina Engine Class: " + clazz.getName());
            	
            	CtMethod m = clazz.getDeclaredMethod("setDefaultHost");
            	String insert = PerfMonTimerTransformer.class.getName() + ".getValveHookInserter().installValve(this);";
    			m.insertAfter(insert);
        	}
        }
    }

    
    private static class SystemGCDisabler implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className, 
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
                byte[] classfileBuffer) {
            byte[] result = null;
            
            if ("java/lang/System".equals(className)) {
	            try {
	            	ClassPool classPool;
		            
		            
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
		            RuntimeTimerInjector.disableSystemGC(clazz);
		            result = clazz.toBytecode();
		            logger.logInfo("Perfmon4j disabled System.gc()");
	            } catch (Exception ex) {
	            	logger.logError("Unable to disable System.gc()", ex);
	            }
            }
			
			return result;
		}
    }
    
    private static void addPerfmon4jToJBoss7SystemPackageList() {
    	// For the JBoss 7 package list, we must set include org.perfmon4j in the
    	// jboss.modules.system.pkgs system property...  If we are not 
    	// running on jboss7 this property will have no impact.
    	final String PROP_KEY = "jboss.modules.system.pkgs";
    	final String PERFMON_4J_PACKAGES = "org.perfmon4j";
    	
    	String existing = System.getProperty(PROP_KEY);
    	if (existing == null) {
    		System.setProperty(PROP_KEY, PERFMON_4J_PACKAGES);
    	} else {
    		System.setProperty(PROP_KEY, existing + "," + PERFMON_4J_PACKAGES);
    	}
    }
    
    public static void premain(String packageName,  Instrumentation inst)  {
    	addPerfmon4jToJBoss7SystemPackageList();
    	
    	final String hideBannerProperty="Perfmon4j.HideBanner";
    	
    	if (!Boolean.getBoolean(hideBannerProperty)) {
	    	logger.logInfo(" _____            __                      _  _   _ ");
	    	logger.logInfo("|  __ \\          / _|                    | || | (_)");
	    	logger.logInfo("| |__) |___ _ __| |_ _ __ ___   ___  _ __| || |_ _ ");
	    	logger.logInfo("|  ___// _ \\ '__|  _| '_ ` _ \\ / _ \\| '_ \\__   _| |");
	    	logger.logInfo("| |   |  __/ |  | | | | | | | | (_) | | | | | | | |");
	    	logger.logInfo("|_|    \\___|_|  |_| |_| |_| |_|\\___/|_| |_| |_| | |");
	    	logger.logInfo("                                               _/ |");
	    	logger.logInfo("                                              |__/ ");
	    	logger.logInfo("To hide banner add \"-D" + hideBannerProperty + "=true\" to your command line");
    	}
    	logger.logInfo("Perfmon4j Instrumentation Agent v." + PerfMonTimerTransformer.class.getPackage().getImplementationVersion() + " installed. (http://perfmon4j.org)");
    	
        String javassistVersion = javassist.CtClass.class.getPackage().getSpecificationVersion();
        if (javassistVersion != null) {
        	logger.logInfo("Perfmon4j found Javassist bytcode instrumentation library version: " + javassistVersion);
        }
    	logger.logInfo(MiscHelper.getHighResolutionTimerEnabledDisabledMessage());
    	
        PerfMonTimerTransformer t = new PerfMonTimerTransformer(packageName);

        LoggerFactory.setDefaultDebugEnbled(t.params.isDebugEnabled());
        LoggerFactory.setVerboseInstrumentationEnabled(t.params.isVerboseInstrumentationEnabled());
        
        logger.logInfo("Perfmon4j transformer paramsString: " + (packageName == null ? "" : packageName));
        if (t.params.isExtremeInstrumentationEnabled() && !t.params.isVerboseInstrumentationEnabled()) {
        	logger.logInfo("Perfmon4j verbose instrumentation logging disabled.  Add -vtrue to javaAgent parameters to enable.");
        }
        SystemGCDisabler disabler = null;
        
        if (t.params.isExtremeSQLMonitorEnabled()) {
        	SQLTime.setEnabled(true);
        	logger.logInfo("Perfmon4j SQL instrumentation enabled.");
        } else {
        	logger.logInfo("Perfmon4j SQL instrumentation disabled.  Add -eSQL to javaAgent parameters to enable.");
        }
        
        inst.addTransformer(t);
        if (t.params.isDisableSystemGC()) {
    		if (inst.isRedefineClassesSupported()) {
    			logger.logInfo("Perfmon4j is installing SystemGCDisabler agent");
    			disabler = new SystemGCDisabler();
	        	inst.addTransformer(disabler);
    		} else {
    			logger.logError("Perfmon4j can not disable java.lang.System.gc() JVM does not support redefining classes");
    		}
        }

        if (t.params.isInstallServletValve()) {
        	BootConfiguration.ServletValveConfig valveConfig = null;
        	String configFile = t.params.getXmlFileToConfig();
        	if (configFile != null) {
        		FileReader reader = null;
        		try {
        			reader = new FileReader(configFile);
					valveConfig = XMLBootParser.parseXML(reader).getServletValveConfig();
				} catch (FileNotFoundException e) {
					logger.logError("Perfmon4j unable to load boot configuration", e);
				} finally {
					if (reader != null) {
						try {reader.close();} catch (Exception ex) {}
					}
				}
        	}
        	valveHookInserter = new ValveHookInserter(valveConfig);
            inst.addTransformer(valveHookInserter);
        	logger.logInfo("Perfmon4j will attempt to install a Servlet Valve");
        } else {
        	logger.logInfo("Perfmon4j will NOT attempt to install a Servlet Valve.  If this is a tomcat or jbossweb based application, " +
        			"add -eVALVE to javaAgent parameters to enable.");
        }
        
        // Check for all the preloaded classes and try to instrument any that might
        // match our perfmon4j javaagent configuration
        if (!t.params.isBootStrapInstrumentationEnabled()) {
        	logger.logInfo("Perfmon4j bootstrap implementation disabled.  Add -btrue to javaAgent parameters to enable.");
        	if (disabler != null) {
        		try {
	        		Class<?> clazz = System.class;       		
	                ClassLoader loader = clazz.getClassLoader();
	                if (loader == null) {
	                    loader = ClassLoader.getSystemClassLoader();
	                }
	                String resourceName =  clazz.getName().replace('.', '/') + ".class";
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
	                    inst.redefineClasses(new ClassDefinition[]{def});
	                }
        		} catch (Exception ex) {
        			logger.logError("Perfmon4j failed disabling System.gc()", ex);
        		}
        	}
        } else {
	        Class<?> loadedClasses[] = inst.getAllLoadedClasses();
	        List<ClassDefinition> redefineList = new ArrayList<ClassDefinition>(loadedClasses.length);
	        for (int i = 0; i < loadedClasses.length; i++) {
	            Class<?> clazz = loadedClasses[i];
	            logger.logDebug("Found preloaded class: " + clazz.getName());
	            
	            String resourceName = clazz.getName().replace('.', '/') + ".class";
	            if (allowedClass(resourceName) 
	            		&& ((t.params.getTransformMode(clazz) != TransformerParams.MODE_NONE)
	            				|| (t.params.isDisableSystemGC() && resourceName.equals("java/lang/System.class")))) {
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
        
        if (disabler != null) {
        	inst.removeTransformer(disabler);
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
        
        if (t.params.isRemoteManagementEnabled()) {
        	int port = t.params.getRemoteManagementPort();
        	new LazyLoadRemoteListener(port).schedule(Integer.getInteger(REMOTE_INTERFACE_DELAY_SECONDS_PROPERTY, REMOTE_INTERFACE_DEFAULT_DELAY_SECONDS).intValue());
        }
    }
    
    private static class LazyLoadRemoteListener extends TimerTask {
    	final int port;
    	
    	LazyLoadRemoteListener(int port) {
    		this.port = port;
    	}
    	
    	public void schedule(int delaySeconds) {
    		String delayMessage = "To override delay duration add \"-D" + REMOTE_INTERFACE_DELAY_SECONDS_PROPERTY + "=<number of seconds>\" to your command line.";
    		if (delaySeconds > 0) {
    			PerfMon.utilityTimer.schedule(this, delaySeconds * 1000);
    			logger.logInfo("*** PerfMon4j remote management interface is scheduled to be instantiated in " + delaySeconds + " seconds. " 
    					+ delayMessage);
    		} else {
    			logger.logInfo("*** PerfMon4j remote management will be instantiated immediately. " 
    					+ delayMessage);
    			run();
    		}
    	}
    	
		@Override
		public void run() {
			/**
			 * For an application server, give services a chance to aquire their ports
			 * before we tie up a port for the remote monitor interface.
			 */
        	try {
        		RemoteImpl.registerRMIListener(port);
			} catch (RemoteException e) {
				if (port == TransformerParams.REMOTE_PORT_AUTO) {
					String range = RemoteImpl.AUTO_RMI_PORT_RANGE_START +
						" and " + RemoteImpl.AUTO_RMI_PORT_RANGE_END;
					logger.logError("Error starting management listener on an open port between " + range, e);
				} else {
					logger.logError("Error starting management listener on port: " + port, e);
				}
			}
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


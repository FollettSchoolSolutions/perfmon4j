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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.rmi.RemoteException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.perfmon4j.BootConfiguration;
import org.perfmon4j.BootConfiguration.ExceptionElement;
import org.perfmon4j.BootConfiguration.ExceptionTrackerConfig;
import org.perfmon4j.ExceptionTracker;
import org.perfmon4j.PerfMon;
import org.perfmon4j.SQLTime;
import org.perfmon4j.XMLBootParser;
import org.perfmon4j.XMLConfigurator;
import org.perfmon4j.instrument.jmx.JMXSnapShotProxyFactory;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.remotemanagement.RemoteImpl;
import org.perfmon4j.util.GlobalClassLoader;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;


public class PerfMonTimerTransformer implements ClassFileTransformer {
    private final TransformerParams params; 
    private static Logger logger = LoggerFactory.initLogger(PerfMonTimerTransformer.class);
	
    private final static String REMOTE_INTERFACE_DELAY_SECONDS_PROPERTY="Perfmon4j.RemoteInterfaceDelaySeconds"; 
	private final static int REMOTE_INTERFACE_DEFAULT_DELAY_SECONDS=30;

    public final static String USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY="Perfmon4j.UseLegacyInstrumentationWrapper"; 
	public final static boolean USE_LEGACY_INSTRUMENTATION_WRAPPER=Boolean.getBoolean(USE_LEGACY_INSTRUMENTATION_WRAPPER_PROPERTY);

    public final static String DISABLE_CLASS_INSTRUMENTATION_PROPERTY="Perfmon4j.DisableClassInstrumentation"; 
	public final static boolean DISABLE_CLASS_INSTRUMENTATION=Boolean.getBoolean(DISABLE_CLASS_INSTRUMENTATION_PROPERTY);
	
	
	
    // Redefined classes will store their timers in this array...
    public static PerfMon[][] monitorsForRedefinedClasses = null;
	
	
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

	public static final RuntimeTimerInjector runtimeTimerInjector;
	public static final SnapShotGenerator snapShotGenerator;
	public static final JMXSnapShotProxyFactory jmxSnapShotProxyFactory;
	private static ClassLoader javassistClassLoader = null;
	private static String javassistVersion = null;
	
	/**
	 * If perfmon4j is loaded with this property set to false it will ignore the javassist jar
	 * embedded within the perfmon4j JAR.  Instead it will look for a javassist.jar in 
	 * the same folder as the perfmon4j agent OR if that is not found it will attempt to
	 * load javassist from the classpath.
	 */
	private static final String PROPERTY_FORCE_EXTERNAL_JAVASSIST_JAR = "PERFMON4J_FORCE_EXTERNAL_JAVASSIST_JAR";
	
	static private class IsolateJavassistClassLoader extends URLClassLoader {
		public IsolateJavassistClassLoader(File perfmon4j, URL javassist) throws MalformedURLException {
			super(new URL[]{toURL(perfmon4j), javassist}, new ClassLoader() {
			});
		}
		
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			Class<?> result = null;
			
			boolean isJavassistClass = name.toLowerCase().contains("javassist");
			if (!isJavassistClass) {
				result = super.loadClass(name);
			} else {
				result = findLoadedClass(name);
				if (result == null) {
					try {
						result = findClass(name);
					} catch (ClassNotFoundException cnf) {
						result = super.loadClass(name);
					}
				}
			}
			return result;
		}
		
		private static URL toURL(File file) throws MalformedURLException {
			URI uri = file.toURI();
			return uri.toURL();
		}
		
		public String toString() {
			return "Perfmon4j JavassistClassLoader(" + super.toString() + ")";
		}
	}
	
	
	static {
		/**
		 * Do NOT use logger here.  Logger depends on PerfMonTimerTransformer class which is being initialized now.
		 */
		
		File perfmon4j = findPerfmon4jAgentFile();
		if (perfmon4j != null) {
			File agentInstallFolder = perfmon4j.getParentFile();
			File externalJavassistJar = null;
			URL embeddedJavassist = null;

			boolean useExternalJavassist = Boolean.getBoolean(PROPERTY_FORCE_EXTERNAL_JAVASSIST_JAR);
			
			if (!useExternalJavassist) {
				File tempJavassistJarFile = null;
				// Check to see if we can access and load javassist classes from the embedded javassist.jar.
				embeddedJavassist = Thread.currentThread().getContextClassLoader().getResource("lib/javassist.jar");
				if (embeddedJavassist != null) {
					InputStream in = null;
					OutputStream out = null;
					
					try {
						tempJavassistJarFile = File.createTempFile("javassist.", ".jar");
						tempJavassistJarFile.deleteOnExit();
						out = new FileOutputStream(tempJavassistJarFile);
						in = embeddedJavassist.openStream();
						byte[] buffer = new byte[5120];
						int byteCount;
						while ((byteCount = in.read(buffer)) != -1) {
						   out.write(buffer, 0, byteCount);
						}
						embeddedJavassist = tempJavassistJarFile.toURI().toURL();
					} catch (IOException ioex) {
						try { tempJavassistJarFile.delete(); } catch (Exception ex) {}
						tempJavassistJarFile = null;
					} finally {
						if (in != null) {
							try { in.close(); } catch (Exception ex) {}
						}
						if (out != null) {
							try { out.close(); } catch (Exception ex) {}
						}
					}
				}
				if (tempJavassistJarFile != null) {
					if (!canJavassistClassesBeLoadedFromEmbeddedJar(perfmon4j, embeddedJavassist)) {
						try {
							if (tempJavassistJarFile.exists()) {
								tempJavassistJarFile.delete(); 
							}
						} catch (Exception ex) {
							// Nothing todo..
						}
						tempJavassistJarFile = null;
						useExternalJavassist = true;
					}
				}
				
				if (tempJavassistJarFile != null) {
						System.out.println("Perfmon4j will use embedded javassist.jar copied to temporary file "
								+ "(" + tempJavassistJarFile.getAbsolutePath() + ")"
								+ ".  To use an external "
								+ "javassist.jar set system property \"" + PROPERTY_FORCE_EXTERNAL_JAVASSIST_JAR + "=true\"."); 
				} else {
					System.err.println("Perfmon4j is unabled to load classes from the embedded javassist.jar "
						+ "and will be forced to use the external javassist.jar." ); 
					embeddedJavassist = null;
					useExternalJavassist = true;
				}
			} else {
				System.out.println("Perfmon4j found system property: \"" + PROPERTY_FORCE_EXTERNAL_JAVASSIST_JAR + "=true\". " 
						+ "The embedded javassist.jar will not be used.");
			}
			
			if (useExternalJavassist) {
				externalJavassistJar = new File(agentInstallFolder, "javassist.jar");
			}
			
			if (!useExternalJavassist && embeddedJavassist == null) {
				// This should not happen unless someone modifies the shipped perfmon4j agent.
				System.err.println("Perfmon4j could not find embedded javassist.jar");
			} else if (useExternalJavassist && !externalJavassistJar.exists()) {
				System.err.println("Perfmon4j could not find javassist.jar based on agent location - " + externalJavassistJar.getPath());
			} else if (!perfmon4j.exists()) {
				System.err.println("Perfmon4j could not find perfmon4j.jar based on agent location - " + perfmon4j.getPath());
			} else {
				try {
					URL javassistURLToUse = useExternalJavassist ? externalJavassistJar.toURI().toURL() : embeddedJavassist;
					javassistClassLoader = new IsolateJavassistClassLoader(perfmon4j, javassistURLToUse);
				} catch (MalformedURLException e) {
					System.err.println("Perfmon4j is unable to create the javaasist classloader.");
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("Perfmon4j could not find perfmon4j agent on java command line.");
		}

		if (javassistClassLoader == null) {
			javassistClassLoader = Thread.currentThread().getContextClassLoader();
			System.err.println("Perfmon4j will attempt to use the default classloader to load javassist classes.");
		} else {
			System.err.println("Perfmon4j using isolated javassist classloader.");
		}
		
		try {
			Class<?> ctClass =  javassistClassLoader.loadClass("javassist.CtClass");
	        javassistVersion = ctClass.getPackage().getSpecificationVersion();
			
			snapShotGenerator = (SnapShotGenerator)javassistClassLoader.loadClass("org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator").newInstance();
			runtimeTimerInjector = (RuntimeTimerInjector)javassistClassLoader.loadClass("org.perfmon4j.instrument.JavassistRuntimeTimerInjector").newInstance();
			jmxSnapShotProxyFactory = (JMXSnapShotProxyFactory)javassistClassLoader.loadClass("org.perfmon4j.instrument.jmx.JavassistJMXSnapShotProxyFactory").newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Fatal exception", e);
		} 		
	}
	
	
    private PerfMonTimerTransformer(String paramsString) {
        params = new TransformerParams(paramsString);
    }
    
    
    private static boolean allowedClass(String className) {
        boolean result = !className.startsWith("["); // Skip all array classes...
        
        // Perfmon classes should not be instrumented --- except for demo classes and Test Classes
        if (result && className.startsWith("org/perfmon4j")) {
            result = className.startsWith("org/perfmon4j/demo") 
            	|| className.endsWith("Test.class")
            	|| className.endsWith("Test");
        }
        
        return result;
    }
    
        
    public byte[] transform(ClassLoader loader, String className, 
        Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
        byte[] classfileBuffer) {
    	Logger verboseLogger = LoggerFactory.getVerboseInstrumentationLogger();
    	
        byte[] result = null;
        long startMillis = -1;
        if (className != null) {
	        if (InstrumentationRecursionPreventor.isThreadInInstrumentation()) {
	        	InstrumentationMonitor.incRecursionSkipCount();
	        } else {
	            // !!!! IMPORTANT !!!! DO NOT DO ANYTHING HERE BEFORE BEFORE
	        	// YOU set recursionPreventor.get().threadInScope = true
	        	// IF YOU DO, YOU ARE RISKING RECURSION!
	        	try {
	        		InstrumentationMonitor.incCurrentInstThreads();
	
	        		InstrumentationRecursionPreventor.setThreadInInstrumentation(true);
	        		
	        		if (classBeingRedefined != null && !params.isBootStrapInstrumentationEnabled()) {
	        			// Only redefine classes if bootstrap implementation IS enabled....
	        			// TODO need an instrumentation count for this!
	        			return result;
	        		}
	        		
	                if (loader != null) {
	                    GlobalClassLoader.getClassLoader().addClassLoader(loader);
	                }
	                
	                boolean shouldConsiderTransforming = allowedClass(className) &&
	                		((params.getTransformMode(className.replace('/', '.')) != TransformerParams.MODE_NONE) 
	                			|| params.isPossibleJDBCDriver(className.replace('/', '.')));
	                
	        		if (shouldConsiderTransforming) {
	        			startMillis = MiscHelper.currentTimeWithMilliResolution();
	        			verboseLogger.logDebug("Loading class: " + className);
	                	RuntimeTimerInjector.TimerInjectionReturn timers = runtimeTimerInjector.injectPerfMonTimers(classfileBuffer, classBeingRedefined != null, params, loader, protectionDomain);

	                    int count = timers.getNumTimersAdded();
	                    if (count > 0) {
	                        if (classBeingRedefined != null) {
	                        	InstrumentationMonitor.incBootstrapClassesInst();
	                            verboseLogger.logDebug("Inserting timer into bootstrap class: " + className);
	                        }
	                    	InstrumentationMonitor.incClassesInst();
	                    	InstrumentationMonitor.incMethodsInst(count);
	                        result = timers.getClassBytes();
	                        verboseLogger.logDebug(count + " timers inserted into class: " + className);
		                    }
		        	} // if (shouldConsiderTransforming)
	            } catch (Throwable ex) {
	            	InstrumentationMonitor.incClassInstFailures();
	                final String msg = "Unable to inject PerfMonTimers into class: " + className;
	                if (logger.isDebugEnabled()) {
	                    logger.logInfo(msg, ex);
	                } else {
	                    logger.logInfo(msg + " Throwable: " + ex.getMessage());
	                }
	            } finally {
	        		InstrumentationMonitor.decCurrentInstThreads();
	        		InstrumentationRecursionPreventor.setThreadInInstrumentation(false);
	            }
	            if (startMillis > 0) {
	            	InstrumentationMonitor.incInstrumentationMillis(MiscHelper.currentTimeWithMilliResolution() - startMillis);
	            }
	        }
        }
        return result;
    }

    public static ValveHookInserter getValveHookInserter() {
    	return valveHookInserter;
    }
    
    public static class ValveHookInserter implements ClassFileTransformer {
    	final private String undertowDeployInfoClassName = "io/undertow/servlet/api/DeploymentInfo";
    	final private String tomcatRegistryClassName =  System.getProperty("Perfmon4j.tomcatRegistry", "org.apache.tomcat.util.modeler.Registry").replaceAll("\\.", "/");
    	
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
            
            boolean installValve = engineClassName.equals(className)  || undertowDeployInfoClassName.equals(className);
            boolean wrapTomcatRegistry = tomcatRegistryClassName.equals(className);
            boolean installRequestSkipLogTracker = "io/undertow/servlet/spec/HttpServletRequestImpl".equals(className);
            
        	try {
	            if (installValve) {
	            	result = runtimeTimerInjector.installUndertowOrTomcatSetValveHook(classfileBuffer, loader);
	            } else if (wrapTomcatRegistry){
	            	result = runtimeTimerInjector.wrapTomcatRegistry(classfileBuffer, loader);
	            } else if (installRequestSkipLogTracker) {
	            	result = runtimeTimerInjector.installRequestSkipLogTracker(classfileBuffer, loader);
	            }
            } catch (Exception ex) {
            	logger.logError(installValve ? "Unable to insert addValveHook" : "Unable to wrap tomcat registry", ex);
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
    }

    private static class ExceptionTrackerInstaller implements ClassFileTransformer {
    	private final ExceptionTrackerConfig config;
    	private final boolean verboseEnabled;
    	private final Set<String> exceptionClasses = new HashSet<String>();
    	
    	ExceptionTrackerInstaller(ExceptionTrackerConfig config, boolean verboseEnabled) {
    		this.config = config;
    		this.verboseEnabled = verboseEnabled;
    		for (ExceptionElement element : config.getElements()) {
    			exceptionClasses.add(element.getClassName().replaceAll("\\.", "/"));
    		}
    	}
    	
        public byte[] transform(ClassLoader loader, String className, 
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
                byte[] classfileBuffer) {
            byte[] result = null;
            
            if (exceptionClasses.contains(className)) {
	            try {
            		result = runtimeTimerInjector.instrumentExceptionOrErrorClass(className, classfileBuffer, loader, protectionDomain);
            		if (verboseEnabled) {
            			logger.logInfo("Added class to exceptionTracker: " + className);
            		}
	            } catch (Exception ex) {
	            	logger.logWarn("Perfmon4j was unable to add ExceptionTracker to Class: " + className, ex);
	            }
            }
			
			return result;
		}
    }

    private static class SystemGCDisabler implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className, 
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
                byte[] classfileBuffer) {
            byte[] result = null;
            
            if ("java/lang/System".equals(className)) {
	            try {
		            result = runtimeTimerInjector.disableSystemGC(classfileBuffer, loader);
		            logger.logInfo("Perfmon4j disabled System.gc()");
	            } catch (Exception ex) {
	            	logger.logError("Unable to disable System.gc()", ex);
	            }
            }
			
			return result;
		}
    }

    private static class PerfMom4JAPIInserter implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className, 
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
                byte[] classfileBuffer) {
            byte[] result = null;
            
            if ("api/org/perfmon4j/agent/PerfMon".equals(className)) {
	            try {
		            result = runtimeTimerInjector.attachAgentToPerfMonAPIClass(classfileBuffer, loader, protectionDomain);
		            logger.logInfo("Attached PerfMon API class to agent");
	            } catch (Exception ex) {
	            	logger.logError("Unable to attach PerfMon API class to agent", ex);
	            }
            } else if ("api/org/perfmon4j/agent/PerfMonTimer".equals(className)) {
	            try {
		            result = runtimeTimerInjector.attachAgentToPerfMonTimerAPIClass(classfileBuffer, loader, protectionDomain);
		            logger.logInfo("Attached PerfMonTimer API class to agent");
	            } catch (Exception ex) {
	            	logger.logError("Unable to attach PerfMonTimer API class to agent", ex);
	            }
	        } else if ("api/org/perfmon4j/agent/SQLTime".equals(className)) {
	            try {
		            result = runtimeTimerInjector.attachAgentToSQLTimeAPIClass(classfileBuffer, loader, protectionDomain);
		            logger.logInfo("Attached SQLTime API class to agent");
	            } catch (Exception ex) {
	            	logger.logError("Unable to attach SQLTime API class to agent", ex);
	            }
	        }

			return result;
		}
    }
    
    
    private static class HystrixHookInserter implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className, 
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, 
                byte[] classfileBuffer) {
            byte[] result = null;
            
            if ("com/netflix/hystrix/HystrixCommandMetrics".equals(className)) {
	            try {
		            result = runtimeTimerInjector.installHystrixCommandMetricsHook(classfileBuffer, loader, protectionDomain);
		            logger.logInfo("Injected monitor code into HystrixCommandMetrics");
	            } catch (Exception ex) {
	            	logger.logError("Unable to inject monitor code into HystrixCommandMetrics", ex);
	            }
            } else if ("com/netflix/hystrix/HystrixThreadPoolMetrics".equals(className)) {
	            try {
		            result = runtimeTimerInjector.installHystrixThreadPoolMetricsHook(classfileBuffer, loader, protectionDomain);
		            logger.logInfo("Injected monitor code into HystrixThreadPoolMetrics");
	            } catch (Exception ex) {
	            	logger.logError("Unable to inject monitor code into HystrixCommandMetrics", ex);
	            }
            }

			return result;
		}
    }

    private static void redefineClass(Instrumentation inst, Class<?> clazz) throws IOException, ClassNotFoundException, UnmodifiableClassException {
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
    	try {
    		InstrumentationRecursionPreventor.setThreadInPremain(true);
    		doPremain(packageName, inst);
    	} finally {
    		InstrumentationRecursionPreventor.setThreadInPremain(false);
    	}
    }
    	
    private static void doPremain(String packageName,  Instrumentation inst)  {
    	addPerfmon4jToJBoss7SystemPackageList();
    	
    
    	
        PerfMonTimerTransformer t = new PerfMonTimerTransformer(packageName);

        LoggerFactory.setDefaultDebugEnbled(t.params.isDebugEnabled() || t.params.isVerboseInstrumentationEnabled());
        LoggerFactory.setVerboseInstrumentationEnabled(t.params.isVerboseInstrumentationEnabled());
        // Reset logger so debug and verbose flags are respected...
        logger = LoggerFactory.initLogger(PerfMonTimerTransformer.class);
    	
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
    	
        if (javassistVersion != null) {
        	logger.logInfo("Perfmon4j found Javassist bytcode instrumentation library version: " + javassistVersion);
        }
        
    	logger.logInfo(MiscHelper.getHighResolutionTimerEnabledDisabledMessage());
    	 
        logger.logInfo("Perfmon4j transformer paramsString: " + (packageName == null ? "" : packageName));
        if (t.params.isExtremeInstrumentationEnabled() && !t.params.isVerboseInstrumentationEnabled()) {
        	logger.logInfo("Perfmon4j verbose instrumentation logging disabled.  Add -vtrue to javaAgent parameters to enable.");
        }
        SystemGCDisabler disabler = null;
        
        if (DISABLE_CLASS_INSTRUMENTATION) {
        	logger.logWarn("!!** Found system property " + DISABLE_CLASS_INSTRUMENTATION_PROPERTY + "=true. Perfmon4j class instrumentation is disabled "
        			+ " -- Extreme, Annotation and SQL monitoring will not be implemented. **!");
        } else {
	        if (t.params.isExtremeSQLMonitorEnabled() ) {
	        	SQLTime.setEnabled(true);
	        	logger.logInfo("Perfmon4j SQL instrumentation enabled.");
	        } else {
	        	logger.logInfo("Perfmon4j SQL instrumentation disabled.  Add -eSQL to javaAgent parameters to enable.");
	        }
	        inst.addTransformer(t);
        }
        
        if (t.params.isDisableSystemGC()) {
    		if (inst.isRedefineClassesSupported()) {
    			logger.logInfo("Perfmon4j is installing SystemGCDisabler agent");
    			disabler = new SystemGCDisabler();
	        	inst.addTransformer(disabler);
    		} else {
    			logger.logError("Perfmon4j can not disable java.lang.System.gc() JVM does not support redefining classes");
    		}
        }

        BootConfiguration bootConfiguration = BootConfiguration.getDefault();
    	String configFile = t.params.getXmlFileToConfig();
    	if (configFile != null) {
    		FileReader reader = null;
    		try {
    			reader = new FileReader(configFile);
    			bootConfiguration = XMLBootParser.parseXML(reader);
			} catch (FileNotFoundException e) {
				logger.logError("Perfmon4j unable to load boot configuration -- using default", e);
			} finally {
				if (reader != null) {
					try {reader.close();} catch (Exception ex) {}
				}
			}
    	}
        
        if (t.params.isInstallServletValve()) {
        	BootConfiguration.ServletValveConfig valveConfig = bootConfiguration.getServletValveConfig();
        	valveHookInserter = new ValveHookInserter(valveConfig);
            inst.addTransformer(valveHookInserter);
        	logger.logInfo("Perfmon4j will attempt to install a Servlet Valve in Tomcat, JBoss or Wildfly Servers");
        	logger.logInfo("In JBoss and Wildfly servers the Valve will NOT be installed until a Web Application is deployed");
        } else {
        	logger.logInfo("Perfmon4j will NOT attempt to install a Servlet Valve.  If this is a tomcat or jbossweb based application, " +
        			"add -eVALVE to javaAgent parameters to enable.");
        }
        
        if (t.params.isHystrixInstrumentationEnabled()) {
        	inst.addTransformer(new HystrixHookInserter());
        	logger.logInfo("Perfmon4j will attempt to install instrumentation into Hystrix Commands and Thread Pools");
        } else {
        	logger.logInfo("Perfmon4j will NOT attempt to install instrumentation into Hystrix Commands and Thread Pools.  If this application uses Hystrix " +
        			"add -eHYSTRIX to javaAgent parameters to enable.");
        }
        
        
        if (!Boolean.getBoolean("PerfMon4J.IgnoreAgentAPIClasses")) {
        	inst.addTransformer(new PerfMom4JAPIInserter());
        	logger.logInfo("Perfmon4j will attempt to install instrumentation into Hystrix Commands and Thread Pools");
        }        
        
        // Check for all the preloaded classes and try to instrument any that might
        // match our perfmon4j javaagent configuration
        if (!t.params.isBootStrapInstrumentationEnabled() || DISABLE_CLASS_INSTRUMENTATION) {
        	if (!DISABLE_CLASS_INSTRUMENTATION) {
        		logger.logInfo("Perfmon4j bootstrap implementation disabled.  Add -btrue to javaAgent parameters to enable.");
        	}
        	if (disabler != null) {
        		try {
        			redefineClass(inst, System.class);
        		} catch (Exception ex) {
        			logger.logError("Perfmon4j failed disabling System.gc()", ex);
        		}
        	}
        } else {
	        Class<?> loadedClasses[] = inst.getAllLoadedClasses();
	        List<ClassDefinition> redefineList = new ArrayList<ClassDefinition>(loadedClasses.length);
	        for (int i = 0; i < loadedClasses.length; i++) {
	            Class<?> clazz = loadedClasses[i];
	            String resourceName = clazz.getName().replace('.', '/') + ".class";
	            if (allowedClass(resourceName) 
	            		&& ((t.params.getTransformMode(clazz) != TransformerParams.MODE_NONE)
	            				|| (t.params.isDisableSystemGC() && resourceName.equals("java/lang/System.class")))) {
	                logger.logInfo("Perfmon4j found preloaded class to instrument: " + clazz.getName());
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
	            } else {
		            logger.logDebug("Perfmon4j Found preloaded class (not a candidate for instrumentation based on javaagent params): " + clazz.getName());
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
	                	PerfMonTimerTransformer.monitorsForRedefinedClasses = new PerfMon[numClasses][];
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
        
        if (!bootConfiguration.isExceptionTrackerEnabled() && t.params.isExceptionTrackerEnabled()) {
        	ExceptionTrackerConfig  config = new ExceptionTrackerConfig();
        	config.addElement(new ExceptionElement("java.lang.Exception", "Java Exception"));
        	config.addElement(new ExceptionElement("java.lang.RuntimeException", "Java Runtime Exception"));
        	config.addElement(new ExceptionElement("java.lang.Error", "Java Error"));
        	bootConfiguration.setExceptionTrackerConfig(config);
        }

        if (bootConfiguration.isExceptionTrackerEnabled()) {
        	boolean trackerInitialized = false;
        	BootConfiguration.ExceptionTrackerConfig config = bootConfiguration.getExceptionTrackerConfig();
        	try {
	        	ExceptionTrackerInstaller installer = new ExceptionTrackerInstaller(config, t.params.isVerboseInstrumentationEnabled());
	        	inst.addTransformer(installer);
        		runtimeTimerInjector.createExceptionTrackerBridgeClass(Object.class.getClassLoader(), Object.class.getProtectionDomain());
        		trackerInitialized = ExceptionTracker.registerWithBridge(config);
        	} catch (Exception ex) {
        		logger.logError("Perfmon4j unable to initialize Exception Tracker", ex);
        	}
	        if (trackerInitialized && inst.isRedefineClassesSupported()) {
	        	// Check to see if any exception classes have already been loaded,
	        	// and instrument any we find.
	           	Set<String> classNames = new HashSet<String>();
	        	for (ExceptionElement element : config.getElements()) {
	        		classNames.add(element.getClassName());
	        	}
        		for (Class<?> clazz : inst.getAllLoadedClasses()) {
        			if (classNames.contains(clazz.getName())) {
        				try {
	        				redefineClass(inst, clazz);
	        				logger.logDebug("Redefined loaded class for Exception Tracker: " + clazz.getName());
        				} catch (Exception ex) {
	        				logger.logError("Perfmon4j unable to redefined loaded class for Exception Tracker: " + clazz.getName());
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

    
    /**
     * Look for the perfmon4j.jar javaagent and return the path of the file.
     * @return
     */
    private static File findPerfmon4jAgentFile() {
        final Pattern pattern = Pattern.compile("^\\-javaagent\\:(.*)(perfmon4j.*?\\.jar)", Pattern.CASE_INSENSITIVE);
    	File result = null;
    	
    	List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
    	for(int i = 0; i < inputArgs.size() && result == null; i++) {
    		String arg = inputArgs.get(i);
        	Matcher matcher = pattern.matcher(arg);
        	if (matcher.find()) {
        		String path = matcher.group(1);
        		if (path.trim().equals("")) {
        			path = ".";
        		}
        		result = new File(path,matcher.group(2));
        	}
    	}
    	
    	return result;
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
    
    private static boolean canJavassistClassesBeLoadedFromEmbeddedJar(File perfmon4j, URL url) {
    	/**
    	 * This method is called from the static class initializer... Do not 
    	 * use logger here!
    	 */
    	boolean result = false;
    	
    	if (url != null) {
	    	try {
		    	URLClassLoader tmpLoader = null;
		    	try {
		    		tmpLoader = new IsolateJavassistClassLoader(perfmon4j, url);
		    		tmpLoader.loadClass("javassist.CtClass");
		    		result = true;
		    	} finally {
		    		if (tmpLoader != null) {
		    			tmpLoader.close();
		    		}
		    	}
	    	} catch (Exception ex) {
	    		// Nothing todo.
	    	}
    	}
    	
    	return result;
    }
}


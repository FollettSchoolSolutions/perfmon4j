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
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.perfmon4j.ExceptionTracker;
import org.perfmon4j.hystrix.CommandStatsProvider;
import org.perfmon4j.hystrix.ThreadPoolStatsProvider;
import org.perfmon4j.instrument.javassist.SerialVersionUIDHelper;
import org.perfmon4j.instrument.tomcat.TomcatDataSourceRegistry;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;

public class JavassistRuntimeTimerInjector extends RuntimeTimerInjector {
    final private static String IMPL_METHOD_SUFFIX = "$Impl";
    private static int serialNumber = 0;
    final private static Logger logger = LoggerFactory.initLogger(JavassistRuntimeTimerInjector.class);

    private static final String PERFMON_API_CLASSNAME = "api.org.perfmon4j.agent.PerfMon";
    private static final String PERFMON_TIMER_API_CLASSNAME = "api.org.perfmon4j.agent.PerfMonTimer";
    private static final String PERFMON_SQL_TIME_API_CLASSNAME = "api.org.perfmon4j.agent.SQLTime";
    
    /* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#injectPerfMonTimers(javassist.CtClass, boolean)
	 */
    public int injectPerfMonTimers(CtClass clazz, boolean beingRedefined) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        return injectPerfMonTimers(clazz, beingRedefined, null);
    }
   
    private static class VerboseMessages {
    	private final String className;
    	private final boolean extremeTimer;
    	private List<String> annotationMessages = new ArrayList<String>();
    	private boolean outputGenericSQLMessageForClass = true;
    	
    	public VerboseMessages(String className, boolean extremeTimer) {
    		this.className = className;
    		this.extremeTimer = extremeTimer;
    	}
    	
    	public void addSkipMessage(String monitorName, String reason) {
    		annotationMessages.add("** Skipping extreme monitor (" + reason + "): " + monitorName);
    	}
    	
    	public void addExtremeMsg(String monitorName) {
    		annotationMessages.add("** Adding extreme monitor: " + monitorName);
    	}

    	public void addExtremeSQLMsg(String monitorName) {
    		if ("SQL".equals(monitorName)) {
    			if (outputGenericSQLMessageForClass) {
    				annotationMessages.add("** Adding extreme SQLmonitor(" + className + "): " + monitorName);
    			}
    			outputGenericSQLMessageForClass = false;
    		} else {
        		annotationMessages.add("** Adding extreme SQLmonitor: " + monitorName);
    		}
    	}

		public void addAnnotationMsg(String methodName, String timerKeyAnnotation) {
    		annotationMessages.add("** Adding annotation monitor ("  + timerKeyAnnotation + ") to method: " + methodName);
		}
    	
    	
		public void outputDetails() {
			Logger verbose = LoggerFactory.getVerboseInstrumentationLogger();
			verbose.logDebug("** Instrumenting class: " + className);
			
			Iterator<String> itr = annotationMessages.iterator();
			while (itr.hasNext()) {
				verbose.logDebug(itr.next());
			}
		}

		public boolean isExtremeTimer() {
			return extremeTimer;
		}
    }
 
    static Set<CtClass> getInterfaces(CtClass clazz) throws NotFoundException {
    	Set<CtClass> result = new HashSet<CtClass>();
    	
    	CtClass interfaces[] = clazz.getInterfaces();
    	for (int i = 0; i < interfaces.length; i++) {
    		result.add(interfaces[i]);
    		result.addAll(getInterfaces(interfaces[i]));
		}
    	
    	return result;
    }
    
    /* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#disableSystemGC(javassist.CtClass)
	 */
    public byte[] disableSystemGC(byte[] classfileBuffer, ClassLoader loader) throws Exception {
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
    	
    	CtMethod gcMethod = clazz.getDeclaredMethod("gc");
    	gcMethod.insertBefore("if (1==1) {return;}\r\n");
    	
    	return clazz.toBytecode();
    }

    public void createExceptionTrackerBridgeClass(ClassLoader loader, ProtectionDomain protectionDomain) throws Exception {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader().getParent();
        } 
        ClassPool classPool = new ClassPool(false);
        classPool.appendClassPath(new LoaderClassPath(loader));
        
        CtClass bridgeClass = classPool.makeClass(ExceptionTracker.BRIDGE_CLASS_NAME);
        bridgeClass.addField(CtField.make("private static java.util.function.Consumer exceptionConsumer = null;", bridgeClass));

        String incrementMethodSrc = 
        		"public static void notifyExceptionCreate(String className, Object exception) "
        		+ "{if (exceptionConsumer != null) {exceptionConsumer.accept(new java.util.AbstractMap.SimpleEntry(className, exception));}}\r\n";
        bridgeClass.addMethod(CtMethod.make(incrementMethodSrc, bridgeClass));
        
        String registerMethodSrc = 
        		"	public static void registerExceptionConsumer(java.util.function.Consumer newConsumer) {\r\n"
        		+ "		exceptionConsumer = newConsumer;\r\n"
        		+ "	}\r\n";
        bridgeClass.addMethod(CtMethod.make(registerMethodSrc, bridgeClass));

        bridgeClass.toClass(loader, protectionDomain);
        bridgeClass.detach();
    }
    
    private boolean isClassDescendedFrom(CtClass clazz, String className) throws NotFoundException {
    	while ((clazz = clazz.getSuperclass()) != null) {
    		if (clazz.getName().equals(className)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public byte[] instrumentExceptionOrErrorClass(String className, byte[] classfileBuffer, ClassLoader loader, 
    		ProtectionDomain protectionDomain) throws Exception {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader().getParent();
        } 
    
        ClassPool classPool = new ClassPool(false);
        classPool.appendClassPath(new LoaderClassPath(loader));
        
        ByteArrayInputStream inStream = new ByteArrayInputStream(classfileBuffer);
        CtClass clazz = classPool.makeClass(inStream);
        if (clazz.isFrozen()) {
            clazz.defrost();
        }    	
       
        if (!isClassDescendedFrom(clazz, "java.lang.Throwable")) {
        	logger.logWarn("Class " + className  + " is NOT derived from java.lang.Throwable and cannot be added to the Perfmon4j ExceptionTracker");
        	return null;
        } else {
	        final String methodBody =
	        	"\r\n{\r\n" +
	        	"\tClass clazzBridge = ClassLoader.getSystemClassLoader().loadClass(\"" + ExceptionTracker.BRIDGE_CLASS_NAME + "\");\r\n" +
	        	"\tjava.lang.reflect.Method m = clazzBridge.getDeclaredMethod(\"notifyExceptionCreate\", new Class[] {String.class, Object.class});\r\n" +
	        	"\tm.invoke(null, new Object[] {\"" + className.replaceAll("/", ".") + "\", this});\r\n" +
	    		"}\r\n";
//System.out.println(methodBody);        
	        
	        for (CtConstructor constructor : clazz.getDeclaredConstructors()) {
	        	constructor.insertAfter(methodBody);
	        }
	        
	    	return clazz.toBytecode();
        } 
    }
    
    private boolean classHaveSkipIndicator(CtClass clazz) {
    	boolean result = false;

    	try {
			CtField field = clazz.getDeclaredField(TransformerParams.PERFMON_SKIP_FIELD_NAME);
			if (field != null) {
				result = Modifier.isStatic(field.getModifiers());
			}
		} catch (NotFoundException e) {
			// Nothing to do..
		}
    	return result;
    }

    public TimerInjectionReturn injectPerfMonTimers(byte[] classfileBuffer, boolean beingRedefined, 
    		TransformerParams params, ClassLoader loader, ProtectionDomain protectionDomain) throws Exception {
    	
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
	    
	    int numTimers = injectPerfMonTimers(clazz, beingRedefined, params, loader, protectionDomain);
	    return new TimerInjectionReturn(numTimers, clazz.toBytecode());
    }
    
    
    /* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#injectPerfMonTimers(javassist.CtClass, boolean, org.perfmon4j.instrument.TransformerParams)
	 */
    @Deprecated // Always pass in the class loader to use.  This is important if we need to generate a dynamic class.
    public int injectPerfMonTimers(CtClass clazz, boolean beingRedefined, TransformerParams params) throws ClassNotFoundException, NotFoundException, CannotCompileException {
    	return injectPerfMonTimers(clazz, beingRedefined, params, clazz.getClass().getClassLoader(), null);
    }
    
	/* (non-Javadoc)
	 * @see org.perfmon4j.instrument.RuntimeTimerInjectorInterface#injectPerfMonTimers(javassist.CtClass, boolean, org.perfmon4j.instrument.TransformerParams, java.lang.ClassLoader, java.security.ProtectionDomain)
	 */
	public int injectPerfMonTimers(CtClass clazz, boolean beingRedefined, TransformerParams params, ClassLoader loader, 
			ProtectionDomain protectionDomain) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        int mode = TransformerParams.MODE_ANNOTATE;
        TransformerParams.TransformOptions transformOptions = TransformerParams.TransformOptions.DEFAULT;
        int numTimers = 0;
        List<PendingTimer> pendingTimers = new ArrayList<PendingTimer>();
        boolean extremeSQLClass = false;
        
        
        if (!clazz.isInterface()) {
            if (params != null) {
                mode = params.getTransformMode(clazz.getName());
                transformOptions = params.getTransformOptions(clazz.getName());
                extremeSQLClass = params.isPossibleJDBCDriver(clazz.getName()) && isExtremeSQLClass(params, clazz);
				if (extremeSQLClass) {
					if (logger.isInfoEnabled()) {
						String n = clazz.getName();
						String interfaces = "";
					
						CtClass intf[] = getInterfaces(clazz).toArray(new CtClass[]{});
						for (int i = 0; i < intf.length; i++) {
							if (i > 0) {
								interfaces += ", ";
							}
							interfaces += intf[i].getName();
						}
						InstrumentationMonitor.incSQLClassesInst();
						logger.logInfo("PerfMon4j found SQL/JDBC class: (" + n + ") With interfaces: " + interfaces);
					}
				}
            }

            if (mode != TransformerParams.MODE_NONE) {
            	if (classHaveSkipIndicator(clazz)) {
            		if (LoggerFactory.isVerboseInstrumentationEnabled()) {
            			Logger verbose = LoggerFactory.getVerboseInstrumentationLogger();
            			verbose.logDebug("** Skipping class (found " + TransformerParams.PERFMON_SKIP_FIELD_NAME  + " static field) : " + clazz.getName());
            		}
            		mode = TransformerParams.MODE_NONE;
            	}
            }
            
            if ((mode != TransformerParams.MODE_NONE) || extremeSQLClass) {
            	boolean mustMaintainSerialVersion = false;
            
             	SerialVersionUIDHelper serialVersionHelper = SerialVersionUIDHelper.getHelper();
                /**
                 * if we are using the legacy instrumentation method, make sure we do not change the serialVersionUID
                 * by inserting our instrumentation...
                 */
                if (!beingRedefined) {
                	if (serialVersionHelper.requiresSerialVesionUID(clazz)) {
                		
                		if (PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER) {
                    		if (serialVersionHelper.isSkipGenerationOfSerialVersionUID()) {
                                logger.logInfo("Skipping instrumentation of serializable class: " + clazz.getName() +
                                " because the class does not contain an explicit serialVesionUID.");
                                return 0;
                    		}
                			// If we are using the legacy instrumentation (which adds wrapper methods), we 
                			// just have to byte the bullet and have javassist attempt
                			// to calculate and insert a serialVersionUID 
                			serialVersionHelper.setSerialVersionUID(clazz);
                		} else {
                			// If we are using the new instrumentation 
                			// we will use a slower method, that will not alter the 
                			// serial version of the class.
                			mustMaintainSerialVersion = true;  // Do NOT alter the signature of the class (i.e add methods or member data),  
                		}
                	}
                }
                
                serialNumber++; // We have to ensure we have a unique name for each
                // method that will not match a member name in our parent!

                VerboseMessages verboseMessages = null;
                if (LoggerFactory.isVerboseInstrumentationEnabled()) {
                	verboseMessages = new VerboseMessages(clazz.getName(), mode == TransformerParams.MODE_EXTREME || mode == TransformerParams.MODE_BOTH);
                }
                
                
                int len = clazz.getDeclaredMethods().length;
                CtMethod methods[] = new CtMethod[len];
                System.arraycopy(clazz.getDeclaredMethods(), 0, methods, 0, len);    
                for (int i = 0; i < methods.length; i++) {
                    CtMethod method = methods[i];

                    if (!method.getName().endsWith(IMPL_METHOD_SUFFIX) 
                        && !Modifier.isAbstract(method.getModifiers())
                        && !Modifier.isNative(method.getModifiers())
                        && !Modifier.isStrict(method.getModifiers())
                        && !Modifier.isVolatile(method.getModifiers())
//                        && !Modifier.isTransient(method.getModifiers())
                        && methodHasBody(method)) {
                        String anValue = getRuntimeAnnotationValue(method);
                        String timerKeyAnnotation = null;
                        String timerKeyExtreme = null;
                        
                        if (anValue != null && 
                            ((mode == TransformerParams.MODE_BOTH) || (mode == TransformerParams.MODE_ANNOTATE))) {
                            timerKeyAnnotation = anValue;
                        } 
 
                        String originalMethodName = clazz.getName() + "." + method.getName();
                        
                        
                        if (((mode == TransformerParams.MODE_BOTH) || (mode == TransformerParams.MODE_EXTREME))
                            && !isGetterOrSetter(method, transformOptions, verboseMessages, originalMethodName)) {
                            timerKeyExtreme = originalMethodName;
                            
                            if (timerKeyExtreme.equals(timerKeyAnnotation)) {
                                timerKeyAnnotation = null;
                            } else if (timerKeyExtreme.startsWith(timerKeyAnnotation + ".")) {
                                timerKeyAnnotation = null;
                            }
                        }
                        
                        String extremeSQLKey = null;
                        if (extremeSQLClass) {
                        	extremeSQLKey = "SQL";  // Any method on a JDBC Class will be monitored...
                    		extremeSQLKey += "." + method.getName();
                        }
                        if ((timerKeyAnnotation != null) || (timerKeyExtreme != null) || (extremeSQLKey != null)) {
                            numTimers += timerKeyAnnotation != null ? 1 : 0;
                            numTimers += timerKeyExtreme != null ? 1 : 0;
                            numTimers += extremeSQLKey != null ? 1 : 0;
                            pendingTimers.add(new PendingTimer(originalMethodName, method, timerKeyAnnotation, timerKeyExtreme, extremeSQLKey));
                        }
                    }
                }
                
                if (numTimers > 0) {
                    logger.logDebug("Injecting timer into: " + clazz.getName());
                	
            		Class<?> externalClazzForMonitors = null;
                    Integer offsetInStaticMonitorArray = null;
                    if (!beingRedefined) {
                    	if (!mustMaintainSerialVersion) {
                            String timerArray = "static final private org.perfmon4j.PerfMon[] pm$MonitorArray" + 
    	                            " = new org.perfmon4j.PerfMon[" + numTimers + "];";
	                        CtField field = CtField.make(timerArray, clazz);
	                        clazz.addField(field);
                    	} else if (!PerfMonTimerTransformer.DONT_CREATE_EXTERNAL_CLASS_ON_INSTRUMENTATION){
                    		// If we need to maintain serial version, create another class that will
                    		// hold the PerfMon monitors for each method.
                    		
                    		// Note: This method is NOT used with the legacy instrumentation method,
                    		// since that will always alter the class with the wrapper methods.
                            String timerArray = "static final public org.perfmon4j.PerfMon[] pm$MonitorArray" + 
    	                            " = new org.perfmon4j.PerfMon[" + numTimers + "];";
                    		ClassPool pool = clazz.getClassPool();
                    		String className = clazz.getName() + "_P4J_" + Integer.toHexString(serialNumber++); 
                    		CtClass tmpClass = pool.makeClass(className);
                    		
	                        CtField field = CtField.make(timerArray, tmpClass);
	                        tmpClass.addField(field);
	                        externalClazzForMonitors = tmpClass.toClass(loader, protectionDomain);
                    	}
                    } else {
                        // When a class is being redefined we are unable to add any data members to it.
                        // Find an empty offset to store this classes timer array... If you can not find one
                        // we will be unable to add timers to the class...
                        if (PerfMonTimerTransformer.monitorsForRedefinedClasses != null) {
                            for (int i = 0; i < PerfMonTimerTransformer.monitorsForRedefinedClasses.length; i++) {
                                if (PerfMonTimerTransformer.monitorsForRedefinedClasses[i] == null) {
                                    offsetInStaticMonitorArray = new Integer(i);
                                    PerfMonTimerTransformer.monitorsForRedefinedClasses[i] = new org.perfmon4j.PerfMon[numTimers];
                                    break;
                                }
                            }
                        }
                        if (offsetInStaticMonitorArray == null) {
                            logger.logError("Unable to instrument redefined class: " + clazz.getName() +
                                " no available location for storing PerfMon array");
                            return 0;
                        }
                    }
                    
                    int offset = 0;
                    Iterator<PendingTimer> itr = pendingTimers.iterator();
                    while (itr.hasNext()) {
                        PendingTimer t = itr.next();
                        if (offsetInStaticMonitorArray != null) {
                            offset = insertPerfMonTimerIntoRedefinedClass(clazz, t.method, t.timerKeyAnnotation, 
                                t.timerKeyExtreme, offset, offsetInStaticMonitorArray);
                        } else {
                        	if (PerfMonTimerTransformer.USE_LEGACY_INSTRUMENTATION_WRAPPER) {
	                            offset = insertPerfMonTimerWithLegacyWrapper(clazz, t.method, t.timerKeyAnnotation, t.timerKeyExtreme, t.extremeSQLKey, offset);
                        	} else {
	                            offset = insertPerfMonTimer(clazz, t.method, t.timerKeyAnnotation, t.timerKeyExtreme, t.extremeSQLKey, offset, mustMaintainSerialVersion, externalClazzForMonitors);
                        	}
                        }
                        if (verboseMessages != null) {
                        	if (t.timerKeyAnnotation != null) {
                        		verboseMessages.addAnnotationMsg(t.originalMethodName, t.timerKeyAnnotation);
                        	}
                        	
                        	if (t.timerKeyExtreme != null) {
                        		verboseMessages.addExtremeMsg(t.timerKeyExtreme);
                        	}

                        	if (t.extremeSQLKey != null) {
                        		verboseMessages.addExtremeSQLMsg(t.extremeSQLKey);
                        	}
                        }
                    }
                }
                if (verboseMessages != null) {
                	verboseMessages.outputDetails();
                }
            }
        }
        return numTimers;
    }
	
	public boolean isExtremeSQLClass(TransformerParams params, CtClass clazz) {
		boolean result = false;
		if (params.isExtremeSQLMonitorEnabled() && params.isPossibleJDBCDriver(clazz.getName())) {
			try {
				CtClass interfaces[] = getInterfaces(clazz).toArray(new CtClass[]{});
				for (int i = 0; i < interfaces.length && !result; i++) {
					result = params.isExtremeSQLInterface(interfaces[i].getName());
				}
			} catch (NotFoundException ex) {
				// nothing todo...
			}
		}
		return result;
	}
	
    
    private static class PendingTimer {
    	final String originalMethodName;
        final CtMethod method;
        final String timerKeyAnnotation;
        final String timerKeyExtreme;
        final String extremeSQLKey;
       
        
        PendingTimer(String originalMethodName, CtMethod method, String timerKeyAnnotation, String timerKeyExtreme, String extremeSQLKey) {
        	this.originalMethodName = originalMethodName;
        	this.method = method;
            this.timerKeyAnnotation = timerKeyAnnotation;
            this.timerKeyExtreme = timerKeyExtreme;
            this.extremeSQLKey = extremeSQLKey;
        }
    }
    
    private boolean methodHasBody(CtMethod method) {
        return method.getMethodInfo().getCodeAttribute() != null;
    }
    
    private boolean methodStartsWith(String prefix, String value) {
        return value.startsWith(prefix) && !value.equals(prefix) 
            && Character.isUpperCase(value.charAt(prefix.length()));
    }
    
    
    private boolean isVoidReturnType(CtMethod method) {
    	boolean result = false;
    	try {
    		result = method.getReturnType().getName().equals("void");
    	} catch (NotFoundException nfe) {
    		// Nothing to do... If we can't load the class it must not be a void return type!
    	}
    	return result;
    }

    private int getNumParameters(CtMethod method) {
    	int result = -1; // Don't unable to determine...
    	try {
    		result = method.getParameterTypes().length;
    	} catch (NotFoundException nfe) {
    		// Unable to determine!
    	}
    	return result;
    }
    
    
    /**
     * Check to see if this method is a getter or setter.  The options can override if setters/getters are 
     * instrumented.
     * @param method
     * @param options
     * @return
     * @throws NotFoundException
     */
    private boolean isGetterOrSetter(CtMethod method, TransformerParams.TransformOptions options, 
    		VerboseMessages messages, String extremeMonitorName) {
        boolean result = false;
        String name = method.getName();
        
        
        if (!options.isInstrumentGetters() && (methodStartsWith("is", name) || methodStartsWith("get", name))) {
            if (getNumParameters(method) == 0 && 
                !isVoidReturnType(method)) {
                result = true;
                if (messages != null) {
                	messages.addSkipMessage(extremeMonitorName, "getter method");
                }
            }
        } else if (!options.isInstrumentSetters() && methodStartsWith("set", name)) {
            if (getNumParameters(method) == 1 && isVoidReturnType(method)) {
                result = true;
                if (messages != null) {
                	messages.addSkipMessage(extremeMonitorName, "setter method");
                }
            }
        }
        return result;
    }

    
    private String getRuntimeAnnotationValue(CtMethod method) throws ClassNotFoundException {
        String result = null;
        try {
	        Object annotations[] = method.getAnnotations();
	        for (int i = 0; (i < annotations.length) && (result == null); i++) {
            	Annotation an = (Annotation)annotations[i];
	            if (an instanceof DeclarePerfMonTimer) {
	                result = ((DeclarePerfMonTimer)an).value();
	            } else if ("api.org.perfmon4j.agent.instrument.DeclarePerfMonTimer".equals(an.annotationType().getName())) {
	            	 // This will find the annotation in the agent-api jar.
	            	Class<?> clazz = an.annotationType();
	            	Method m = clazz.getDeclaredMethod("value");
	            	if (m != null 
	            		&& String.class.equals(m.getReturnType())
	            		&& m.getParameterCount() == 0) {
	            		try {
	            			result = (String) m.invoke(annotations[i]);
	            		} catch (Exception ex) {
	            			// Ignore
	            		}
	            	}
	            }
	        }
        } catch (Exception ex) {
        	if (logger.isDebugEnabled()) {
        		logger.logDebug("Unable to determine if method: " + method.getLongName() + " has DeclarePerfMonTimer annotation.", ex);
        	}
        }
        return result;
    }
    
    private String buildCallToMethod(CtMethod method) throws NotFoundException {
        String type = method.getReturnType().getName();
        if ("void".equals(type)) {
            return method.getName() + "($$);\n";
        } else {
            return "return " + method.getName() + "($$);\n";
        }
    }
    
    @SuppressWarnings("unchecked")
	private void moveAnnotations(CtMethod src, CtMethod dest) throws CannotCompileException {
        // Move over all of the annotations....
        List sAttributes = src.getMethodInfo().getAttributes();
        List dAttributes = dest.getMethodInfo().getAttributes();
        
        for(int i = sAttributes.size(); i > 0; i--) {
            AttributeInfo attr = (AttributeInfo)sAttributes.get(i-1);
            if (attr instanceof AnnotationsAttribute || attr instanceof ParameterAnnotationsAttribute) {
                dAttributes.add(sAttributes.remove(i-1));
            }
        }
    }
    
    private int makeModifierPrivateFinal(int modifier) {
        return Modifier.setPrivate(modifier) | Modifier.FINAL;
    }
    
    private String buildMonitorJavaSource(int offset, String timerKey, String timerType, boolean mustMaintainSerialVersion, 
    		Class<?> externalClazzForMonitors) {
    	String result = null;
    
    	final boolean canNotStoreMonitors = mustMaintainSerialVersion && externalClazzForMonitors == null; 
   
    	if (canNotStoreMonitors) {
    		// Slower..  But it does not require modification of the serial version of the class.
    		result = "\tperfmon4j$ContainerBefore." + timerType + " = org.perfmon4j.PerfMonTimer.start(\"" + timerKey + "\");\n";
    	} else {
	    	String monitorArrayName = "pm$MonitorArray";
	    	 
	    	if (externalClazzForMonitors != null) {
	    		monitorArrayName = externalClazzForMonitors.getName() + "." + monitorArrayName;
	    		mustMaintainSerialVersion = false; 
	    	} 
	    	
	    	StringBuilder builder = new StringBuilder();
	        builder.append("\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
	    		.append("\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKey + "\");\n")
	    		.append("\t}\n")
	    		.append("\tperfmon4j$ContainerBefore." + timerType + " = org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]);\n");
	        result = builder.toString();
    	}
        
        return result;
    }
    
    private int insertPerfMonTimer(CtClass clazz, CtMethod method, String timerKeyAnnotation, 
            String timerKeyExtreme, String extremeSQLKey, int offset, boolean mustMaintainSerialVersion, Class<?> externalClazzForMonitors) throws NotFoundException {
        try {
	        // Create the body of the code for the new method...
	        StringBuilder before = new StringBuilder();
	        before.append("{\n");
	        before.append("\torg.perfmon4j.NoWrapTimerContainer perfmon4j$ContainerBefore = new org.perfmon4j.NoWrapTimerContainer();\n");
	        before.append("\t((org.perfmon4j.NoWrapTimerContainer.ArrayStack)org.perfmon4j.NoWrapTimerContainer.callStack.get()).push(perfmon4j$ContainerBefore);\n");
	        
	        if (timerKeyAnnotation != null) {
	        	before.append(buildMonitorJavaSource(offset, timerKeyAnnotation, "annotationTimer", mustMaintainSerialVersion, externalClazzForMonitors));
	            offset++;
	        }	        
	        if (timerKeyExtreme != null) {
	        	before.append(buildMonitorJavaSource(offset, timerKeyExtreme, "extremeTimer", mustMaintainSerialVersion, externalClazzForMonitors));
	            offset++;
	        }
	        if (extremeSQLKey != null) {
	        	before.append(buildMonitorJavaSource(offset, extremeSQLKey, "extremeSQLTimer", mustMaintainSerialVersion, externalClazzForMonitors))
	        		.append("\torg.perfmon4j.SQLTime.startTimerForThread();\n");
	            offset++;
	        }	        
	        
	        before.append("}\n");
	        
//System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");	        
//System.err.println(before);	        
//System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");	        
	        
	        method.insertBefore(before.toString());
	        
	        StringBuilder after = new StringBuilder();
	        after.append("{\n");
	        after.append("\torg.perfmon4j.NoWrapTimerContainer perfmon4j$ContainerFinally = ((org.perfmon4j.NoWrapTimerContainer.ArrayStack)org.perfmon4j.NoWrapTimerContainer.callStack.get()).pop();\n");
	        if (extremeSQLKey != null) {
	        	after.append("\torg.perfmon4j.PerfMonTimer.stop(perfmon4j$ContainerFinally.extremeSQLTimer);\n")
	        		.append("\torg.perfmon4j.SQLTime.stopTimerForThread();\n");
	        }
	        if (timerKeyExtreme != null) {
	        	after.append("\torg.perfmon4j.PerfMonTimer.stop(perfmon4j$ContainerFinally.extremeTimer);\n");
	        }
	        if (timerKeyAnnotation != null) {
	        	after.append("\torg.perfmon4j.PerfMonTimer.stop(perfmon4j$ContainerFinally.annotationTimer);\n");
	        }
	        after.append("}\n");

//System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");	        
//System.err.println(after);	        
//System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");	        
	        
	        method.insertAfter(after.toString(), true);
        } catch (Throwable th) {
        	String msg = "Error inserting timer into class: " + clazz.getName() 
	    	+ " method: " + method.getName();
        	if (logger.isDebugEnabled()) {
        		logger.logWarn(msg, th);
        	} else {
        		logger.logWarn(msg + " Throwable: " + th.getMessage());
        	}
        }
        return offset;
    }
    
    private int insertPerfMonTimerWithLegacyWrapper(CtClass clazz, CtMethod method, String timerKeyAnnotation, 
        String timerKeyExtreme, String extremeSQLKey, int offset) throws NotFoundException {
    	
        final String originalMethodName = method.getName();
        final int originalModifiers = method.getModifiers();
        boolean wrapperInserted = false;
        CtMethod wrapperMethod = null;
        
        String implMethod = originalMethodName + "$" + serialNumber + IMPL_METHOD_SUFFIX;
       
        // Rename the method to be timed...
        method.setName(implMethod);
        
        try {
	        // Now create the new method with the original name....
	        // This method will be a simple wrapper around the original method
	        // with the PerfMonTimer start/stop around it....
	        wrapperMethod = CtNewMethod.copy(method, originalMethodName, clazz, null);
	        
	        // Make the method that we are going to wrap final
	        // and set to a private scope..
	        method.setModifiers(makeModifierPrivateFinal(originalModifiers));
	        
	        // If the method was synchronized... We dont want the wrapper method
	        // to be synchronized.   Remove the synchronized
	        // flag from our wrapper, so our timing will include any
	        // time used to obtain the synchronization token...
	        wrapperMethod.setModifiers(Modifier.clear(wrapperMethod.getModifiers(), Modifier.SYNCHRONIZED));
	        
	        // Create the body of the code for the new method...
	        StringBuffer body = new StringBuffer();
	        body.append("{\n");
	        String monitorArrayName = "pm$MonitorArray";
	        
	        if (timerKeyAnnotation != null) {
	            body.append("\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
	                .append("\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKeyAnnotation + "\");\n")
	                .append("\t}\n")
	                .append("\torg.perfmon4j.PerfMonTimer pm$TimerAnnotation = org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]);\n");
	            offset++;
	        }
	        if (timerKeyExtreme != null) {
	            body.append("\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
	                .append("\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKeyExtreme + "\");\n")
	                .append("\t}\n")
	                .append("\torg.perfmon4j.PerfMonTimer pm$TimerExtreme = org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]);\n");
	            offset++;
	        }
	        if (extremeSQLKey != null) {
	            body.append("\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
	                .append("\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + extremeSQLKey + "\");\n")
	                .append("\t}\n")
	                .append("\torg.perfmon4j.PerfMonTimer pm$TimerExtremeSQL = org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]);\n")
                	.append("\torg.perfmon4j.SQLTime.startTimerForThread();\n");
	            offset++;
	        }
	        body.append("\ttry {\n");
	        
	        body.append("\t\t" + buildCallToMethod(method));
	        
	        body.append("\t} finally {\n");
	        if (timerKeyExtreme != null) {
	            body.append("\t\torg.perfmon4j.PerfMonTimer.stop(pm$TimerExtreme);\n");
	        }
	        if (timerKeyAnnotation != null) {
	            body.append("\t\torg.perfmon4j.PerfMonTimer.stop(pm$TimerAnnotation);\n");
	        }
	        if (extremeSQLKey != null) {
	            body.append("\t\torg.perfmon4j.PerfMonTimer.stop(pm$TimerExtremeSQL);\n")
            		.append("\torg.perfmon4j.SQLTime.stopTimerForThread();\n");
	        }
	        body.append("\t}\n")
	            .append("}");
	        
	        
	        // Add the body to our wrapperMethod...
	        wrapperMethod.setBody(body.toString());
	        
	        // Insert the method into the class...
	        clazz.addMethod(wrapperMethod);
	        wrapperInserted = true;

	        moveAnnotations(method, wrapperMethod);
        } catch (Throwable th) {
        	String msg = "Error inserting timer into class: " + clazz.getName() 
	    	+ " method: " + originalMethodName;
        	// If something fails we must put the class back the way it was.
        	if (wrapperInserted) {
        		clazz.removeMethod(wrapperMethod);
        	}
        	method.setName(originalMethodName);
        	method.setModifiers(originalModifiers);
        	if (logger.isDebugEnabled()) {
        		logger.logWarn(msg, th);
        	} else {
        		logger.logWarn(msg + " Throwable: " + th.getMessage());
        	}
        }
        return offset;
    }

    
    private int insertPerfMonTimerIntoRedefinedClass(CtClass clazz, CtMethod method, String timerKeyAnnotation, 
        String timerKeyExtreme, int offset, Integer offsetInStaticMonitorArray)
        throws CannotCompileException {
        
        // Create the code to insert at the start of the method
        StringBuffer methodPrefix = new StringBuffer();
        methodPrefix.append("{\n");
        methodPrefix.append("\tif (!org.perfmon4j.instrument.PerfMonTimerTransformer.runtimeTimerInjector.isThreadInTimer()) {\n");
        methodPrefix.append("\t\ttry {\n");
        methodPrefix.append("\t\t\torg.perfmon4j.instrument.PerfMonTimerTransformer.runtimeTimerInjector.signalThreadInTimer();\n");
        
        String monitorArrayName  = PerfMonTimerTransformer.class.getName() + ".monitorsForRedefinedClasses["
            + offsetInStaticMonitorArray.intValue() + "]";
        
        if (timerKeyAnnotation != null) {
            methodPrefix.append("\t\t\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
                .append("\t\t\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKeyAnnotation + "\");\n")
                .append("\t\t\t}\n")
                .append("\t\t\torg.perfmon4j.instrument.PushTimerForBootClass.pushTimer(org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]));\n");
            offset++;
        }
        if (timerKeyExtreme != null) {
            methodPrefix.append("\t\t\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
                .append("\t\t\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKeyExtreme + "\");\n")
                .append("\t\t\t}\n")
                .append("\t\t\torg.perfmon4j.instrument.PushTimerForBootClass.pushTimer(org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]));\n");
            offset++;
        }
        methodPrefix.append("\t\t} finally {\n");
        methodPrefix.append("\t\t\torg.perfmon4j.instrument.PerfMonTimerTransformer.runtimeTimerInjector.releaseThreadInTimer();\n");
        methodPrefix.append("\t\t}\n");
        methodPrefix.append("\t}\n");
        methodPrefix.append("}\n");
        
        StringBuffer methodSuffix = new StringBuffer();
        methodSuffix.append("{\n");
        methodSuffix.append("\tif (!org.perfmon4j.instrument.PerfMonTimerTransformer.runtimeTimerInjector.isThreadInTimer()) {\n");
        methodSuffix.append("\t\ttry {\n");
        methodSuffix.append("\t\t\torg.perfmon4j.instrument.PerfMonTimerTransformer.runtimeTimerInjector.signalThreadInTimer();\n");
        
        if (timerKeyExtreme != null) {
            methodSuffix.append("\t\t\torg.perfmon4j.PerfMonTimer.stop(org.perfmon4j.instrument.PushTimerForBootClass.popTimer());\n");
        }
        if (timerKeyAnnotation != null) {
            methodSuffix.append("\t\t\torg.perfmon4j.PerfMonTimer.stop(org.perfmon4j.instrument.PushTimerForBootClass.popTimer());\n");
        }
        
        methodSuffix.append("\t\t} finally {\n");
        methodSuffix.append("\t\t\torg.perfmon4j.instrument.PerfMonTimerTransformer.runtimeTimerInjector.releaseThreadInTimer();\n");
        methodSuffix.append("\t\t}\n");
        methodSuffix.append("\t}\n");
        
        methodSuffix.append("}\n");
        
        if (logger.isDebugEnabled()) {
            logger.logDebug("Inserting Prefix:\n" + methodPrefix.toString());
            logger.logDebug("Inserting Suffix:\n" + methodSuffix.toString());
        }
        
        boolean insertBeforeWorked = false;
        try {
            method.insertBefore(methodPrefix.toString());
            insertBeforeWorked = true;
        } catch (Exception ex) {
            logger.logError("Unable to insert timer into method: " + method.getName(), ex);
        }
        
        // Important... If the insertBefore works we want to throw out
        // an exception if the insertAfter fails.  If we didnt the insert would
        // not be balance with a remove in the finally
        if (insertBeforeWorked) {
            method.insertAfter(methodSuffix.toString(), true);
        }
        
        return offset;
    }
    
    public byte[] installRequestSkipLogTracker(byte[] classfileBuffer, ClassLoader loader) throws Exception {
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

    	logger.logInfo("Perfmon4j found Undertow HttpServletRequestImpl Class: " + clazz.getName());

    	CtMethod m = clazz.getDeclaredMethod("setAttribute");
    	String insertBlock = 
    			" web.org.perfmon4j.extras.wildfly8.RequestSkipLogTracker.getTracker().setAttribute($1, $2);\r\n";
		m.insertBefore(insertBlock);

    	m = clazz.getDeclaredMethod("removeAttribute");
    	insertBlock = 
    			" web.org.perfmon4j.extras.wildfly8.RequestSkipLogTracker.getTracker().removeAttribute($1);\r\n";
		m.insertBefore(insertBlock);
		
    	return clazz.toBytecode();
    }
    
    public byte[] installUndertowOrTomcatSetValveHook(byte[] classfileBuffer, ClassLoader loader) throws Exception {
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
    	
    	return clazz.toBytecode();
    }
    
    public byte[] wrapTomcatRegistry(byte[] classfileBuffer, ClassLoader loader) throws Exception {
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
    		
        	CtClass objectClazz = classPool.getCtClass(Object.class.getName());
        	CtClass objectNameClazz = classPool.getCtClass(ObjectName.class.getName());
           	CtClass stringClazz = classPool.getCtClass(String.class.getName());
           	CtMethod methodRegisterComponent = clazz.getDeclaredMethod("registerComponent", new CtClass[]{objectClazz, objectNameClazz, stringClazz});
           	
           
           	final String codeToInsert = 
	           	"{" +
		        "  	if (($1 != null) && ($1 instanceof javax.sql.DataSource) && ($2 != null)) {" +
		        "  		" + TomcatDataSourceRegistry.class.getName() + ".registerDataSource($2, (javax.sql.DataSource)$1);" +
		        "  	}" +
	           	"}";
           	
           	methodRegisterComponent.insertAfter(codeToInsert);
           	logger.logInfo("Perfmon4j found TomcatRegistry and installed Global DataSource registry");
           	return clazz.toBytecode();
    }

	@Override
	public byte[] installHystrixCommandMetricsHook(byte[] classfileBuffer, ClassLoader loader, ProtectionDomain protectionDomain) throws Exception {
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

        /* Add the static field to the main class.  This is required because
         * we will only hold a WeakReference to this provider. This ensures that we will not prevent
         * this class from being unloaded
         * */
        CtField field = CtField.make("private static final org.perfmon4j.hystrix.CommandStatsProvider p4jStatsProvider;", clazz);
        clazz.addField(field);

        
        /**
         * Dynamically create the class that will implement the org.perfmon4j.hystrix.CommandStatsProvider
         */
    	CtClass implementationClass = classPool.makeClass("generated.org.perfmon4j.hystrix.Perfmon4jCommandStatsProvider");
    	implementationClass.addInterface(classPool.getCtClass(CommandStatsProvider.class.getName()));
    
    	/**
    	 * Create the collectStats method.
    	 */
    	String src = "public void collectStats(org.perfmon4j.hystrix.CommandStatsAccumulator accumulator) {\r\n"
        		+ "\tjava.util.Collection set = com.netflix.hystrix.HystrixCommandMetrics.getInstances();\r\n"
        		+ "\tjava.util.Iterator itr = set.iterator();\r\n"
        		+ "\twhile(itr.hasNext()) {\r\n"
        		+ "\t\tcom.netflix.hystrix.HystrixCommandMetrics metrics = (com.netflix.hystrix.HystrixCommandMetrics)itr.next();\r\n"
        		+ "\t\tjava.lang.String context = metrics.getCommandKey().name();\r\n"
        		+ "\t\tcom.netflix.hystrix.HystrixCommandGroupKey groupKey = metrics.getCommandGroup();\r\n"
        		+ "\t\torg.perfmon4j.hystrix.CommandStats.Builder builder = org.perfmon4j.hystrix.CommandStats.builder();\r\n"
        		+ "\t\tbuilder.setSuccessCount(metrics.getCumulativeCount(com.netflix.hystrix.HystrixEventType.SUCCESS));\r\n"
        		+ "\t\tbuilder.setFailureCount(metrics.getCumulativeCount(com.netflix.hystrix.HystrixEventType.FAILURE));\r\n"
        		+ "\t\tbuilder.setTimeoutCount(metrics.getCumulativeCount(com.netflix.hystrix.HystrixEventType.TIMEOUT));\r\n"
        		+ "\t\tbuilder.setShortCircuitedCount(metrics.getCumulativeCount(com.netflix.hystrix.HystrixEventType.SHORT_CIRCUITED));\r\n"
        		+ "\t\tbuilder.setThreadPoolRejectedCount(metrics.getCumulativeCount(com.netflix.hystrix.HystrixEventType.THREAD_POOL_REJECTED));\r\n"
        		+ "\t\tbuilder.setSemaphoreRejectedCount(metrics.getCumulativeCount(com.netflix.hystrix.HystrixEventType.SEMAPHORE_REJECTED));\r\n"
        		+ "\t\taccumulator.increment(context, builder.build());\r\n"
        		+ "\t\tif (groupKey != null) {\r\n"
        		+ "\t\t\taccumulator.increment(\"GROUP:\" + groupKey.name(), builder.setGroupStat(true).build());\r\n"
        		+ "\t\t}\r\n"
        		+ "\t}\r\n"
        		+ "}";
    	
//System.out.println("******************\r\n" + src + "\r\n******************");    	
    	CtMethod collectStats = CtMethod.make(src, implementationClass);
    	implementationClass.addMethod(collectStats);
    	
    	classPool.toClass(implementationClass, loader, protectionDomain);
    	

        /**
         *  Make class Initializer (Or append to it if it already exists)
         */
        CtConstructor staticInitializer = clazz.makeClassInitializer();
        staticInitializer.insertAfter("com.netflix.hystrix.HystrixCommandMetrics.p4jStatsProvider = "
        		+ "new generated.org.perfmon4j.hystrix.Perfmon4jCommandStatsProvider();\r\n"
        		+ "org.perfmon4j.hystrix.CommandStatsRegistry.getRegistry().registerProvider(p4jStatsProvider);");
    	
    	
    	return clazz.toBytecode();
	}


	@Override
	public byte[] installHystrixThreadPoolMetricsHook(byte[] classfileBuffer, ClassLoader loader, ProtectionDomain protectionDomain) throws Exception {
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

        /* Add the static field to the main class.  This is required because
         * we will only hold a WeakReference to this provider. This ensures that we will not prevent
         * this class from being unloaded
         * */
        CtField field = CtField.make("private static final org.perfmon4j.hystrix.ThreadPoolStatsProvider p4jStatsProvider;", clazz);
        clazz.addField(field);

        
        /**
         * Dynamically create the class that will implement the org.perfmon4j.hystrix.CommandStatsProvider
         */
    	CtClass implementationClass = classPool.makeClass("generated.org.perfmon4j.hystrix.Perfmon4jThreadPoolStatsProvider");
    	implementationClass.addInterface(classPool.getCtClass(ThreadPoolStatsProvider.class.getName()));
    
		String srcToLongMethod = "private long toLong(Number number) {\r\n" 
			+ "\tif (number == null) {\r\r"
			+ "\t\treturn 0l;\r\n" 
			+ "\t} else {\r\n"
			+ "\t\treturn number.longValue();\r\n"
			+  "\t}\r\n"
		 	+ "}";        		
    	CtMethod toLongMethod = CtMethod.make(srcToLongMethod, implementationClass);
    	implementationClass.addMethod(toLongMethod);
    	
    	/**
    	 * Create the collectStats method.
    	 */
    	String src = "public void collectStats(org.perfmon4j.hystrix.ThreadPoolStatsAccumulator accumulator) {\r\n"
        		+ "\tjava.util.Collection set = com.netflix.hystrix.HystrixThreadPoolMetrics.getInstances();\r\n"
        		+ "\tjava.util.Iterator itr = set.iterator();\r\n"
        		+ "\twhile(itr.hasNext()) {\r\n"
        		+ "\t\tcom.netflix.hystrix.HystrixThreadPoolMetrics metrics = (com.netflix.hystrix.HystrixThreadPoolMetrics)itr.next();\r\n"
        		+ "\t\tjava.lang.String context = metrics.getThreadPoolKey().name();\r\n"
        		+ "\t\torg.perfmon4j.hystrix.ThreadPoolStats.Builder builder = org.perfmon4j.hystrix.ThreadPoolStats.builder();\r\n"
        		+ "\t\tbuilder.setExecutedThreadCount(metrics.getCumulativeCountThreadsExecuted());\r\n"
        		+ "\t\tbuilder.setRejectedThreadCount(metrics.getCumulativeCountThreadsRejected());\r\n"
        		+ "\t\tbuilder.setCompletedTaskCount(toLong(metrics.getCurrentCompletedTaskCount()));\r\n"
        		+ "\t\tbuilder.setScheduledTaskCount(toLong(metrics.getCurrentTaskCount()));\r\n"
        		+ "\t\tbuilder.setMaxActiveThreads(metrics.getRollingMaxActiveThreads());\r\n"
        		+ "\t\tbuilder.setCurrentQueueSize(toLong(metrics.getCurrentQueueSize()));\r\n"
        		+ "\t\tbuilder.setCurrentPoolSize(toLong(metrics.getCurrentPoolSize()));\r\n"
        		+ "\t\taccumulator.increment(context, builder.build());\r\n"
        		+ "\t}\r\n"
        		+ "}";
    	
//System.out.println("******************\r\n" + src + "\r\n******************");    	
    	CtMethod collectStats = CtMethod.make(src, implementationClass);
    	implementationClass.addMethod(collectStats);
    	
    	classPool.toClass(implementationClass, loader, protectionDomain);

        /**
         *  Make class Initializer (Or append to it if it already exists)
         */
        CtConstructor staticInitializer = clazz.makeClassInitializer();
        staticInitializer.insertAfter("com.netflix.hystrix.HystrixThreadPoolMetrics.p4jStatsProvider = "
        		+ "new generated.org.perfmon4j.hystrix.Perfmon4jThreadPoolStatsProvider();\r\n"
        		+ "org.perfmon4j.hystrix.ThreadPoolStatsRegistry.getRegistry().registerProvider(p4jStatsProvider);");
    	
    	
    	return clazz.toBytecode();
	}
	
	@Override
	/**
	 * Unit tests for this code can be found in: org.perfmon4j.instrument.PerfMonAgentAPITest
	 */
	public byte[] attachAgentToPerfMonAPIClass(byte[] classfileBuffer, ClassLoader loader,
			ProtectionDomain protectionDomain) throws Exception {
    	ClassPool classPool = getClassPool(loader);
    	CtClass clazz = getClazz(classPool, classfileBuffer);

    	logger.logInfo("Instrumenting agent class " + PERFMON_API_CLASSNAME + ". " );
    	
        updateIsAttachedToAgent(clazz);
        
        clazz.addInterface(classPool.getCtClass(PerfMonAgentApiWrapper.class.getName()));
        clazz.addField(CtField.make("private org.perfmon4j.PerfMon nativeObject = null;", clazz));
        
        String src =
        		"private PerfMon(org.perfmon4j.PerfMon nativeObject) {\r\n"
        		+ "	this.nativeObject = nativeObject;\r\n"
        		+ " this.name = nativeObject.getName();\r\n"
        		+ "}";
        clazz.addConstructor(CtNewConstructor.make(src, clazz));
        
//		public static PerfMon getMonitor(String key, boolean isDynamicPath);
        src = "{"
        		+ "org.perfmon4j.PerfMon p = org.perfmon4j.PerfMon.getMonitor($1, $2);\r\n"
        		+ "return new  api.org.perfmon4j.agent.PerfMon(p);"
        		+ "}";
        replaceMethodIfExists(clazz, "getMonitor", src, String.class.getName(), CtClass.booleanType.getName());
        

//    	public static boolean isConfigured();
        src = "{\r\n"
        		+ " return org.perfmon4j.PerfMon.isConfigured();\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "isConfigured", src);
        

//    	public static void moveReactiveContextToCurrentThread(String contextID) {
        src = "{\r\n"
        		+ " org.perfmon4j.reactive.ReactiveContextManager.getContextManagerForThread().moveContext($1);\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "moveReactiveContextToCurrentThread", src, String.class.getName());

//    	public static void dissociateReactiveContextFromCurrentThread(String contextID) {
        src = "{\r\n"
        		+ " org.perfmon4j.reactive.ReactiveContextManager.getContextManagerForThread().dissociateContextFromThread($1);\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "dissociateReactiveContextFromCurrentThread", src, String.class.getName());
        
        
//      public boolean isActive();
        src = "{\r\n"
        		+ " return nativeObject.isActive();\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "isActive", src);

        
        src = "public org.perfmon4j.PerfMon getNativeObject() {\r\n" 
        		+ " return nativeObject;\r\n"
        		+ "}";
        clazz.addMethod(CtMethod.make(src, clazz));
        
    	logger.logDebug("Completed instrumenting agent class " + PERFMON_API_CLASSNAME + ". " );
        
        return clazz.toBytecode();
	}

	@Override
	/**
	 * Unit tests for this code can be found in: org.perfmon4j.instrument.PerfMonAgentAPITest
	 */
	public byte[] attachAgentToPerfMonTimerAPIClass(byte[] classfileBuffer, ClassLoader loader,
			ProtectionDomain protectionDomain) throws Exception {
		ClassPool classPool = getClassPool(loader);
    	CtClass clazz = getClazz(classPool, classfileBuffer);

   	
    	logger.logInfo("Instrumenting agent class " + PERFMON_TIMER_API_CLASSNAME + ". " );
    	
    	
//      public static boolean isAttachedToAgent();
        updateIsAttachedToAgent(clazz);

        clazz.addInterface(classPool.getCtClass(PerfMonTimerAgentApiWrapper.class.getName()));
        clazz.addField(CtField.make("private org.perfmon4j.PerfMonTimer nativeObject = null;", clazz));
        
        String src =
        		"private PerfMonTimer(org.perfmon4j.PerfMonTimer nativeObject) {\r\n"
        		+ "	this.nativeObject = nativeObject;\r\n"
        		+ "}";
        clazz.addConstructor(CtNewConstructor.make(src, clazz));
       
//  	public static PerfMonTimer start(PerfMon mon) {
        src = "{ "
        		+ "org.perfmon4j.PerfMon nativePerfMon = ((org.perfmon4j.instrument.PerfMonAgentApiWrapper)$1).getNativeObject();\r\n"
        		+ "return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.start(nativePerfMon));"
        		+ "}";
        replaceMethodIfExists(clazz, "start", src, PERFMON_API_CLASSNAME);

//    	public static PerfMonTimer start(PerfMon mon, String reactiveContextID) {
        src = "{ "
        		+ "org.perfmon4j.PerfMon nativePerfMon = ((org.perfmon4j.instrument.PerfMonAgentApiWrapper)$1).getNativeObject();\r\n"
        		+ "return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.start(nativePerfMon, $2));"
        		+ "}";
        replaceMethodIfExists(clazz, "start", src, PERFMON_API_CLASSNAME, String.class.getName());

        
//      public static PerfMonTimer start(String key) {
        src = "{ "
        		+ " return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.start($1));"
        		+ "}";
        replaceMethodIfExists(clazz, "start", src, String.class.getName());

//     	public static PerfMonTimer start(String key, boolean isDynamicKey) {
        src = "{ "
        		+ " return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.start($1, $2));"
        		+ "}";
        replaceMethodIfExists(clazz, "start", src, String.class.getName(), CtClass.booleanType.getName());

//      public static PerfMonTimer start(String key, boolean isDynamicKey, String reactiveContextID) {
        src = "{ "
        		+ " return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.start($1, $2, $3));"
        		+ "}";
        replaceMethodIfExists(clazz, "start", src, String.class.getName(), CtClass.booleanType.getName(), String.class.getName());
        

//      public static void abort(PerfMonTimer timer)
        src = "{\r\n"
        		+ "org.perfmon4j.PerfMonTimer nativeTimer = ((org.perfmon4j.instrument.PerfMonTimerAgentApiWrapper)$1).getNativeObject();"
        		+ "org.perfmon4j.PerfMonTimer.abort(nativeTimer);\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "abort", src, PERFMON_TIMER_API_CLASSNAME);
        
//      public static void abort(PerfMonTimer timer, String reactiveContextID)
        src = "{\r\n"
        		+ "org.perfmon4j.PerfMonTimer nativeTimer = ((org.perfmon4j.instrument.PerfMonTimerAgentApiWrapper)$1).getNativeObject();"
        		+ "org.perfmon4j.PerfMonTimer.abort(nativeTimer, $2);\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "abort", src, PERFMON_TIMER_API_CLASSNAME, String.class.getName());

        
//      public static void stop(PerfMonTimer timer)
        src = "{\r\n"
        		+ "org.perfmon4j.PerfMonTimer nativeTimer = ((org.perfmon4j.instrument.PerfMonTimerAgentApiWrapper)$1).getNativeObject();"
        		+ "org.perfmon4j.PerfMonTimer.stop(nativeTimer);\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "stop", src, PERFMON_TIMER_API_CLASSNAME);
        
//      public static void stop(PerfMonTimer timer, String reactiveContextID)
        src = "{\r\n"
        		+ "org.perfmon4j.PerfMonTimer nativeTimer = ((org.perfmon4j.instrument.PerfMonTimerAgentApiWrapper)$1).getNativeObject();"
        		+ "org.perfmon4j.PerfMonTimer.stop(nativeTimer, $2);\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "stop", src, PERFMON_TIMER_API_CLASSNAME, String.class.getName());
        
        org.perfmon4j.PerfMonTimer.getNullTimer();
        src = "{\r\n"
        		+ " return new api.org.perfmon4j.agent.PerfMonTimer(org.perfmon4j.PerfMonTimer.getNullTimer());\r\n"  	
        		+ "}";
        replaceMethodIfExists(clazz, "getNullTimer", src);
        
        src = "public org.perfmon4j.PerfMonTimer getNativeObject() {\r\n"  
        		+ " return nativeObject;\r\n" 
        		+ "}"; 
        clazz.addMethod(CtMethod.make(src, clazz));         
        
    	logger.logDebug("Completed instrumenting agent class " + PERFMON_TIMER_API_CLASSNAME + ". " );

        return clazz.toBytecode();
	}

	@Override
	/**
	 * Unit tests for this code can be found in: org.perfmon4j.instrument.PerfMonAgentAPITest
	 */
	public byte[] attachAgentToSQLTimeAPIClass(byte[] classfileBuffer, ClassLoader loader,
			ProtectionDomain protectionDomain) throws Exception {
		
    	logger.logInfo("Instrumenting agent class " + PERFMON_SQL_TIME_API_CLASSNAME + ". " );

    	
		ClassPool classPool = getClassPool(loader);
    	CtClass clazz = getClazz(classPool, classfileBuffer);
    	
       	updateIsAttachedToAgent(clazz);

        String src = "{ return org.perfmon4j.SQLTime.isEnabled(); }";
        replaceMethodIfExists(clazz, "isEnabled", src);
    	
        src = "{ return org.perfmon4j.SQLTime.getSQLTime(); }";
        replaceMethodIfExists(clazz, "getSQLTime", src);


    	logger.logDebug("Completed instrumenting agent class " + PERFMON_SQL_TIME_API_CLASSNAME + ". " );
        
        return clazz.toBytecode();
	}
	
	private void updateIsAttachedToAgent(CtClass clazz) throws Exception {
    	String methodDescription = buildMethodDescription(clazz, "isAttachedToAgent"); 
		
        CtMethod method = findMethod(clazz, "isAttachedToAgent");
        if (method != null) {
        	String newMethodBody = "return true;";
        	method.setBody(newMethodBody);
        	logger.logDebug("Replacing method " + methodDescription + " with body: '" + newMethodBody + "'");
        } else {
        	throw new Exception("Unable to attach agent class.  Could not find method: " + methodDescription);
        }
	}
	
	private CtMethod findMethod(CtClass clazz, String methodName) throws Exception {
		return findMethod(clazz, methodName, new String[]{});
	}

    private void replaceMethodIfExists(CtClass clazz, String methodName, String newMethodBody) throws Exception {
    	replaceMethodIfExists(clazz, methodName, newMethodBody, new String[] {});
    }

    private void replaceMethodIfExists(CtClass clazz, String methodName, String newMethodBody, String... argumentTypes) throws Exception {
    	String methodDescription = logger.isDebugEnabled() ? buildMethodDescription(clazz, methodName, argumentTypes) : ""; 
    	
        CtMethod method = findMethod(clazz, methodName, argumentTypes);
        if (method != null) {
        	method.setBody(newMethodBody);
        	logger.logDebug("Replacing method " + methodDescription + " with body: '" + newMethodBody + "'");
        } else {
        	logger.logDebug("Skipping update of method " + methodDescription + ". Method not found.");
        	
        }
    }

    private String buildMethodDescription(CtClass clazz, String methodName, String... argumentTypes) {
    	StringBuilder builder =  new StringBuilder(clazz.getName() + "." + methodName + "(");
    	
    	boolean firstArg = true;
    	for (String argumentType : argumentTypes) {
    		if (!firstArg) {
    			builder.append(", ");
    		} else {
    			firstArg = false;
    		}
    		builder.append(argumentType);
    	}
    	builder.append(")");
    	
    	return builder.toString();
    }
    

	private CtMethod findMethod(CtClass clazz, String methodName, String... argumentTypes) throws Exception {
		for (CtMethod method : clazz.getDeclaredMethods()) {
			if (methodName.equals(method.getName())
					&& parameterTypesMatch(method, argumentTypes)
					) {
				return method;
			}
		}
		
		return null;
	}
	
	
	private boolean parameterTypesMatch(CtMethod method, String... argumentTypes) throws NotFoundException {
		boolean result = true;
		
		CtClass[] parameters = method.getParameterTypes();
		if (parameters.length == argumentTypes.length) {
			for (int i = 0; i < parameters.length && result; i++) {
				result = argumentTypes[i].equals(parameters[i].getName());
			}
		} else {
			result = false;
		}
		
		return result;
	}
	
	
	private ClassPool getClassPool(ClassLoader loader) {
    	ClassPool classPool;
        if (loader == null) {
            classPool = new ClassPool(true);
        } else {
            classPool = new ClassPool(false);
            classPool.appendClassPath(new LoaderClassPath(loader));
        }
        
        return classPool;
	}
	
	
	private CtClass getClazz(ClassPool classPool, byte[] classfileBuffer) throws IOException {
	  	ByteArrayInputStream inStream = new ByteArrayInputStream(classfileBuffer);
        CtClass clazz = classPool.makeClass(inStream);
        if (clazz.isFrozen()) {
            clazz.defrost();
        }    
        
        return clazz;
	}
}    

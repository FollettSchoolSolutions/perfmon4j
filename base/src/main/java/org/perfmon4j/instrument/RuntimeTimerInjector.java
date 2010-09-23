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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.SerialVersionUID;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;

public class RuntimeTimerInjector {
    final private static String IMPL_METHOD_SUFFIX = "$Impl";
    private static int serialNumber = 0;
    final private static Logger logger = LoggerFactory.initLogger(RuntimeTimerInjector.class);
    
    // Redefined classes will store their timers in this array...
    public static PerfMon[][] monitorsForRedefinedClasses = null;
    
    public static int injectPerfMonTimers(CtClass clazz, boolean beingRedefined) throws ClassNotFoundException, NotFoundException, CannotCompileException {
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
    
    public static void disableSystemGC(CtClass clazz) throws ClassNotFoundException, NotFoundException, CannotCompileException {
    	CtMethod gcMethod = clazz.getDeclaredMethod("gc");
    	gcMethod.insertBefore("if (1==1) {return;}\r\n");
    }
    
    public static int injectPerfMonTimers(CtClass clazz, boolean beingRedefined, TransformerParams params) throws ClassNotFoundException, NotFoundException, CannotCompileException {
        int mode = TransformerParams.MODE_ANNOTATE;
        TransformerParams.TransformOptions transformOptions = TransformerParams.TransformOptions.DEFAULT;
        int numTimers = 0;
        List<PendingTimer> pendingTimers = new Vector();
        boolean extremeSQLClass = false;
        
        logger.logDebug("Injecting timer into: " + clazz.getName());
        
        if (!clazz.isInterface()) {
            if (params != null) {
                mode = params.getTransformMode(clazz.getName());
                transformOptions = params.getTransformOptions(clazz.getName());
                extremeSQLClass = params.isPossibleJDBCDriver(clazz.getName()) && params.isExtremeSQLClass(clazz);
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
            
            if ((mode != TransformerParams.MODE_NONE) || extremeSQLClass) {
                /**
                 * Make sure we do not change the serial version ID of the class
                 * by inserting our instrumentation...
                 */
                if (!beingRedefined) {
                    // No need to set serial versionID because when a redefined class
                    // is being instrumented it does not have methods added.
                    SerialVersionUID.setSerialVersionUID(clazz);
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
                        && !Modifier.isTransient(method.getModifiers())
                        && methodHasBody(method)) {
                        DeclarePerfMonTimer an = getRuntimeAnnotation(method);
                        String timerKeyAnnotation = null;
                        String timerKeyExtreme = null;
                        
                        if (an != null && 
                            ((mode == TransformerParams.MODE_BOTH) || (mode == TransformerParams.MODE_ANNOTATE))) {
                            timerKeyAnnotation = an.value();
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
                    Integer offsetInStaticMonitorArray = null;
                    if (!beingRedefined) {
                        String timerArray = "static final private org.perfmon4j.PerfMon[] pm$MonitorArray" + 
                            " = new org.perfmon4j.PerfMon[" + numTimers + "];";
                        CtField field = CtField.make(timerArray, clazz);
                        clazz.addField(field);
                    } else {
                        // When a class is being redefined we are unable to add any data members to it.
                        // Find an empty offset to store this classes timer array... If you can not find one
                        // we will be unable to add timers to the class...
                        if (monitorsForRedefinedClasses != null) {
                            for (int i = 0; i < monitorsForRedefinedClasses.length; i++) {
                                if (monitorsForRedefinedClasses[i] == null) {
                                    offsetInStaticMonitorArray = new Integer(i);
                                    monitorsForRedefinedClasses[i] = new org.perfmon4j.PerfMon[numTimers];
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
                            offset = insertPerfMonTimer(clazz, t.method, t.timerKeyAnnotation, t.timerKeyExtreme, t.extremeSQLKey, offset);
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
    
    private static boolean methodHasBody(CtMethod method) {
        return method.getMethodInfo().getCodeAttribute() != null;
    }
    
    private static boolean methodStartsWith(String prefix, String value) {
        return value.startsWith(prefix) && !value.equals(prefix) 
            && Character.isUpperCase(value.charAt(prefix.length()));
    }
    
    
    /**
     * Check to see if this method is a getter or setter.  The options can override if setters/getters are 
     * instrumented.
     * @param method
     * @param options
     * @return
     * @throws NotFoundException
     */
    private static boolean isGetterOrSetter(CtMethod method, TransformerParams.TransformOptions options, 
    		VerboseMessages messages, String extremeMonitorName) throws NotFoundException {
        boolean result = false;
        String name = method.getName();
        
        if (!options.isInstrumentGetters() && (methodStartsWith("is", name) || methodStartsWith("get", name))) {
            if (method.getParameterTypes().length == 0 && 
                !method.getReturnType().getName().equals("void")) {
                result = true;
                if (messages != null) {
                	messages.addSkipMessage(extremeMonitorName, "getter method");
                }
            }
        } else if (!options.isInstrumentSetters() && methodStartsWith("set", name)) {
            if (method.getParameterTypes().length == 1 && (method.getReturnType().getName().equals("void"))) {
                result = true;
                if (messages != null) {
                	messages.addSkipMessage(extremeMonitorName, "setter method");
                }
            }
        }
        return result;
    }

    
    private static DeclarePerfMonTimer getRuntimeAnnotation(CtMethod method) throws ClassNotFoundException {
        DeclarePerfMonTimer result = null;
        Object annotations[] = method.getAnnotations();
        for (int i = 0; (i < annotations.length) && (result == null); i++) {
            if (annotations[i] instanceof DeclarePerfMonTimer) {
                result = (DeclarePerfMonTimer)annotations[i];
            }
        }
        return result;
    }
    
    private static String buildCallToMethod(CtMethod method) throws NotFoundException {
        String type = method.getReturnType().getName();
        if ("void".equals(type)) {
            return method.getName() + "($$);\n";
        } else {
            return "return " + method.getName() + "($$);\n";
        }
    }
    
    private static void moveAnnotations(CtMethod src, CtMethod dest) {
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
    
    private static int makeModifierPrivateFinal(int modifier) {
        return Modifier.setPrivate(modifier) | Modifier.FINAL;
    }
    
    
    private static int insertPerfMonTimer(CtClass clazz, CtMethod method, String timerKeyAnnotation, 
        String timerKeyExtreme, String extremeSQLKey, int offset) throws NotFoundException {
    	
        final String originalMethodName = method.getName();
        final int originalModifiers = method.getModifiers();
        boolean wrapperInserted = false;
        CtMethod wrapperMethod = null;
        
        String implMethod = originalMethodName + "$" + serialNumber + IMPL_METHOD_SUFFIX;
       
        // Rename the method to be timed...
        method.setName(implMethod);
        
        
        String sqlMonitorKey = null;
        
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

    
    private static int insertPerfMonTimerIntoRedefinedClass(CtClass clazz, CtMethod method, String timerKeyAnnotation, 
        String timerKeyExtreme, int offset, Integer offsetInStaticMonitorArray)
        throws CannotCompileException {
        
        // Create the code to insert at the start of the method
        StringBuffer methodPrefix = new StringBuffer();
        methodPrefix.append("{\n");
        methodPrefix.append("\tif (!org.perfmon4j.instrument.RuntimeTimerInjector.isThreadInTimer()) {\n");
        methodPrefix.append("\t\ttry {\n");
        methodPrefix.append("\t\t\torg.perfmon4j.instrument.RuntimeTimerInjector.signalThreadInTimer();\n");
        
        String monitorArrayName  = RuntimeTimerInjector.class.getName() + ".monitorsForRedefinedClasses["
            + offsetInStaticMonitorArray.intValue() + "]";
        
        if (timerKeyAnnotation != null) {
            methodPrefix.append("\t\t\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
                .append("\t\t\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKeyAnnotation + "\");\n")
                .append("\t\t\t}\n")
                .append("\t\t\torg.perfmon4j.instrument.RuntimeTimerInjector.PushTimerForBootClass.pushTimer(org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]));\n");
            offset++;
        }
        if (timerKeyExtreme != null) {
            methodPrefix.append("\t\t\tif (" + monitorArrayName + "[" + offset + "] == null) {\n")
                .append("\t\t\t\t" + monitorArrayName + "[" + offset + "] = org.perfmon4j.PerfMon.getMonitor(\"" + timerKeyExtreme + "\");\n")
                .append("\t\t\t}\n")
                .append("\t\t\torg.perfmon4j.instrument.RuntimeTimerInjector.PushTimerForBootClass.pushTimer(org.perfmon4j.PerfMonTimer.start(" + monitorArrayName + "[" + offset + "]));\n");
            offset++;
        }
        methodPrefix.append("\t\t} finally {\n");
        methodPrefix.append("\t\t\torg.perfmon4j.instrument.RuntimeTimerInjector.releaseThreadInTimer();\n");
        methodPrefix.append("\t\t}\n");
        methodPrefix.append("\t}\n");
        methodPrefix.append("}\n");
        
        StringBuffer methodSuffix = new StringBuffer();
        methodSuffix.append("{\n");
        methodSuffix.append("\tif (!org.perfmon4j.instrument.RuntimeTimerInjector.isThreadInTimer()) {\n");
        methodSuffix.append("\t\ttry {\n");
        methodSuffix.append("\t\t\torg.perfmon4j.instrument.RuntimeTimerInjector.signalThreadInTimer();\n");
        
        if (timerKeyExtreme != null) {
            methodSuffix.append("\t\t\torg.perfmon4j.PerfMonTimer.stop(org.perfmon4j.instrument.RuntimeTimerInjector.PushTimerForBootClass.popTimer());\n");
        }
        if (timerKeyAnnotation != null) {
            methodSuffix.append("\t\t\torg.perfmon4j.PerfMonTimer.stop(org.perfmon4j.instrument.RuntimeTimerInjector.PushTimerForBootClass.popTimer());\n");
        }
        
        methodSuffix.append("\t\t} finally {\n");
        methodSuffix.append("\t\t\torg.perfmon4j.instrument.RuntimeTimerInjector.releaseThreadInTimer();\n");
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
    
    // Unfortunatly Javassist does not currently allow an inserted finally to 
    // access local variables in the method.  This is OK when we are able to add methods to the
    // class since we simply create a wrapper around the method.  This is not acceptable 
    // for boot classes (classes that are being redefined) since the javaagent instrumentation
    // api does not allow you to add methods when redefining a class.  Too get around these
    // limitations we stick the timers on a thread local.  This does have some disadvantages,
    // namely a modest performance penalty, we only do this if we have to.
    // Oh and in fairness, javassist is a great tool and I am pretty confident it can do
    // what I want, however, you have to use it to directly manipulate java byte code, ick.
    public static class PushTimerForBootClass {
        private static ThreadLocal<Stack<PerfMonTimer>> bootClassTimers = new ThreadLocal() {
             protected synchronized Stack<PerfMonTimer> initialValue() {
                 return new Stack<PerfMonTimer>();
             }
        };
        
        public static void pushTimer(PerfMonTimer timer) {
            bootClassTimers.get().push(timer);
        }
        
        public static PerfMonTimer popTimer() {
            return bootClassTimers.get().pop();
        }
    }
    
    private static ThreadLocal<Boolean> singnalThreadInTimer = new ThreadLocal<Boolean>() {
        public Boolean initialValue() {
            return Boolean.FALSE;
        }
    }; 

    public static void signalThreadInTimer() {
        singnalThreadInTimer.set(Boolean.TRUE);
    }

    public static void releaseThreadInTimer() {
        singnalThreadInTimer.set(Boolean.FALSE);
    }
    
    public static boolean isThreadInTimer() {
        return singnalThreadInTimer.get().booleanValue();
    }
}    

/*
 *	Copyright 2008-2015 Follett School Solutions 
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

package org.perfmon4j.instrument.snapshot;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonObservableData;
import org.perfmon4j.SQLWriteable;
import org.perfmon4j.SQLWriteableWithDatabaseVersion;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.SnapShotSQLWriterWithDatabaseVersion;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.jmx.JMXSnapShotProxyFactory.JMXSnapShotImpl;
import org.perfmon4j.instrument.jmx.JavassistJMXSnapShotProxyFactory;
import org.perfmon4j.remotemanagement.MonitorKeyWithFields;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.NumberFormatter;

public class JavassistSnapShotGenerator extends SnapShotGenerator {
    static private final Logger logger = LoggerFactory.initLogger(JavassistSnapShotGenerator.class);
	private static int SERIAL_NUMBER = 0;
	
	private String generateFieldName(String methodName) {
		if (!methodName.startsWith("get")) {
			throw new IllegalArgumentException("Invalid method expected expected to start with get"); 
		}
		StringBuffer m = new StringBuffer(methodName.substring(3));
		m.setCharAt(0, Character.toLowerCase(m.charAt(0)));
		
		return m.toString();
	}
	
	private String generateGetterName(String fieldName) {
		StringBuffer f = new StringBuffer(fieldName);
		f.setCharAt(0, Character.toUpperCase(f.charAt(0)));
		
		return "get" + f.toString();
	}
	
	private void generateRatio(SnapShotRatio ratio, CtClass cls, StringBuffer toAppenderStringBody, StringBuffer getObservationsBody) throws CannotCompileException {
		String methodName = generateGetterName(ratio.name());
		String denominatorMethod = generateGetterName(ratio.denominator());
		String numeratorMethod = generateGetterName(ratio.numerator());
		boolean displayAsPercentage = ratio.displayAsPercentage();
		boolean displayAsDuration = ratio.displayAsDuration();
		
		String methodSource = "public org.perfmon4j.instrument.snapshot.Ratio " + methodName + "() {" +
				"return org.perfmon4j.instrument.snapshot.Ratio.generateRatio(" + numeratorMethod + "(), " + denominatorMethod + "());}";
		
		String formatAsPercent = Boolean.FALSE.toString();
		
		if (ratio.displayAsPercentage()) {
			formatAsPercent = Boolean.TRUE.toString();
		} 
	
		String appendToGetObservations = " result.add(org.perfmon4j.PerfMonObservableDatum.newDatum(\"" + ratio.name() + "\", "
				+ methodName + "()," + formatAsPercent + "));\r\n";
		logger.logDebug("Appending to getObservations: " + appendToGetObservations);
		getObservationsBody.append(appendToGetObservations);
		
		
		logger.logDebug("Adding method: " + methodSource);
		addMethod(cls, methodSource);
		String appendToAppenderString;
		if (displayAsPercentage) {
			appendToAppenderString = " result += \" \" +  " + MiscHelper.class.getName() + ".formatTextDataLine(25, \"" + ratio.name() + "\", " + methodName + "().getRatio()*100, \"%\", 3);\r\n";
		} else if (displayAsDuration){
			appendToAppenderString = " result += \" \" +  " + MiscHelper.class.getName() + ".formatTextDataLine(25, \"" + ratio.name() + "\", " +  MiscHelper.class.getName() 
					+ ".getMillisDisplayable((long)"  + methodName + "().getRatio()));\r\n";
		} else {
			appendToAppenderString = " result += \" \" +  " + MiscHelper.class.getName() + ".formatTextDataLine(25, \"" + ratio.name() + "\", " + methodName + "().getRatio(), \"\", 3);\r\n";
		}
		toAppenderStringBody.append(appendToAppenderString);
	}
	
	
	private void generateCounter(Method method, CtClass cls, StringBuffer initBody, StringBuffer providerBody,
			StringBuffer toAppenderStringBody, StringBuffer getObservationsBody, SnapShotCounter counterAnnotation) throws CannotCompileException {
		String fieldName = generateFieldName(method.getName());
		
		Class<?> retType = method.getReturnType();
		if (!retType.equals(long.class) && !retType.equals(int.class) && !retType.equals(short.class)) {
			throw new IllegalArgumentException("Invalid field type: " + retType.getName());
		} 
		
		cls.addField(CtField.make("private java.lang.Long " + fieldName + "_initial = null;", cls));
		cls.addField(CtField.make("private java.lang.Long " + fieldName + "_final = null;", cls));
		
		String methodSource = "public org.perfmon4j.instrument.snapshot.Delta " + method.getName() + "() {" +
				"org.perfmon4j.instrument.snapshot.Delta result = null;\r\n" +
				"if ((" + fieldName + "_initial != null) && (" + fieldName + "_final != null)) {\r\n" +
				"	result = new org.perfmon4j.instrument.snapshot.Delta(" 
					+ fieldName + "_initial.longValue(), " + fieldName + "_final.longValue(), getDuration());" +
				"}" +
				"return result;}";
		addMethod(cls, methodSource);
			
		String appendToInit = fieldName + "_initial = new java.lang.Long((long)provider." + method.getName() + "());\r\n";
		initBody.append(appendToInit);
		
		String appendToProvider = fieldName + "_final = new java.lang.Long((long)provider." + method.getName() + "());\r\n";
		providerBody.append(appendToProvider);

		boolean formatAsPerSecond = SnapShotCounter.Display.DELTA_PER_MIN.equals(counterAnnotation.preferredDisplay()) 
				|| SnapShotCounter.Display.DELTA_PER_SECOND.equals(counterAnnotation.preferredDisplay());
		
		String appendToGetObservations = "result.add(org.perfmon4j.PerfMonObservableDatum.newDatum(\"" + fieldName + "\", "
				+ method.getName() + "(), " + Boolean.toString(formatAsPerSecond) + "));\r\n";
		logger.logDebug("Appending to getObservations: " + appendToGetObservations);
		getObservationsBody.append(appendToGetObservations);
		
		SnapShotCounter.Display pref = counterAnnotation.preferredDisplay();
		String suffix = pref.getSuffix();
		String getter = pref.getGetter();
		
		if (!"".equals(counterAnnotation.suffix())) {
			suffix = counterAnnotation.suffix();
		}
		
		String appendToAppenderString = 
			" delta = " + method.getName() + "();\r\n" +
			" if (delta != null) {\r\n" +
			"     " +  NumberFormatter.class.getName() + " f = " + NumberFormatter.class.getName() + ".newInstance(\"" + counterAnnotation.formatter().getName() + "\");\r\n " +
			"     result += \" \" +  " + MiscHelper.class.getName() + ".formatTextDataLine(25, \"" + fieldName + "\", f.format(delta." + getter + "()), \"" + suffix + "\");\r\n" +
			" }\r\n";
		
		toAppenderStringBody.append(appendToAppenderString);
	}
	
	private void generateGauge(Method method, CtClass cls, StringBuffer providerBody, StringBuffer toAppenderStringBody, StringBuffer getObservationsBody, 
			SnapShotGauge gaugeAnnotation) throws CannotCompileException {
		String fieldName = generateFieldName(method.getName());
		CtField f;
		String fieldTypeName;
		String toNumberMethod = null;
		
		Class<?> retType = method.getReturnType();
		if (retType.equals(long.class)) {
			f = new CtField(CtClass.longType, fieldName, cls);
			fieldTypeName = "long";
			toNumberMethod = "Long.valueOf";
		} else if (retType.equals(int.class)) {
			f = new CtField(CtClass.intType, fieldName, cls);
			fieldTypeName = "int";
			toNumberMethod = "Integer.valueOf";
		} else if (retType.equals(short.class)) {
			f = new CtField(CtClass.shortType, fieldName, cls);
			fieldTypeName = "short";
			toNumberMethod = "Short.valueOf";
		} else if (retType.equals(float.class)) {
			f = new CtField(CtClass.floatType, fieldName, cls);
			fieldTypeName = "float";
			toNumberMethod = "Float.valueOf";
		} else if (retType.equals(double.class)) {
			f = new CtField(CtClass.doubleType, fieldName, cls);
			fieldTypeName = "double";
			toNumberMethod = "Double.valueOf";
		} else if (retType.equals(boolean.class)) {
			f = new CtField(CtClass.booleanType, fieldName, cls);
			fieldTypeName = "boolean";
		} else  {
			throw new IllegalArgumentException("Invalid field type: " + retType.getName());
		} 
		
		logger.logDebug("Adding field: " + f.toString());
		cls.addField(f);
			
		String methodSource = "public " + fieldTypeName + " " + method.getName() + "() {" +
			"return " + fieldName + ";}";
		logger.logDebug("Adding method: " + methodSource);
		addMethod(cls, methodSource);
		
		String appendToProvider =  "this." + fieldName + "= provider." + method.getName() + "();\r\n" ;
		logger.logDebug("Appending to provideData: " + appendToProvider);
		providerBody.append(appendToProvider);

		String appendToGetObservations = " result.add(org.perfmon4j.PerfMonObservableDatum.newDatum(\"" + fieldName + "\", "  
				+ method.getName() + "()));\r\n";
		logger.logDebug("Appending to getObservations: " + appendToGetObservations);
		getObservationsBody.append(appendToGetObservations);
		
		String appendToAppenderString;
		if (toNumberMethod != null) {
			appendToAppenderString = 
				" {\r\n" +
				" 	Number n= " + toNumberMethod + "(" + fieldName + ");\r\n" +
				"  " +  NumberFormatter.class.getName() + " f = " + NumberFormatter.class.getName() + ".newInstance(\"" + gaugeAnnotation.formatter().getName() + "\");\r\n " +
				"  result += \" \" +  " + MiscHelper.class.getName() + ".formatTextDataLine(25, \"" + fieldName + "\", f.format(n));\r\n" +
				" }";
		} else {
			appendToAppenderString = " result += \" \" +  " + MiscHelper.class.getName() + ".formatTextDataLine(25, \"" + fieldName + "\", " + fieldName + " + \"\");\r\n";
		}
		toAppenderStringBody.append(appendToAppenderString);
	}

	private void generateStringAnnotation(Method method, CtClass cls, StringBuffer providerBody, StringBuffer toAppenderStringBody, StringBuffer getObservationsBody, SnapShotString stringAnnotation) throws CannotCompileException {
		Class<?> retType = method.getReturnType();
		if (retType.isPrimitive()) {
			throw new IllegalArgumentException("Invalid field type: " + retType.getName());
		} 
		String fieldName = generateFieldName(method.getName());
		String src = "private " + retType.getName() + " " + fieldName + " = null;";
		CtField f = CtField.make(src, cls);
		
		logger.logDebug("Adding field: " + f.toString());
		cls.addField(f);
			
		String methodSource = "public " + retType.getName() + " "  + method.getName() + "() {" +
			"return " + fieldName + ";}";
		logger.logDebug("Adding method: " + methodSource);
		addMethod(cls, methodSource);
			
		String appendToProvider =  "this." + fieldName + "= provider." + method.getName() + "();\r\n" ;
		logger.logDebug("Appending to provideData: " + appendToProvider);
		providerBody.append(appendToProvider);
		
		String appendToGetObservations = " result.add(org.perfmon4j.PerfMonObservableDatum.newDatum(\"" + fieldName + "\", " 
				+ method.getName() + "()));\r\n";
		logger.logDebug("Appending to getObservations: " + appendToGetObservations);
		getObservationsBody.append(appendToGetObservations);		

		String appendToAppenderString = " stringFormatter = org.perfmon4j.instrument.SnapShotStringFormatter.newInstance(\"" + stringAnnotation.formatter().getName() + "\");\r\n" +
				"result +=  stringFormatter.format(25, \"" + fieldName + "\", " + fieldName + ");\r\n";
		toAppenderStringBody.append(appendToAppenderString);
	}
	
	
	private void addMethod(CtClass ctClass, String methodBody) {
		try {
			ctClass.addMethod(CtMethod.make(methodBody, ctClass));
		} catch (CannotCompileException e) {
			logger.logError("Cannot add method: " + methodBody, e);
		}
	}
	
	public Class<?> generateSnapShotDataImpl(Class<?> dataProvider) throws GenerateSnapShotException {
		return generateSnapShotDataImpl(dataProvider, null, null);
	}

	
	private Class<?> generateSnapShotDataImpl(Class<?> dataProvider, JavassistJMXSnapShotProxyFactory.Config jmxConfig, ClassPool classPool) throws GenerateSnapShotException {
		final boolean useJMXConfig = jmxConfig != null;
		boolean isStatic = false;
		Class<?> dataInterface = null;
		Class<?> sqlWriter = null;
		boolean writerIncludesDatabaseVersion = false;
		
		SnapShotProvider provider =  (SnapShotProvider)dataProvider.getAnnotation(SnapShotProvider.class);
		if (provider == null && !useJMXConfig) {
			throw new GenerateSnapShotException("Provider class must include a SnapShotProvider annotation");
		}

		if (!useJMXConfig) {
			isStatic = SnapShotProvider.Type.STATIC.equals(provider.type());
		
			dataInterface =  provider.dataInterface();
			sqlWriter = provider.sqlWriter();
			if (SnapShotSQLWriter.class.equals(sqlWriter)) {
				sqlWriter = null;
			} else {
				writerIncludesDatabaseVersion =  SnapShotSQLWriterWithDatabaseVersion.class.isAssignableFrom(sqlWriter);
			}
			
			if (void.class.equals(dataInterface)) {
				dataInterface = null;
			}
			
			if (dataInterface != null && !dataInterface.isInterface()) {
				throw new GenerateSnapShotException("Can only generate SnapShotData implmentation for an interface");
			}
		}

		if (classPool == null) {
			classPool = new ClassPool();
			classPool.appendSystemPath();
			classPool.appendClassPath(new ClassClassPath(dataProvider));
		}
		
		try {
			String className = dataProvider.getName() + "SnapShot" + (++SERIAL_NUMBER);
			CtClass superClass = classPool.get(SnapShotData.class.getName());
			CtClass ctClass = classPool.makeClass(className, superClass);

			ctClass.addInterface(classPool.get(GeneratedData.class.getName()));
			ctClass.addInterface(classPool.get(PerfMonObservableData.class.getName()));
			
			if (dataInterface != null) {
				CtClass ctInterface = classPool.get(dataInterface.getName());
				ctClass.addInterface(ctInterface);
			}

		
			if (sqlWriter != null) {
				CtClass ctSqlWriteableIntf = null;
				if (writerIncludesDatabaseVersion) {
					ctSqlWriteableIntf = classPool.get(SQLWriteableWithDatabaseVersion.class.getName());
				} else {
					ctSqlWriteableIntf = classPool.get(SQLWriteable.class.getName());
				}
				ctClass.addInterface(ctSqlWriteableIntf);
			}
			
			CtClass lifeCycle = classPool.get(SnapShotLifecycle.class.getName());
			ctClass.addInterface(lifeCycle);
			
			CtConstructor constructor = new CtConstructor(new CtClass[]{}, ctClass);
			constructor.setBody(";");
			ctClass.addConstructor(constructor);

			// Add static instance of our SQLWriter 
			if (sqlWriter != null) {
				if (writerIncludesDatabaseVersion) {
					final String s = "private static final org.perfmon4j.SnapShotSQLWriterWithDatabaseVersion sqlWriter = " +
							" new " + sqlWriter.getName() + "();";
					ctClass.addField(CtField.make(s, ctClass));
					
					final String m = "public void writeToSQL(java.sql.Connection conn, String dbSchema, long systemID, double databaseVersion) throws java.sql.SQLException {" +
							" sqlWriter.writeToSQL(conn, dbSchema, this, systemID, databaseVersion);" +
							"}";
					addMethod(ctClass, m);
				} else {
					final String s = "private static final org.perfmon4j.SnapShotSQLWriter sqlWriter = " +
							" new " + sqlWriter.getName() + "();";
					ctClass.addField(CtField.make(s, ctClass));
					
					final String m = "public void writeToSQL(java.sql.Connection conn, String dbSchema, long systemID) throws java.sql.SQLException {" +
							" sqlWriter.writeToSQL(conn, dbSchema, this, systemID);" +
							"}";
					addMethod(ctClass, m);
				}
			}
			
			// Add startTime, endTime and duration fields.
			ctClass.addField(CtField.make("private long startTime = org.perfmon4j.PerfMon.NOT_SET;", ctClass));
			ctClass.addField(CtField.make("private long endTime = org.perfmon4j.PerfMon.NOT_SET;", ctClass));
			ctClass.addField(CtField.make("private long duration = org.perfmon4j.PerfMon.NOT_SET;", ctClass));
			
			addMethod(ctClass, "public long getStartTime() {return startTime;}");
			addMethod(ctClass, "public long getEndTime() {return endTime;}");
			addMethod(ctClass, "public long getDuration() {return duration;}");
			
			// Add Methods required by PerfMonObservableData interface.
			addMethod(ctClass, "public String getDataCategory() {return \"Snapshot.\" + getName();}");
			addMethod(ctClass, "public long getTimestamp() {return getEndTime();}");
			addMethod(ctClass, "public long getDurationMillis() {return getEndTime() - getStartTime();}");
			StringBuffer getObservationsBody = new StringBuffer();
			getObservationsBody.append("public java.util.Set getObservations() {\r\n")
				.append(" java.util.Set result = new java.util.HashSet();\r\n");
			
			

			StringBuffer initBody = new StringBuffer();
			initBody.append("public void init(Object d, long timeStamp) {\r\n")
				.append(dataProvider.getName() +  " provider = (" + dataProvider.getName() + ")d;\r\n")
				.append(" startTime = timeStamp;\r\n");
			
			StringBuffer providerBody = new StringBuffer();
			providerBody.append("public void takeSnapShot(Object d, long timeStamp) {\r\n")
				.append(dataProvider.getName() +  " provider = (" + dataProvider.getName() + ")d;\r\n")
				.append(" endTime = timeStamp;\r\n")
				.append(" duration = endTime - startTime;\r\n");

			
			StringBuffer toAppenderStringBody = new StringBuffer();
			toAppenderStringBody.append("public String toAppenderString() {\r\n")
				.append(" org.perfmon4j.instrument.SnapShotStringFormatter stringFormatter = null;\r\n")
				.append(" org.perfmon4j.instrument.snapshot.Delta delta;\r\n")
				.append(" String result = \"\\r\\n********************************************************************************\\r\\n\";\r\n")
				.append(" result += getName() + \"\\r\\n\";\r\n")
				.append(" result += " + MiscHelper.class.getName() + ".formatTimeAsString(startTime) + \" -> \" + " + MiscHelper.class.getName() + ".formatTimeAsString(endTime) + \"\\r\\n\";\r\n");
			

			StringBuffer objectToStringBody = new StringBuffer();
			objectToStringBody.append("private String objectToStringSnapShotGenerator(Object obj, String fieldName, int prefLableLength) {\r\n")
				.append(" String result = null;\r\n")
				.append(" try {\r\n");
			
			// Find each method in the interface and provide and implementation and a field
			if (useJMXConfig) {
				Iterator<JavassistJMXSnapShotProxyFactory.AttributeConfig> itr =  jmxConfig.getAttributeConfigs().iterator();
				while (itr.hasNext()) {
					JavassistJMXSnapShotProxyFactory.AttributeConfig attr = itr.next();
					Method m = dataProvider.getDeclaredMethod("get" + attr.getName(), new Class[]{});
					
					SnapShotGauge gaugeAnnotation = attr.getSnapShotGauge();
					SnapShotCounter counterAnnotation = attr.getSnapShotCounter();
					
					if (gaugeAnnotation != null) {
						generateGauge(m, ctClass, providerBody, toAppenderStringBody, getObservationsBody, gaugeAnnotation);
					}
		
					if (counterAnnotation != null) {
						generateCounter(m, ctClass, initBody, providerBody, toAppenderStringBody, getObservationsBody, counterAnnotation);
					}
				}
				
				
			} else {
				Method methods[] = dataProvider.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					Method m = methods[i];
					int modifiers = m.getModifiers();
					if (isStatic == Modifier.isStatic(modifiers)) {
						SnapShotString stringAnnotation = m.getAnnotation(SnapShotString.class);
						SnapShotGauge gaugeAnnotation = m.getAnnotation(SnapShotGauge.class);
						SnapShotCounter counterAnnotation = m.getAnnotation(SnapShotCounter.class);
						
						if (stringAnnotation != null) {
							if (Modifier.isPublic(modifiers)) {
								generateStringAnnotation(m, ctClass, providerBody, toAppenderStringBody, getObservationsBody, stringAnnotation);
							} else {
								logger.logError("Unable to implemnent SnapShot String for inaccessible method: " +  
										ctClass.getName() + "." + m.getName());
							}
						}
						
						if (gaugeAnnotation != null) {
							if (Modifier.isPublic(modifiers)) {
								generateGauge(m, ctClass, providerBody, toAppenderStringBody, getObservationsBody, gaugeAnnotation);
							} else {
								logger.logError("Unable to implemnent SnapShot Gauge for inaccessible method: " +  
										ctClass.getName() + "." + m.getName());
							}
						}
			
						if (counterAnnotation != null) {
							if (Modifier.isPublic(modifiers)) {
								generateCounter(m, ctClass, initBody, providerBody, toAppenderStringBody, getObservationsBody, counterAnnotation);
							} else {
								logger.logError("Unable to implemnent SnapShot Counter for inaccessible method: " +  
										ctClass.getName() + "." + m.getName());
							}
						}
					}
				}
			}
			// Add any Ratios
			SnapShotRatios ratios = null;
			if (useJMXConfig) {
				ratios = jmxConfig.getSnapShotRatios();
			} else {
				ratios = (SnapShotRatios)dataProvider.getAnnotation(SnapShotRatios.class);
			}
			if (ratios != null) {
				SnapShotRatio rArray[] = ratios.value();
				for (int i = 0; i < rArray.length; i++) {
					generateRatio(rArray[i], ctClass, toAppenderStringBody, getObservationsBody);
				}
			} else if (!useJMXConfig) {
				// A single SnapShotRatio annotation is also supported.
				SnapShotRatio singleRatio = (SnapShotRatio)dataProvider.getAnnotation(SnapShotRatio.class);
				if (singleRatio != null) {
					generateRatio(singleRatio, ctClass, toAppenderStringBody, getObservationsBody);
				}
			}

			objectToStringBody
			.append(" 	if((result == null) && (obj != null)) {\r\n")
			.append(" 		result = \" \" + " + MiscHelper.class.getName() + ".formatTextDataLine(prefLableLength, fieldName, obj.toString());\r\n")
			.append("    }\r\n")
			.append(" } catch (Exception ex) {\r\n")
			.append(" 	org.perfmon4j.util.Logger otosssdLogger = org.perfmon4j.util.LoggerFactory.initLogger(this.getClass());\r\n")
			.append(" 	otosssdLogger.logError(\"Error converting object to String for: fieldName\", ex);\r\n")
			.append(" }\r\n")
			.append(" return result;\r\n")
			.append("}\r\n");
			addMethod(ctClass, objectToStringBody.toString());

			providerBody.append("}");
			initBody.append("}");
			getObservationsBody.append(" return result;")
				.append("}");
			
			toAppenderStringBody.append("result += \"********************************************************************************\";\r\n");
			toAppenderStringBody.append(" return result;\r\n}");
			
//System.out.println(initBody.toString());			
//System.out.println(providerBody.toString());			
			
			addMethod(ctClass, initBody.toString());
			addMethod(ctClass, providerBody.toString());
			addMethod(ctClass, toAppenderStringBody.toString());
			addMethod(ctClass, getObservationsBody.toString());
			
			return ctClass.toClass(PerfMon.getClassLoader());
		} catch (Exception ex) {
			throw new GenerateSnapShotException("Error generating SnapShotDataImpl", ex);
		}
	}
	
	
	public Bundle generateBundle(Class<?> provider) throws GenerateSnapShotException {
		return generateBundle(provider, null);
	}
	
	public Bundle generateBundle(Class<?> provider, String instanceName) throws GenerateSnapShotException {
		return generateBundle(provider, instanceName, null, null);
	}
	
	private Bundle generateBundle(Class<?> provider, String instanceName, JavassistJMXSnapShotProxyFactory.JMXSnapShotImpl jmxWrapper, ClassPool classPool) throws GenerateSnapShotException {
		final boolean isJMXWrapperClass = jmxWrapper != null;
		JavassistJMXSnapShotProxyFactory.Config jmxSnapShotConfig = null;
		
		if (isJMXWrapperClass) {
			jmxSnapShotConfig = jmxWrapper.getConfig();
		}
		
		
		boolean usePriorityTimer = false;
		Object providerInstance = null;
		Class<?> dataClass = generateSnapShotDataImpl(provider, jmxSnapShotConfig, classPool);
		if (isJMXWrapperClass) {
			providerInstance = jmxWrapper;
		} else {
			SnapShotProvider pAnnotation = (SnapShotProvider)provider.getAnnotation(SnapShotProvider.class);
			if (pAnnotation == null) {
				throw new GenerateSnapShotException("Provider class must include a SnapShotProvider annotation");
			}
			usePriorityTimer = pAnnotation.usePriorityTimer();
			if (SnapShotProvider.Type.STATIC.equals(pAnnotation.type())) {
				if (instanceName != null) {
					logger.logWarn("Instance name \"" + instanceName + "\" ignored with STATIC provider");
				}
			} else if (SnapShotProvider.Type.INSTANCE_PER_MONITOR.equals(pAnnotation.type())) {
				try {
					Constructor<?> c;
					Object args[];
					if (instanceName != null) {
						c = provider.getConstructor(new Class[]{String.class});
						args = new Object[]{instanceName};
					} else {
						c = provider.getConstructor(new Class[]{});
						args = new Object[]{};
					}
					providerInstance = c.newInstance(args);
				} catch (Exception e) {
					throw new GenerateSnapShotException("Unable to instantiate provider", e);
				} 
			} else if (SnapShotProvider.Type.FACTORY.equals(pAnnotation.type())) {
				try {
					String methodName = "getInstance";
					Method m;
					Object args[];
					if (instanceName != null) {
						m = provider.getMethod(methodName, new Class[]{String.class});
						args = new Object[]{instanceName};
					} else {
						m = provider.getMethod(methodName, new Class[]{});
						args = new Object[]{};
					}
					providerInstance = m.invoke(null, args);
				} catch (Exception e) {
					throw new GenerateSnapShotException("Unable to obtain provider via static getInstance method", e);
				}
			}
		}
		return new Bundle(dataClass, providerInstance, usePriorityTimer);
	}
	


	/**
	 * NOTE: The unit test for this method are found in JMXSnapShotFactoryTest.java
	 */
	public Bundle generateBundle(JMXSnapShotImpl impl, ClassPool classPool) throws GenerateSnapShotException {
		return generateBundle(impl.getClass(), null, impl, classPool);
	}
	
	private List<FieldKey> createFields(MonitorKey keys[], String name, String fieldType) {
		List<FieldKey> fields = new ArrayList<FieldKey>();
		
		for (int i = 0; i < keys.length; i++) {
			fields.add(new FieldKey(keys[i], name, fieldType));
		}
		
		return fields;
	}

	private MonitorKey[] getMonitorKeysForClass(Class<?> clazz) {
		List<MonitorKey> result = new ArrayList<MonitorKey>();

		Method methods[] = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			
			SnapShotInstanceDefinition  instanceDef = method.getAnnotation(SnapShotInstanceDefinition.class);
			if (instanceDef != null) {
				try {
					String instances[] = (String[])method.invoke(null, new Object[]{});
					for (int j = 0; j < instances.length; j++) {
						result.add(MonitorKey.newSnapShotKey(clazz.getName(), instances[j]));
					}
					break;
				} catch (Exception e) {
					logger.logWarn("Unable to determine instances for SnapShot class: " + clazz.getName(),
							e);
				}
			}
		}
		if (result.isEmpty()) {
			result.add(MonitorKey.newSnapShotKey(clazz.getName()));
		}
		
		return result.toArray(new MonitorKey[result.size()]);
	}
	
	
	
	public MonitorKeyWithFields[] generateExternalMonitorKeys (Class<?> clazz) {
		MonitorKey monitorKey[] = getMonitorKeysForClass(clazz);
		
		List<FieldKey> fields = new ArrayList<FieldKey>();
		
		Annotation classAnnotations[] = clazz.getAnnotations();
		for (int i = 0; i < classAnnotations.length; i++) {
			Annotation annotation = classAnnotations[i];
			if (annotation instanceof SnapShotRatio) {
				SnapShotRatio snapShotRatio = (SnapShotRatio)annotation;
				fields.addAll(createFields(monitorKey, snapShotRatio.name(),
						FieldKey.DOUBLE_TYPE));
			} else if (annotation instanceof SnapShotRatios) {
				SnapShotRatios snapShotRatios = (SnapShotRatios)annotation;
				for (int j = 0; j < snapShotRatios.value().length; j++) {
					SnapShotRatio snapShotRatio = snapShotRatios.value()[j];
					fields.addAll(createFields(monitorKey, snapShotRatio.name(),
							FieldKey.DOUBLE_TYPE));
				}
			}
		}
		Method methods[] = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			
			SnapShotCounter counter = method.getAnnotation(SnapShotCounter.class);
			if (counter != null) {
				fields.addAll(createFields(monitorKey, generateFieldName(method.getName()) + DELTA_FIELD_SUFFIX,
						FieldKey.DOUBLE_TYPE));
			}
			SnapShotGauge gauge = method.getAnnotation(SnapShotGauge.class);
			if (gauge != null) {
				String fieldType = FieldKey.LONG_TYPE;
				if (method.getReturnType().equals(Integer.class) || method.getReturnType().equals(int.class)) {
					fieldType = FieldKey.INTEGER_TYPE;
				}
				fields.addAll(createFields(monitorKey, generateFieldName(method.getName()),
						fieldType));
			}
			SnapShotString str = method.getAnnotation(SnapShotString.class);
			if (str != null && !str.isInstanceName()) {
				fields.addAll(createFields(monitorKey, generateFieldName(method.getName()),
						FieldKey.STRING_TYPE));
			}
		}
		return MonitorKeyWithFields.groupFields(fields.toArray(new FieldKey[fields.size()]));
	}
}
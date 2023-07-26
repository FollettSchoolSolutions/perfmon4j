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

package org.perfmon4j.instrument.jmx;

import java.util.Iterator;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.perfmon4j.PerfMon;
import org.perfmon4j.SnapShotProviderWrapper;
import org.perfmon4j.instrument.PerfMonTimerTransformer;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.NumberFormatter;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;

public class JavassistJMXSnapShotProxyFactory  extends JMXSnapShotProxyFactory {
	public static long SERIAL_NUMBER = 0; 

	public SnapShotProviderWrapper getnerateSnapShotWrapper(String monitorName, String configXML) throws Exception {
		ClassPool classPool = new ClassPool();
		
		JMXSnapShotImpl impl = newSnapShotImpl(configXML, classPool);
		JavassistSnapShotGenerator.Bundle bundle = ((JavassistSnapShotGenerator)PerfMonTimerTransformer.snapShotGenerator).generateBundle(impl, classPool);
		
		return new SnapShotProviderWrapper(monitorName, bundle);
	}
	
	public JMXSnapShotImpl newSnapShotImpl(String xml) throws Exception {
		return newSnapShotImpl(xml, new ClassPool());
	}
	
	private JMXSnapShotImpl newSnapShotImpl(String xml, ClassPool classPool) throws Exception {
		Config config = parseConfig(xml);
		Class<?> clazz = generateDerivedJMXClass(config, classPool);
		JMXSnapShotImpl result = (JMXSnapShotImpl)clazz.getConstructor(new Class[]{JavassistJMXSnapShotProxyFactory.Config.class}).newInstance(new Object[]{config});
		return result;
	}

	private MBeanAttributeInfo findMBeanInfo(MBeanAttributeInfo info[], String attrName) {
		MBeanAttributeInfo result = null;
		
		for (int i = 0; i < info.length && result == null; i++) {
			MBeanAttributeInfo attr = info[i];
			if (attrName.equals(attr.getName())) {
				result = attr;
			}
		}
		return result;
	}
	
	private Class<?> generateDerivedJMXClass(Config config, ClassPool classPool) throws Exception {
		classPool.appendSystemPath();

		String className = "org.perfmon4j.JMXSnapShot_" + (++SERIAL_NUMBER);
		CtClass superClass = classPool.get(JavassistJMXSnapShotProxyFactory.JMXSnapShotImpl.class.getName());
		CtClass configClass = classPool.get(JavassistJMXSnapShotProxyFactory.Config.class.getName());
		CtClass ctClass = classPool.makeClass(className, superClass);

		// Add the constructor...
		CtConstructor constructor = CtNewConstructor.make(new CtClass[]{configClass}, new CtClass[]{}, ctClass); 
		ctClass.addConstructor(constructor);

		MBeanServer server = MiscHelper.findMBeanServer(config.getServerDomain());
		if (server == null) {
			throw new GenerateSnapShotException("Unable to find attribute mBeanServer for domain: " 
					+ config.getServerDomain());
		}
		Iterator<AttributeConfig> itr = config.getAttributeConfigs().iterator();
		while (itr.hasNext()) {
			AttributeConfig attr = itr.next();
			
			ObjectName objName = new ObjectName(attr.getObjectName());
			MBeanInfo info = server.getMBeanInfo(objName);
			MBeanAttributeInfo mBeanInfo = findMBeanInfo(info.getAttributes(), attr.getJMXName());
			if (mBeanInfo == null) {
				throw new GenerateSnapShotException("Unable to find attribute for attribute: " + attr.getJMXName() + 
						" on MBean: " + objName.getCanonicalName());
			}
			String src = "";
			String getterName = "get" + attr.getName() + "()";
			
			boolean stringType = false;
			if ("long".equals(mBeanInfo.getType())) {
				src = "public long " + getterName + "{\r\n" +
					"    return ((java.lang.Long)getAttribute(\"" + 
						attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", new java.lang.Long(-1l))).longValue();\r\n" +
					"}";
			} else if ("double".equals(mBeanInfo.getType())) {
				src = "public double " + getterName + "{\r\n" +
					"    return ((java.lang.Double)getAttribute(\"" + 
						attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", new java.lang.Double(-1.0))).doubleValue();\r\n" +
					"}";
			} else if ("float".equals(mBeanInfo.getType())) {
				src = "public float " + getterName + "{\r\n" +
					"    return ((java.lang.Float)getAttribute(\"" + 
						attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", new java.lang.Float(-1.0f))).floatValue();\r\n" +
					"}";
			} else if ("int".equals(mBeanInfo.getType())) {
				src = "public int " + getterName + "{\r\n" +
					"    return ((java.lang.Integer)getAttribute(\"" + 
						attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", new java.lang.Integer(-1))).intValue();\r\n" +
					"}";
			} else if ("short".equals(mBeanInfo.getType())) {
				src = "public short " + getterName + "{\r\n" +
					"    return ((java.lang.Short)getAttribute(\"" + 
						attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", new java.lang.Short((short)-1))).shortValue();\r\n" +
					"}";
			} else if ("boolean".equals(mBeanInfo.getType())) {
				src = "public boolean " + getterName + "{\r\n" +
					"    return ((java.lang.Boolean)getAttribute(\"" + 
						attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", Boolean.FALSE)).booleanValue();\r\n" +
					"}";
			} else {
				src = "public String " + getterName + "{\r\n" +
				"    return ((java.lang.String)getAttribute(\"" + 
					attr.getObjectName() + "\", \"" + attr.getJMXName() + "\", \"\").toString());\r\n" +
				"}";
				stringType = true;
			}

			boolean hasAnnotation = (attr.getSnapShotCounter() != null) || (attr.getSnapShotGauge() != null)
				|| (attr.getSnapShotString() != null);
			if (!hasAnnotation) {
				if (stringType) {
					attr.setSnapShotString(new SnapShotStringVO(SnapShotStringFormatter.class));
				} else {
					attr.setSnapShotGauge(new SnapShotGaugeVO(NumberFormatter.class));
				}
			}
			CtMethod method = CtMethod.make(src, ctClass);
			ctClass.addMethod(method);
		}
		
		return ctClass.toClass(/* neighbor class */ PerfMon.class);
	}
	
}
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

package org.perfmon4j.instrument.jmx;

import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewConstructor;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.perfmon4j.InvalidConfigException;
import org.perfmon4j.PerfMon;
import org.perfmon4j.SnapShotProviderWrapper;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.SnapShotStringFormatter;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.NumberFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class JMXSnapShotProxyFactory {
	private static final Logger logger = LoggerFactory.initLogger(JMXSnapShotProxyFactory.class);
	
	/**
	 * Package level for testing.
	 */
	static Config parseConfig(String xml) throws Exception {
		return XMLParser.parseXML(xml);
	}
	
	public static SnapShotProviderWrapper getnerateSnapShotWrapper(String monitorName, String configXML) throws Exception {
		ClassPool classPool = new ClassPool();
		JMXSnapShotImpl impl = newSnapShotImpl(configXML, classPool);
		SnapShotGenerator.Bundle bundle = SnapShotGenerator.generateBundle(impl, classPool);
		
		return new SnapShotProviderWrapper(monitorName, bundle);
	}
	
	public static JMXSnapShotImpl newSnapShotImpl(String xml) throws Exception {
		return newSnapShotImpl(xml, new ClassPool());
	}
	
	private static JMXSnapShotImpl newSnapShotImpl(String xml, ClassPool classPool) throws Exception {
		Config config = parseConfig(xml);
		Class clazz = generateDerivedJMXClass(config, classPool);
		JMXSnapShotImpl result = (JMXSnapShotImpl)clazz.getConstructor(new Class[]{JMXSnapShotProxyFactory.Config.class}).newInstance(new Object[]{config});
		return result;
	}
	
	public static class Config {
		private String serverDomain = null;
		private String defaultObjectName = null;
		private final List<AttributeConfig> attributeConfig = new ArrayList<AttributeConfig>();
		private final SnapShotRatiosVO snapShotRatios = new SnapShotRatiosVO();
		
		
		public String getServerDomain() {
			return serverDomain;
		}

		public void setServerDomain(String serverDomain) {
			this.serverDomain = serverDomain;
		}

		public List<AttributeConfig> getAttributeConfigs() {
			return attributeConfig;
		}

		public void addAttributeConfig(AttributeConfig jmxAttribute) {
			attributeConfig.add(jmxAttribute);
		}

		public SnapShotRatios getSnapShotRatios() {
			return snapShotRatios;
		}

		public String getDefaultObjectName() {
			return defaultObjectName;
		}

		public void setDefaultObjectName(String defaultObjectName) {
			this.defaultObjectName = defaultObjectName;
		}
	}
	
	public static class AttributeConfig {
		private final String objectName;
		private final String name;  //  
		private final String jmxName; // jmxName defaults to name if not set.
		
		private SnapShotGauge snapShotGauge = null;
		private SnapShotCounter snapShotCounter = null;
		private SnapShotString snapShotString = null;
		
		
		AttributeConfig(String objectName, String name, String jmxName) {
			this.objectName = objectName;
			this.name = name;
			this.jmxName = jmxName;
		}

		public String getObjectName() {
			return objectName;
		}

		public String getName() {
			return name;
		}

		public String getJMXName() {
			return (jmxName == null) ? name : jmxName;
		}
		
		public SnapShotGauge getSnapShotGauge() {
			return snapShotGauge;
		}

		public void setSnapShotGauge(SnapShotGauge snapShotGauge) {
			this.snapShotGauge = snapShotGauge;
		}

		public SnapShotCounter getSnapShotCounter() {
			return snapShotCounter;
		}

		public void setSnapShotCounter(SnapShotCounter snapShotCounter) {
			this.snapShotCounter = snapShotCounter;
		}

		public SnapShotString getSnapShotString() {
			return snapShotString;
		}

		public void setSnapShotString(SnapShotString snapShotString) {
			this.snapShotString = snapShotString;
		}
	}
	
	static private class XMLParser extends DefaultHandler {
	    private final Config config = new Config();
	    private AttributeConfig lastJMXAttribute = null;
	    
	    public static Config parseXML(String xml) throws InvalidConfigException {
	        Config result = null;
	        try {
	            XMLParser handler = new XMLParser();
	            XMLReader xr = XMLReaderFactory.createXMLReader();
	            xr.setContentHandler(handler);
	            xr.setErrorHandler(handler);
	            xr.parse(new InputSource(new StringReader(xml)));
	            result = handler.config;
	        } catch (SAXException ex) {
	        	throw new InvalidConfigException("Parse Exception", ex);
	        } catch (IOException ex) {
	        	throw new InvalidConfigException("IOException", ex);
	        }
	        return result;
	    }
	    
	    private static String SECTION_WRAPPER = "JMXWrapper";
	    private static String SECTION_SNAP_SHOT_RATIO = "snapShotRatio";
	    private static String SECTION_ATTRIBUTE = "attribute";
	    private static String SECTION_SNAP_SHOT_COUNTER = "snapShotCounter";
	    private static String SECTION_SNAP_SHOT_GAUGE = "snapShotGauge";

	    private Class<? extends NumberFormatter> extractNumberFormatter(Attributes atts) {
	    	Class<? extends NumberFormatter> result = NumberFormatter.class;
    		String formatterValue = atts.getValue("formatter");
    		if (formatterValue != null) {
    			try {
					result = (Class<? extends NumberFormatter>)Class.forName(formatterValue);
				} catch (ClassNotFoundException e) {
					logger.logWarn(formatterValue + " class not found! - Using default number formatter");
				}
    		}
	    	return result;
	    }
	    
	    public void startElement(@SuppressWarnings("unused") String uri, 
	    	String name, @SuppressWarnings("unused") String qName,
	        Attributes atts) throws SAXException {
	    	if (SECTION_WRAPPER.equals(name)) {
	    		String value = atts.getValue("serverDomain");
//	    		validateArg(SECTION_WRAPPER, "serverDomain", value);
	    		config.setServerDomain(value);
	    		config.setDefaultObjectName(atts.getValue("defaultObjectName"));
	    		
	    	} else if (SECTION_ATTRIBUTE.equals(name)) {
	    		String nameValue = atts.getValue("name");
	    		String objectNameValue = atts.getValue("objectName");
	    		validateArg(SECTION_ATTRIBUTE, "name", nameValue);
	    		if (objectNameValue == null || "".equals(objectNameValue)) {
	    			objectNameValue = config.getDefaultObjectName();
		    		if (objectNameValue == null || "".equals(objectNameValue)) {
		    			throw new SAXException("Attribute: objectName required for section " + 
		    					SECTION_ATTRIBUTE +
		    					" when defaultObjectName is NOT specified in section " + SECTION_WRAPPER);
		    		}
	    		}
	    		lastJMXAttribute = new AttributeConfig(objectNameValue, nameValue, 
	    				atts.getValue("jmxName"));
	    		config.getAttributeConfigs().add(lastJMXAttribute);
	    	} else if (SECTION_SNAP_SHOT_RATIO.equals(name)) {
	    		String nameValue = atts.getValue("name");
	    		String numeratorValue = atts.getValue("numerator");
	    		String denominatorValue = atts.getValue("denominator");
	    		String displayAsPercentageValue = atts.getValue("displayAsPercentage");
	    		
	    		validateArg(SECTION_SNAP_SHOT_RATIO, "name", nameValue);
	    		validateArg(SECTION_SNAP_SHOT_RATIO, "numerator", numeratorValue);
	    		validateArg(SECTION_SNAP_SHOT_RATIO, "denominator", denominatorValue);
	    		
	    		boolean displayAsPercentage = Boolean.parseBoolean(displayAsPercentageValue);
	    		config.snapShotRatios.addSnapShotRatio(new SnapShotRatioVO(
	    				nameValue, numeratorValue, denominatorValue, displayAsPercentage));
	    	} else if (SECTION_SNAP_SHOT_COUNTER.equals(name)) {
	    		Class<? extends NumberFormatter> formatter =
	    			extractNumberFormatter(atts);
	    		String suffix = "";
	    		
	    		String displayValue = atts.getValue("display");
	    		String suffixValue = atts.getValue("suffix");
	    		
	    		if (suffixValue != null) {
	    			suffix = suffixValue;
	    		}
	    		SnapShotCounter.Display display = SnapShotCounter.Display.DELTA;
	    		if ("DELTA_PER_MIN".equals(displayValue)) {
	    			display = SnapShotCounter.Display.DELTA_PER_MIN;
	    		} else if ("DELTA_PER_SECOND".equals(displayValue)) {
	    			display = SnapShotCounter.Display.DELTA_PER_SECOND;
	    		} else if ("FINAL_VALUE".equals(displayValue)) {
	    			display = SnapShotCounter.Display.FINAL_VALUE;
	    		} else if ("INITIAL_VALUE".equals(displayValue)) {
	    			display = SnapShotCounter.Display.INITIAL_VALUE;
	    		}
	    		SnapShotCounterVO counter =
	    			new SnapShotCounterVO(display,
	    					formatter,
	    					suffix);
	    		lastJMXAttribute.setSnapShotCounter(counter);
	    	} else if (SECTION_SNAP_SHOT_GAUGE.equals(name)) {
	    		Class<? extends NumberFormatter> formatter =
	    			extractNumberFormatter(atts);
	    		SnapShotGaugeVO gauge =
	    			new SnapShotGaugeVO(formatter);
	    		lastJMXAttribute.setSnapShotGauge(gauge);
	    	} else {
	    		throw new SAXException("Uknown element name: " + name);
	    	}
	    }

	    private static void validateArg(String section, String name, String value) throws SAXException {
	        if (value == null || "".equals(value)) {
	            throw new SAXException("Attribute: " + name + " required for section: " + section);
	        }
	    }
	}
	
	public static long SERIAL_NUMBER = 0; 

	private static MBeanAttributeInfo findMBeanInfo(MBeanAttributeInfo info[], String attrName) {
		MBeanAttributeInfo result = null;
		
		for (int i = 0; i < info.length && result == null; i++) {
			MBeanAttributeInfo attr = info[i];
			if (attrName.equals(attr.getName())) {
				result = attr;
			}
		}
		return result;
	}
	
	private static Class generateDerivedJMXClass(Config config, ClassPool classPool) throws Exception {
		classPool.appendSystemPath();

		String className = "JMXSnapShot_" + (++SERIAL_NUMBER);
		CtClass superClass = classPool.get(JMXSnapShotProxyFactory.JMXSnapShotImpl.class.getName());
		CtClass configClass = classPool.get(JMXSnapShotProxyFactory.Config.class.getName());
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
		return ctClass.toClass(PerfMon.getClassLoader());
	}
	
	public static class JMXSnapShotImpl {
		private final JMXSnapShotProxyFactory.Config config;
		private WeakReference<MBeanServer> mBeanServer = null;
		
		public JMXSnapShotImpl(JMXSnapShotProxyFactory.Config config) {
			this.config = config;
		}
		
		public String getServerDomain() {
			return config.getServerDomain();
		}
		
		public JMXSnapShotProxyFactory.Config getConfig() {
			return config;
		}
		
		public MBeanServer getMBeanServer() {
			MBeanServer result = null;
			if (mBeanServer != null) {
				result = mBeanServer.get();
			}
			if (result == null) {
				result = MiscHelper.findMBeanServer(getServerDomain());
				if (result != null) {
					mBeanServer = new WeakReference<MBeanServer>(result);
				}
			}
			return result;
		}
		

		private String buildWarning(String objName, String attributeName) {
			String domain = getServerDomain();
			
			String result = "Unable to obtain attribute: " + attributeName + 
				" for MBean: " + objName;
			
			if (domain != null) {
				result += " on MBeanServer Domain: " + domain;
			}
			
			result += " - Returning default value";
			
			return result;
		}
		
		protected Object getAttribute(String objName, String attributeName, Object defaultObject) {
			Object result = defaultObject;
			
			MBeanServer server = getMBeanServer();
			if (server == null) {
				logger.logWarn("MBeanServer NOT found - " + buildWarning(objName, attributeName));
			} else {
				try {
					Object tmp = server.getAttribute(new ObjectName(objName),attributeName);
					if (tmp != null) {
						result = tmp;
					}
				} catch (Exception e) {
					logger.logWarn(buildWarning(objName, attributeName), e);
				} 
			}
			return result;
		}
	}
	
	// Container implementation of all annotations
	 @SuppressWarnings("intfAnnotation") 
	 private static class SnapShotRatiosVO implements SnapShotRatios {
		private final List<SnapShotRatio> snapShotRatios = new ArrayList<SnapShotRatio>();
		
		private void addSnapShotRatio(SnapShotRatio ratio) {
			snapShotRatios.add(ratio);
		}
		
		public SnapShotRatio[] value() {
			return snapShotRatios.toArray(new SnapShotRatio[]{});
		}

		public Class<? extends Annotation> annotationType() {
			return SnapShotRatio.class;
		}
	}
	
	@SuppressWarnings("intfAnnotation") 
	private static class SnapShotRatioVO implements SnapShotRatio {
		private final String name;
		private final String numerator;
		private final String denominator;
		private final boolean displayAsPercentage;
		
		private SnapShotRatioVO(String name, String numerator, String denominator,
			boolean displayAsPercentage) {
			this.name = name;
			this.numerator = numerator;
			this.denominator = denominator;
			this.displayAsPercentage = displayAsPercentage;
		}
		
		public String denominator() {
			return denominator;
		}

		public boolean displayAsPercentage() {
			return displayAsPercentage;
		}

		public String name() {
			return name;
		}

		public String numerator() {
			return numerator;
		}

		public Class<? extends Annotation> annotationType() {
			return SnapShotRatio.class;
		}
	}

	 @SuppressWarnings("intfAnnotation") 
	 private static class SnapShotGaugeVO implements SnapShotGauge {
		 private final Class<? extends NumberFormatter> formatter;

		 public SnapShotGaugeVO(Class<? extends NumberFormatter> formatter) {
			 this.formatter = formatter;
		 }
		 
		public Class<? extends NumberFormatter> formatter() {
			return formatter;
		}

		public Class<? extends Annotation> annotationType() {
			return SnapShotGauge.class;
		}
	 }
	
	 @SuppressWarnings("intfAnnotation") 
	 private static class SnapShotStringVO implements SnapShotString {
		 private final Class<? extends SnapShotStringFormatter> formatter;

		 public SnapShotStringVO(Class<? extends SnapShotStringFormatter> formatter) {
			 this.formatter = formatter;
		 }
		 
		 public Class<? extends SnapShotStringFormatter> formatter() {
			 return formatter;
		 }

		 public Class<? extends Annotation> annotationType() {
			 return SnapShotString.class;
		 }

		 public boolean isInstanceName() {
			 return false;
		 }
	 }
	 
	 @SuppressWarnings("intfAnnotation") 
	 private static class SnapShotCounterVO implements SnapShotCounter {
		 private final SnapShotCounter.Display preferredDisplay;
		 private final Class<? extends NumberFormatter> formatter;
		 private final String suffix;
		 
		 public SnapShotCounterVO(SnapShotCounter.Display preferredDisplay,
				 Class<? extends NumberFormatter> formatter,
				 String suffix) {
			 this.preferredDisplay = preferredDisplay;
			 this.formatter = formatter;
			 this.suffix = suffix;
		 }
		 
		public Class<? extends NumberFormatter> formatter() {
			return formatter;
		}

		public Display preferredDisplay() {
			return preferredDisplay;
		}

		public String suffix() {
			return suffix;
		}

		public Class<? extends Annotation> annotationType() {
			return SnapShotCounter.class;
		}
	 }
}

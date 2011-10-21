/*
 *	Copyright 2008, 2011 Follett Software Company 
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class BeanHelper {
    private BeanHelper() {
    }
 
    final private static Map<Class<?>, Class<?>> primitiveToJavaMap;
    
    static {
        primitiveToJavaMap = Collections.synchronizedMap(new HashMap<Class<?>, Class<?>>());
        
        primitiveToJavaMap.put(int.class, Integer.class);
        primitiveToJavaMap.put(long.class, Long.class);
        primitiveToJavaMap.put(float.class, Float.class);
        primitiveToJavaMap.put(double.class, Double.class);
        primitiveToJavaMap.put(short.class, Short.class);
        primitiveToJavaMap.put(char.class, Character.class);
        primitiveToJavaMap.put(byte.class, Byte.class);
        primitiveToJavaMap.put(boolean.class, Boolean.class);
    }
    
    public static void setValue(Object to, String attributeName, Object value) throws UnableToSetAttributeException {
        final String setterName = "set" + attributeName;
  
        try {
            Class<?> clazz = to.getClass();
            Method methods[] = clazz.getMethods();
            Method method = null;
                
            for (int i = 0; i < methods.length && method == null; i++) {
               Method t = methods[i];
               if (setterName.equalsIgnoreCase(t.getName()) 
                   && t.getParameterTypes().length == 1) {
                   method = t;
               }
            }
            
            Class<?> clazzParam = method.getParameterTypes()[0];
            Class<?> mappedClazz = primitiveToJavaMap.get(clazzParam);
            if (mappedClazz != null) {
                clazzParam = mappedClazz;
            }
            if (clazzParam.isAssignableFrom(value.getClass())) {
            	method.invoke(to, new Object[]{value});
            } else if (value.getClass().equals(String.class)){
	            if (clazzParam.equals(String.class)) {
	                method.invoke(to, new Object[]{value});
	            } else if (clazzParam.equals(Character.class)) {
	                method.invoke(to, new Character(((String)value).charAt(0)));
	            } else {
	                Constructor<?> constructor = clazzParam.getConstructor(new Class[]{String.class});
	                method.invoke(to, new Object[]{constructor.newInstance(new Object[]{value})});
	            }
            }
        } catch (Exception ex) {
            throw new UnableToSetAttributeException(attributeName, value, ex);
        }
    }
    
    public static class UnableToSetAttributeException extends Exception {
		private static final long serialVersionUID = 1L;

		UnableToSetAttributeException(String attributeName, Object value, Exception rootException) {
            super("Unable to set attribute: \"" + attributeName + "\" to value: \"" + value + "\"", 
                rootException);
        }
    }
    
    public static class UnableToGetAttributeException extends Exception {
		private static final long serialVersionUID = 1L;

		UnableToGetAttributeException(String attributeName, Exception rootException) {
            super("Unable to Get attribute: \"" + attributeName + "\"", 
                rootException);
        }
    }    
    
    public static Object getValue(Object to, String attributeName) throws UnableToGetAttributeException {
    	Object result = null;
    	
    	final String getterName = "get" + attributeName;
    	final String isserName = "is" + attributeName;
        
        try {
            Class<?> clazz = to.getClass();
            Method methods[] = clazz.getMethods();
            Method method = null;
                
            for (int i = 0; i < methods.length && method == null; i++) {
               Method t = methods[i];
               if ((isserName.equalsIgnoreCase(t.getName()) ||
            		getterName.equalsIgnoreCase(t.getName())) 
                   && t.getParameterTypes().length == 0
                   && !t.getReturnType().equals(void.class)) {
                   method = t;
               }
            }
            result = method.invoke(to, new Object[]{});
        } catch (Exception ex) {
            throw new UnableToGetAttributeException(attributeName, ex);
        }
        
        return result;
    }
}

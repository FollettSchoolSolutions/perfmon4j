/*
 *	Copyright 2008 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class ObjectID {
    private final String className;
    private final Properties attributes;

    private String attributeString = null;
    private Integer hashCode = null;
    
    protected ObjectID(String className) {
        this(className, null);
    }
    
    protected ObjectID(String className, Properties attributes) {
        this.className = className;
        this.attributes = attributes;
    }
    
    public int hashCode() {
        if (hashCode == null) {
            hashCode = generateHashCode();
        }
        return hashCode.intValue();
    }
    
    protected String getAttributeString() {
        if (attributeString == null) {
            attributeString = buildAttributeString();
            if (attributeString == null) {
                attributeString = "";
            }
        }
        return attributeString;
    }
    
    protected String buildAttributeString() {
        String result = "";

        if (attributes != null && attributes.size() > 0) {
            Set sortedKeys = new TreeSet(attributes.keySet());
        
            Iterator itr = sortedKeys.iterator();
            while (itr.hasNext()) {
                String key = (String)itr.next();
                result += "KEY:(" + key
                    + ")VALUE:(" + attributes.getProperty(key)
                    + ")";
            }
        }
        return result;
    }
    
    private Integer generateHashCode() {
        return new Integer(className.hashCode() + getAttributeString().hashCode());
    }
    
    public boolean equals(Object obj) {
        boolean result = (this == obj);  // First do a reference compare...
        if (!result) {
            if (this.getClass().isInstance(obj)) {
                ObjectID id = (ObjectID)obj;
                result = (hashCode() == id.hashCode())
                    && className.equals(id.className) 
                    && getAttributeString().equals(id.getAttributeString());
            }
        }
        return result;
    }

    public String getClassName() {
        return className;
    }

    public Properties getAttributes() {
        Properties result = new Properties();
        if (attributes != null) {
            result.putAll(attributes);
        }
        return result;
    }
}


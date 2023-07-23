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
package org.perfmon4j;

import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.util.BeanHelper;
import org.perfmon4j.util.BeanHelper.UnableToGetAttributeException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public abstract class SnapShotData implements PerfMonData {
	private static final Logger logger = LoggerFactory.initLogger(SnapShotData.class);
	
    private String name;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public Map<FieldKey, Object> getFieldData(FieldKey[] fields) {
    	Map<FieldKey, Object> result = new HashMap<FieldKey, Object>();

    	for (int i = 0; i < fields.length; i++) {
			FieldKey field = fields[i];
			
			Object value = null;
			try {
				value = BeanHelper.getValue(this, field.getFieldName());
				if (value instanceof Ratio) {
					value = Double.valueOf(((Ratio)value).getRatio());
				}
			} catch (UnableToGetAttributeException e) {
				if (field.getFieldName().endsWith(SnapShotGenerator.DELTA_FIELD_SUFFIX) &&	
						FieldKey.DOUBLE_TYPE.equals(field.getFieldType())) {
					
					String attrName = field.getFieldName().replaceAll(SnapShotGenerator.DELTA_FIELD_SUFFIX + "$", "");
					try {
						Object tmp = BeanHelper.getValue(this, attrName);
						if (tmp instanceof Delta) {
							value = ((Delta)tmp).getDeltaPerSecond_object();
						}
					} catch (UnableToGetAttributeException e1) {
						logger.logError("", e1);
						// Nothing todo... We will log the original exception below.
					}
				} 
				if (value == null) {
					logger.logWarn("Unable to get attribute", e);
				}
			}
			if (value != null) {
				result.put(field, field.matchObjectToFieldType(value));
			}
		}
    	return result;
    }
}

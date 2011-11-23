/*
 *	Copyright 2011 Follett Software Company 
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

package org.perfmon4j.visualvm.chart;

import java.awt.Color;

import org.perfmon4j.remotemanagement.intf.FieldKey;

public class FieldElement {

    private final FieldKey fieldKey;
    private Color color;
    private float factor;
    private boolean visibleInChart = true;
    private boolean highlighted = false;

    public FieldElement(FieldKey fieldKey, float factor, Color color) {
        this.fieldKey = fieldKey;
        this.factor = factor;
        this.color = color;
    }

    public FieldKey getFieldKey() {
        return fieldKey;
    }

    public float getFactor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }
    
    public boolean isVisibleInChart() {
        return visibleInChart;
    }

    public void setVisibleInChart(boolean visibleInChart) {
        this.visibleInChart = visibleInChart;
    }
    
    public boolean isNumeric() {
        return isFieldNumeric(getFieldKey());
    }

    public static boolean isFieldNumeric(FieldKey fieldKey) {
        final String fieldType = fieldKey.getFieldType();
        return fieldType.equals(FieldKey.DOUBLE_TYPE)
                || fieldType.equals(FieldKey.LONG_TYPE)
                || fieldType.equals(FieldKey.INTEGER_TYPE);
    }

    public Color getColor() {
        return color;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }   
    
    public boolean isHighlighted() {
        return this.highlighted;
    }
}

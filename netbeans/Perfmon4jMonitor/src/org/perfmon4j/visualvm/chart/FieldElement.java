/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.perfmon4j.visualvm.chart;

import java.awt.Color;

import org.perfmon4j.remotemanagement.intf.FieldKey;

public class FieldElement {

    private final FieldKey fieldKey;
    private final Color color;
    private float factor;
    private boolean visibleInChart = true;

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
        final String fieldType = getFieldKey().getFieldType();
        return fieldType.equals(FieldKey.DOUBLE_TYPE)
                || fieldType.equals(FieldKey.LONG_TYPE)
                || fieldType.equals(FieldKey.INTEGER_TYPE);
    }

    public Color getColor() {
        return color;
    }
}

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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;
import org.perfmon4j.remotemanagement.intf.FieldKey;

public class DynamicTimeSeriesChart extends JPanel implements FieldManager.FieldHandler {

    private static final long serialVersionUID = 1L;
    private final TimeSeriesCollection dataset;
    private final XYItemRenderer renderer;
    private final int maxAgeInSeconds;
    private final Object elementLock = new Object();
    private final List<ElementWrapper> currentFields = new ArrayList<ElementWrapper>();
    private static int nextLabel = 0;

    public DynamicTimeSeriesChart(int maxAgeInSeconds) {
        super(new BorderLayout());
        this.maxAgeInSeconds = maxAgeInSeconds;

        dataset = new TimeSeriesCollection();
        renderer = new MyXYRenderer();
        renderer.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL));

        NumberAxis numberAxis = new NumberAxis();
        numberAxis.setAutoRange(false);
        numberAxis.setRange(new Range(0d, 100d));

        DateAxis dateAxis = new DateAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        dateAxis.setAutoRange(true);
        dateAxis.setTickUnit(new DateTickUnit(DateTickUnitType.SECOND, 30));


        XYPlot plot = new XYPlot(dataset, dateAxis, numberAxis, renderer);
        JFreeChart chart = new JFreeChart(null, null, plot, false);
        chart.setBackgroundPaint(Color.white);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), BorderFactory.createLineBorder(Color.black)));

        add(chartPanel);
    }

    static class TimeSeriesWithFactor extends TimeSeries {

        private float factor;

        public TimeSeriesWithFactor(Comparable c, Class clazz) {
            super(c, clazz);
            factor = 1.0f;
        }

        public float getFactor() {
            return factor;
        }

        public void setFactor(float factor) {
            this.factor = factor;
        }

        public Number adjustNumberBasedOnFactor(Number value) {
            Number result = value;
            if (value != null) {
                double dValue = value.doubleValue();
                dValue *= factor;

                dValue = Math.max(0.0d, dValue);
                result = new Double(Math.min(100.0, dValue));
            }
            return result;
        }
    }

    public static class MyXYRenderer extends XYLineAndShapeRenderer {

        MyXYRenderer() {
            super(true, false);
        }

        @Override
        public void drawItem(java.awt.Graphics2D g2,
                XYItemRendererState state,
                java.awt.geom.Rectangle2D dataArea,
                PlotRenderingInfo info,
                XYPlot plot,
                ValueAxis domainAxis,
                ValueAxis rangeAxis,
                XYDataset dataset,
                int series,
                int item,
                CrosshairState crosshairState,
                int pass) {

            TimeSeriesCollection col = (TimeSeriesCollection) dataset;
            TimeSeriesWithFactor s = (TimeSeriesWithFactor) col.getSeries(series);
        
            TimeSeriesDataItem dataItemCurrent = (TimeSeriesDataItem) s.getItems().get(item);
            TimeSeriesDataItem dataItemPrevious = null;
            if (item > 0) {
                dataItemPrevious = (TimeSeriesDataItem) s.getItems().get(item-1);
            }
            
            Number current = dataItemCurrent.getValue();
            Number previous = dataItemPrevious == null ? null : dataItemPrevious.getValue();
            try {
                dataItemCurrent.setValue(s.adjustNumberBasedOnFactor(current));
                if (dataItemPrevious != null) {
                    dataItemPrevious.setValue(s.adjustNumberBasedOnFactor(previous));
                }
                super.drawItem(g2,
                        state,
                        dataArea,
                        info,
                        plot,
                        domainAxis,
                        rangeAxis,
                        dataset,
                        series,
                        item,
                        crosshairState,
                        pass);
            } finally {
                dataItemCurrent.setValue(current);
                if (dataItemPrevious != null) { 
                    dataItemPrevious.setValue(previous);
                }
            }
        }
    }

    private ElementWrapper getWrapperForKey(FieldKey fieldKey) {
        ElementWrapper result = null;
        synchronized (elementLock) {
            Iterator<ElementWrapper> itr = currentFields.iterator();
            while (itr.hasNext() && result == null) {
                ElementWrapper w = itr.next();
                if (w.element.getFieldKey().equals(fieldKey)) {
                    result = w;
                }
            }
        }
        return result;
    }

    private ElementWrapper removeWrapperForKey(FieldKey fieldKey) {
        ElementWrapper result = null;
        synchronized (elementLock) {
            result = getWrapperForKey(fieldKey);
            if (result != null) {
                currentFields.remove(result);
            }
        }
        return result;
    }

    @Override
    public void addOrUpdateElement(FieldElement element) {
        if (element.isNumeric()) {
            synchronized (elementLock) {
                ElementWrapper wrapper = getWrapperForKey(element.getFieldKey());
                if (wrapper == null) {
                    // Adding a new field...
                    TimeSeriesWithFactor timeSeries = new TimeSeriesWithFactor(Integer.toString(nextLabel++), Second.class);
                    timeSeries.setMaximumItemAge(maxAgeInSeconds);

                    dataset.addSeries(timeSeries);
                    renderer.setSeriesPaint(dataset.getSeriesCount() - 1, element.getColor());
                    wrapper = new ElementWrapper(element, timeSeries);
                    currentFields.add(wrapper);
                } else {
                    // We are updating an existing field...  
                    wrapper.element = element;
                    // First reset the color...
                    renderer.setSeriesPaint(dataset.getSeries().indexOf(wrapper.timeSeries), element.getColor());
                }
            }
        }
    }

    @Override
    public void handleData(Map<FieldKey, Object> data) {
        synchronized (elementLock) {
            Iterator<FieldKey> itr = data.keySet().iterator();
            Second now = new Second();
            while (itr.hasNext()) {
                FieldKey field = itr.next();
                ElementWrapper wrapper = getWrapperForKey(field);
                if (wrapper != null) {
                    
//                    long value = 0;
//                    Object obj = data.get(field);
//                    if (obj != null && obj instanceof Number) {
//                        double dValue = ((Number) obj).doubleValue();
//                        dValue *= wrapper.element.getFactor();
//
//                        dValue = Math.max(0.0d, dValue);
//                        value = (long) Math.min(100.0, dValue);
//                    }
                    wrapper.timeSeries.setFactor(wrapper.element.getFactor());
                    wrapper.timeSeries.add(now, (Number)data.get(field));

                    int offset = dataset.getSeries().indexOf(wrapper.timeSeries);
                    renderer.setSeriesVisible(offset, Boolean.valueOf(wrapper.element.isVisibleInChart()));
                }
            }
        }
    }

    @Override
    public void removeElement(FieldElement element) {
        if (element.isNumeric()) {
            synchronized (elementLock) {
                ElementWrapper wrapper = removeWrapperForKey(element.getFieldKey());
                if (wrapper != null) {
                    // Adding a new field...
                    TimeSeries timeSeries = wrapper.timeSeries;
                    dataset.removeSeries(timeSeries);

                    // Must reset all of the colors
                    int index = 0;
                    Iterator<ElementWrapper> itr = currentFields.iterator();
                    while (itr.hasNext()) {
                        renderer.setSeriesPaint(index++, itr.next().element.getColor());
                    }
                }
            }
        }
    }

    private static class ElementWrapper {

        private FieldElement element;
        private TimeSeriesWithFactor timeSeries;

        ElementWrapper(FieldElement element, TimeSeriesWithFactor timeSeries) {
            this.element = element;
            this.timeSeries = timeSeries;
        }
    }
}

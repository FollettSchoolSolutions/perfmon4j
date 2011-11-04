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
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;

public class DynamicTimeSeriesChart extends JPanel implements FieldManager.FieldHandler {

    private static final long serialVersionUID = 1L;
    private final TimeSeriesCollection dataset;
    private final XYItemRenderer renderer;
    private final int maxAgeInSeconds;

    private static class ElementWrapper {

        private FieldElement element;
        private TimeSeries timeSeries;

        ElementWrapper(FieldElement element, TimeSeries timeSeries) {
            this.element = element;
            this.timeSeries = timeSeries;
        }
    }
    private final Object elementLock = new Object();
    private final Map<FieldKey, ElementWrapper> fieldsMap = new HashMap<FieldKey, ElementWrapper>();
    private static int nextLabel = 0;
    
    @Override
    public void addOrUpdateElement(FieldElement element) {
        if (element.isNumeric()) {
            synchronized (elementLock) {
                ElementWrapper wrapper = fieldsMap.get(element.getFieldKey());
                if (wrapper == null) {
                    // Adding a new field...
                    TimeSeries timeSeries = new TimeSeries(Integer.toString(nextLabel++), Second.class);
                    timeSeries.setMaximumItemAge(maxAgeInSeconds);
                    dataset.addSeries(timeSeries);
                    renderer.setSeriesPaint(dataset.getSeriesCount() - 1, element.getColor());
                    wrapper = new ElementWrapper(element, timeSeries);
                    fieldsMap.put(element.getFieldKey(), wrapper);
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
                ElementWrapper wrapper = fieldsMap.get(field);
                if (wrapper != null) {
                    long value = 0;
                    Object obj = data.get(field);
                    if (obj != null && obj instanceof Number) {
                        double dValue = ((Number) obj).doubleValue();
                        dValue *= wrapper.element.getFactor();

                        dValue = Math.max(0.0d, dValue);
                        value = (long) Math.min(100.0, dValue);
                    }
                    wrapper.timeSeries.add(now, value);
                    
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
                ElementWrapper wrapper = fieldsMap.remove(element.getFieldKey());
                if (wrapper != null) {
                    // Adding a new field...
                    TimeSeries timeSeries = wrapper.timeSeries;
                    dataset.removeSeries(timeSeries);

                    // Must reset all of the colors
                    int index = 0;
                    Iterator<ElementWrapper> itr = fieldsMap.values().iterator();
                    while (itr.hasNext()) {
                        renderer.setSeriesPaint(index++, itr.next().element.getColor());
                    }
                }
            }
        }
    }

    public DynamicTimeSeriesChart(int maxAgeInSeconds) {
        super(new BorderLayout());
        this.maxAgeInSeconds = maxAgeInSeconds;

        dataset = new TimeSeriesCollection();
        renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT,
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
    
//        JFreeChart chart = new JFreeChart(plot);
        chart.setBackgroundPaint(Color.white);
      

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), BorderFactory.createLineBorder(Color.black)));
        
        add(chartPanel);
    }

    private static FieldKey getFieldByName(RemoteManagementWrapper wrapper, String monitorName, String fieldName) throws Exception {
        MonitorKey monitorKey = null;
        FieldKey fieldKey = null;
        MonitorKey[] monitors = wrapper.getMonitors();

        for (int i = 0; i < monitors.length && monitorKey == null; i++) {
            if (monitors[i].getName().equals(monitorName)) {
                monitorKey = monitors[i];
            }
        }

        if (monitorKey != null) {
            FieldKey fields[] = wrapper.getFieldsForMonitor(monitorKey);
            for (int i = 0; i < fields.length && fieldKey == null; i++) {
                if (fields[i].getFieldName().equals(fieldName)) {
                    fieldKey = fields[i];
                }
            }
        }

        return fieldKey;
    }

    /**
     * Entry point for the sample application.
     * 
     * @param args
     *            ignored.
     */
    public static void main(String[] args) throws Exception {
        RemoteManagementWrapper wrapper = RemoteManagementWrapper.open("localhost", 5959);
        FieldManager manager = new FieldManager(wrapper, 1);
        manager.start();

        JFrame frame = new JFrame("Memory Usage Demo");

        DynamicTimeSeriesChart panel = new DynamicTimeSeriesChart(60);
        manager.addDataHandler(panel);




        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setBounds(200, 120, 600, 280);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        FieldKey fieldThroughputKey = getFieldByName(wrapper, "org.apache.catalina.connector.Request", "ThroughputPerMinute");
        manager.addOrUpdateField(new FieldElement(fieldThroughputKey, .01f, Color.RED));

        manager.addOrUpdateField(new FieldElement(fieldThroughputKey, .01f, Color.black));

        FieldKey fieldAverage = getFieldByName(wrapper, "org.apache.catalina.connector.Request", "AverageDuration");
        manager.addOrUpdateField(new FieldElement(fieldAverage, 1f, Color.RED));


//		manager.removeField(fieldThroughputKey);

    }
}

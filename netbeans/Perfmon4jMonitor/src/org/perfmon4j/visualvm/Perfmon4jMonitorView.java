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

package org.perfmon4j.visualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.visualvm.chart.ChartElementsTable;
import org.perfmon4j.visualvm.chart.DynamicTimeSeriesChart;
import org.perfmon4j.visualvm.chart.FieldElement;
import org.perfmon4j.visualvm.chart.FieldManager;
import org.perfmon4j.visualvm.chart.ThreadTraceList;
import org.perfmon4j.visualvm.chart.ThreadTraceTable;

/**
 *
 * @author ddeucher
 */
public class Perfmon4jMonitorView extends DataSourceView {
    private DataViewComponent dvc;
    //Reusing an image from the sources:
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/coredump/resources/coredump.png"; // NOI18N
    private final RemoteManagementWrapper wrapper;
    private final FieldManager fieldManager;
    private DynamicTimeSeriesChart chart;
    private ChartElementsTable table;
    private ThreadTraceTable threadTraceTable;
    
    public Perfmon4jMonitorView(Application app) {
        super(app, "Perfmon4j", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        wrapper = Perfmon4jModel.getModelForApp(app).getRemoteWrapper();
        fieldManager = new FieldManager(wrapper);
    }

    public static JFrame getParentFrame(Component c) {
        JFrame result = null;
        while (c != null && result == null) {
            if (c instanceof JFrame) {
                result = (JFrame)c;
            } else {
                c = c.getParent();
            }
        }
        return result;
    } 
 
    @Override
    protected DataViewComponent createComponent() {
        JPanel generalDataArea = new JPanel(new BorderLayout());
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        
        JButton addMonitorsButton = new JButton("Add Monitors...");
        addMonitorsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    FieldElement result = AEDMonitor.showModel(Perfmon4jMonitorView.getParentFrame(dvc.getParent()), wrapper);
                    if (result != null) {
                        fieldManager.addOrUpdateField(result);
//                      JOptionPane.showMessageDialog(null, "Selected field: " + result.getFieldKey());
                    }
                    
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        generalDataArea.add(addMonitorsButton, BorderLayout.LINE_END);
        int secondsToDisplay = 180; // 3 Minutes
        
        chart = new DynamicTimeSeriesChart(secondsToDisplay);
        fieldManager.addDataHandler(chart);
        
        table = new ChartElementsTable(fieldManager, secondsToDisplay);
        fieldManager.addDataHandler(table);
        
        threadTraceTable = new ThreadTraceTable(fieldManager.getThreadTraceList());
        
        final String CHART_VIEW = "Chart";
        final String THREAD_TRACE_VIEW = "Thread Trace View";
        final String DETAILS_VIEW = "Details";

        //Master view:
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView
                ("Perfmon4j Overview", "This is the master view description", generalDataArea);
        
        //Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration = 
                new DataViewComponent.MasterViewConfiguration(false);
        
        //Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);
      
        //Add configuration details to the component, which are the show/hide checkboxes at the top:
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                CHART_VIEW, true), DataViewComponent.TOP_LEFT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                THREAD_TRACE_VIEW, true), DataViewComponent.BOTTOM_RIGHT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                DETAILS_VIEW, true), DataViewComponent.BOTTOM_LEFT);

        //Add detail views to the component:
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                CHART_VIEW, null, 10, chart, null), DataViewComponent.TOP_LEFT);
        
        DataViewComponent.DetailsView d = new DataViewComponent.DetailsView(
                THREAD_TRACE_VIEW, null, 10, threadTraceTable, null);
        dvc.addDetailsView(d, DataViewComponent.TOP_RIGHT);
        
        
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                DETAILS_VIEW, null, 10, table, null), DataViewComponent.BOTTOM_RIGHT);

        return dvc;
    }

    @Override
    protected void added() {
        super.added();
        fieldManager.start();
    }

    @Override
    protected void removed() {
        fieldManager.stop();
        super.removed();
    }
}

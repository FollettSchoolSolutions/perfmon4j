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
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.openide.util.Utilities;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.visualvm.chart.ChartElementsTable;
import org.perfmon4j.visualvm.chart.DynamicTimeSeriesChart;
import org.perfmon4j.visualvm.chart.FieldManager;
import org.perfmon4j.visualvm.chart.ThreadTraceTable;

/**
 *
 * @author ddeucher
 */
public class Perfmon4jMonitorView extends DataSourceView {
    private DataViewComponent dvc;
    
    // TODO: Add a perfmon4j image...
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/coredump/resources/coredump.png"; // NOI18N
    
    private final RemoteManagementWrapper wrapper;
    private final FieldManager fieldManager;
    
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

    protected DataViewComponent createComponent() {
        JPanel generalDataArea = new JPanel(new BorderLayout());
        generalDataArea.setBorder(BorderFactory.createEmptyBorder());

        MainWindow window = new MainWindow(fieldManager, wrapper);
        
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView
                ("Perfmon4j Overview", "This is the master view description", generalDataArea);
        
        DataViewComponent.MasterViewConfiguration masterConfiguration = 
                new DataViewComponent.MasterViewConfiguration(false);
        
        dvc = new DataViewComponent(masterView, masterConfiguration);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "", false), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "", null, 10, window, null), DataViewComponent.TOP_LEFT);
        
        
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

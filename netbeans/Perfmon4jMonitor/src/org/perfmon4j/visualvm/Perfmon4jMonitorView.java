/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.perfmon4j.visualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.visualvm.chart.ChartElementsTable;
import org.perfmon4j.visualvm.chart.DynamicTimeSeriesChart;
import org.perfmon4j.visualvm.chart.FieldElement;
import org.perfmon4j.visualvm.chart.FieldManager;

/**
 *
 * @author ddeucher
 */
public class Perfmon4jMonitorView extends DataSourceView {
    private DataViewComponent dvc;
    //Reusing an image from the sources:
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/coredump/resources/coredump.png"; // NOI18N
    private final Application app;
    private final RemoteManagementWrapper wrapper;
    private final FieldManager fieldManager;
    private DynamicTimeSeriesChart chart;
    private ChartElementsTable table;
    
    
    public Perfmon4jMonitorView(Application app) {
        super(app, "Perfmon4j", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        this.app = app;
        wrapper = Perfmon4jModel.getModelForApp(app).getRemoteWrapper();
        fieldManager = new FieldManager(wrapper, 1);
    }

    @Override
    protected DataViewComponent createComponent() {
        JPanel generalDataArea = new JPanel(new BorderLayout());
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        
        JButton addMonitorsButton = new JButton("Add Monitors");
        addMonitorsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    FieldElement result = AEDMonitor.showModel(wrapper);
                    if (result != null) {
                        fieldManager.addOrUpdateField(result);
                        
//                        JOptionPane.showMessageDialog(null, "Selected field: " + result.getFieldKey());
                    }
                    
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        generalDataArea.add(addMonitorsButton, BorderLayout.LINE_END);
        int secondsToDisplay = 60*5;
        
        chart = new DynamicTimeSeriesChart(secondsToDisplay);
        fieldManager.addDataHandler(chart);
        
        table = new ChartElementsTable(fieldManager);
        fieldManager.addDataHandler(table);
        
        JPanel panelStatusMessages = new JPanel();
        
        final String CHART_VIEW = "Chart";
        final String STATUS_MESSAGES_VIEW = "Status Messages";
        final String DETAILS_VIEW = "Details";
        

        //Master view:
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView
                ("Perfmon4j Overview", null, generalDataArea);

        //Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration = 
                new DataViewComponent.MasterViewConfiguration(false);

        //Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        //Add configuration details to the component, which are the show/hide checkboxes at the top:
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                CHART_VIEW, true), DataViewComponent.TOP_LEFT);
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
//                STATUS_MESSAGES_VIEW, true), DataViewComponent.TOP_RIGHT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                DETAILS_VIEW, true), DataViewComponent.BOTTOM_RIGHT);

        //Add detail views to the component:
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                CHART_VIEW, null, 10, chart, null), DataViewComponent.TOP_LEFT);
        
//        dvc.addDetailsView(new DataViewComponent.DetailsView(
//                STATUS_MESSAGES_VIEW, null, 10, panelStatusMessages, null), DataViewComponent.TOP_RIGHT);
        
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

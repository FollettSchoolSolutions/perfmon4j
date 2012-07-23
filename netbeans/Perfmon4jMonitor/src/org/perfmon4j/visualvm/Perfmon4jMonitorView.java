/*
 *	Copyright 2011-2012 Follett Software Company 
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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.visualvm.chart.FieldElement;
import org.perfmon4j.visualvm.chart.FieldManager;

/**
 *
 * @author ddeucher
 */
public class Perfmon4jMonitorView extends DataSourceView {

    private DataViewComponent dvc;
    private static final String IMAGE_PATH = "org/perfmon4j/visualvm/gear.png"; // NOI18N
    private final RemoteManagementWrapper wrapper;
    private final FieldManager fieldManager;
    private final String pluginInfo;

    public Perfmon4jMonitorView(Application app) {
        super(app, "Perfmon4j", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        wrapper = Perfmon4jModel.getModelForApp(app).getRemoteWrapper();
        fieldManager = new FieldManager(wrapper);
        
        ModuleInfo info = Modules.getDefault().ownerOf(Perfmon4jModel.class);
       
        String name = info.getDisplayName();
        String buildVersion = info.getBuildVersion();
        String specVersion = info.getSpecificationVersion().toString();
        
        String tmp = name + " " + specVersion + " (" + buildVersion + ")";
        
        try {
            tmp +=  " -- Attached to Perfmon4j Agent Version: " + wrapper.getServerManagementVersion();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        pluginInfo = tmp;
    }

    public static JFrame getParentFrame(Component c) {
        JFrame result = null;
        while (c != null && result == null) {
            if (c instanceof JFrame) {
                result = (JFrame) c;
            } else {
                c = c.getParent();
            }
        }
        return result;
    }

    protected DataViewComponent createComponent() {
        JPanel generalDataArea = new JPanel(new BorderLayout());

        generalDataArea.add(new FileManager(fieldManager), BorderLayout.LINE_END);
        generalDataArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        MainWindow window = new MainWindow(fieldManager, wrapper);

        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView(pluginInfo, "This is the master view description", generalDataArea);

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

    private static final class FileManager extends JPanel implements FieldManager.FieldHandler {
        private final JButton loadButton = new JButton("Load");
        private final JButton saveButton = new JButton("Save");
        private final FieldManager fieldManager;
        private File currentFile = null;
        private final static String CONFIG_FILE_SUFFIX = ".p4j";
        private final File DEFAULT_FILE = new File("./save" + CONFIG_FILE_SUFFIX);

        FileManager(FieldManager fieldManager) {
            super(new FlowLayout());
            this.fieldManager = fieldManager;
            this.fieldManager.addDataHandler(this);
            saveButton.setEnabled(false);
           
            
            this.add(loadButton);
            this.add(saveButton);

            loadButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    doLoadButton(e);
                }
            });

            saveButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    doSaveButton(e);
                }
            });
        }
        
        
        private File appendExtensionIfMissing(File file) {
            File result = file;
            
            if (file != null) {
                String path = file.getPath();
                if (!path.endsWith(CONFIG_FILE_SUFFIX)) {
                    result = new File(path + CONFIG_FILE_SUFFIX);
                }
            }
            
            return result;
        }

        private void doSaveButton(ActionEvent e) {
            JFileChooser fc = new JFileChooser();

            fc.setSelectedFile(currentFile != null ? currentFile : DEFAULT_FILE);
            fc.setFileFilter(new P4JFileFilter());
           

            int result = fc.showSaveDialog(Perfmon4jMonitorView.getParentFrame((Component) e.getSource()));

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = appendExtensionIfMissing(fc.getSelectedFile());
                if (selectedFile.exists() && !selectedFile.equals(currentFile)) {
                    int response = JOptionPane.showConfirmDialog(null,
                            "Overwrite file?", "Confirm Overwrite",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (response != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                currentFile = selectedFile;
                Properties props = fieldManager.writeToProperies();
                FileOutputStream fileOut = null;
                try {
                    fileOut = new FileOutputStream(selectedFile);
                    props.storeToXML(fileOut, "Perfmon4j VisualVM monitor configuration");
                } catch (FileNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    if (fileOut != null) {
                        try { fileOut.close(); } catch (Exception ex) {}
                    }
                }
            }
        }

        private void doLoadButton(ActionEvent e) {
            JFileChooser fc = new JFileChooser();

            fc.setSelectedFile(currentFile != null ? currentFile : DEFAULT_FILE);
            fc.setFileFilter(new P4JFileFilter());
            
            int result = fc.showOpenDialog(Perfmon4jMonitorView.getParentFrame((Component) e.getSource()));
            if (result == JFileChooser.APPROVE_OPTION) {
                Properties props = new Properties();
                currentFile = fc.getSelectedFile();
                FileInputStream fileIn = null;
                try {
                    fileIn = new FileInputStream(currentFile);
                    props.loadFromXML(fileIn);
                    try {
                        boolean oneOrMoreSkipped = fieldManager.readFromProperties(props);
                        if (oneOrMoreSkipped) {
                            JOptionPane.showMessageDialog(Perfmon4jMonitorView.getParentFrame((Component) e.getSource()), 
                                "One or more fields could not be loaded because they do not exist in the target VM");
                        }
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } catch (FileNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    if (fileIn != null) {
                        try { fileIn.close(); } catch (Exception ex) {}
                    }
                }
            }
        }

        @Override
        public void handleData(Map<FieldKey, Object> data) {
        }

        @Override
        public void addOrUpdateElement(FieldElement element) {
            saveButton.setEnabled(fieldManager.getNumElements() > 0);
        }

        @Override
        public void removeElement(FieldElement element) {
            saveButton.setEnabled(fieldManager.getNumElements() > 0);
        }
        
        static class P4JFileFilter extends FileFilter {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(CONFIG_FILE_SUFFIX);
            }

            @Override
            public String getDescription() {
                return "Perfmon4j VisualVM monitor configuration";
            }
        }
    }
    
    
}

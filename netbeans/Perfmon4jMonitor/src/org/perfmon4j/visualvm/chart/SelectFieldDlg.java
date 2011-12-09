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

/*
 * AEDMonitor.java
 *
 * Created on Oct 31, 2011, 9:03:21 AM
 */
package org.perfmon4j.visualvm.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Arrays;
import org.openide.util.Exceptions;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.visualvm.MainWindow;

/**
 *
 * @author ddeucher
 */
public class SelectFieldDlg extends javax.swing.JDialog {

    private FieldKey selectedField = null;
    private final MainWindow mainWindow;

    /** Creates new form AEDMonitor */
    public SelectFieldDlg(MainWindow mainWindow) {
        super(mainWindow.getParentFrame(), "Select Field", true);
        this.mainWindow = mainWindow;
        initComponents();
    }

    private static class FieldWrapper implements Comparable<FieldWrapper> {

        final FieldKey field;

        FieldWrapper(FieldKey field) {
            this.field = field;
        }

        @Override
        public String toString() {
            return field.getFieldName();
        }

        @Override
        public int compareTo(FieldWrapper o) {
            return toString().compareTo(o.toString());
        }
    }

    public static void doSelectFieldForChart(MainWindow mainWindow, MonitorKey monitorKey) {
        try {
            FieldKey fields[] = mainWindow.getManagementWrapper().getFieldsForMonitor(monitorKey);
            FieldElement element = SelectFieldDlg.doSelectFieldForChart(mainWindow, fields);
            if (element != null) {
                mainWindow.getFieldManager().addOrUpdateField(element);
                if (element.isNumeric()) {
                    mainWindow.bringDetailsWindowToFront();
                } else {
                    mainWindow.bringTextFieldsWindowToFront();
                }
            }
        } catch (SessionNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (RemoteException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static FieldElement doSelectFieldForChart(MainWindow mainWindow, FieldKey fields[]) {
        FieldElement result = null;

        if (mainWindow.selectFieldDlg == null) {
            mainWindow.selectFieldDlg = new SelectFieldDlg(mainWindow);
        }
        final SelectFieldDlg dlg = mainWindow.selectFieldDlg;


        dlg.fieldsCombo.removeAllItems();;
        dlg.fieldsCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                FieldWrapper wrapper = (FieldWrapper) dlg.fieldsCombo.getSelectedItem();
                if (wrapper == null || !FieldElement.isFieldNumeric(wrapper.field)) {
                    dlg.factorCombo.setEnabled(false);
                    dlg.colorButton.setEnabled(false);
                    dlg.colorPanel.setEnabled(false);
                } else {
                    dlg.factorCombo.setEnabled(true);
                    dlg.colorButton.setEnabled(true);
                    dlg.colorPanel.setEnabled(true);
                }
            }
        });


        FieldWrapper[] wrappedFields = new FieldWrapper[fields.length];
        for (int i = 0; i < wrappedFields.length; i++) {
            wrappedFields[i] = new FieldWrapper(fields[i]);
        }

        Arrays.sort(wrappedFields);
        for (int i = 0; i < wrappedFields.length; i++) {
            FieldWrapper fieldWrapper = wrappedFields[i];
            dlg.fieldsCombo.addItem(fieldWrapper);
            if (fieldWrapper.toString().startsWith("Through")) {
                dlg.fieldsCombo.setSelectedItem(fieldWrapper);
            }
        }

        dlg.setDefaultCloseOperation(HIDE_ON_CLOSE);
        dlg.pack();
        dlg.setLocationRelativeTo(mainWindow.getParentFrame());
        dlg.setVisible(true);

        if (dlg.selectedField != null) {
            Float value = Float.parseFloat(dlg.factorCombo.getSelectedItem().toString());

            result = new FieldElement(dlg.selectedField, value,
                    dlg.colorPanel.getBackground());
        }

        return result;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        fieldsCombo = new javax.swing.JComboBox();
        factorCombo = new javax.swing.JComboBox();
        colorButton = new javax.swing.JButton();
        colorPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(SelectFieldDlg.class, "SelectFieldDlg.title")); // NOI18N

        fieldsCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fieldsComboActionPerformed(evt);
            }
        });

        factorCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "10000.0", "1000.0", "100.0", "10.0", "1", "0.1", "0.01", "0.001", "0.0001", "0.00001" }));
        factorCombo.setSelectedIndex(4);
        factorCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                factorComboActionPerformed(evt);
            }
        });

        colorButton.setText(org.openide.util.NbBundle.getMessage(SelectFieldDlg.class, "SelectFieldDlg.colorButton.text")); // NOI18N
        colorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorButtonActionPerformed(evt);
            }
        });

        colorPanel.setBackground(new java.awt.Color(51, 51, 255));
        colorPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        javax.swing.GroupLayout colorPanelLayout = new javax.swing.GroupLayout(colorPanel);
        colorPanel.setLayout(colorPanelLayout);
        colorPanelLayout.setHorizontalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 101, Short.MAX_VALUE)
        );
        colorPanelLayout.setVerticalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        okButton.setText(org.openide.util.NbBundle.getMessage(SelectFieldDlg.class, "SelectFieldDlg.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(org.openide.util.NbBundle.getMessage(SelectFieldDlg.class, "SelectFieldDlg.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jLabel1.setText(org.openide.util.NbBundle.getMessage(SelectFieldDlg.class, "SelectFieldDlg.jLabel1.text")); // NOI18N

        jLabel2.setText(org.openide.util.NbBundle.getMessage(SelectFieldDlg.class, "SelectFieldDlg.jLabel2.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fieldsCombo, 0, 319, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(colorButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(colorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(factorCombo, 0, 319, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldsCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(factorCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(colorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(colorButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void factorComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_factorComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_factorComboActionPerformed

    private void fieldsComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fieldsComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fieldsComboActionPerformed

    private void colorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorButtonActionPerformed
        Color color = SelectColorDlg.doSelectColor(mainWindow);
        if (color != null) {
            colorPanel.setBackground(color);
        }
    }//GEN-LAST:event_colorButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        FieldWrapper fw = (FieldWrapper) fieldsCombo.getSelectedItem();
        if (fw != null) {
            selectedField = fw.field;
        }
        this.setVisible(false);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        selectedField = null;
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton colorButton;
    private javax.swing.JPanel colorPanel;
    private javax.swing.JComboBox factorCombo;
    private javax.swing.JComboBox fieldsCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables
}

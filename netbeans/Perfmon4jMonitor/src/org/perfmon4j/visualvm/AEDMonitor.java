/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * AEDMonitor.java
 *
 * Created on Oct 31, 2011, 9:03:21 AM
 */
package org.perfmon4j.visualvm;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.visualvm.chart.FieldElement;

/**
 *
 * @author ddeucher
 */
public class AEDMonitor extends javax.swing.JDialog {

    private final RemoteManagementWrapper wrapper;
    private final MonitorModel monitorModel;
    private FieldKey selectedField = null;
    private String selectedLabel;
    private float selectedFactor = 1.0f;
    private Color selectedColor = Color.BLUE;

    /** Creates new form AEDMonitor */
    public AEDMonitor(java.awt.Frame parent, boolean modal, RemoteManagementWrapper wrapper) throws SessionNotFoundException, RemoteException {
        super(parent, modal);
        this.wrapper = wrapper;
        initComponents();
        monitorModel = new MonitorModel();
        monitorList.setModel(monitorModel);
        monitorList.addListSelectionListener(monitorModel);

    }

    private static class FieldWrapper {

        final FieldKey field;

        FieldWrapper(FieldKey field) {
            this.field = field;
        }

        @Override
        public String toString() {
            return field.getFieldName();
        }
    }

    private class MonitorModel extends AbstractListModel implements ListSelectionListener {

        private MonitorKey selectedKey = null;
        private final MonitorKey[] allKeys;
        private final List<MonitorKey> filteredKeys = new ArrayList<MonitorKey>();

        MonitorModel() throws SessionNotFoundException, RemoteException {
            this.allKeys = wrapper.getMonitors();
            Arrays.sort(allKeys);

            filteredKeys.addAll(Arrays.asList(allKeys));
        }

        public void setFilter(String filter) {
            filteredKeys.clear();
            if (filter == null || "".equals(filter)) {
                filteredKeys.addAll(Arrays.asList(allKeys));
            } else {
                for (int i = 0; i < allKeys.length; i++) {
                    MonitorKey monitorKey = allKeys[i];
                    if (monitorKey.getName().startsWith(filter)) {
                        filteredKeys.add(monitorKey);
                    }
                }
            }
            this.fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        @Override
        public int getSize() {
            return filteredKeys.size();
        }

        @Override
        public Object getElementAt(int index) {
            return filteredKeys.get(index).getName();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            selectedKey = filteredKeys.get(e.getFirstIndex());
            try {
                FieldKey fields[] = wrapper.getFieldsForMonitor(selectedKey);
                Arrays.sort(fields);
                int selectedIndex = 0;
                fieldsCombo.removeAllItems();;

                for (int i = 0; i < fields.length; i++) {
                    FieldKey fieldKey = fields[i];
                    if (fieldKey.getFieldName().startsWith("Throughput")) {
                        selectedIndex = i;
                    }
                    fieldsCombo.addItem(new FieldWrapper(fieldKey));
                }
                fieldsCombo.setSelectedIndex(selectedIndex);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
//        public MonitorKey getSelectedKey() {
//            return selectedKey;
//        }
//        
//        public FieldKey[] getFieldKeys() {
//            try {
//                return wrapper.getFieldsForMonitor(selectedKey);
//            } catch (SessionNotFoundException ex) {
//                throw new RuntimeException("stuff", ex);
//            } catch (RemoteException ex) {
//                throw new RuntimeException("stuff", ex);
//            }
//        }
    }

    public static FieldElement showModel(RemoteManagementWrapper wrapper) throws Exception {
        FieldElement result = null;
        AEDMonitor dialog = new AEDMonitor(new javax.swing.JFrame(), true, wrapper);

        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
        if (dialog.selectedField != null) {
            // String label, FieldKey fieldKey, float factor, Color colo
            result = new FieldElement(dialog.selectedField, dialog.selectedFactor, dialog.selectedColor);
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
        jScrollPane1 = new javax.swing.JScrollPane();
        monitorList = new javax.swing.JList();
        fieldsCombo = new javax.swing.JComboBox();
        factorCombo = new javax.swing.JComboBox();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        filterComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        colorComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        monitorList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(monitorList);

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

        okButton.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.okButton.text")); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jLabel1.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.jLabel1.text")); // NOI18N

        filterComboBox.setEditable(true);
        filterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterComboBoxActionPerformed(evt);
            }
        });

        jLabel2.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.jLabel2.text")); // NOI18N

        jLabel3.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.jLabel3.text")); // NOI18N

        jLabel4.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.jLabel4.text")); // NOI18N

        colorComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "black", "blue", "cyan", "dark gray", "gray", "green", "magenta", "orange", "red", "yellow" }));
        colorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorComboBoxActionPerformed(evt);
            }
        });

        jLabel6.setText(org.openide.util.NbBundle.getMessage(AEDMonitor.class, "AEDMonitor.jLabel6.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6))
                        .addGap(16, 16, 16)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fieldsCombo, 0, 362, Short.MAX_VALUE)
                            .addComponent(factorCombo, 0, 362, Short.MAX_VALUE)
                            .addComponent(colorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterComboBox, 0, 391, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addContainerGap(378, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelButton)
                        .addContainerGap(303, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(filterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(fieldsCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(factorCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(colorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private Color parseColorFromString(String color) {
        Color result = Color.blue;

        if (color.equals("black")) {
            result = Color.black;
        } else if (color.equals("cyan")) {
            result = Color.cyan;
        } else if (color.equals("dark gray")) {
            result = Color.darkGray;
        } else if (color.equals("gray")) {
            result = Color.gray;
        } else if (color.equals("green")) {
            result = Color.green;
        } else if (color.equals("magenta")) {
            result = Color.magenta;
        } else if (color.equals("orange")) {
            result = Color.orange;
        } else if (color.equals("red")) {
            result = Color.red;
        } else if (color.equals("yellow")) {
            result = Color.yellow;
        }

        return result;
    }

    private void factorComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_factorComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_factorComboActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        FieldWrapper fw = (FieldWrapper) fieldsCombo.getSelectedItem();
        if (fw != null) {
            selectedField = fw.field;
            selectedFactor = Float.parseFloat((String) factorCombo.getSelectedItem());
            selectedColor = parseColorFromString((String) colorComboBox.getSelectedItem());
        }
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        selectedField = null;
        this.dispose();;
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void fieldsComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fieldsComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fieldsComboActionPerformed

    private void filterComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterComboBoxActionPerformed
        monitorModel.setFilter(filterComboBox.getSelectedItem().toString());
    }//GEN-LAST:event_filterComboBoxActionPerformed

    private void colorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_colorComboBoxActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AEDMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AEDMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AEDMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AEDMonitor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    RemoteManagementWrapper wrapper = RemoteManagementWrapper.open("localhost",
                            5959);

                    AEDMonitor dialog = new AEDMonitor(new javax.swing.JFrame(), true, wrapper);
                    dialog.addWindowListener(new java.awt.event.WindowAdapter() {

                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            System.exit(0);
                        }
                    });
                    dialog.setVisible(true);
                } catch (SessionNotFoundException ex) {
                    ex.printStackTrace();
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                } catch (IncompatibleClientVersionException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox colorComboBox;
    private javax.swing.JComboBox factorCombo;
    private javax.swing.JComboBox fieldsCombo;
    private javax.swing.JComboBox filterComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList monitorList;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MainWindow.java
 *
 * Created on Nov 11, 2011, 4:02:24 PM
 */
package org.perfmon4j.visualvm;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.openide.util.Exceptions;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.visualvm.chart.ChartElementsTable;
import org.perfmon4j.visualvm.chart.DynamicTimeSeriesChart;
import org.perfmon4j.visualvm.chart.FieldElement;
import org.perfmon4j.visualvm.chart.FieldManager;
import org.perfmon4j.visualvm.chart.SelectFieldDlg;
import org.perfmon4j.visualvm.chart.ThreadTraceTable;

/**
 *
 * @author ddeucher
 */
public class MainWindow extends javax.swing.JPanel {

    private final FieldManager fieldManager;
    private final RemoteManagementWrapper managementWrapper;
    private DynamicTimeSeriesChart chart;
    private ChartElementsTable table;
    private ThreadTraceTable threadTraceTable;
    private MonitorKeyWrapper root = null;

    /** Creates new form MainWindow */
    public MainWindow(FieldManager fm, RemoteManagementWrapper wrapper) {
        this.fieldManager = fm;
        this.managementWrapper = wrapper;
        int secondsToDisplay = 180; // 3 Minutes

        initComponents();

        chart = new DynamicTimeSeriesChart(secondsToDisplay);
        fieldManager.addDataHandler(chart);
        topRightPanel.add(chart);


        table = new ChartElementsTable(fieldManager, secondsToDisplay);
        fieldManager.addDataHandler(table);
        detailsPanel.add(table);

        threadTraceTable = new ThreadTraceTable(fieldManager);
        threadTraceListPanel.add(threadTraceTable);

        DefaultTreeModel model = (DefaultTreeModel) monitorFieldTree.getModel();
        root = new MonitorKeyWrapper(model, null, "Interval Monitors");
        model.setRoot(root);

        MouseAdapter ma = new MouseAdapter() {

            private void doPopup(MouseEvent e) {
                JTree tree = (JTree) e.getSource();
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    tree.setSelectionPath(path);
                    final MonitorKeyWrapper wrapper = (MonitorKeyWrapper) path.getLastPathComponent();
                    JPopupMenu popup = new JPopupMenu();

                    JMenuItem addFieldToChart = new JMenuItem("Add Field to Chart...");
                    addFieldToChart.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                FieldKey fields[] = managementWrapper.getFieldsForMonitor(wrapper.getMonitorKey());
                                FieldElement element = SelectFieldDlg.doSelectFieldForChart((Component) e.getSource(), fields);
                                if (element != null) {
                                    fieldManager.addOrUpdateField(element);
                                }
                            } catch (SessionNotFoundException ex) {
                                Exceptions.printStackTrace(ex);
                            } catch (RemoteException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    });
                    popup.add(addFieldToChart);

                    JMenuItem threadTrace = new JMenuItem("Schedule Thread Trace...");
                    threadTrace.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ThreadTraceOptions.getOptionsAndScheduleThreadTrace((Component) e.getSource(),
                                    fieldManager, wrapper.getMonitorKey());
                        }
                    });
                    popup.add(threadTrace);
                    popup.show(tree, e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doPopup(e);
                }
            }
        };
        monitorFieldTree.addMouseListener(ma);

        refreshMonitors();

        int numRootChildren = root.getChildCount();
        for (int i = 0; i < numRootChildren; i++) {
            monitorFieldTree.expandPath(new TreePath(
                    model.getPathToRoot(root.getChildAt(i))));
        }
    }

    private void refreshMonitors() {
        try {
            MonitorKey keys[] = managementWrapper.getMonitors();
            Arrays.sort(keys);
            for (int i = 0; i < keys.length; i++) {
                MonitorKey keyToAdd = keys[i];
                String str[] = parseIntoArray(keyToAdd.getName());
                root.addMonitor(keyToAdd, str, -1);
            }
        } catch (SessionNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (RemoteException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static String[] parseIntoArray(String value) {
        List<String> result = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(value, ".");
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken());
        }
        return result.toArray(new String[result.size()]);
    }

    static class MonitorKeyWrapper implements TreeNode {

        private final MonitorKeyWrapper parent;
        private final DefaultTreeModel model;
        private final String name;
        private final List<String> childNames = new ArrayList<String>();
        private final Map<String, MonitorKeyWrapper> children = new HashMap<String, MonitorKeyWrapper>();
        private MonitorKey monitorKey;

        MonitorKeyWrapper(DefaultTreeModel model, MonitorKeyWrapper parent, String name) {
            this.parent = parent;
            this.name = name;
            this.model = model;
        }

        void addMonitor(MonitorKey key, String[] parsedName, int offset) {
            if (parsedName.length == (offset + 1)) {
                monitorKey = key;
            } else {
                offset++;
                String childName = parsedName[offset];
                MonitorKeyWrapper child = children.get(childName);
                if (child == null) {
                    child = new MonitorKeyWrapper(model, this, childName);
                    childNames.add(childName);
                    children.put(childName, child);
                    //TODO: Insert child so we retain alpha order!
                    model.nodesWereInserted(this, new int[]{childNames.indexOf(childName)});
                }
                child.addMonitor(key, parsedName, offset);
            }
        }

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        MonitorKey getMonitorKey() {
            return monitorKey;
        }

        @Override
        public TreeNode getChildAt(int childIndex) {
            MonitorKeyWrapper result = null;

            String childName = childNames.get(childIndex);
            result = children.get(childName);

            return result;
        }

        @Override
        public int getChildCount() {
            return childNames.size();
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        @Override
        public int getIndex(TreeNode node) {
            return childNames.indexOf(((MonitorKeyWrapper) node).getName());
        }

        @Override
        public boolean getAllowsChildren() {
            return !isLeaf();
        }

        @Override
        public boolean isLeaf() {
            return getChildCount() == 0;
        }

        @Override
        public Enumeration children() {
            return new WrapperEnumeration(this);
        }

        static class WrapperEnumeration implements Enumeration<TreeNode> {

            int currentOffset = 0;
            final MonitorKeyWrapper monitorKeyWrapper;

            public WrapperEnumeration(MonitorKeyWrapper monitorKeyWrapper) {
                this.monitorKeyWrapper = monitorKeyWrapper;
            }

            @Override
            public boolean hasMoreElements() {
                return monitorKeyWrapper.getChildCount() < currentOffset;
            }

            @Override
            public TreeNode nextElement() {
                return monitorKeyWrapper.getChildAt(currentOffset++);
            }
        }
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        baseSplitPane = new javax.swing.JSplitPane();
        leftPanel = new javax.swing.JPanel();
        monitorTreeRefreshButton = new javax.swing.JButton();
        monitorTreeScrollPane = new javax.swing.JScrollPane();
        monitorFieldTree = new javax.swing.JTree();
        rightSplitPane = new javax.swing.JSplitPane();
        topRightPanel = new javax.swing.JPanel();
        bottomRightTabbedPanel = new javax.swing.JTabbedPane();
        detailsPanel = new javax.swing.JPanel();
        threadTraceListPanel = new javax.swing.JPanel();
        threadTraceDetailsPanel = new javax.swing.JPanel();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        baseSplitPane.setDividerLocation(180);

        monitorTreeRefreshButton.setText(org.openide.util.NbBundle.getMessage(MainWindow.class, "MainWindow.monitorTreeRefreshButton.text")); // NOI18N
        monitorTreeRefreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monitorTreeRefreshButtonActionPerformed(evt);
            }
        });

        monitorFieldTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                monitorFieldTreeMouseClicked(evt);
            }
        });
        monitorFieldTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                monitorFieldTreeValueChanged(evt);
            }
        });
        monitorTreeScrollPane.setViewportView(monitorFieldTree);

        javax.swing.GroupLayout leftPanelLayout = new javax.swing.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(monitorTreeRefreshButton)
                .addContainerGap())
            .addComponent(monitorTreeScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
        );
        leftPanelLayout.setVerticalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, leftPanelLayout.createSequentialGroup()
                .addComponent(monitorTreeScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(monitorTreeRefreshButton)
                .addContainerGap())
        );

        baseSplitPane.setLeftComponent(leftPanel);

        rightSplitPane.setDividerLocation(250);
        rightSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        topRightPanel.setLayout(new java.awt.BorderLayout());
        rightSplitPane.setTopComponent(topRightPanel);

        detailsPanel.setLayout(new java.awt.BorderLayout());
        bottomRightTabbedPanel.addTab(org.openide.util.NbBundle.getMessage(MainWindow.class, "MainWindow.detailsPanel.TabConstraints.tabTitle"), detailsPanel); // NOI18N

        threadTraceListPanel.setLayout(new java.awt.BorderLayout());
        bottomRightTabbedPanel.addTab(org.openide.util.NbBundle.getMessage(MainWindow.class, "MainWindow.threadTraceListPanel.TabConstraints.tabTitle"), threadTraceListPanel); // NOI18N

        threadTraceDetailsPanel.setLayout(new java.awt.BorderLayout());
        bottomRightTabbedPanel.addTab(org.openide.util.NbBundle.getMessage(MainWindow.class, "MainWindow.threadTraceDetailsPanel.TabConstraints.tabTitle"), threadTraceDetailsPanel); // NOI18N

        rightSplitPane.setRightComponent(bottomRightTabbedPanel);

        baseSplitPane.setRightComponent(rightSplitPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(baseSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 591, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(baseSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void monitorTreeRefreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monitorTreeRefreshButtonActionPerformed
        refreshMonitors();
    }//GEN-LAST:event_monitorTreeRefreshButtonActionPerformed

    private void monitorFieldTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_monitorFieldTreeValueChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_monitorFieldTreeValueChanged

    private void monitorFieldTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_monitorFieldTreeMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_monitorFieldTreeMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSplitPane baseSplitPane;
    private javax.swing.JTabbedPane bottomRightTabbedPanel;
    private javax.swing.JPanel detailsPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JTree monitorFieldTree;
    private javax.swing.JButton monitorTreeRefreshButton;
    private javax.swing.JScrollPane monitorTreeScrollPane;
    private javax.swing.JSplitPane rightSplitPane;
    private javax.swing.JPanel threadTraceDetailsPanel;
    private javax.swing.JPanel threadTraceListPanel;
    private javax.swing.JPanel topRightPanel;
    // End of variables declaration//GEN-END:variables
}

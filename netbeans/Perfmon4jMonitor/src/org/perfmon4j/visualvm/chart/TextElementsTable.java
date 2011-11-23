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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.openide.awt.MouseUtils.PopupMouseAdapter;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.visualvm.MainWindow;
import org.perfmon4j.visualvm.ThreadTraceOptionsDlg;

public class TextElementsTable extends JPanel implements
        FieldManager.FieldHandler {

    private static final long serialVersionUID = 1L;
    private final Object elementLock = new Object();
    private final TableModel tableModel;
    private final TableColumnModel columnModel;
    private final MainWindow mainWindow;
    private final JTable table;

    public TextElementsTable(MainWindow mainWindow) {
        super(new BorderLayout());
        tableModel = new TableModel();
        this.mainWindow = mainWindow;

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(175);
        columnModel.getColumn(1).setPreferredWidth(125);
        columnModel.getColumn(2).setPreferredWidth(175);
        columnModel.getColumn(3).setPreferredWidth(175);

        final String rightOptions = "Right click for options...";
        DefaultTableCellRenderer rightClick = new DefaultTableCellRenderer();
        rightClick.setToolTipText(rightOptions);
        for (int i = 0; i < 3; i++) {
            TableColumn c = table.getColumnModel().getColumn(i);
            c.setCellRenderer(rightClick);
        }
        JScrollPane scroller = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.add(scroller);
        table.addMouseListener(new TableMouseAdapter());
    }

    private class TableMouseAdapter extends PopupMouseAdapter {
        @Override
        public void showPopup(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            final int row =  table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.getSelectionModel().setSelectionInterval(row, row);
                JPopupMenu popup = new JPopupMenu();

                JMenuItem threadTrace = new JMenuItem("Schedule Thread Trace...");
                threadTrace.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ElementWrapper elm = backingData.get(row);
                        ThreadTraceOptionsDlg.doScheduleThreadTrace(mainWindow, 
                                elm.fieldElement.getFieldKey().getMonitorKey());
                    }
                });
                popup.add(threadTrace);
                popup.add(new JPopupMenu.Separator());
                
                JMenuItem remove = new JMenuItem("Remove");
                remove.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ElementWrapper elm = backingData.get(row);
                        final FieldKey fieldToDelete = elm.fieldElement.getFieldKey();

                        String message = "Are you sure you want to delete Monitor: \""
                                + fieldToDelete.getMonitorKey().getName() + "\" Field: \""
                                + fieldToDelete.getFieldName() + "\"?";

                        if (JOptionPane.showConfirmDialog((Component) e.getSource(), message, "Delete",
                                JOptionPane.YES_NO_OPTION)
                                == JOptionPane.YES_OPTION) {
                            mainWindow.getFieldManager().removeField(elm.fieldElement);
                        }
                    }
                });
                popup.add(remove);
                popup.show(table, e.getX(), e.getY());
            }
        }
    }
    
    private static class ElementWrapper {
        final FieldElement fieldElement;
        String currentValue;
        String previousValue;
        
        ElementWrapper(FieldElement fieldElement) {
            this.fieldElement = fieldElement;
        }
        
        boolean updateCurrentValue(String newValue) {
            boolean changed = false;
            
            if (!newValue.equals(currentValue)) {
                previousValue = currentValue;
                currentValue = newValue;
                changed = true;
            }
            
            return  changed;
        }
    }
    
    
    private final Map<FieldKey, ElementWrapper> fieldsMap = new HashMap<FieldKey, ElementWrapper>();
    private final List<ElementWrapper> backingData = new ArrayList<ElementWrapper>();

    @Override
    public void addOrUpdateElement(FieldElement fieldElement) {
        int insertedRow = -1;
        int updatedRow = -1;

        if (!fieldElement.isNumeric()) {
            synchronized (elementLock) {
                ElementWrapper w = fieldsMap.get(fieldElement.getFieldKey());
                if (w == null) {
                    w = new ElementWrapper(fieldElement);
                    fieldsMap.put(fieldElement.getFieldKey(), w);
                    backingData.add(w);
                    insertedRow = backingData.indexOf(w);
                } else {
                    updatedRow = backingData.indexOf(w);
                }
            }
            if (insertedRow >= 0) {
                tableModel.fireTableRowsInserted(insertedRow, insertedRow);
            }
            if (updatedRow >= 0) {
                tableModel.fireTableRowsUpdated(updatedRow, updatedRow);
            }
        }
    }

    final private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    
    @Override
    public void handleData(Map<FieldKey, Object> data) {
        synchronized (elementLock) {
            Iterator<FieldKey> itr = data.keySet().iterator();
            while (itr.hasNext()) {
                FieldKey field = itr.next();
                ElementWrapper w = fieldsMap.get(field);
                if (w != null) {
                    Object value = data.get(field);
                    if (value != null && (FieldKey.TIMESTAMP_TYPE.equals(field.getFieldType()))) {
                        value = dateFormat.format(new Date(((Number)(value)).longValue()));
                    }                    
                    if (w.updateCurrentValue(value == null ? "" : value.toString())) {
                        int row = backingData.indexOf(w);
                        tableModel.fireTextFieldUpdatedForRow(row);
                    }
                }
            }
        }
    }

    @Override
    public void removeElement(FieldElement element) {
        if (!element.isNumeric()) {
            int row = -1;
            synchronized (elementLock) {
                ElementWrapper w = fieldsMap.remove(element.getFieldKey());
                if (w != null) {
                    row = backingData.indexOf(w);
                    backingData.remove(row);
                }
            }
            if (row >= 0) {
                tableModel.fireTableRowsDeleted(row, row);
            }
        }
    }

    private static final String[] HEADER_VALUE = new String[]{
        "Monitor Name", // 0
        "Field Name", // 1
        "Current Value", // 2
        "Previous Value", // 3
    };

    private class TableModel extends AbstractTableModel {

        @Override
        public Class<?> getColumnClass(int column) {
            return super.getColumnClass(column);
        }

        @Override
        public String getColumnName(int columnIndex) {
            return HEADER_VALUE[columnIndex];
        }

        @Override
        public int getColumnCount() {
            // TODO Auto-generated method stub
            return HEADER_VALUE.length;
        }

        @Override
        public int getRowCount() {
            synchronized (elementLock) {
                // TODO Auto-generated method stub
                return backingData.size();
            }
        }

        public void fireTextFieldUpdatedForRow(int row) {
            tableModel.fireTableCellUpdated(row, 2);
            tableModel.fireTableCellUpdated(row, 3);
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ElementWrapper w = null;

            synchronized (elementLock) {
                int offset = rowIndex;
                if (offset < backingData.size()) {
                     w = backingData.get(offset);
                }
            }
            if (w != null) {
                switch (columnIndex) {
                    case 0: {
                        MonitorKey key = w.fieldElement.getFieldKey().getMonitorKey();
                        String instanceName = key.getInstance();
                        if (instanceName == null || "".equals(instanceName)) {
                            return w.fieldElement.getFieldKey().getMonitorKey().getName();
                        } else {
                            return w.fieldElement.getFieldKey().getMonitorKey().getName() + "("
                                    + instanceName + ")";
                        }
                    }
                    case 1:
                        return w.fieldElement.getFieldKey().getFieldName();
                    case 2:
                        return w.currentValue;
                    case 3:
                        return w.previousValue;
                }
            }

            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
}
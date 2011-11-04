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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;

public class ChartElementsTable extends JPanel implements
        FieldManager.FieldHandler {

    private static final long serialVersionUID = 1L;
    private final Object elementLock = new Object();
    private final TableModel tableModel;
    private final TableColumnModel columnModel;
    private final FieldManager manager;
    private final JTable table;
    
    private static class DataElement {

        Number[] last5 = new Number[5];
        int currentOffset = 0;
        Number maxValue = null;
        Number minValue = null;
        Number lastValue = null;

        synchronized void setLastValue(Number lastValue) {
            this.lastValue = lastValue;
            if (minValue == null || minValue.doubleValue() > lastValue.doubleValue()) {
                minValue = lastValue;
            }
            if (maxValue == null || maxValue.doubleValue() < lastValue.doubleValue()) {
                maxValue = lastValue;
            }
            last5[currentOffset++] = lastValue;
            if (currentOffset >= last5.length) {
                currentOffset = 0;
            }
        }

        synchronized Number getAverage() {
            double total = 0.0d;
            int numValues = 0;
            for (int i = 0; i < last5.length; i++) {
                Number x = last5[i];
                if (x != null) {
                    numValues++;
                    total += x.doubleValue();
                }
            }
            if (numValues > 0) {
                return Double.valueOf(total / numValues);
            } else {
                return Double.valueOf(0.0d);
            }
        }
    }

    private static class ElementWrapper {

        private FieldElement element;
        private DataElement data;

        ElementWrapper(FieldElement element) {
            this.element = element;
            this.data = new DataElement();
        }
    }
    private final Map<FieldKey, ElementWrapper> fieldsMap = new HashMap<FieldKey, ElementWrapper>();
    private final List<ElementWrapper> backingData = new ArrayList<ElementWrapper>();

    public void addOrUpdateElement(FieldElement element) {
        int insertedRow = -1;
        int updatedRow = -1;

        if (element.isNumeric()) {
            synchronized (elementLock) {
                ElementWrapper wrapper = fieldsMap.get(element.getFieldKey());
                if (wrapper == null) {
                    wrapper = new ElementWrapper(element);
                    fieldsMap.put(element.getFieldKey(), wrapper);
                    backingData.add(wrapper);
                    insertedRow = backingData.indexOf(wrapper);
                } else {
                    wrapper.element = element;
                    updatedRow = backingData.indexOf(wrapper);
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

    @Override
    public void handleData(Map<FieldKey, Object> data) {
        synchronized (elementLock) {
            Iterator<FieldKey> itr = data.keySet().iterator();
            while (itr.hasNext()) {
                FieldKey field = itr.next();
                ElementWrapper wrapper = fieldsMap.get(field);
                if (wrapper != null) {
                    Number value = new Double(0.0d);
                    Object obj = data.get(field);
                    if (obj != null && obj instanceof Number) {
                        value = (Number) obj;
                    }
                    wrapper.data.setLastValue(value);
                }
            }
        }
        // TODO -- Should be more elegant to prevent flicker!
        tableModel.fireTableDataChanged();
    }

    @Override
    public void removeElement(FieldElement element) {
        if (element.isNumeric()) {
            int row = -1;
            synchronized (elementLock) {
                ElementWrapper wrapper = fieldsMap.remove(element.getFieldKey());
                if (wrapper != null) {
                    row = backingData.indexOf(wrapper);
                    backingData.remove(row);
                }
            }
            if (row >= 0) {
                tableModel.fireTableRowsDeleted(row, row);
            }
        }
    }

    private static Number roundIfNeeded(Number value) {
        if (value != null
                && (value instanceof Float || value instanceof Double)) {
            double v = value.doubleValue();
            value = new Double((double) Math.round(v * 100) / 100);
        }

        return value;
    }
    private static final String[] HEADER_VALUE = new String[]{
        "",             // 0
        "Monitor Name", // 1 
        "Field Name",   // 2
        "Last Value",   // 3
        "Avg(last 5)",  // 4
        "Max Value",    // 5
        "Min Value",    // 6
        "Factor",       // 7
        "Visible"};     // 8

    private class TableModel extends AbstractTableModel {

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 0) {
                return Color.class;
            } else if (column == 8) {
                return Boolean.class;
            } else if (column >= 3 && column < 8) {
                return Number.class;
            } else {
                return super.getColumnClass(column);
            }
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
                    case 0:
                        return w.element.getColor();

                    case 1:
                        return w.element.getFieldKey().getMonitorKey().getName();

                    case 2:
                        return w.element.getFieldKey().getFieldName();

                    case 3:
                        return roundIfNeeded(w.data.lastValue);
                    case 4:
                        return roundIfNeeded(w.data.getAverage());
                    case 5:
                        return roundIfNeeded(w.data.maxValue);
                    case 6:
                        return roundIfNeeded(w.data.minValue);
                    case 7:
                        return new Float(w.element.getFactor());
                    case 8:
                        return Boolean.valueOf(w.element.isVisibleInChart());
                }
            }

            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col == 0 || col == 7 || col == 8) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 7 || col == 8) {
                ElementWrapper w = null;
                synchronized (elementLock) {
                    if (row < backingData.size()) {
                        w = backingData.get(row);
                    }
                }
                if (w != null) {
                    if (col == 7) {
                        w.element.setFactor(((Float) value).floatValue());
                    } else {
                        w.element.setVisibleInChart(((Boolean) value).booleanValue());
                    }
                }
            }
        }
    }

    public class ColorRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component cell = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            if (value instanceof Color) {
                cell.setBackground((Color) value);
                cell.setForeground((Color) value);
            }
            return cell;
        }
    }

    private class DeleteButtonActionListener implements ActionListener {
        final int row;

        DeleteButtonActionListener(int row) {
            this.row = row;
        }

        public void actionPerformed(ActionEvent e) {
            final FieldKey fieldToDelete = backingData.get(row).element.getFieldKey();

            String message = "Are you sure you want to delete Monitor: \""
                    + fieldToDelete.getMonitorKey().getName() + "\" Field: \""
                    + fieldToDelete.getFieldName() + "\"?";

            if (JOptionPane.showConfirmDialog((Component) e.getSource(), message, "Delete",
                    JOptionPane.YES_NO_OPTION)
                    == JOptionPane.YES_OPTION) {
                manager.removeField(backingData.get(row).element.getFieldKey());
            }
            table.editingStopped(new ChangeEvent(this));
        }
    }

    public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            JButton result = new JButton();
            result.addActionListener(new DeleteButtonActionListener(row));
            result.setBackground((Color)value);
            result.setForeground(Color.white);

            return result;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    public ChartElementsTable(FieldManager manager) {
        super(new BorderLayout());
        tableModel = new TableModel();
        this.manager = manager;
        

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(Color.class, new ColorRenderer());

        columnModel = table.getColumnModel();
        // columnModel.getColumn(0).setPreferredWidth(25);
        columnModel.getColumn(1).setPreferredWidth(350);
        columnModel.getColumn(2).setPreferredWidth(250);

        TableColumn factorColumn = table.getColumnModel().getColumn(7);
        JComboBox comboBox = new JComboBox();

        comboBox.addItem(Float.valueOf(100000.0f));
        comboBox.addItem(Float.valueOf(10000.0f));
        comboBox.addItem(Float.valueOf(1000.0f));
        comboBox.addItem(Float.valueOf(100.0f));
        comboBox.addItem(Float.valueOf(10.0f));
        comboBox.addItem(Float.valueOf(1.0f));
        comboBox.addItem(Float.valueOf(0.1f));
        comboBox.addItem(Float.valueOf(0.01f));
        comboBox.addItem(Float.valueOf(0.001f));
        comboBox.addItem(Float.valueOf(0.0001f));
        comboBox.addItem(Float.valueOf(0.00001f));
        comboBox.addItem(Float.valueOf(0.000001f));
        factorColumn.setCellEditor(new DefaultCellEditor(comboBox));

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setToolTipText("Click to edit");
        factorColumn.setCellRenderer(renderer);

        TableColumn colorColumn = table.getColumnModel().getColumn(0);
        colorColumn.setCellEditor(new ButtonCellEditor());
        
        JScrollPane scroller = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.add(scroller);
    }

    private static FieldKey getFieldByName(RemoteManagementWrapper wrapper,
            String monitorName, String fieldName) throws Exception {
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
 }
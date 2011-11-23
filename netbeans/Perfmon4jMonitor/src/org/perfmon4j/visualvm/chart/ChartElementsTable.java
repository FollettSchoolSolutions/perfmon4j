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
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.openide.awt.MouseUtils.PopupMouseAdapter;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.visualvm.MainWindow;
import org.perfmon4j.visualvm.ThreadTraceOptionsDlg;

public class ChartElementsTable extends JPanel implements
        FieldManager.FieldHandler {

    private static final long serialVersionUID = 1L;
    private final int secondsToDisplay;
    private final Object elementLock = new Object();
    private final TableModel tableModel;
    private final TableColumnModel columnModel;
    private final MainWindow mainWindow;
    private final JTable table;

    public ChartElementsTable(MainWindow mainWindow, int secondsToDisplay) {
        super(new BorderLayout());
        tableModel = new TableModel();
        this.mainWindow = mainWindow;
        this.secondsToDisplay = secondsToDisplay;

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        ListSelectionModel list = table.getSelectionModel();
        list.addListSelectionListener(new ListSelection());

        columnModel = table.getColumnModel();
        // columnModel.getColumn(0).setPreferredWidth(25);
        columnModel.getColumn(1).setPreferredWidth(175);
        columnModel.getColumn(2).setPreferredWidth(125);

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
        renderer.setToolTipText("Click to edit...");
        factorColumn.setCellRenderer(renderer);

        final String rightOptions = "Right click for options...";
        DefaultTableCellRenderer rightClick = new DefaultTableCellRenderer();
        rightClick.setToolTipText(rightOptions);
        for (int i = 1; i < 6; i++) {
            TableColumn c = table.getColumnModel().getColumn(i);
            c.setCellRenderer(rightClick);
        }

        table.setDefaultRenderer(Color.class, new ColorRenderer(rightOptions));
        
        JScrollPane scroller = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.add(scroller);
        table.addMouseListener(new TableMouseAdapter());
        
        
    }


    private class ListSelection implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                ListSelectionModel m = (ListSelectionModel)e.getSource();
                synchronized(elementLock) {
                    Iterator<ElementWrapper> itr =  backingData.iterator();
                    int offset = 0;
                    int selectedRow = m.getMaxSelectionIndex();
                    while (itr.hasNext()) {
                        itr.next().element.setHighlighted((offset++ == selectedRow));
                    }
                }
            }
            
        }
    }
    
    private class TableMouseAdapter extends PopupMouseAdapter {

        @Override
        public void showPopup(MouseEvent e) {
            JTable table = (JTable) e.getSource();
            final int row =  table.rowAtPoint(e.getPoint());
            if (row >= 0) {
                table.getSelectionModel().setSelectionInterval(row, row);
                JPopupMenu popup = new JPopupMenu();
                
                JMenuItem changeColor = new JMenuItem("Change Color...");
                changeColor.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Color color = SelectColorDlg.doSelectColor(mainWindow);
                        if (color != null) {
                            FieldElement elm = backingData.get(row).element;
                            elm.setColor(color);
                            mainWindow.getFieldManager().addOrUpdateField(elm);
                        }
                    }
                });
                popup.add(changeColor);

                JMenuItem threadTrace = new JMenuItem("Schedule Thread Trace...");
                threadTrace.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        FieldElement elm = backingData.get(row).element;
                        ThreadTraceOptionsDlg.doScheduleThreadTrace(mainWindow, 
                                elm.getFieldKey().getMonitorKey());
                    }
                });
                popup.add(threadTrace);
                popup.add(new JPopupMenu.Separator());
                
                JMenuItem remove = new JMenuItem("Remove");
                remove.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        FieldElement elm = backingData.get(row).element;
                        final FieldKey fieldToDelete = elm.getFieldKey();

                        String message = "Are you sure you want to delete Monitor: \""
                                + fieldToDelete.getMonitorKey().getName() + "\" Field: \""
                                + fieldToDelete.getFieldName() + "\"?";

                        if (JOptionPane.showConfirmDialog((Component) e.getSource(), message, "Delete",
                                JOptionPane.YES_NO_OPTION)
                                == JOptionPane.YES_OPTION) {
                            mainWindow.getFieldManager().removeField(elm);
                        }
                    }
                });
                popup.add(remove);
                
                popup.show(table, e.getX(), e.getY());
            }
        }
    }
    
    private static class NumberElement {

        private final long time;
        private final Number number;

        NumberElement(long now, Number number) {
            this.time = now;
            this.number = number;
        }
    }

    private class DataElement {

        private final Object samplesLockToken = new Object();
        List<NumberElement> samples = new ArrayList<NumberElement>();
        Number maxValue = null;
        Number minValue = null;
        Number lastValue = null;
        Number avgValue = null;

        void setLastValue(Number lastValue) {
            double tmpMaxValue = Double.MIN_VALUE;
            double tmpMinValue = Double.MAX_VALUE;
            int count = 0;
            double total = 0.0;
            long now = System.currentTimeMillis();
            long miniteAgo = now - (secondsToDisplay * 1000);

            synchronized (samplesLockToken) {
                samples.add(new NumberElement(now, lastValue));
                for (int i = samples.size() - 1; i >= 0; i--) {
                    NumberElement current = samples.get(i);
                    double currentValue = current.number.doubleValue();
                    if (current.time < miniteAgo) {
                        samples.remove(i);
                    } else {
                        count++;
                        total += currentValue;
                        if (currentValue > tmpMaxValue) {
                            tmpMaxValue = currentValue;
                        }
                        if (currentValue < tmpMinValue) {
                            tmpMinValue = currentValue;
                        }
                    }
                }
            }
            this.lastValue = lastValue;
            this.minValue = new Double(tmpMinValue);
            this.maxValue = new Double(tmpMaxValue);
            this.avgValue = new Double(total / (double) count);
        }
    }

    private class ElementWrapper {

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
                    int row = backingData.indexOf(wrapper);
                    tableModel.fireNumericFieldsUpdatedForRow(row);
                }
            }
        }
        
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
        "", // 0
        "Monitor Name", // 1 
        "Field Name", // 2
        "Last Value", // 3
        "Average", // 4
        "Max Value", // 5
        "Min Value", // 6
        "Factor", // 7
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

        public void fireNumericFieldsUpdatedForRow(int row) {
            for (int i = 3; i <= 6; i++) {
                tableModel.fireTableCellUpdated(row, i);
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
                    case 1: {
                        MonitorKey key = w.element.getFieldKey().getMonitorKey();
                        String instanceName = key.getInstance();
                        if (instanceName == null || "".equals(instanceName)) {
                            return w.element.getFieldKey().getMonitorKey().getName();
                        } else {
                            return w.element.getFieldKey().getMonitorKey().getName() + "("
                                    + instanceName + ")";
                        }
                    }
                    case 2:
                        return w.element.getFieldKey().getFieldName();
                    case 3:
                        return roundIfNeeded(w.data.lastValue);
                    case 4:
                        return roundIfNeeded(w.data.avgValue);
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
            if (col == 7 || col == 8) {
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

        ColorRenderer(String toolTipText) {
            this.setToolTipText(toolTipText);
        }
        
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
}
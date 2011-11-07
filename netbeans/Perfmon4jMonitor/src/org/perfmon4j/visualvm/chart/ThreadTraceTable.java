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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.perfmon4j.visualvm.Perfmon4jMonitorView;
import org.perfmon4j.visualvm.ThreadTraceViewDlg;


public class ThreadTraceTable extends JPanel implements
        ThreadTraceList.ThreadTraceListListener {

    private static final long serialVersionUID = 1L;
    private final TableModel tableModel;
    private final JTable table;
//	private final TableColumnModel columnModel;
    private final ThreadTraceList list;
    private static final String[] HEADER_VALUE = new String[]{"Time Submitted",
        "Monitor Name", "View", "Cancel",};

    private class TableModel extends AbstractTableModel {
//		@Override
//	    public Class<?> getColumnClass(int column) {
//			if (column == 0) {
//				return Color.class;
//			} else {
//				return super.getColumnClass(column);
//			}
//	    }

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
            return list.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ThreadTraceList.ThreadTraceElement element = list.get(rowIndex);
            if (element != null) {
                switch (columnIndex) {
                    case 0:
                        return element.getTimeSubmitted();
                    case 1:
                        return element.getFieldKey().getMonitorKey().getName();
                }
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (col == 2 || col == 3) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

        @Override
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int row, int column) {

            JButton result = new JButton();
//			result.addActionListener(new RowActionListener(row));
//			result.setBackground((Color)value);
            final ThreadTraceList.ThreadTraceElement element = list.get(row);
            
            
            if (list.get(row).isPending()) {
                if (column == 2) {
                    result.setText("Pending");
                    result.setEnabled(false);
                } else {
                    result.setText("Cancel");
                    result.setEnabled(true);
                    result.setBackground(Color.red);
                }
//				result.setBackground(Color.gray);
            } else {
                if (column == 2) {
                    result.setText("View");
                    result.setEnabled(true);
                    result.setBackground(Color.green);
                    result.addActionListener(new ActionListener() {
                        final ThreadTraceList.ThreadTraceElement myElement = element;
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ThreadTraceViewDlg.showModel(Perfmon4jMonitorView.getParentFrame((Component)e.getSource()), myElement);
                        }
                    });
                    
                    
                } else {
                    result.setText("Delete");
                    result.setEnabled(true);
                }

            }

            return result;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            // TODO Auto-generated method stub
            return getTableCellEditorComponent(table,
                    value, isSelected, row, column);
        }
    }

    public ThreadTraceTable(ThreadTraceList list) {
        super(new BorderLayout());
        this.list = list;
        list.addDataHandler(this);

        tableModel = new TableModel();
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);


        TableCellRenderer tableCellRenderer = new DefaultTableCellRenderer() {

            SimpleDateFormat f = new SimpleDateFormat("MM/dd HH:mm:ss");

            @Override
            public Component getTableCellRendererComponent(JTable table,
                    Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {
                if (value instanceof Date) {
                    value = f.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected,
                        hasFocus, row, column);
            }
        };

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(tableCellRenderer);
        columnModel.getColumn(0).setPreferredWidth(100);

        columnModel.getColumn(1).setPreferredWidth(250);


        TableColumn viewColumn = columnModel.getColumn(2);
        TableColumn cancelColumn = columnModel.getColumn(3);
        ButtonCellEditor editor = new ButtonCellEditor();
        viewColumn.setCellEditor(editor);
        viewColumn.setCellRenderer(editor);

        cancelColumn.setCellEditor(editor);
        cancelColumn.setCellRenderer(editor);


        JScrollPane scroller = new JScrollPane(table,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.add(scroller);
    }

    @Override
    public void rowDeleted(int row) {
        tableModel.fireTableRowsDeleted(row, row);
        // TODO Auto-generated method stub
    }

    @Override
    public void rowInserted(int row) {
        tableModel.fireTableRowsInserted(row, row);
    }

    @Override
    public void rowUpdated(int row) {
        tableModel.fireTableRowsUpdated(row, row);
    }
}

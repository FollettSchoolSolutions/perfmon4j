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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.MonitorKey;

public class ThreadTraceList implements FieldManager.FieldHandler {
    private final Object lockToken = new Object();
    private final List<ThreadTraceElement> elements = new ArrayList<ThreadTraceElement>();
    private final List<ThreadTraceListListener> listeners = new ArrayList<ThreadTraceListListener>();

    public void addDataHandler(ThreadTraceListListener handler) {
        listeners.add(handler);
    }

    public void removeDataHandler(ThreadTraceListListener handler) {
        listeners.remove(handler);
    }

    public int size() {
        synchronized (lockToken) {
            return elements.size();
        }
    }

    public ThreadTraceElement get(int index) {
        synchronized (lockToken) {
            return elements.get(index);
        }
    }

    public void delete(int row) {
        synchronized (lockToken) {
            elements.remove(row);
        }
        Iterator<ThreadTraceListListener> itr = listeners.iterator();
        while (itr.hasNext()) {
            itr.next().rowDeleted(row);
        }
    }

    public void add(FieldKey fieldKey, Color color) {
        synchronized (lockToken) {
            elements.add(0, new ThreadTraceElement(fieldKey, color));
        }
        Iterator<ThreadTraceListListener> itr = listeners.iterator();
        while (itr.hasNext()) {
            itr.next().rowInserted(0);
        }
    }

    public void updateField(FieldKey fieldKey, String value) {
        int rowUpdated = -1;
        synchronized (lockToken) {
            Iterator<ThreadTraceElement> itr = elements.iterator();
            int rowCount = -1;
            while (itr.hasNext() && rowUpdated < 0) {
                rowCount++;
                ThreadTraceElement element = itr.next();
                if (element.isPending() && element.fieldKey.equals(fieldKey)) {
                    element.setResult(value);
                    rowUpdated = rowCount;
                }
            }
        }
        if (rowUpdated >= 0) {
            Iterator<ThreadTraceListListener> itr = listeners.iterator();
            while (itr.hasNext()) {
                itr.next().rowUpdated(rowUpdated);
            }
        }
    }

    public boolean hasPendingRequest(FieldKey fieldKey) {
        boolean result = false;
        synchronized (lockToken) {
            Iterator<ThreadTraceElement> itr = elements.iterator();
            while (itr.hasNext() && !result) {
                ThreadTraceElement element = itr.next();
                result = fieldKey.equals(element.getFieldKey()) && element.isPending();
            }
        }
        return result;
    }

    public List<ThreadTraceElement> getElements() {
        return elements;
    }

    @Override
    public void addOrUpdateElement(FieldElement element) {
        // Nothing todo....
    }

    @Override
    public void handleData(Map<FieldKey, Object> data) {
        Iterator<Map.Entry<FieldKey, Object>> itr = data.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<FieldKey, Object> entry = itr.next();
            FieldKey key = entry.getKey();
            if (MonitorKey.THREADTRACE_TYPE.equals(key.getMonitorKey().getType())) {
                updateField(key, (String) entry.getValue());
            }
        }
        // Nothing todo....
    }

    @Override
    public void removeElement(FieldElement element) {
        // Nothing todo....
    }

    public interface ThreadTraceListListener {

        public void rowUpdated(int row);

        public void rowDeleted(int row);

        public void rowInserted(int row);
    }

    public static class ThreadTraceElement {

        private final Date submitted;
        private final FieldKey fieldKey;
        private String result = null;
        private final Color color;

        public ThreadTraceElement(FieldKey fieldKey, Color color) {
            this.submitted = new Date();
            this.fieldKey = fieldKey;
            this.color = color;
        }

        public boolean isPending() {
            return result == null || FieldKey.THREAD_TRACE_PENDING.equals(result);
        }

        public String getResult() {
            return result;
        }

        public Date getTimeSubmitted() {
            return submitted;
        }

        public FieldKey getFieldKey() {
            return fieldKey;
        }
        
        public Color getColor() {
            return color;
        }

        private void setResult(String result) {
            this.result = result;
        }
    }
}

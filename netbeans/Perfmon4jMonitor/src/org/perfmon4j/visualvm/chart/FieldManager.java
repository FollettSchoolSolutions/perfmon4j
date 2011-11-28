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

import com.sun.tools.visualvm.core.options.GlobalPreferences;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;

public class FieldManager implements PreferenceChangeListener {

    private final Object wrapperToken = new Object();
    private final RemoteManagementWrapper wrapper;
    private final GlobalPreferences preferences;
    private final Object mapToken = new Object();
    private final Map<FieldKey, FieldElement> mapElements = new HashMap<FieldKey, FieldElement>();
    private final List<FieldHandler> fieldHandlers = new ArrayList<FieldHandler>();
    private final ThreadTraceList threadTraceList = new ThreadTraceList();
    /**
     * Note... It would be tempting to use the Scheduler provided by the
     * visual vm package...  However as of 11/3/11 that would end up
     * throwing null pointer exceptions when the polling interval is
     * adjusted at runtime.
     */
    private final Timer timer = new Timer();
    private TimerTask task = null;

    public FieldManager(RemoteManagementWrapper wrapper) {
        this.wrapper = wrapper;
        this.addDataHandler(threadTraceList);

        preferences = GlobalPreferences.sharedInstance();
        preferences.watchMonitoredDataPoll(this);
    }

    public boolean isStarted() {
        return task != null;
    }

    public void start() {
        if (isStarted()) {
            stop();
        }
        int duration = preferences.getMonitoredDataPoll() * 1000;
        task = new FieldTask();
        timer.schedule(task, duration, duration);
    }

    public void stop() {
        if (isStarted()) {
            task.cancel();
            task = null;
        }
    }

    public int getNumElements() {
        synchronized(mapToken) {
            return mapElements.size();
        }
    }
    
    
    public void addOrUpdateField(FieldElement element) {
        FieldKey[] fields = null;
        synchronized (mapToken) {
            mapElements.put(element.getFieldKey(), element);
            fields = mapElements.keySet().toArray(new FieldKey[mapElements.size()]);
        }

        FieldHandler[] handlers = fieldHandlers.toArray(new FieldHandler[fieldHandlers.size()]);
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].addOrUpdateElement(element);
        }
        synchronized (wrapperToken) {
            try {
                wrapper.subscribe(fields);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (SessionNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeField(FieldKey fieldKey) {
        removeField(new FieldElement(fieldKey, 1, Color.black));
    }

    public void removeField(FieldElement element) {
        FieldKey[] fields = null;
        
        synchronized (mapToken) {
            mapElements.remove(element.getFieldKey());
            fields = mapElements.keySet().toArray(new FieldKey[mapElements.size()]);
        }

        FieldHandler[] handlers = fieldHandlers.toArray(new FieldHandler[fieldHandlers.size()]);
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].removeElement(element);
        }

        synchronized (wrapperToken) {
            try {
                wrapper.subscribe(fields);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (SessionNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void scheduleThreadTrace(FieldElement element, Map<String,String> map) {
        scheduleThreadTrace(element.getFieldKey().getMonitorKey(), map);
    }
    
    
    /**
     * Returns true if one or more fields could not be added...
     * 
     * @param props
     * @return
     * @throws Exception 
     */
    public boolean readFromProperties(Properties props) throws Exception {
        boolean fieldSkipped = false;
        
        // First get a list of all of the currently tracked fields...
        List<FieldKey> currentMonitoredFields = new ArrayList<FieldKey>();
        
        synchronized(mapToken) {
            FieldElement fields[] = mapElements.values().toArray(new FieldElement[mapElements.values().size()]);
            for (int i = 0; i < fields.length; i++) {
                currentMonitoredFields.add(fields[i].getFieldKey());
            }
        }
       
        List<MonitorKey> availableMonitors = null;
        synchronized (wrapperToken) {
            availableMonitors = Arrays.asList(wrapper.getMonitors());
        }
        

        boolean loading = true;
        int i = -1;
        while (loading) {
            i++;
            String key = props.getProperty("field[" + i + "].fieldKey");
            if (key != null) {
                FieldKey fieldKey = FieldKey.parseNoThrow(key);
                if (fieldKey != null) {
                    boolean isExistingField = currentMonitoredFields.remove(fieldKey);
                    
                    if (!isExistingField) {
                        // Make sure the Monitor is available in the existing VM...
                        // Otherwise, refuse to load it!!!
                        if (!availableMonitors.contains(fieldKey.getMonitorKey())) {
                            fieldSkipped = true;
                            continue;
                        }
                    }
                    float factor = Float.parseFloat(props.getProperty("field[" + i + "].factor"));
                    Color color =  new Color(Integer.valueOf(props.getProperty("field[" + i + "].color")).intValue());
                    boolean visible = Boolean.valueOf(props.getProperty("field[" + i + "].visible"));
                    
                    FieldElement element = new FieldElement(fieldKey, factor, color);
                    element.setVisibleInChart(visible);
                    addOrUpdateField(element);
                }
            } else {
                loading = false;
            }
        }
        
        // Now remove any fields that are no longer being monitored.
        Iterator<FieldKey> itr = currentMonitoredFields.iterator();
        while (itr.hasNext()) {
            this.removeField(itr.next());
        }
        
        return fieldSkipped;
    }
    
    public Properties writeToProperies() {
        Properties result = new Properties();

        FieldElement fields[] = null;
        synchronized(mapToken) {
           fields = mapElements.values().toArray(new FieldElement[mapElements.values().size()]);
        }
        
        for (int i = 0; i < fields.length; i++) {
            FieldElement fieldElement = fields[i];
//System.out.println(fieldElement.getFieldKey().toString());               
            result.setProperty("field[" + i + "].fieldKey", fieldElement.getFieldKey().toString());
            result.setProperty("field[" + i + "].factor", Float.toString(fieldElement.getFactor()));
            result.setProperty("field[" + i + "].color", Integer.toString(fieldElement.getColor().getRGB()));
            result.setProperty("field[" + i + "].visible", Boolean.toString(fieldElement.isVisibleInChart()));
        }
       
       return result;
    }
    

    
    public void scheduleThreadTrace(MonitorKey key, Map<String,String> map) {
        FieldKey threadTraceKey = FieldKey.buildThreadTraceKeyFromInterval(key, 
                map);

        if (!threadTraceList.hasPendingRequest(threadTraceKey)) {
            try {
                wrapper.scheduleThreadTrace(threadTraceKey);
                threadTraceList.add(threadTraceKey, Color.BLUE);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SessionNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidMonitorTypeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void unScheduleThreadTrace(FieldKey sourceFieldKey) {
        MonitorKey sourceKey = sourceFieldKey.getMonitorKey();
        FieldKey threadTraceKey = new FieldKey(new MonitorKey(MonitorKey.THREADTRACE_TYPE, sourceKey.getName()), "stack", FieldKey.STRING_TYPE);

        if (threadTraceList.hasPendingRequest(threadTraceKey)) {
            try {
                wrapper.unScheduleThreadTrace(threadTraceKey);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (SessionNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidMonitorTypeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public ThreadTraceList getThreadTraceList() {
        return threadTraceList;
    }

    public void addDataHandler(FieldHandler handler) {
        fieldHandlers.add(handler);
    }

    public void removeDataHandler(FieldHandler handler) {
        fieldHandlers.remove(handler);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        if (isStarted()) {
            // Restart to pick up the polling interval change.
            start();
        };
    }

    public static interface FieldHandler {

        public void handleData(Map<FieldKey, Object> data);

        public void addOrUpdateElement(FieldElement element);

        public void removeElement(FieldElement element);
    }

    private class FieldTask extends TimerTask {

        @Override
        public void run() {
            try {
                FieldHandler[] handlers = fieldHandlers.toArray(new FieldHandler[fieldHandlers.size()]);
                if (handlers.length > 0) {
                    synchronized (wrapperToken) {
                        Map<FieldKey, Object> data = wrapper.getData();
                        if (data.size() > 0) {
                            for (int i = 0; i < handlers.length; i++) {
                                handlers[i].handleData(data);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
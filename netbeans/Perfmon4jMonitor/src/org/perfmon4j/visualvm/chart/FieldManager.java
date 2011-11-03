/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.perfmon4j.visualvm.chart;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;

public class FieldManager {

    private final int pollingSeconds;
    private final Object wrapperToken = new Object();
    private final RemoteManagementWrapper wrapper;
    private Object mapToken = new Object();
    private final Map<FieldKey, FieldElement> mapElements = new HashMap<FieldKey, FieldElement>();
    private final List<FieldHandler> fieldHandlers = new ArrayList<FieldHandler>();
    private ThreadRunner runner = null;

    public FieldManager(RemoteManagementWrapper wrapper, int pollingSeconds) {
        this.pollingSeconds = pollingSeconds;
        this.wrapper = wrapper;
    }

    public boolean isStarted() {
        return runner != null;
    }

    public void start() {
        if (isStarted()) {
            stop();
        }
        runner = new ThreadRunner();
        runner.start();
    }

    public void stop() {
        if (isStarted()) {
            runner.setStopFlag();
            runner = null;
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

        FieldHandler[] handlers = fieldHandlers.toArray(new FieldHandler[fieldHandlers.size()]);
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].removeElement(element);
        }

        synchronized (mapToken) {
            mapElements.remove(element.getFieldKey());
            fields = mapElements.keySet().toArray(new FieldKey[mapElements.size()]);
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

    public void addDataHandler(FieldHandler handler) {
        fieldHandlers.add(handler);
    }

    public void removeDataHandler(FieldHandler handler) {
        fieldHandlers.remove(handler);
    }

    public static interface FieldHandler {

        public void handleData(Map<FieldKey, Object> data);

        public void addOrUpdateElement(FieldElement element);

        public void removeElement(FieldElement element);
    }

    private class ThreadRunner extends Thread {

        private boolean running = true;

        ThreadRunner() {
            this.setDaemon(true);
        }

        public void setStopFlag() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
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
                try {
                    Thread.sleep(pollingSeconds * 1000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}
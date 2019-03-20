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

package org.perfmon4j.remotemanagement;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.perfmon4j.ExternalThreadTraceConfig;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotMonitor;
import org.perfmon4j.SnapShotProviderWrapper;
import org.perfmon4j.instrument.PerfMonTimerTransformer;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.MonitorNotFoundException;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.util.BeanHelper;
import org.perfmon4j.util.BeanHelper.UnableToSetAttributeException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class ExternalAppender {
	private static final Logger logger = LoggerFactory.initLogger(ExternalAppender.class);
	
	public static final int DEFAULT_TIMEOUT_SECONDS = 60 * 5;  // 5 minutes
	private static final SessionManager<MonitorMap> sessionManager = new SessionManager<MonitorMap>();
	private static boolean enabled = false;
	
	private static final Object snapShotLockToken = new Object();
	private static Map<String, RegisteredSnapShotElement> registeredSnapShots = new HashMap<String, RegisteredSnapShotElement>(); 
	
	private ExternalAppender() {
	}
	
	public static String connect() {
		return connect(DEFAULT_TIMEOUT_SECONDS);
	}
	
	public static String connect(int timeoutSeconds) {
		MonitorMap map = new MonitorMap();
		String result = sessionManager.addSession(map, timeoutSeconds * 1000);
		logger.logInfo("Connected external appender - sessionID: " + result);
		return result;
	}
	
	public static void validateSession(String sessionID) throws SessionNotFoundException {
		SessionManager.SessionData result = sessionManager.getSession(sessionID);
		if (result == null) {
			throw new SessionNotFoundException(sessionID);
		}
	}
	
	public static void disconnect(String sessionID) {
		sessionManager.disposeSession(sessionID);
		logger.logInfo("Disconnected external appender - sessionID: " + sessionID);
	}

	
	public static void scheduleThreadTrace(String sessionID, FieldKey threadTraceKey) 
		throws SessionNotFoundException, InvalidMonitorTypeException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		if (!MonitorKey.THREADTRACE_TYPE.equals(threadTraceKey.getMonitorKey().getType())) {
			throw new InvalidMonitorTypeException(threadTraceKey.getMonitorKey().getType(),
				"scheduleThreadTrace");
		}
		map.scheduleThreadTrace(threadTraceKey);
		logger.logInfo("External appender (sessionID:" + sessionID + ") schedule thread trace on field: " + threadTraceKey);
	}
	
	public static void unScheduleThreadTrace(String sessionID, FieldKey threadTraceKey) throws SessionNotFoundException
		, InvalidMonitorTypeException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		if (!MonitorKey.THREADTRACE_TYPE.equals(threadTraceKey.getMonitorKey().getType())) {
			throw new InvalidMonitorTypeException(threadTraceKey.getMonitorKey().getType(),
				"scheduleThreadTrace");
		}
		map.unScheduleThreadTrace(threadTraceKey);
		logger.logInfo("External appender (sessionID:" + sessionID + ") unschedule thread trace on field: " + threadTraceKey);
	}
	
	public static void subscribe(String sessionID, MonitorKeyWithFields monitorKey) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		map.subscribe(monitorKey);
		logger.logInfo("External appender (sessionID:" + sessionID + ") subscribed to monitor: " + monitorKey);
	}
	
	public static void forceDynamicChildCreation(String sessionID, MonitorKey monitorKey) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		if (MonitorKey.INTERVAL_TYPE.equals(monitorKey.getType())) {
			PerfMon mon = PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY(monitorKey.getName());
			if (mon != null) {
				mon.forceDynamicChildCreation(map);
			}
		}
	}
	
    public static void unForceDynamicChildCreation(String sessionID, MonitorKey dynamicPathMonitorKey) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}

		if (MonitorKey.INTERVAL_TYPE.equals(dynamicPathMonitorKey.getType())) {
			PerfMon mon = PerfMon.getMonitorNoCreate_PERFMON_USE_ONLY(dynamicPathMonitorKey.getName());
			if (mon != null) {
				mon.unForceDynamicChildCreation(map);
			}
		}
    }

	public static MonitorKeyWithFields[] getSubscribedMonitors(String sessionID) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		return map.fieldProperties.values().toArray(new MonitorKeyWithFields[]{});
	}
	

	public static Map<FieldKey, Object> getThreadTraceData(String sessionID) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		return map.getThreadTraceData();
	}
	
	public static Map<FieldKey, Object> takeSnapShot(String sessionID, MonitorKeyWithFields monitorKey) throws SessionNotFoundException, MonitorNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		Map<FieldKey, Object> result = map.takeSnapShot(monitorKey);
		if (logger.isDebugEnabled()) {
			logger.logDebug("External appender (sessionID:" + sessionID + ") took snapshot of monitor: " + monitorKey + "\r\n" 
						+ FieldKey.buildDebugString(result));
		}
		return result;
	}
	
	public static void unSubscribe(String sessionID, MonitorKeyWithFields monitorKey) {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map != null) {
			map.unSubscribe(monitorKey);	
		}
		logger.logInfo("External appender (sessionID:" + sessionID + ") unsubscribed from monitor: " + monitorKey);
	}
	
//	public String[] getMonitors() {
//		return new String[]{};
//	}
	
	public static boolean isActive() {
		return enabled && sessionManager.getSessionCount() > 0;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	/**
	 * Is enabled/disabled when the remote RMIPort
	 * is opened for the external appender.
	 * 
	 * This requires that the perfmon4j agent is started
	 * with the "-p" flag.
	 * 
	 * @param onOff
	 */
	public static void setEnabled(boolean onOff) {
		enabled = onOff;
	}

	private static final class MonitorMap implements SessionManager.SessionData {
		private final Map<MonitorKey, PerfMonData> intervalMonitors = new HashMap<MonitorKey, PerfMonData>();
		private final Map<MonitorKey, SnapShotMonitorAndData> snapShotMonitors = new HashMap<MonitorKey, SnapShotMonitorAndData>();
		private final Map<MonitorKey, MonitorKeyWithFields> fieldProperties = new HashMap<MonitorKey, MonitorKeyWithFields>();
		private final Map<FieldKey, ExternalThreadTraceConfig> scheduledThreadTraces = new HashMap<FieldKey, ExternalThreadTraceConfig>();
		
		private void scheduleThreadTrace(FieldKey threadTraceKey) {
			String extraParams = threadTraceKey.getMonitorKey().getInstance();
			PerfMon monitor = PerfMon.getMonitor(threadTraceKey.getMonitorKey().getName());
		
			ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
			if (extraParams != null) {
				String [] params = MiscHelper.tokenizeCSVString(extraParams);
				for (int i = 0; i < params.length; i++) {
					String[] p = params[i].split("=");
					if (p.length == 2) {
						try {
							BeanHelper.setValue(config, p[0], p[1]);
						} catch (UnableToSetAttributeException e) {
							logger.logWarn("Unable to set attribute", e);
						}
					} else {
						logger.logWarn("Unable to parse value into a attribute name and value: " +
								params[i]);
					}
				}
			}
			scheduledThreadTraces.put(threadTraceKey, config);
			monitor.scheduleExternalThreadTrace(config);
		}
		
		public Map<FieldKey, Object> getThreadTraceData() {
			Map<FieldKey, Object> result = null;
			if (scheduledThreadTraces.size() > 0) {
				result = new HashMap<FieldKey, Object>();
				FieldKey[] fields = scheduledThreadTraces.keySet().toArray(new FieldKey[scheduledThreadTraces.size()]);
				for (FieldKey fieldKey : fields) {
					ExternalThreadTraceConfig config = scheduledThreadTraces.get(fieldKey);
					if (config != null) {
						if (config.hasData()) {
							result.put(fieldKey, config.getData().toAppenderString());
							scheduledThreadTraces.remove(fieldKey);
						} else {
							result.put(fieldKey, FieldKey.THREAD_TRACE_PENDING);
						}
					}
				}
			}
			return result;
		}

		private void unScheduleThreadTrace(FieldKey threadTraceKey) {
			ExternalThreadTraceConfig config = scheduledThreadTraces.remove(threadTraceKey);
			if (config != null) {
				PerfMon monitor = PerfMon.getMonitor(threadTraceKey.getMonitorKey().getName());
				monitor.unScheduleExternalThreadTrace(config);
			}
		}

		private void subscribe(MonitorKeyWithFields monitorKeyWithFields) {
			MonitorKey monitorKey = monitorKeyWithFields.getMonitorKeyOnly();
			// Always store the fields we are monitoring...
			// Even if we are not "subscribing" to a new data element
			// we might be adding/removing fields.
			fieldProperties.put(monitorKey, monitorKeyWithFields);
				
			
			if (monitorKey.getType().equals(MonitorKey.INTERVAL_TYPE)) {
				if (intervalMonitors.get(monitorKey) == null) {
					PerfMon mon = PerfMon.getMonitor(monitorKey.getName());
					IntervalData d = new IntervalData(mon);
					mon.addExternalElement(d);
					
					intervalMonitors.put(monitorKey, d);
				}
			} else if (monitorKey.getType().equals(MonitorKey.SNAPSHOT_TYPE)) {
				if (snapShotMonitors.get(monitorKey) == null) {
					String className = monitorKey.getName();
					String instanceName = monitorKey.getInstance();
					try {
						Class<?> clazz = PerfMon.getClassLoader().loadClass(className);
						JavassistSnapShotGenerator.Bundle bundle = PerfMonTimerTransformer.snapShotGenerator.generateBundle(clazz, instanceName);
		            	SnapShotMonitor monitor = new SnapShotProviderWrapper("", bundle);
		            	SnapShotData data = monitor.initSnapShot(MiscHelper.currentTimeWithMilliResolution());
		            	
		            	snapShotMonitors.put(monitorKey, new SnapShotMonitorAndData(monitor, data));
		            	
					} catch (Exception e) {
						logger.logError("Unable to create snapShotInstance for monitor: " + monitorKey, e);
					}
				}
			} else {
				logger.logError("Unable to subscribe to monitor: " + monitorKey);
			}
		}
		
		private void unSubscribe(MonitorKeyWithFields monitorKeyWithFields) {
			MonitorKey monitorKey = monitorKeyWithFields.getMonitorKeyOnly();
			if (MonitorKey.INTERVAL_TYPE.equals(monitorKey.getType())) {
				IntervalData data = (IntervalData)intervalMonitors.remove(monitorKey);
				if (data != null) { 
					PerfMon mon = PerfMon.getMonitor(monitorKey.getName());
					mon.removeExternalElement(data);
				}
			} else if (MonitorKey.SNAPSHOT_TYPE.equals(monitorKey.getType())) {
				snapShotMonitors.remove(monitorKey);
			}
			fieldProperties.remove(monitorKey);
		}

		private Map<FieldKey, Object> takeSnapShot(MonitorKeyWithFields monitorKeyWithFields) throws MonitorNotFoundException {
			Map<FieldKey, Object> result = null;
			MonitorKey monitorKey = monitorKeyWithFields.getMonitorKeyOnly();
			
			
			if (MonitorKey.INTERVAL_TYPE.equals(monitorKey.getType())) {
				IntervalData data = (IntervalData)intervalMonitors.get(monitorKey);
				if (data == null) {
					throw new MonitorNotFoundException(monitorKey.toString());
				}
				PerfMon mon = PerfMon.getMonitor(monitorKey.getName());
				IntervalData newData = mon.replaceExternalElement((IntervalData)data, new IntervalData(mon));
				intervalMonitors.put(monitorKey, newData);
				data.setTimeStop(MiscHelper.currentTimeWithMilliResolution());
				result = data.getFieldData(monitorKeyWithFields.getFields());
			} else if (MonitorKey.SNAPSHOT_TYPE.equals(monitorKey.getType())){
				SnapShotMonitorAndData monitorAndData = snapShotMonitors.get(monitorKey);
				if (monitorAndData == null) {
					throw new MonitorNotFoundException(monitorKey.toString());
				}
				long now = MiscHelper.currentTimeWithMilliResolution();
				SnapShotData nowSnapShot = monitorAndData.monitor.takeSnapShot(monitorAndData.data, now);
				
				// Initialize the next snapshot
				monitorAndData.data = monitorAndData.monitor.initSnapShot(now);
				result = nowSnapShot.getFieldData(monitorKeyWithFields.getFields());
			} else {
				logger.logError("Unable to take shapshot of monitor: " + monitorKey);
			}
			return result;
		}

		public void destroy() {
			MonitorKeyWithFields keys[] = fieldProperties.values().toArray(new MonitorKeyWithFields[]{});
			for (int i = 0; i < keys.length; i++) {
				unSubscribe(keys[i]);
			}
		}
	}

	public static void registerSnapShotClass(String name) {
		synchronized (snapShotLockToken) {
			if (!registeredSnapShots.containsKey(name)) {
				registeredSnapShots.put(name, new RegisteredSnapShotElement());
			}
		}
	}
	
	private static RegisteredSnapShotElement[] populateAndRetrieveElements() {
		List<RegisteredSnapShotElement> result = new ArrayList<RegisteredSnapShotElement>(); 
		
		synchronized (snapShotLockToken) {
			Iterator<Map.Entry<String, RegisteredSnapShotElement>> itr = registeredSnapShots.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry<String, RegisteredSnapShotElement> entry = itr.next();
				String className = entry.getKey();
				RegisteredSnapShotElement element = entry.getValue();
				Class<?> clazz = element.monitorClass == null ? null : element.monitorClass.get();
				if (clazz == null) {
					// Try loading class
					try {
						clazz = PerfMon.getClassLoader().loadClass(className);
						element.monitorClass = new WeakReference<Class<?>>(clazz);
					} catch (ClassNotFoundException e) {
						// Nothing todo....
					}
				} 
				if (clazz != null) {
					// Refresh all the monitor instances and fields.
					MonitorKeyWithFields m[] = PerfMonTimerTransformer.snapShotGenerator.generateExternalMonitorKeys(clazz);
					if (m != null) {
						element.monitors = m;
					}
				}
				result.add(element);
			}
		}
		return result.toArray(new RegisteredSnapShotElement[result.size()]);
	}
	

	public static MonitorKey[] getSnapShotMonitorKeys() {
		List<MonitorKey> result = new ArrayList<MonitorKey>();
		RegisteredSnapShotElement elements[] = populateAndRetrieveElements();
		for (int i = 0; i < elements.length; i++) {
			MonitorKeyWithFields keys[] = elements[i].monitors;
			if (keys != null) {
				for (int j = 0; j < keys.length; j++) {
					result.add(keys[j].getMonitorKeyOnly());
				}
			}
		}
		return result.toArray(new MonitorKey[result.size()]);
	}
	
	private static class RegisteredSnapShotElement {
		WeakReference<Class<?>> monitorClass = null;
		MonitorKeyWithFields monitors[] = null;
	}
	
	private static class SnapShotMonitorAndData {
		final SnapShotMonitor monitor;
		SnapShotData data;
		
		SnapShotMonitorAndData(SnapShotMonitor monitor, SnapShotData data) {
			this.monitor = monitor;
			this.data = data;
		}
	}

	public static FieldKey[] getFieldsForSnapShotMonitor(MonitorKey monitorKey) {
		MonitorKeyWithFields result = null;
		
		if (MonitorKey.SNAPSHOT_TYPE.equals(monitorKey.getType())) {
			RegisteredSnapShotElement elements[] = populateAndRetrieveElements();
			for (int i = 0; i < elements.length && result == null; i++) {
				MonitorKeyWithFields monitors[] = elements[i].monitors;
				if (monitors != null) {
					for (int j = 0; j < monitors.length && result == null; j++) {
						MonitorKeyWithFields m = monitors[j];
						if (monitorKey.equals(m.getMonitorKeyOnly())) {
							result = m;
						}
					}
				}
			}
		}
		if (result == null) {
			return new FieldKey[]{};
		} else {
			return result.getFields();
		}
	}
}

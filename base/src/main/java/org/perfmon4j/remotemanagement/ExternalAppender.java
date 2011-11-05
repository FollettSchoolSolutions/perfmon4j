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

import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.ExternalThreadTraceConfig;
import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.InvalidMonitorTypeException;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.MonitorNotFoundException;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class ExternalAppender {
	private static final Logger logger = LoggerFactory.initLogger(ExternalAppender.class);
	
	public static final int DEFAULT_TIMEOUT_SECONDS = 60 * 5;  // 5 minutes
	private static final SessionManager<MonitorMap> sessionManager = new SessionManager<MonitorMap>();
	private static boolean enabled = false;
	
	
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
	
	public String[] getMonitors() {
		return new String[]{};
	}
	
	public static boolean isActive() {
		return enabled || sessionManager.getSessionCount() > 0;
	}

	public static boolean isEnabled() {
		return enabled;
	}
	
	public static void setEnabled(boolean onOff) {
		enabled = onOff;
	}

	private static final class MonitorMap implements SessionManager.SessionData {
		private final Map<MonitorKey, PerfMonData> map = new HashMap<MonitorKey, PerfMonData>();
		private final Map<MonitorKey, MonitorKeyWithFields> fieldProperties = new HashMap<MonitorKey, MonitorKeyWithFields>();
		private final Map<FieldKey, ExternalThreadTraceConfig> scheduledThreadTraces = new HashMap<FieldKey, ExternalThreadTraceConfig>();
		
		private void scheduleThreadTrace(FieldKey threadTraceKey) {
			PerfMon monitor = PerfMon.getMonitor(threadTraceKey.getMonitorKey().getName());
			ExternalThreadTraceConfig config = new ExternalThreadTraceConfig();
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
				
			if (map.get(monitorKey) != null) {
				return;  // Already subscribed... Nothing to do.
			}
			
			if (monitorKey.getType().equals(MonitorKey.INTERVAL_TYPE)) {
				PerfMon mon = PerfMon.getMonitor(monitorKey.getName());
				IntervalData d = new IntervalData(mon);
				mon.addExternalElement(d);
				
				map.put(monitorKey, d);
			} else {
				logger.logError("Unable to subscribe to monitor: " + monitorKey);
			}
		}
		
		private void unSubscribe(MonitorKeyWithFields monitorKeyWithFields) {
			MonitorKey monitorKey = monitorKeyWithFields.getMonitorKeyOnly();
			if (MonitorKey.INTERVAL_TYPE.equals(monitorKey.getType())) {
				IntervalData data = (IntervalData)map.remove(monitorKey);
				if (data != null) { 
					PerfMon mon = PerfMon.getMonitor(monitorKey.getName());
					mon.removeExternalElement(data);
				}
				fieldProperties.remove(monitorKey);
			}
		}

		private Map<FieldKey, Object> takeSnapShot(MonitorKeyWithFields monitorKeyWithFields) throws MonitorNotFoundException {
			Map<FieldKey, Object> result = null;
			MonitorKey monitorKey = monitorKeyWithFields.getMonitorKeyOnly();
			
			PerfMonData r = null;
			
			if (MonitorKey.INTERVAL_TYPE.equals(monitorKey.getType())) {
				IntervalData data = (IntervalData)map.get(monitorKey);
				if (data == null) {
					throw new MonitorNotFoundException(monitorKey.toString());
				}
				PerfMon mon = PerfMon.getMonitor(monitorKey.getName());
				r = data;
				data = mon.replaceExternalElement((IntervalData)data, new IntervalData(mon));
				map.put(monitorKey, data);
				((IntervalData)r).setTimeStop(MiscHelper.currentTimeWithMilliResolution());
				result = r.getFieldData(monitorKeyWithFields.getFields());
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
}

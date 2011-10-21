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
import java.util.Iterator;
import java.util.Map;

import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonData;
import org.perfmon4j.remotemanagement.intf.MonitorNotFoundException;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class ExternalAppender {
	private static final Logger logger = LoggerFactory.initLogger(ExternalAppender.class);
	
	public static final String INTERVAL_PREFIX = "INTERVAL:";
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
		if (sessionManager.getSession(sessionID) == null) {
			throw new SessionNotFoundException(sessionID);
		}
	}
	
	public static void disconnect(String sessionID) {
		sessionManager.disposeSession(sessionID);
		logger.logInfo("Disconnected external appender - sessionID: " + sessionID);
	}
	
	public static void subscribe(String sessionID, String monitorKey) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		map.subscribe(monitorKey);
		logger.logInfo("External appender (sessionID:" + sessionID + ") subscribed to monitor: " + monitorKey);
	}

	public static String[] getSubscribedMonitors(String sessionID) throws SessionNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		return map.map.keySet().toArray(new String[]{});
	}
	
	
	public static PerfMonData takeSnapShot(String sessionID, String monitorKey) throws SessionNotFoundException, MonitorNotFoundException {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map == null) {
			throw new SessionNotFoundException(sessionID);
		}
		PerfMonData result = map.takeSnapShot(monitorKey);
		if (logger.isDebugEnabled()) {
			logger.logDebug("External appender (sessionID:" + sessionID + ") took snapshot of monitor: " + monitorKey);
		}
		return result;
	}
	
	public static void unSubscribe(String sessionID, String monitorKey) {
		MonitorMap map = sessionManager.getSession(sessionID);
		if (map != null) {
			map.unSubscribe(monitorKey);	
		}
		logger.logInfo("External appender (sessionID:" + sessionID + ") unsubscribed from monitor: " + monitorKey);
	}
	
	public String[] getMonitors() {
		return new String[]{};
	}

	public static String buildIntervalMonitorKey(String intervalMonitor) {
		return INTERVAL_PREFIX + intervalMonitor;
	}
	
	public static String getIntervalMonitorName(String monitorKey) {
		String result = null;
		
		if (monitorKey != null && 
				monitorKey.startsWith(INTERVAL_PREFIX)
				&& monitorKey.length() > INTERVAL_PREFIX.length()) {
			result = monitorKey.substring(INTERVAL_PREFIX.length());
		}
		return result;
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
		private final Map<String, PerfMonData> map = new HashMap<String, PerfMonData>();
		
		void subscribe(String monitorKey) {
			if (map.get(monitorKey) != null) {
				return;  // Already subscribed... Nothing to do.
			}
			String im = getIntervalMonitorName(monitorKey);
			if (im != null) {
				PerfMon mon = PerfMon.getMonitor(im);
				IntervalData d = new IntervalData(mon);
				mon.addExternalElement(d);
				
				map.put(monitorKey, d);
			} else {
				logger.logError("Unable to subscribe to monitor: " + monitorKey);
			}
		}
		
		public void unSubscribe(String monitorKey) {
			String im = getIntervalMonitorName(monitorKey);
			
			if (im != null) {
				IntervalData data = (IntervalData)map.remove(monitorKey);
				if (data != null) { 
					PerfMon mon = PerfMon.getMonitor(im);
					mon.removeExternalElement(data);
				}
			}
		}

		public PerfMonData takeSnapShot(String monitorKey) throws MonitorNotFoundException {
			PerfMonData result = null;
			String im = getIntervalMonitorName(monitorKey);
			
			if (im != null) {
				IntervalData data = (IntervalData)map.get(monitorKey);
				if (data == null) {
					throw new MonitorNotFoundException(monitorKey);
				}
				PerfMon mon = PerfMon.getMonitor(im);
				result = data;
				data = mon.replaceExternalElement((IntervalData)data, new IntervalData(mon));
				map.put(monitorKey, data);
				((IntervalData)result).setTimeStop(MiscHelper.currentTimeWithMilliResolution());
			} else {
				logger.logError("Unable to subscribe to monitor: " + monitorKey);
			}
			return result;
		}

		public void destroy() {
			String keys[] = map.keySet().toArray(new String[]{});
			for (int i = 0; i < keys.length; i++) {
				unSubscribe(keys[i]);
			}
		}
	}
}

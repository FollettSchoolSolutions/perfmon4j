/*
 *	Copyright 2026 Follett Software Company
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
*/

package org.perfmon4j.remotemanagement.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.perfmon4j.IntervalData;
import org.perfmon4j.PerfMon;
import org.perfmon4j.remotemanagement.ExternalAppender;
import org.perfmon4j.remotemanagement.MonitorKeyWithFields;
import org.perfmon4j.remotemanagement.intf.FieldKey;
import org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException;
import org.perfmon4j.remotemanagement.intf.ManagementVersion;
import org.perfmon4j.remotemanagement.intf.MonitorKey;
import org.perfmon4j.remotemanagement.intf.MonitorNotFoundException;
import org.perfmon4j.remotemanagement.intf.SessionNotFoundException;
import org.perfmon4j.remotemanagement.intf.UnableToParseKeyException;
import org.perfmon4j.util.Logger;

/**
 * perfmon4j's remote-management MBean. Registered once, on a best-effort
 * basis, from PerfMon's static initializer, following the same pattern as
 * org.perfmon4j.selfmanagement.SelfManagement.
 * <p>
 * PerfMon.class can legitimately load under more than one classloader in a
 * single JVM (see org.perfmon4j.util.SingletonTracker). The platform
 * MBeanServer is JVM-wide, not per-classloader, so a second PerfMon
 * class-load will attempt to register this same ObjectName again - that is
 * an expected, benign condition, not a bug, and registerMBean() must handle
 * it without throwing.
 * <p>
 * This is a JMX/Jolokia-reachable re-exposure of the legacy RMI remote
 * management implementation (org.perfmon4j.remotemanagement.RemoteImpl):
 * every operation here delegates to the exact same
 * org.perfmon4j.remotemanagement.ExternalAppender / PerfMon statics
 * RemoteImpl already calls, rather than proxying through RMI or
 * reimplementing logic. Sessions are shared, process-wide state
 * (ExternalAppender's static SessionManager) - a session opened here is
 * visible to, and shares subscription state with, a session opened over
 * RMI, and vice versa.
 * <p>
 * connect() marks ExternalAppender as enabled (sticky - never turned back
 * off by this class). This is safe to leave on indefinitely once no remote
 * sessions remain: ExternalAppender.isActive() is
 * (enabled &amp;&amp; sessionManager.getSessionCount() &gt; 0), so every call site
 * that gates on it re-checks the live session count on every invocation, and
 * disconnecting the last session already tears down all subscribed
 * monitors/snapshots and cancels the session-reaper timer. Note that
 * RemoteImpl.unregisterRMIListener() unconditionally sets enabled back to
 * false, which would also deactivate a concurrently-live session opened
 * through this MBean if RMI is separately unregistered - an accepted,
 * documented interaction, not something this class needs to guard against.
 */
public final class RemoteManagement implements RemoteManagementMBean {
	public static final String OBJECT_NAME = "org.perfmon4j:type=RemoteManagement";

	@Override
	public String connect(String clientVersion) throws IncompatibleClientVersionException {
		int clientMajorVersion = ManagementVersion.extractMajorVersion(clientVersion);
		if (clientMajorVersion != ManagementVersion.MAJOR_VERSION) {
			throw new IncompatibleClientVersionException(clientVersion, ManagementVersion.VERSION);
		}
		ExternalAppender.setEnabled(true);
		return ExternalAppender.connect(ExternalAppender.DEFAULT_TIMEOUT_SECONDS);
	}

	@Override
	public void disconnect(String sessionID) {
		ExternalAppender.disconnect(sessionID);
	}

	@Override
	public String[] getMonitors(String sessionID) throws SessionNotFoundException {
		List<String> result = new ArrayList<String>();
		ExternalAppender.validateSession(sessionID);

		for (MonitorKey key : PerfMon.getMonitorKeys()) {
			result.add(key.toString());
		}

		for (MonitorKey key : ExternalAppender.getSnapShotMonitorKeys()) {
			result.add(key.toString());
		}

		return result.toArray(new String[]{});
	}

	@Override
	public String[] getFieldsForMonitor(String sessionID, String monitorKey) throws SessionNotFoundException {
		ExternalAppender.validateSession(sessionID);

		String[] result = new String[]{};
		try {
			MonitorKey key = MonitorKey.parse(monitorKey);
			if (MonitorKey.INTERVAL_TYPE.equals(key.getType())) {
				FieldKey fields[] = IntervalData.getFields(key).getFields();
				result = FieldKey.toStringArray(fields);
			} else if (MonitorKey.THREADTRACE_TYPE.equals(key.getType())) {
				FieldKey fields[] = new FieldKey[]{new FieldKey(key, "stack", FieldKey.STRING_TYPE)};
				result = FieldKey.toStringArray(fields);
			} else if (MonitorKey.SNAPSHOT_TYPE.equals(key.getType())) {
				FieldKey fields[] = ExternalAppender.getFieldsForSnapShotMonitor(key);
				result = FieldKey.toStringArray(fields);
			}
		} catch (UnableToParseKeyException e) {
			throw new IllegalArgumentException("Unable to parse monitor key: " + monitorKey, e);
		}

		return result;
	}

	@Override
	public void subscribe(String sessionID, String[] fieldKeys) throws SessionNotFoundException {
		MonitorKeyWithFields[] monitorKeys = MonitorKeyWithFields.groupFields(FieldKey.toFieldKeyArrayNoThrow(fieldKeys));

		List<MonitorKeyWithFields> alreadySubscribed = new ArrayList<MonitorKeyWithFields>(monitorKeys.length);
		List<MonitorKeyWithFields> newSubscribed = Arrays.asList(monitorKeys);
		MonitorKeyWithFields[] subscribed = ExternalAppender.getSubscribedMonitors(sessionID);

		for (MonitorKeyWithFields key : subscribed) {
			if (newSubscribed.contains(key)) {
				// Already subscribed... Dont need to add again.
				alreadySubscribed.add(key);
			} else {
				// No longer subscribed... Unsubscribe.
				ExternalAppender.unSubscribe(sessionID, key);
			}
		}

		Iterator<MonitorKeyWithFields> itr = newSubscribed.iterator();
		while (itr.hasNext()) {
			MonitorKeyWithFields key = itr.next();
			if (!alreadySubscribed.contains(key)) {
				ExternalAppender.subscribe(sessionID, key);
			}
		}
	}

	@Override
	public Map<String, Object> getData(String sessionID) throws SessionNotFoundException {
		Map<String, Object> result = new HashMap<String, Object>();
		MonitorKeyWithFields monitors[] = ExternalAppender.getSubscribedMonitors(sessionID);

		for (MonitorKeyWithFields monitor : monitors) {
			try {
				copyToResultMap(ExternalAppender.takeSnapShot(sessionID, monitor), result);
			} catch (MonitorNotFoundException e) {
				// A monitor that was subscribed a moment ago disappeared before this
				// poll could take a snapshot of it - treat it the same as "no data yet"
				// rather than failing the whole getData() call.
			}
		}

		Map<FieldKey, Object> threadTraceData = ExternalAppender.getThreadTraceData(sessionID);
		if (threadTraceData != null) {
			copyToResultMap(threadTraceData, result);
		}

		return result;
	}

	private void copyToResultMap(Map<FieldKey, Object> from, Map<String, Object> to) {
		for (Map.Entry<FieldKey, Object> entry : from.entrySet()) {
			to.put(entry.getKey().toString(), entry.getValue());
		}
	}

	public static void registerMBean(Logger logger) {
		try {
			ObjectName objectName = new ObjectName(OBJECT_NAME);
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			if (server.isRegistered(objectName)) {
				// Expected on a second class-load of PerfMon under a different
				// classloader in the same JVM - not an error.
				logger.logInfo("perfmon4j remote-management MBean already registered "
					+ "(likely a second PerfMon class-load under a different classloader) - skipping.");
				return;
			}
			server.registerMBean(new RemoteManagement(), objectName);
		} catch (InstanceAlreadyExistsException ex) {
			// Race: two classloaders' static initializers both saw isRegistered() == false
			// before either completed registerMBean(). Same benign explanation as above.
			logger.logInfo("perfmon4j remote-management MBean registration race - "
				+ "already registered by another classloader.");
		} catch (Exception ex) {
			// Registration is best-effort - it must never break PerfMon's static init.
			logger.logWarn("Unable to register perfmon4j remote-management MBean", ex);
		}
	}
}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

import org.perfmon4j.PerfMon;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


class SessionManager<T extends SessionManager.SessionData> {	
	static private final Logger logger = LoggerFactory.initLogger(SessionManager.class); 
	static final int NO_TIMEOUT = -1;
	static private final int DEFAULT_REAPER_MILLIS = 
		Integer.getInteger(SessionManager.class.getName() + ".DEFAULT_REAPER_MILLIS",
				10 * 60 * 1000); // Ten minutes by default.

	private final Object mapLockToken = new Object();
	private final Map<String, SessionWrapper> map = new HashMap<String, SessionWrapper>();
	private final int timerIntervalMillis;
	private MyTimerTask timerTask;
	
	SessionManager() {
		this(DEFAULT_REAPER_MILLIS);
	}
	
	SessionManager(int timerIntervalMillis) {
		this.timerIntervalMillis = timerIntervalMillis;
	}

	String addSession(T sessionData, int timeoutMillis) {
		String sessionID = UUID.randomUUID().toString();
		synchronized(mapLockToken) {
			map.put(sessionID, new SessionWrapper(sessionID, sessionData, timeoutMillis));
			if (timerTask == null) {
				logger.logDebug("Starting SessionManager timer task");
				timerTask = new MyTimerTask();
				PerfMon.utilityTimer.schedule(timerTask, 
						timerIntervalMillis, timerIntervalMillis);
			}
		}
		return sessionID;
	}
	
	int getSessionCount() {
		synchronized(mapLockToken) {
			return map.size();
		}
	}
	
	T getSession(String sessionID) {
		T result = null;
		synchronized(mapLockToken) {
			SessionWrapper wrapper = map.get(sessionID);
			if (wrapper != null) {
				result = wrapper.getSessionData();
			}
		}
		return result;
	}
	
	void disposeSession(String sessionID) {
		T sessionData = null;
		synchronized(mapLockToken) {
			SessionWrapper w = map.remove(sessionID);
			if (w != null) {
				sessionData = w.getSessionData();
			}
			if (map.isEmpty() && timerTask != null) {
				logger.logDebug("Stopping SessionManager timer task");
				timerTask.cancel();
				timerTask = null;
			}
		}
		if (sessionData != null) {
			sessionData.destroy();
		}
	}
	
	void deInit() {
		runTimerCleanup(true);
	}
	
	private void runTimerCleanup(boolean force) {
		List<String> expired = new ArrayList<String>();
		// First lock the map and get all of the sessionID's that
		// are expired;
		synchronized (mapLockToken) {
			Iterator<SessionWrapper> itr = map.values().iterator();
			while (itr.hasNext()) {
				SessionWrapper w = itr.next();
				if (w.isExpired() || force) {
					expired.add(w.getSessionID());
				}
			}
		}
		
		// Now call dispose to remove all the expired elements.
		Iterator<String> itr = expired.iterator();
		while (itr.hasNext()) {
			disposeSession(itr.next());
		}
	}
	
	static interface SessionData {
		void destroy();
	}
	
	private class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			logger.logDebug("Running SessionnManager clean timer task");
			runTimerCleanup(false);
		}
	}

	private class SessionWrapper {
		private final String sessionID;
		private final T sessionData;
		private final int timeoutPeriod;
		private long lastTouched = System.currentTimeMillis();
		
		SessionWrapper(String sessionID, T sessionData, int timeoutPeriod) {
			this.sessionID = sessionID;
			this.sessionData = sessionData;
			this.timeoutPeriod = timeoutPeriod;
		}
		
		boolean isExpired() {
			return (timeoutPeriod != NO_TIMEOUT)
				&& (lastTouched + timeoutPeriod) < System.currentTimeMillis();
		}

		public String getSessionID() {
			return sessionID;
		}

		public T getSessionData() {
			lastTouched = System.currentTimeMillis();
			return sessionData;
		}
	}
}

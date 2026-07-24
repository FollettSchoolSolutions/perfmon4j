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
package org.perfmon4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class ExternalThreadTraceConfig  extends ThreadTraceConfig  {
	private PerfMonData data = null;

	public boolean hasData() {
		return data != null;
	}

	public PerfMonData getData() {
		return data;
	}

	public void outputData(PerfMonData data) {
		this.data = data;
	}

	/**
	 * Reflectively invoked by BeanHelper.setValue() when an on-demand scheduled thread
	 * trace's extraParams includes a "Trigger" key (see FieldKey.THREAD_TRACE_TRIGGER_ARG /
	 * FieldKey.encodeTriggerArg). Only the three HTTP-request-associated trigger types are
	 * supported for remote/on-demand scheduling -- ThreadNameTrigger and
	 * ThreadPropertyTrigger remain XML-config-only.
	 *
	 * @param encoded Base64 URL-safe (no padding) encoding of "PREFIX:name=value"
	 * @throws IllegalArgumentException if encoded is malformed or names an unsupported trigger type
	 */
	public void setTrigger(String encoded) {
		String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);

		int colonIndex = decoded.indexOf(':');
		if (colonIndex < 0) {
			throw new IllegalArgumentException("Invalid trigger encoding, missing ':' separator: " + decoded);
		}
		String prefix = decoded.substring(0, colonIndex);
		String nameValue = decoded.substring(colonIndex + 1);

		int equalsIndex = nameValue.indexOf('=');
		if (equalsIndex < 0) {
			throw new IllegalArgumentException("Invalid trigger encoding, missing '=' separator: " + decoded);
		}
		String name = nameValue.substring(0, equalsIndex);
		String value = nameValue.substring(equalsIndex + 1);

		TriggerType type = TriggerType.fromPrefix(prefix);
		if (type == null) {
			throw new IllegalArgumentException("Unrecognized trigger type prefix: " + prefix);
		}

		Trigger trigger;
		if (type == TriggerType.HTTP_REQUEST_PARAM) {
			trigger = new HTTPRequestTrigger(name, value);
		} else if (type == TriggerType.HTTP_SESSION_PARAM) {
			trigger = new HTTPSessionTrigger(name, value);
		} else if (type == TriggerType.HTTP_COOKIE_PARAM) {
			trigger = new HTTPCookieTrigger(name, value);
		} else {
			throw new IllegalArgumentException("Trigger type not supported for on-demand scheduling: " + prefix);
		}

		setTriggers(new Trigger[]{trigger});
	}

	public static class Queue {
		private final Object lockToken = new Object();
		private final List<ExternalThreadTraceConfig> list = new ArrayList<ExternalThreadTraceConfig>();
		private volatile boolean pendingElements;
		
		public boolean hasPendingElements() {
			return pendingElements;
		}
		
		public void schedule(ExternalThreadTraceConfig config) {
			synchronized (lockToken) {
				list.add(config);
				pendingElements = true;
			}
		}
		
		/** @return true if config was still queued (and so was actually removed) */
		public boolean unSchedule(ExternalThreadTraceConfig config) {
			synchronized (lockToken) {
				boolean removed = list.remove(config);
				pendingElements = list.size() > 0;
				return removed;
			}
		}

		public ExternalThreadTraceConfig assignToThread() {
			ExternalThreadTraceConfig result = null;
			synchronized (lockToken) {
				for (int i = 0; i < list.size() && result == null; i++) {
					if (list.get(i).shouldTrace()) {
						result = list.remove(i);
					}
				}
				pendingElements = list.size() > 0;
			}
			return result;
		}
	}
}

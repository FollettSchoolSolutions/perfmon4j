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

import java.util.ArrayList;
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
		
		public void unSchedule(ExternalThreadTraceConfig config) {
			synchronized (lockToken) {
				list.remove(config);
				pendingElements = list.size() > 0;
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

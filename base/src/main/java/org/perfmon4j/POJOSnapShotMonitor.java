/*
 *	Copyright 2008 Follett Software Company 
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

import org.perfmon4j.POJOSnapShotMonitor.POJODataSnapShot;
import org.perfmon4j.POJOSnapShotRegistry.POJOInstance;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotPOJOLifecycle;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class POJOSnapShotMonitor extends SnapShotMonitorBase<POJODataSnapShot[]> {
    private final static Logger logger = LoggerFactory.initLogger(POJOSnapShotMonitor.class);

    private final String pojoClassName;
    private final POJOSnapShotRegistry registry;

/*----------------------------------------------------------------------------*/    
    public POJOSnapShotMonitor(String name, boolean usePriorityTimer, String pojoClassName, POJOSnapShotRegistry registry) {
        super(name, usePriorityTimer);
        this.registry = registry;
        this.pojoClassName = pojoClassName;
    }

	@Override
	public POJODataSnapShot[] initSnapShot(long currentTimeMillis) {
		List<POJODataSnapShot> result = new ArrayList<POJODataSnapShot>();
		
		for (POJOInstance instance : registry.getInstances(pojoClassName)) {
			Object pojo = instance.getItem();
			if (pojo != null) {
				Bundle snapShotBundle = instance.getSnapShotBundle();
				
				SnapShotData data = snapShotBundle.newSnapShotData();
				((SnapShotGenerator.SnapShotLifecycle)data).init(pojo, currentTimeMillis);
				
				result.add(new POJODataSnapShot(data, instance));
			}
		}
		
		if (logger.isDebugEnabled() && result.isEmpty()) {
			logger.logDebug("POJOSnapShotMonitor (" + this.getName() + ") does not have any active SnapShot Providers");
		}
		
		return result.toArray(new POJODataSnapShot[] {});
	}

	@Override
	public POJODataSnapShot[] takeSnapShot(POJODataSnapShot[] dataArray, long currentTimeMillis) {
		List<POJODataSnapShot> result = new ArrayList<POJODataSnapShot>();
		
		for (POJODataSnapShot data : dataArray) {
			POJOInstance instance = data.getPojoInstance();
			Object pojo = instance.getItem();
			if (pojo != null) {
				((SnapShotGenerator.SnapShotLifecycle)data.getSnapShotData()).takeSnapShot(pojo, currentTimeMillis);
				result.add(data);
			}
		}
		return result.toArray(new POJODataSnapShot[] {});
	}

	@Override
	protected void appendData(Appender appender, POJODataSnapShot[] dataArray) {
		for (POJODataSnapShot data : dataArray) {
			SnapShotData snapShotData = data.getSnapShotData();
			snapShotData.setName(getName());
			String instanceName = data.getPojoInstance().getInstanceName();
			if (instanceName != null) {
				((SnapShotPOJOLifecycle)snapShotData).setInstanceName(instanceName);
			}
			appender.appendData(snapShotData);
		}
	}

    public static class POJODataSnapShot {
    	private final SnapShotData snapShotData;
    	private final POJOInstance pojoInstance;
    	
		public POJODataSnapShot(SnapShotData snapShotData, POJOInstance pojoInstance) {
			this.snapShotData = snapShotData;
			this.pojoInstance = pojoInstance;
		}

		public SnapShotData getSnapShotData() {
			return snapShotData;
		}

		public POJOInstance getPojoInstance() {
			return pojoInstance;
		}
    }
}

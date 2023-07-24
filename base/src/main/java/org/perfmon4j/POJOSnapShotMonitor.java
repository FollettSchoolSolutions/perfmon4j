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


import org.perfmon4j.POJOSnapShotMonitor.POJODataSnapShot;
import org.perfmon4j.POJOSnapShotRegistry.POJOInstance;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class POJOSnapShotMonitor extends SnapShotMonitorBase<POJODataSnapShot[]> {
    private final static Logger logger = LoggerFactory.initLogger(POJOSnapShotMonitor.class);

    private final String pojoClassName;
    private final POJOSnapShotRegistry registry;
    

/*----------------------------------------------------------------------------*/    
    public POJOSnapShotMonitor(String name, boolean usePriorityTimer, String pojoClassName, POJOSnapShotRegistry registry) {
        super(name, false);
        this.registry = registry;
        this.pojoClassName = pojoClassName;
    }

	@Override
	public POJODataSnapShot[] initSnapShot(long currentTimeMillis) {
//		List<POJODataSnapShot> result = new ArrayList<POJODataSnapShot>();
//		
//		for (registry.getInstances(class))
//		
		
		// TODO Auto-generated method stub
		return super.initSnapShot(currentTimeMillis);
	}

	@Override
	public POJODataSnapShot[] takeSnapShot(POJODataSnapShot[] data, long currentTimeMillis) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void appendData(Appender appender, POJODataSnapShot[] dataArray) {
		for (POJODataSnapShot pojoData : dataArray) {
			SnapShotData data = pojoData.getSnapShotData();
			data.setName(getName());
			appender.appendData(data);
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

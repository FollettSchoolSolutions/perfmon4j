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


import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public class SnapShotProviderWrapper extends SnapShotMonitor implements SnapShotMonitorLifecycle {
    final private static Logger logger = LoggerFactory.initLogger(SnapShotProviderWrapper.class);

    final private SnapShotGenerator.Bundle bundle;
    
/*----------------------------------------------------------------------------*/    
    public SnapShotProviderWrapper(String name, JavassistSnapShotGenerator.Bundle bundle) throws GenerateSnapShotException {
        super(name, bundle.isUsePriorityTimer());
        this.bundle = bundle;
    }
    

    public SnapShotData initSnapShot(long currentTimeMillis) {
    	SnapShotData result =  bundle.newSnapShotData();
    	((SnapShotGenerator.SnapShotLifecycle)result).init(bundle.getProviderInstance(), currentTimeMillis);
    	
    	return result;
    }

    public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
    	((SnapShotGenerator.SnapShotLifecycle)data).takeSnapShot(bundle.getProviderInstance(), currentTimeMillis);
    	return data;
    }
}

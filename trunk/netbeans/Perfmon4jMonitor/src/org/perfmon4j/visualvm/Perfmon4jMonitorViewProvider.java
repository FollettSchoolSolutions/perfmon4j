/*
 *	Copyright 2011-2012 Follett Software Company 
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
package org.perfmon4j.visualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;


/**
 *
 * @author ddeucher
 */
public class Perfmon4jMonitorViewProvider extends DataSourceViewProvider<Application> {

    public static final DataSourceViewProvider<Application>
            instance = new Perfmon4jMonitorViewProvider();

    static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(instance, Application.class);
    }
    
    static void unregister() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(instance);
    }    
    

    @Override
    protected boolean supportsViewFor(Application app) {
        return Perfmon4jModel.getModelForApp(app) != null;
    }

    @Override
    protected DataSourceView createView(Application app) {
        return new Perfmon4jMonitorView(app);
    }
}

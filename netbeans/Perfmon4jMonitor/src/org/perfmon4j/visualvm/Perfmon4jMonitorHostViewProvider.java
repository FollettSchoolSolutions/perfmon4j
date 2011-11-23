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

package org.perfmon4j.visualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.host.Host;


/**
 *
 * @author ddeucher
 */
public class Perfmon4jMonitorHostViewProvider extends DataSourceViewProvider<Host> {

    private static final DataSourceViewProvider<Host>
            instance = new Perfmon4jMonitorHostViewProvider();

    static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(instance, Host.class);
    }
    
    static void unregister() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(instance);
    }    
    

    @Override
    protected boolean supportsViewFor(Host host) {
        return Perfmon4jModel.getModelForApp(new ApplicationWrapper(host)) != null;
    }

    @Override
    protected DataSourceView createView(Host host) {
        return new Perfmon4jMonitorView(new ApplicationWrapper(host));
    }
    
    public static class ApplicationWrapper extends Application {
        ApplicationWrapper(Host host) {
            super(host, host.getHostName());
        }
    }
}

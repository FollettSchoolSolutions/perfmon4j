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
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.model.Model;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.sa.SaModel;
import com.sun.tools.visualvm.tools.sa.SaModelFactory;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;

public class Perfmon4jModel extends Model {

    private final RemoteManagementWrapper remoteWrapper;
    static private final Map<Application, Perfmon4jModel> modelMap = new WeakHashMap<Application, Perfmon4jModel>();
    static private final Pattern pattern = Pattern.compile(".*?-javaagent:\\S*?perfmon4j\\S*?.jar=\\S+?-p(\\d+).*");

    @SuppressWarnings("CallToThreadDumpStack")
    public static Perfmon4jModel getModelForApp(Application app) {
        Perfmon4jModel model = modelMap.get(app);
        if (model == null) {
            try {
                model = new Perfmon4jModel(app);
                modelMap.put(app, model);
            } catch (InstException ex) {
                System.out.println("Could not find Perfmon4j installed in app: " + app.getPid());
            }
        }
        return model;
    }

    private Integer getListenerPortViaCommandLine(Application app) {
        Integer result = null;
        try {
            Jvm model = JvmFactory.getJVMFor(app);
            if (model != null) {
                String jvmArgs = model.getJvmArgs();
                if (jvmArgs != null) {
                    Matcher m = pattern.matcher(jvmArgs);
                    if (m.matches()) {
                        result = Integer.valueOf(m.group(1));
                    }
                }
            }
        } catch (java.lang.UnsupportedOperationException ex) {
        }
        return result;
    }

    private Properties getSystemProperiesOfApp(Application app) {
        Properties result = null;
        
        if (result == null) {
            // Try via Jvm
            try {
                Jvm model = JvmFactory.getJVMFor(app);
                if (model != null) {
                    result = model.getSystemProperties();
                }
            } catch (java.lang.UnsupportedOperationException ex) {
                // Nothing todo
            }
        }

        if (result == null) {
            // Try getting via an attach model
            try {
                AttachModel model = AttachModelFactory.getAttachFor(app);
                if (model != null) {
                    result = model.getSystemProperties();
                }
            } catch (java.lang.UnsupportedOperationException ex) {
                // Nothing todo
            }
        }

        if (result == null) {
            // Try getting via Serviceability Agent (Only available for apps 
            // on Linux or Solaris.
            try {
                SaModel model = SaModelFactory.getSAAgentFor(app);
                if (model != null) {
                    result = model.getSystemProperties();
                }
            } catch (java.lang.UnsupportedOperationException ex) {
                // Nothing todo
            }

        }

        if (result == null) {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
            
            if (jmxModel != null) {
                result = jmxModel.getSystemProperties();
//                MBeanServerConnection conn = jmxModel.getMBeanServerConnection();
//                result = jmxModel.getSystemProperties();
            }
        }

        return result;
    }

    private Perfmon4jModel(Application app) throws InstException {
        int port = 0;
        Properties props = getSystemProperiesOfApp(app);
        if (props != null) {
            String listenerPort = props.getProperty(RemoteInterface.P4J_LISTENER_PORT);
            if (listenerPort == null) {
                throw new InstException("Perfmon4j Not Installed In App: " + app.getPid());
            }

            try {
                port = Integer.parseInt(listenerPort);
            } catch (NumberFormatException nfe) {
                throw new InstException("Application does not include Perfmon4j agent");
            }
        } else {
            Integer p = getListenerPortViaCommandLine(app);
            if (p == null) {
                throw new InstException("Application does not include Perfmon4j agent");
            } else {
                port = p.intValue();
                System.err.println("Unable to retrieve System.properties of application... Found Perfmon4j port via command line args");
            }
        }
        
        String host = "localhost";
        if (!app.isLocalApplication()) {
            host = app.getHost().getHostName();
        }

        try {
            remoteWrapper = RemoteManagementWrapper.open(host, port);
        } catch (Exception ex) {
            throw new InstException("Could not look it up", ex);
        }
    }

    public RemoteManagementWrapper getRemoteWrapper() {
        return remoteWrapper;
    }

    private static class InstException extends Exception {

        public InstException(String message) {
            super(message);
        }

        public InstException(String message, Throwable th) {
            super(message, th);
        }
    }
}

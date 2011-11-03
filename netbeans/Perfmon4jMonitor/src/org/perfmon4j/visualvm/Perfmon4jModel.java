/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.perfmon4j.visualvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.model.Model;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import java.util.Map;
import java.util.WeakHashMap;
import org.perfmon4j.remotemanagement.intf.RemoteInterface;
import org.perfmon4j.remotemanagement.intf.RemoteManagementWrapper;

public class Perfmon4jModel extends Model {
    private final RemoteManagementWrapper remoteWrapper;
    
    static private final Map<Application, Perfmon4jModel> modelMap 
        = new WeakHashMap<Application, Perfmon4jModel>();
    

    @SuppressWarnings("CallToThreadDumpStack")
    public static Perfmon4jModel getModelForApp(Application app) {
        Perfmon4jModel model = modelMap.get(app);
        if (model == null) {
            try {
                model = new Perfmon4jModel(app);
                modelMap.put(app, model);
            } catch (InstException ex) {
                // Really no reason to throw an exception here...
                // In most cases this is just not a Perfmon4j enabled 
                // app.
                ex.printStackTrace();
            }
        } 
        return model;
    }
    
    
    private Perfmon4jModel(Application app) throws InstException {
        AttachModel attachModel = AttachModelFactory.getAttachFor(app);
        int port = 5959;
        
        if (attachModel == null) {
            // Forced to try default port....
            System.err.println("Unable to use attach model for app: "  + app.getPid() 
                    + " Trying default port: " + 5959);
        }  else {
            /**
             * TODO:  Use an optional connection to the JMX Monitor to check for
             * perfmon4j..   Based on my reading of the attach model, it looks like
             * access could be limited to systems that are on the same virtual machine.
             */ 
            String listenerPort = attachModel.getSystemProperties().getProperty(RemoteInterface.P4J_LISTENER_PORT);
            if (listenerPort == null) {
                throw new InstException("Perfmon4j Not Installed In App: " + app.getPid());
            }
            try {
                port = Integer.parseInt(listenerPort);
            } catch (NumberFormatException nfe) {
                throw new InstException("Not a valid port number");
            }
        }
        try {
            remoteWrapper = RemoteManagementWrapper.open("localhost", port);
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

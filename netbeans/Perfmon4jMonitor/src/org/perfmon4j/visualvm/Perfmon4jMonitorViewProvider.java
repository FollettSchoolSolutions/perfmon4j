/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    private static final DataSourceViewProvider<Application>
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.perfmon4j.visualvm;

import org.openide.modules.ModuleInstall;


public class Installer extends ModuleInstall {
    @Override
    public void restored() {
        Perfmon4jMonitorViewProvider.initialize();
    }

    @Override
    public void uninstalled() {
        Perfmon4jMonitorViewProvider.unregister();
    }
}

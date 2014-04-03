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



import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JOptionPane;

/**
 * This serves as an example of how to add a menu item to the VisualVM explorer
 * tree.   We don't have a use for this right now, but if you want to
 * try it, uncomment the "folder" element in layer.xml.
 * 
 * For this example, the menu item will be added to each "host" entry.
 * 
 */
public class Perfmon4jAction extends SingleDataSourceAction<Host> {
//    public static final Perfmon4jAction instance = new Perfmon4jAction();
    
    public Perfmon4jAction() {
        super(Host.class);
        putValue(Action.NAME, "Just an Example...");
        putValue(Action.SHORT_DESCRIPTION, "This shows how to add a menu item");
    }
    
    
    @Override
    protected void actionPerformed(Host host, ActionEvent ae) {
        JOptionPane.showConfirmDialog(null, "Congratulations you added a menu item to the explorer window");
    }

    @Override
    protected boolean isEnabled(Host host) {
        return true;
    }
    
}

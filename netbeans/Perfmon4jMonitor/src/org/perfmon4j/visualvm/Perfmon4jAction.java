/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

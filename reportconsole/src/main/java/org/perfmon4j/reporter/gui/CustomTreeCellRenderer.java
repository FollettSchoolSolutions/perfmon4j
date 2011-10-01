package org.perfmon4j.reporter.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = 1L;

	public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

		super.getTreeCellRendererComponent(
            tree, value, sel,
            expanded, leaf, row,
            hasFocus);
		
		if (value instanceof ToolTipInfo) {
			ToolTipInfo i = (ToolTipInfo)value;
			String toolTip = i.getToolTip();
			if (toolTip != null) {
				setToolTipText(i.getToolTip());
				this.setForeground(Color.GREEN);
			}
			
			
		}
		
		return this;
	}	
}

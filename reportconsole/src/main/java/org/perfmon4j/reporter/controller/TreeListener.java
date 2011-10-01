package org.perfmon4j.reporter.controller;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeNode;

import org.perfmon4j.reporter.App;
import org.perfmon4j.reporter.model.Model;
import org.perfmon4j.reporter.model.P4JTreeNode;

public class TreeListener implements HierarchyListener, ComponentListener, MouseListener, TreeSelectionListener  {

	public void hierarchyChanged(HierarchyEvent arg0) {
		System.out.println(arg0);
	}

	public void componentHidden(ComponentEvent e) {
		System.out.println(e);
	}

	public void componentMoved(ComponentEvent e) {
		System.out.println(e);
	}

	public void componentResized(ComponentEvent e) {
		System.out.println(e);
	}

	public void componentShown(ComponentEvent e) {
		System.out.println(e);
	}

	public void mouseClicked(MouseEvent arg0) {
		System.out.println(arg0);
	}

	public void mouseEntered(MouseEvent arg0) {
		System.out.println(arg0);
	}

	public void mouseExited(MouseEvent arg0) {
		System.out.println(arg0);
	}

	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger()) {
			App.getApp().getTreePopupMenu().show(e.getComponent(), e.getX(), e.getY());
		}
		
		System.out.println(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			App.getApp().getTreePopupMenu().show(e.getComponent(), e.getX(), e.getY());
		}
		System.out.println(e);
	}

	public void valueChanged(TreeSelectionEvent e) {
		Object node = e.getNewLeadSelectionPath().getLastPathComponent();
		Model.getModel().getConnectionList().setActiveNode((P4JTreeNode)node);
	}
}

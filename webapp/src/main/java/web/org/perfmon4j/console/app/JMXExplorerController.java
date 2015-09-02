/*
 *	Copyright 2015 Follett School Solutions 
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
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package web.org.perfmon4j.console.app;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

import web.org.perfmon4j.console.app.zk.RefreshableComposer;


public class JMXExplorerController extends RefreshableComposer<Component>  {
	private static final long serialVersionUID = 1L;

	private MBeanServer server = ManagementFactory.getPlatformMBeanServer(); 
	
	@Wire
	private Component jmxExplorerWindow;
	
	@Wire 
	private Tree jmxTree;
	
	@Wire 
	private Textbox filterTextBox;
	
	@Wire
	private Checkbox hasAttributesXBox;
	
	@Wire
	private Checkbox hasOperationsXBox;
	
	private JMXDomain rootDomain;
	
	
    @Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		rootDomain = generateDomainHierarchy();
		jmxTree.setModel(new JMXTreeModel(buildTreeNode(null, rootDomain)));
		jmxTree.setItemRenderer(new JMXTreeRenderer());
    }


	@Override
	protected void handleRefreshEvent(Event event) {
		jmxTree.setModel(new JMXTreeModel(buildTreeNode(null, rootDomain)));
	}
	
	@Listen("onClick = #applyFilterButton")
	public void createOauthToken(Event event) {
		rootDomain.applyFilter(filterTextBox.getText(), hasAttributesXBox.isChecked(), hasOperationsXBox.isChecked());
		handleRefreshEvent(event);
	}	
	
	private JMXDomain generateDomainHierarchy() throws Exception {
		JMXDomain result = new JMXDomain("root");
		
		Set<ObjectName> set = server.queryNames(new ObjectName("*:*"), null);
		for (ObjectName i : set) {
			String[] domainParts = i.getDomain().split("\\.");
			JMXDomain parent = result;
			for (String subDomain : domainParts) {
				JMXDomain child = parent.getSubDomain(subDomain);
				if (child == null) {
					child = new JMXDomain(subDomain); 
					parent.getChildren().add(child);
				}
				parent = child;
			}
			parent.getChildren().add(new JMXObject(i));
		}
		
		return result;
	}
	
	
	private JMXTreeNode buildTreeNode(JMXDomain parent, JMXElement child) {
		JMXTreeNode result = null;
		if (child instanceof JMXObject) {
			if (child.isVisible()) {
				result = new JMXTreeNode(child);
			}
		} else {
			List<JMXTreeNode> children = new ArrayList<JMXTreeNode>();
			JMXDomain c = (JMXDomain)child;
			for (JMXElement c2 : c.getChildren()) {
				JMXTreeNode newChild = buildTreeNode(c, c2);
				if (newChild != null) {
					children.add(newChild);
				}
			}
			if (!children.isEmpty()) {
				result = new JMXTreeNode(child, children);
			}
		}
		
		return result;
	}
	
	
	private abstract static class JMXElement implements Comparable<JMXElement> {
		private final String name;
		
		protected JMXElement(String name) {
			this.name = name;
		}
		
		
		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		public abstract void applyFilter(String filter, boolean hasAttributes, boolean hasOperations);

		public abstract boolean isVisible();
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			JMXElement other = (JMXElement) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}


		@Override
		public int compareTo(JMXElement o) {
			return this.name.compareTo(o.name);
		}
	} 
	
	private static class JMXDomain extends JMXElement {
		private Set<JMXElement> children = new TreeSet<JMXElement>();
		
		public JMXDomain(String name) {
			super(name);
		}
		
		public Set<JMXElement> getChildren() {
			return children;
		}
		
		public JMXDomain getSubDomain(String name) {
			JMXDomain result = null;
			
			Iterator<JMXElement> iter = children.iterator();
			while (iter.hasNext() && result == null) {
				JMXElement element = iter.next();
				if (name.equals(element.getName()) && element instanceof JMXDomain) {
					result = (JMXDomain)element;
				}
			}
			return result;
		}
		
		public String toString() {
			return getName();
		}

		@Override
		public void applyFilter(String filter, boolean hasAttributes, boolean hasOperations) {
			for (JMXElement element : children) {
				element.applyFilter(filter, hasAttributes, hasOperations);
			}
		}

		@Override
		public boolean isVisible() {
			boolean result = false;
			for (JMXElement element : children) {
				result = element.isVisible();
				if (result) {
					break;
				}
			}
			return result;
		}
	}
	
	private class JMXObject extends JMXElement {
		private final MBeanInfo info;
		private final ObjectName objectName;
		private boolean visible = true;
		
		public JMXObject(ObjectName objectName) {
			super(objectName.toString());
			this.objectName = objectName;
			MBeanInfo i = null;
			try {
				i = server.getMBeanInfo(objectName);
			} catch (Exception e) {
				// ignore
			} 
			this.info = i;
		}

		public ObjectName getObjectName() {
			return objectName;
		}
		
		public String toString() {
			return objectName.toString();
		}

		@Override
		public void applyFilter(String filter, boolean hasAttributes, boolean hasOperations) {
			visible = filter == null || filter.isEmpty() || this.getName().toLowerCase().contains(filter.toLowerCase());
			if (visible && hasAttributes) {
				visible = (info != null) && info.getAttributes().length > 0;
			}
			if (visible && hasOperations) {
				visible = (info != null) && info.getOperations().length > 0;
			}
		}

		@Override
		public boolean isVisible() {
			return visible;
		}
	}
	
	private static class JMXTreeNode extends DefaultTreeNode<JMXElement> {
		private static final long serialVersionUID = 1L;

		public JMXTreeNode(JMXElement data,
				Collection<? extends TreeNode<JMXElement>> children) {
			super(data, children);
		}

		public JMXTreeNode(JMXElement data) {
			super(data);
		}
	}
	
    private static final class JMXTreeModel extends DefaultTreeModel<JMXElement> {
		private static final long serialVersionUID = 1L;

		public JMXTreeModel(TreeNode<JMXElement> root) {
			super(root);
		}
    }

    private final class JMXTreeRenderer implements TreeitemRenderer<JMXTreeNode> {

		@Override
		public void render(Treeitem treeItem, JMXTreeNode treeNode, int index)
				throws Exception {
			
			JMXElement element = (JMXElement)treeNode.getData();
			Treerow dataRow = new Treerow();
			dataRow.setParent(treeItem);
			treeItem.setValue(treeNode);
			
			Treecell treeCell = new Treecell();
			treeCell.setParent(dataRow);
			if (element instanceof JMXObject) {
				final JMXObject objectNode = (JMXObject)element;
				Label label = new Label(objectNode.getName());
				treeCell.appendChild(label);
				dataRow.addEventListener(Events.ON_DOUBLE_CLICK, new EventListener<Event>() {

					@Override
					public void onEvent(Event event) throws Exception {
						EditJMXObjectController.openTab(jmxExplorerWindow, objectNode.getObjectName());
					}
				});
				
			} else {
				JMXDomain domainNode = (JMXDomain)element;
				String domainName = domainNode.getName();
				Label label = new Label(domainName);
				treeCell.appendChild(label);
			}
			
			
		}
    }
    
    
	

}

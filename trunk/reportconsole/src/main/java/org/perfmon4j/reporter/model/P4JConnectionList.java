package org.perfmon4j.reporter.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class P4JConnectionList extends P4JTreeNode<P4JTreeNode, P4JConnection> {
	public static interface NodeChangedListener {
		public void connectionChanged(P4JConnection newConnection, P4JConnection oldConnection);
		public void nodeChanged(P4JTreeNode newNode, P4JTreeNode oldNode);
	}
	
	private P4JConnection activeConnection = null;
	private P4JTreeNode activeNode = null;
	private Set<NodeChangedListener> listeners = new HashSet<NodeChangedListener>();
	
	public P4JConnectionList() {
		super("Connections", true);
	}

	public void addConnection(P4JConnection c) {
		c.setParent(this);
		addChild(c);
		setActiveNode(c);
	}
	
	public P4JConnection getActiveConnection() {
		return activeConnection;
	}

	public P4JTreeNode getActiveNode() {
		return activeNode;
	}
	
	public IntervalCategory getCurrentIntervalCategory() {
		if (activeNode != null && activeNode.getType()== P4JTreeNode.Type.P4JIntervalCategory) {
			return (IntervalCategory)activeNode;
		} else {
			return null;
		}
	}
	
	public void addListener(NodeChangedListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(NodeChangedListener listener) {
		listeners.remove(listener);
	}
	
	public void setActiveNode(P4JTreeNode node) {
		P4JTreeNode oldNode = activeNode;
		P4JConnection oldConnection = activeConnection;
		
		activeNode = node;
		if (node != null) {
			if (node.getType() == Type.P4JConnectionList) {
				;
			} else if (node.getType() == Type.P4JConnection) {
				activeConnection = (P4JConnection)node;
			} else {
				while (node != null && node.getType() != Type.P4JConnection) {
					node = node.getParent();
				}
				activeConnection = (P4JConnection)node;
			}
		} else {
			activeConnection = null;
		}
		if (!nodesMatch(activeConnection, oldConnection)) {
			fireConnectionChangeEvent(activeConnection, oldConnection);
		}
		if (!nodesMatch(activeNode, oldNode)) {
			fireNodeChangeEvent(activeNode, oldNode);
		}
	}

	private static boolean nodesMatch(P4JTreeNode a, P4JTreeNode b) {
		return (a == null ? b == null : a.equals(b));
	}

	@Override
	public P4JTreeNode.Type getType() {
		return P4JTreeNode.Type.P4JConnectionList;
	}
	
	private void fireConnectionChangeEvent(P4JConnection newConnection, P4JConnection oldConnection) {
		Iterator<NodeChangedListener> itr = listeners.iterator();
		while (itr.hasNext()) {
			itr.next().connectionChanged(newConnection, oldConnection);
		}
	}
	
	private void fireNodeChangeEvent(P4JTreeNode newNode, P4JTreeNode oldNode) {
		Iterator<NodeChangedListener> itr = listeners.iterator();
		while (itr.hasNext()) {
			itr.next().nodeChanged(newNode, oldNode);
		}
	}

}

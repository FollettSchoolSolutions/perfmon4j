package org.perfmon4j.reporter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.TreeNode;

public abstract class P4JTreeNode<P extends P4JTreeNode, T extends P4JTreeNode> extends Object implements TreeNode {
	protected P parent = null;
	
	public static enum Type {
		P4JConnectionList,
		P4JConnection,
		P4JCategory,
		P4JIntervalCategory
	}
	
	private final List<T> children = new ArrayList<T>();
	private final Map<String, T> childMap = new HashMap<String, T>();
	private final String name;
	private final boolean allowsChildren;
	
	protected P4JTreeNode(String name, boolean allowsChildren) {
		this.name = name;
		this.allowsChildren = allowsChildren;
	}

	public abstract Type getType();
	
	public String getName() {
		return name;
	}
	
	public Enumeration<T> children() {
		return Collections.enumeration(children);
	}

	public boolean getAllowsChildren() {
		return allowsChildren;
	}

	public T getChildAt(int index) {
		return children.get(index);
	}

	public int getChildCount() {
		return children.size();
	}

	public int getIndex(TreeNode node) {
		return children.indexOf(node);
	}

	public P getParent() {
		return parent;
	}
	
	void setParent(P parent) {
		this.parent = parent;
	}

	public boolean isLeaf() {
		return !allowsChildren || (getChildCount()==0);
	}
	
	protected void addChild(T child) {
		children.add(child);
		childMap.put(child.getName(), child);
	}
	
	protected void clearChildren() {
		children.clear();
		childMap.clear();
	}
	
	public T getChild(String name) {
		return childMap.get(name);
	}
	
	public String toString() {
		return name;
	}
}

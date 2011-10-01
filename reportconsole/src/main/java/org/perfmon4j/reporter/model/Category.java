package org.perfmon4j.reporter.model;


public class Category<T extends P4JTreeNode> extends P4JTreeNode<P4JTreeNode, T> {
	public Category(String name) {
		super(name, true);
	}
	
	public void addCategory(T c) {
		c.setParent(this);
		addChild(c);
	}
	
	@Override
	public P4JTreeNode.Type getType() {
		return P4JTreeNode.Type.P4JCategory;
	}	
}

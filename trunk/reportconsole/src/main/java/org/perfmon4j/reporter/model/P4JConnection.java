package org.perfmon4j.reporter.model;


public class P4JConnection extends P4JTreeNode<P4JConnectionList, P4JTreeNode> {
	private final String url;
	
	public P4JConnection(String url) {
		super(url, true);
		this.url = url;
	}
	
	public void addCategory(P4JTreeNode c) {
		c.setParent(this);
		addChild(c);
	}
	
	void setParent(P4JConnectionList parent) {
		this.parent = parent;
	}
	
	protected void clearCategories() {
		clearChildren();
	}
	
	@Override
	public P4JTreeNode.Type getType() {
		return P4JTreeNode.Type.P4JConnection;
	}	
}

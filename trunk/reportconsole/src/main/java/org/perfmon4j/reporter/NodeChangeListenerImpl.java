package org.perfmon4j.reporter;

import org.perfmon4j.reporter.model.IntervalCategory;
import org.perfmon4j.reporter.model.Model;
import org.perfmon4j.reporter.model.P4JConnection;
import org.perfmon4j.reporter.model.P4JConnectionList;
import org.perfmon4j.reporter.model.P4JTreeNode;

public class NodeChangeListenerImpl implements P4JConnectionList.NodeChangedListener {
	private final App app;
	private final P4JConnectionList list = Model.getModel().getConnectionList();
	
	public NodeChangeListenerImpl(App app) {
		this.app = app;
	}
	
	public void connectionChanged(P4JConnection newConnection,
			P4JConnection oldConnection) {
		if (newConnection != null) {
			app.selectConnection.addItem(newConnection);
		}
		app.closeConnectionAction.setEnabled(list.getChildCount() > 0);
	}

	public void nodeChanged(P4JTreeNode newNode, P4JTreeNode oldNode) {
		IntervalCategory c = IntervalCategory.safeCast(newNode) ;
		boolean enabled = (c != null && c.getDatabaseID() != null);
		
		
		app.graphAction.setEnabled(enabled);
	}
}

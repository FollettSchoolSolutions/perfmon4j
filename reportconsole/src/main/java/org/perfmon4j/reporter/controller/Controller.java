package org.perfmon4j.reporter.controller;

public class Controller {
	private static final Controller controller = new Controller();
	
	private final TreeListener treeListener = new TreeListener();
	
	private Controller() {	
	}
	
	public static Controller getController() {
		return controller;
	}
	
	public TreeListener getTreeListener() {
		return treeListener;
	}
}

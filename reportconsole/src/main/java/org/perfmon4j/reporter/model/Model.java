package org.perfmon4j.reporter.model;

public class Model {
	private static Model model = new Model();
	
	private final P4JConnectionList connectionList = new P4JConnectionList();
	
	private Model() {
	}
	
	public static Model getModel() {
		return model;
	}
	
	public P4JConnectionList getConnectionList() {
		return connectionList;
	}
}

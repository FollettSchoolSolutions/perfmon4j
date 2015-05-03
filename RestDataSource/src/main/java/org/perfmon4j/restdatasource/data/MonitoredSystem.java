package org.perfmon4j.restdatasource.data;

public class MonitoredSystem {
	private String name;
	private String id;
	
	public MonitoredSystem() {
		super();
	}
	
	public MonitoredSystem(String name, String id) {
		super();
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getID() {
		return id;
	}
	public void setID(String id) {
		this.id = id;
	}
	@Override
	public String toString() {
		return "MonitoredSystem [name=" + name + ", id=" + id + "]";
	}
}

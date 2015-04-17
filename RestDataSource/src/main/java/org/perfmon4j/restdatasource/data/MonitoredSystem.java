package org.perfmon4j.restdatasource.data;

public class MonitoredSystem {
	private String name;
	private long id;
	
	public MonitoredSystem(String name, long id) {
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
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
}

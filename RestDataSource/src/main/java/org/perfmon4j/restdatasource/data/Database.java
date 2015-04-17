package org.perfmon4j.restdatasource.data;

public class Database {
	private boolean def;
	private String name;

	public Database(String name, boolean def) {
		this.name = name;
		this.def = def;
	}
	
	public boolean isDefault() {
		return def;
	}
	public void setDefault(boolean def) {
		this.def = def;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}

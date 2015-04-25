package org.perfmon4j.restdatasource.data;


public class Database {
	private boolean def;
	private String name;
	private String id;

	public Database(String name, boolean def, String id) {
		this.name = name;
		this.def = def;
		this.id = id;
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

	public boolean isDef() {
		return def;
	}

	public void setDef(boolean def) {
		this.def = def;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}

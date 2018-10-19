/*
 *	Copyright 2015 Follett School Solutions 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett School Solutions
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/

package web.org.perfmon4j.restdatasource.data;

public class MonitoredSystem implements Comparable<MonitoredSystem> {
	private String name;
	private String id;
	private boolean group;
	
	public MonitoredSystem() {
		super();
	}
	
	public MonitoredSystem(String name, String id) {
		this(name, id, false);
	}
	
	public MonitoredSystem(String name, ID id) {
		this(name, id.getDisplayable(), id.isGroup());
	}

	public MonitoredSystem(String name, String id, boolean group) {
		super();
		this.name = name;
		this.id = id;
		this.group = group;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	/* *
	 * A monitor can be associated with a system or a group of
	 * systems (group==true)
	 * 
	 * For a system (group==false) the ID will be
	 * in the following format:
	 * 	<databaseID>.<systemID>
	 * 
	 * DatabaseID is always a 4 character code and systemID will be an integer.
	 * examples:  PROD.1,  DALY.453, XVSS.21
	 *   
	 * For a group (group==true) the ID will be in the following format
	 * 		<databaseID>.GROUP.<groupID>
	 * examples:
	 *   
	 * @return
	 */
	public String getID() {
		return id;
	}
	public void setID(String id) {
		this.id = id;
	}
	
	public boolean isGroup() {
		return group;
	}

	public void setGroup(boolean group) {
		this.group = group;
	}

	@Override
	public String toString() {
		return "MonitoredSystem [name=" + name + ", id=" + id + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (group ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonitoredSystem other = (MonitoredSystem) obj;
		if (group != other.group)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	private String buildComparable() {
		// Want groups to sort before systems.
		if (isGroup()) {
			return "a" + name;
		} else {
			return "z" + name;
		}
	}
	
	@Override
	public int compareTo(MonitoredSystem o) {
		return buildComparable().compareTo(o.buildComparable());
	}
}

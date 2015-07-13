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

package web.org.perfmon4j.console.app.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;


@Entity
@Table(name = "AppConfig")
public class AppConfig {
	private Integer id = null;
	private boolean accessEnabled = true;
	private boolean anonymousAccessEnabled = true;
	
	@Type(type = "numeric_boolean")
	@Column(nullable=false)
	public boolean isAccessEnabled() {
		return accessEnabled;
	}

	public void setAccessEnabled(boolean accessEnabled) {
		this.accessEnabled = accessEnabled;
	}
	
	@Type(type = "numeric_boolean")
	@Column(nullable=false)
	public boolean isAnonymousAccessEnabled() {
		return anonymousAccessEnabled;
	}

	public void setAnonymousAccessEnabled(boolean anonymousAccessEnabled) {
		this.anonymousAccessEnabled = anonymousAccessEnabled;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable=false, unique=true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}	
}

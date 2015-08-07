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

package web.org.perfmon4j.console.app;

import org.springframework.security.core.context.SecurityContextHolder;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Menu;
import org.zkoss.zul.Window;

import web.org.perfmon4j.console.app.spring.security.Perfmon4jUserConsoleLoginService.Perfmon4jUser;

public class HeaderController extends SelectorComposer<Component> {
	private static final long serialVersionUID = -7098664190564412475L;

	@Wire Menu currentUserMenu;
	
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Perfmon4jUser user =  (Perfmon4jUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		String welcome = "Welcome " + user.getDisplayName();
		currentUserMenu.setLabel(welcome);
	}

	@Listen("onClick = #aboutMenuItem")
	public void about() {
		  Window window = (Window)Executions.createComponents(
				  "/app/about.zul", null, null);
		  window.doModal();
	}

	@Listen("onClick = #logoutMenuItem")
	public void logout() {
		Executions.sendRedirect("../j_spring_security_logout");
	}
	
	@Listen("onClick = #configureDataSources")
	public void configureDataSources() {
		Executions.sendRedirect("/app/datasources.zul");
	}

	@Listen("onClick = #jmxExplorer")
	public void jmxExplorer() {
		Executions.sendRedirect("/app/jmxExplorer.zul");
	}
	
	
	@Listen("onClick = #systemInfoMenuItem")
	public void systemInfo() {
		Executions.sendRedirect("/app/systeminfo.zul");
	}

	@Listen("onClick = #aedUsers")
	public void aedUsers() {
		Executions.sendRedirect("/app/users.zul");
	}
	
}



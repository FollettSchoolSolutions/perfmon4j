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

import org.perfmon4j.PerfMon;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

public class SystemInfoController  extends SelectorComposer<Component> {
	   private static final long serialVersionUID = 1L;
	     
		@Wire
	    Grid systemInfoGrid;

	    @Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Row row = new Row();
		row.appendChild(new Label("Java Version"));
		row.appendChild(new Label(System.getProperty("java.version")));
		
		
		systemInfoGrid.getRows().appendChild(row);

		row = new Row();
		row.appendChild(new Label("Perfmon4j enabled"));
		row.appendChild(new Label(Boolean.toString(PerfMon.isConfigured())));
		
		
		systemInfoGrid.getRows().appendChild(row);
	}
}

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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.core.UriBuilder;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

import web.org.perfmon4j.console.app.util.openmbean.TabularDataHelper;

public class EditJMXObjectController extends SelectorComposer<Component> {
	private static final long serialVersionUID = 1L;
	private static final String ATTRIBUTE_KEY = EditJMXObjectController.class.getName() + ".objectName";
	private ObjectName objectName;
	private MBeanAttributeInfo attributes[] = null;
	private MBeanOperationInfo operations[] = null;

	@Wire
	private Window editJMXObjectDialog;

	@Wire
	private Grid attributesGrid;

	@Wire
	private Grid operationsGrid;
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		
		String oName = Executions.getCurrent().getParameter("objectName");
		objectName = new ObjectName(oName);

		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		MBeanInfo info = server.getMBeanInfo(objectName);
		attributes = info.getAttributes();
		operations = info.getOperations();
		

		attributesGrid.setModel(new ListModelArray<MBeanAttributeInfo>(
				attributes, false));
		attributesGrid.setRowRenderer(new AttributesRowRender());
		
		operationsGrid.setModel(new ListModelArray<MBeanOperationInfo>(
				operations, false));
		operationsGrid.setRowRenderer(new OperationsRowRender());
	}

	public static void openTab(Component parent, ObjectName objectName) {
		Map<String, Object> arguments = null;
		if (objectName != null) {
			arguments = new HashMap<String, Object>();
			arguments.put("objectName", objectName);
		}
		
		Execution current = Executions.getCurrent();
		
		current.setAttribute(ATTRIBUTE_KEY , objectName);
		
		UriBuilder builder = UriBuilder.fromUri("/app/editJMXObject.zul")
			.queryParam("objectName", objectName.toString());
		
		current.sendRedirect(builder.build().toString(), "_blank");
		
//		
//		Window window = (Window) Executions.createComponents(
//				"/app/editJMXObject.zul", parent, arguments);
//		window.doModal();
	}

	private class OperationsRowRender implements RowRenderer<MBeanOperationInfo> {

		@Override
		public void render(Row row, MBeanOperationInfo operation, int whatIsThis)
				throws Exception {
			
			row.appendChild(new Label(operation.getName()));
			row.appendChild(new Label(operation.getReturnType()));
			row.appendChild(new Label(operation.getDescription()));
		}
	}
	
	
	private class AttributesRowRender implements
			RowRenderer<MBeanAttributeInfo> {

		@Override
		public void render(Row row, MBeanAttributeInfo attr, int whatIsThis)
				throws Exception {
			final boolean readable = attr.isReadable();
			final boolean writeable = attr.isWritable();
			
			String description = attr.getDescription();
			
			
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			Object obj = null;
			try {
				obj = server.getAttribute(objectName, attr.getName());
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println("Error reading attribute: " + attr.getName() + " Message: " + ex.getMessage() );
			}
			
			if (obj != null && obj.getClass().getName().contains("TabularData")) {
				try {
					List<String> values = new TabularDataHelper().extractTabularData(obj);
					if (values != null) {
						obj = values.toArray();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					// Ignore we might not have the openmbeans classes loades.
				}
			}
			
			final boolean isArray = obj != null && obj.getClass().isArray();
			
			// Column 1 is the Name (i.e. LoggerNames)
			row.appendChild(new Label(attr.getName()));

			
			// Column 2 is the Value 
			if (isArray) {
				row.appendChild(buildLabelForArray(obj));
			} else {
				row.appendChild(buildLabel(obj));
			}

			String type = attr.getType();
			if (type.startsWith("[L") && type.endsWith(";")) {
				type = type.substring(2, type.length()-1);
				type += "[]";
			}
			
			row.appendChild(new Label(type));
			
			
			row.appendChild(new Label(description));
		}
	}
	
	private Label buildLabel(Object attribute) {
		Label result = new Label();
		if (attribute != null) {
			result.setValue(attribute.toString());
		} 
		
		return result;
	}

	private Vlayout buildLabelForArray(Object attribute) {
		Vlayout layout = new Vlayout();

		for (Object o : (Object[])attribute) {
			layout.appendChild(buildLabel(o));
		}
		
		return layout;
	}
	
	
}

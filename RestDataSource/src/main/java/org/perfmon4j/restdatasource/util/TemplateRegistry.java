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

package org.perfmon4j.restdatasource.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.perfmon4j.restdatasource.data.AggregationMethod;
import org.perfmon4j.restdatasource.data.Category;
import org.perfmon4j.restdatasource.data.CategoryTemplate;
import org.perfmon4j.restdatasource.data.Field;

public class TemplateRegistry {
	private final Map<String, CategoryTemplate> categoryTemplates = new HashMap<String, CategoryTemplate>();
	
	public void registerTemplate(CategoryTemplate category) {
		categoryTemplates.put(category.getName(),category);
	}

	public void removeTemplate(String categoryName) {
		categoryTemplates.remove(categoryName);
	}

	public CategoryTemplate getTemplate(String categoryName) {
		return categoryTemplates.get(categoryName);
	}
	
	public Field findField(CategoryTemplate template, String fieldName) {
		Field result = null;
		
		for (Field f : template.getFields()) {
			if (fieldName.equals(f.getName())) {
				result = f;
				break;
			}
		}
		return result;
	}	
	
	private boolean isValidMethod(Field field, AggregationMethod method) {
		return Arrays.asList(field.getAggregationMethods()).contains(method);
	}
	
	public SeriesField resolveField(ParsedSeriesDefinition def) {
		SeriesField result = null;
		String templateName = def.getCategoryName().split("\\.")[0];
		
		CategoryTemplate template = getTemplate(templateName);
		if (template != null) {
			Field field = findField(template, def.getFieldName());
			
			if (field != null) {
				AggregationMethod method = null;
				if (def.getAggregationMethod() != null) {
					method = def.getAggregationMethod();
					if (!isValidMethod(field, method)) {
						throw new BadRequestException("Aggregation method " + method + " not supported for field " + def.getFieldName() + 
								" in " + templateName + " category template");
					}
				} else {
					method = field.getDefaultAggregationMethod();
				}
				
				result = new SeriesField(new Category(def.getCategoryName(), templateName), field, method);
			} else {
				throw new BadRequestException("Category template " + templateName + " field " + def.getFieldName() + " not found");
			}
		} else {
			throw new BadRequestException("Category template " + templateName + " not found");
		}
		return result;
	}
}

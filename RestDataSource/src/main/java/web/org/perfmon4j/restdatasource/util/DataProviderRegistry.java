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

package web.org.perfmon4j.restdatasource.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jboss.resteasy.spi.BadRequestException;

import web.org.perfmon4j.restdatasource.DataProvider;
import web.org.perfmon4j.restdatasource.data.AggregationMethod;
import web.org.perfmon4j.restdatasource.data.Category;
import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Field;

public class DataProviderRegistry {
	private final Map<String, DataProvider> templates = new HashMap<String, DataProvider>();
	
	public void registerDataProvider(DataProvider provider) {
		templates.put(provider.getTemplateName(), provider);
	}

	public void removeDataProvider(String name) {
		templates.remove(name);
	}

	public DataProvider getDataProvider(String name) {
		return templates.get(name);
	}
	
	public DataProvider[] getDataProviders() {
		return templates.values().toArray(new DataProvider[]{});
	}
	
	private Field findField(CategoryTemplate template, String fieldName) {
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
	
	public SeriesField resolveField(ParsedSeriesDefinition def, String alias) {
		SeriesField result = null;
		String templateName = def.getCategoryName().split("\\.")[0];
		
		DataProvider provider = getDataProvider(templateName);
		
		if (provider != null) {
			CategoryTemplate template = provider.getCategoryTemplate();
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
				result = new SeriesField(alias, provider, def.getSystems(), new Category(def.getCategoryName(), templateName), field, method);
			} else {
				throw new BadRequestException("Category template " + templateName + " field " + def.getFieldName() + " not found");
			}
		} else {
			throw new BadRequestException("Category template " + templateName + " not found");
		}
		return result;
	}
}

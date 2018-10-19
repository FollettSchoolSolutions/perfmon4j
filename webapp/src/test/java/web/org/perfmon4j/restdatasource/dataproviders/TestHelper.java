package web.org.perfmon4j.restdatasource.dataproviders;

import web.org.perfmon4j.restdatasource.data.CategoryTemplate;
import web.org.perfmon4j.restdatasource.data.Field;
import web.org.perfmon4j.restdatasource.data.SystemID;

public class TestHelper {
	public static String buildSeriesDefinitionWithAllFields(SystemID system, String category, CategoryTemplate template) {
		String result = "";
		
		for (Field field : template.getFields()) {
			if (!result.isEmpty()) {
				result += "_";
			}
			result += system.toString() + "~" + category + "~" + field.getName();
		}
		
		return result;
	}

}

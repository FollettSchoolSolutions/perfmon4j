package org.perfmon4j.instrument.tomcat;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;
import javax.sql.DataSource;

public class TomcatDataSourceRegistry {
	private static Map<String, WeakReference<DataSource>> tomcatDataSources = new HashMap<String, WeakReference<DataSource>>(); 
	
	public static void registerDataSource(ObjectName name, DataSource dataSource) {
		String singleQuote = "\"";
		
		String jndiSuffix = name.getKeyProperty("name");
		if (jndiSuffix != null) {
			jndiSuffix = jndiSuffix.replaceAll(singleQuote, "");
			String jndiName = "java:/comp/env/" + jndiSuffix;
			tomcatDataSources.put(jndiName, new WeakReference<DataSource>(dataSource));
System.err.println("%%%%%%%%%%%%%%%%%%%% Registering tomcat datasource: " + jndiName);
		}
	}

	public static DataSource lookupTomcatDataSource(String jndiName) {
		DataSource result = null;
		WeakReference<DataSource> ref = tomcatDataSources.get(jndiName);
		if (ref != null) {
			result = ref.get();
		}
		return result;
	}
}

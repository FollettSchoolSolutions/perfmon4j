package org.perfmon4j.util;

import java.util.Map;
import java.util.Properties;

public class ConfigurationProperties {
	private final Properties envProperties;
	private final Properties systemProperties;
	private final Properties localProperties;
	/**
	 * If autoEnvProperties is true, we will automatically resolve properties using the
	 * environment if the property is not explicitly declared as a system or local property.
	 * If false, we will only use environment properties if the property key is prefixed with "env.".
	 */
	private boolean autoEnvProperties = true;

	public ConfigurationProperties() {
		this(getEnvAsProperties(), System.getProperties());
	}

	public ConfigurationProperties(Properties envProperties, Properties systemProperties) {
		this.envProperties = envProperties;
		this.systemProperties = systemProperties;
		localProperties = new Properties();
	}
	
	public void setProperty(String key, String value) {
		localProperties.setProperty(key, value);
	}
	
	public String getProperty(String key) {
		String result = localProperties.getProperty(key);
		if (result == null) {
			result = systemProperties.getProperty(key);
		}
		if ((result == null) 
			&& (autoEnvProperties || key.startsWith("env."))) {
			key = key.replaceFirst("^env\\.", "");
			result = envProperties.getProperty(key.replaceFirst("^env\\.", key));
		}
		return result;
	}

	public String getProperty(String key, String defaultValue) {
		String result = getProperty(key);
		
		return result != null ? result : defaultValue;
	}
	
	boolean isAutoEnvProperties() {
		return autoEnvProperties;
	}

	void setAutoEnvProperties(boolean autoEnvProperties) {
		this.autoEnvProperties = autoEnvProperties;
	}

	private static Properties getEnvAsProperties() {
        Properties result = new Properties();
		Map<String, String> env = System.getenv();
		
        System.getenv().forEach((k, v) -> result.setProperty(k, v));
        
        return result;
	}
}

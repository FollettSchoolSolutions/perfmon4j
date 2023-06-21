package org.perfmon4j;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * Maintains a properties object of Perfmon4j currently configured settings.
 * 
 * Important this is a READ-ONLY object and can not be used to modify any
 * running settings.
 **/
public class ConfiguredSettings {
	static private final Serializable settingsLockToken = new Serializable() {
		private static final long serialVersionUID = 1L;
	};

	/**
	 * These properties are simply read-only copies of many of Perfmon4js
	 * currently configured settings.  Setting these properties should
	 * only be done by Perfmon4j components.
	 */
	static private Properties javaAgentSettings = new Properties();
	static private Properties configFileSettings = new Properties();
	static private Properties allSettings = new Properties();

	public static void setJavaAgentSettings(Properties javaAgentSettings) {
		synchronized (settingsLockToken) {
			ConfiguredSettings.javaAgentSettings = (Properties)javaAgentSettings.clone();
			rebuildAllSettingsProperties();
		}
	}

	public static void setConfigFileSettings(Properties configFileSettings) {
		synchronized (settingsLockToken) {
			ConfiguredSettings.configFileSettings = (Properties)configFileSettings.clone();
			rebuildAllSettingsProperties();
		}
	}
	
	static private void rebuildAllSettingsProperties() {
		synchronized (settingsLockToken) {
			Properties replacement = new Properties();
			
			for (Map.Entry<Object, Object> entry : javaAgentSettings.entrySet()) {
				replacement.setProperty((String)entry.getKey(), (String)entry.getValue());
			}
			for (Map.Entry<Object, Object> entry : configFileSettings.entrySet()) {
				replacement.setProperty((String)entry.getKey(), (String)entry.getValue());
			}
			ConfiguredSettings.allSettings = replacement;
		}
	}
	
	/**
	 * 
	 * @return A copy of the current settings.  This copy is simply a reference
	 * to give the caller access to many (but not all) of Perfmon4j's current
	 * running configuration.  
	 *
	 * Making changes to the copy of properties received has NO impact. 
	 * 
	 */
	static public Properties getConfiguredSettings() {
		synchronized (settingsLockToken) {
			return (Properties)allSettings.clone();
		}
	}
}

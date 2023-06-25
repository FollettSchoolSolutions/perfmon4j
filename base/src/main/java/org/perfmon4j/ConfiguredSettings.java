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
	static private Properties bootConfigSettings = new Properties();
	static private Properties allSettings = new Properties();

	public static void setJavaAgentSettings(Properties javaAgentSettings) {
		synchronized (settingsLockToken) {
			ConfiguredSettings.javaAgentSettings = (Properties)javaAgentSettings.clone();
			ConfiguredSettings.javaAgentSettings.setProperty("perfmon4j.javaAgentSettings.loaded", "true");
			rebuildAllSettingsProperties();
		}
	}

	public static void setConfigFileSettings(Properties configFileSettings) {
		synchronized (settingsLockToken) {
			ConfiguredSettings.configFileSettings = (Properties)configFileSettings.clone();
			ConfiguredSettings.configFileSettings.setProperty("perfmon4j.configFileSettings.loaded", "true");
			rebuildAllSettingsProperties();
		}
	}

	public static void setBootConfigSettings(Properties bootConfigSettings) {
		synchronized (settingsLockToken) {
			ConfiguredSettings.bootConfigSettings = (Properties)bootConfigSettings.clone();
			ConfiguredSettings.bootConfigSettings.setProperty("perfmon4j.bootConfigSettings.loaded", "true");
			rebuildAllSettingsProperties();
		}
	}
	
	static private void rebuildAllSettingsProperties() {
		synchronized (settingsLockToken) {
			Properties replacement = new Properties();
			
			/** Set default values for which configuration settings have been loaded.
			 *  They will be overwritten by individual config files if they have 
			 *  been set. 
			 */
			replacement.setProperty("perfmon4j.javaAgentSettings.loaded", "false");
			replacement.setProperty("perfmon4j.configFileSettings.loaded", "false");
			replacement.setProperty("perfmon4j.bootConfigSettings.loaded", "false");
			
			Properties[] allProps = new Properties[] {
					configFileSettings,
					bootConfigSettings,
					javaAgentSettings,
			};
			
			for (Properties props : allProps) {
				for (Map.Entry<Object, Object> entry : props.entrySet()) {
					replacement.setProperty((String)entry.getKey(), (String)entry.getValue());
				}
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

package org.perfmon4j.util;

import java.util.Properties;

import junit.framework.TestCase;

public class ConfigurationPropertiesTest extends TestCase {

    public void testConstructor() {
        ConfigurationProperties config = new ConfigurationProperties();

        assertNotNull(config.getProperty("PATH")); // Assuming "PATH" is an environment variable
    }

    public void testSetProperty() {
        ConfigurationProperties config = new ConfigurationProperties();

        config.setProperty("local.key", "localValue");
        assertEquals("localValue", config.getProperty("local.key"));
    }

    public void testGetPropertyLocal() {
        ConfigurationProperties config = new ConfigurationProperties();

        config.setProperty("local.key", "localValue");
        assertEquals("localValue", config.getProperty("local.key"));
    }

    public void testGetPropertySystem() {
        Properties systemProps = new Properties();
        systemProps.setProperty("system.key", "systemValue");

        ConfigurationProperties config = new ConfigurationProperties(new Properties(), systemProps);
        assertEquals("systemValue", config.getProperty("system.key"));
    }

    public void testGetPropertyEnv() {
        Properties envProps = new Properties();
        envProps.setProperty("TEST_ENV_KEY", "envValue");

        ConfigurationProperties config = new ConfigurationProperties(envProps, new Properties());
        assertEquals("envValue", config.getProperty("env.TEST_ENV_KEY"));
    }

    public void testGetPropertyDefault() {
        ConfigurationProperties config = new ConfigurationProperties();

        assertEquals("defaultValue", config.getProperty("nonexistent.key", "defaultValue"));
    }

    public void testAutoEnvProperties() {
        Properties envProps = new Properties();
        envProps.setProperty("TEST_ENV_KEY", "envValue");

        ConfigurationProperties config = new ConfigurationProperties(envProps, new Properties());
        config.setAutoEnvProperties(false);

        assertNull(config.getProperty("TEST_ENV_KEY"));
        assertEquals("envValue", config.getProperty("env.TEST_ENV_KEY"));
    }

    public void testSetAutoEnvProperties() {
        ConfigurationProperties config = new ConfigurationProperties();
        
        config.setAutoEnvProperties(false);
        assertFalse(config.isAutoEnvProperties());

        config.setAutoEnvProperties(true);
        assertTrue(config.isAutoEnvProperties());
    }
}

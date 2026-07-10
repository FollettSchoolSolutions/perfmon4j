package org.perfmon4j.hawtioplugin;

/**
 * Attribute names here (Url, Scope, Module) are dictated by io.hawt.web.plugin.PluginServlet
 * in the hawtio-war distribution - it discovers plugins by querying the platform MBeanServer
 * for ObjectName pattern "hawtio:type=plugin,name=*" and reading these exact attributes.
 */
public interface Perfmon4jHawtioPluginMBean {
	String getUrl();
	String getScope();
	String getModule();
}

package com.follett.perfmon4j.hawtioplugin;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Registers this deployment as a discoverable Hawtio remote plugin by registering an MBean
 * on the platform MBeanServer, which io.hawt.web.plugin.PluginServlet (bundled with the
 * hawtio-war distribution deployed elsewhere in this same JVM) queries for at request time.
 *
 * This package deliberately lives OUTSIDE org.perfmon4j.* - on JVMs where the real perfmon4j
 * javaagent is attached, org.perfmon4j is typically listed in jboss.modules.system.pkgs (or
 * an equivalent parent-first classloading reservation), which makes JBoss Modules refuse to
 * load ANY class under that package from a deployment's own WEB-INF/classes - it only looks
 * in the system classloader, where a brand-new class like this one doesn't exist, producing a
 * ClassNotFoundException for a class that is plainly present in the WAR. See
 * hawtio-plugin-webapp/CLAUDE.md for the full diagnosis.
 *
 * The Url attribute is derived from this webapp's own context path rather than hardcoded,
 * so it keeps working under whatever context path/host/port this WAR happens to be deployed
 * at - it doesn't need to know or guess the deployment's hostname.
 *
 * Url must be the plugin's BASE path, not the path to remoteEntry.js itself - Hawtio's
 * Module Federation loader (@module-federation/utilities) appends "/remoteEntry.js" (or
 * whatever RemoteEntryFileName says, default "remoteEntry.js") to Url on its own. Passing
 * the full file path here produces a doubled ".../remoteEntry.js/remoteEntry.js" 404.
 */
@WebListener
public class PluginRegistrationListener implements ServletContextListener {
	private static final String OBJECT_NAME = "hawtio:type=plugin,name=perfmon4j";

	private ObjectName objectName;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			objectName = new ObjectName(OBJECT_NAME);
			String url = sce.getServletContext().getContextPath();

			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			if (mBeanServer.isRegistered(objectName)) {
				mBeanServer.unregisterMBean(objectName);
			}
			mBeanServer.registerMBean(new Perfmon4jHawtioPlugin(url), objectName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register perfmon4j Hawtio plugin MBean", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			if (objectName != null && mBeanServer.isRegistered(objectName)) {
				mBeanServer.unregisterMBean(objectName);
			}
		} catch (Exception e) {
			// Best-effort cleanup on undeploy - nothing useful to do if this fails.
		}
	}
}

package org.perfmon4j.util.mbean;

import javax.management.MBeanServer;

public interface MBeanServerFinder {
	MBeanServer getMBeanServer() throws MBeanQueryException;
}

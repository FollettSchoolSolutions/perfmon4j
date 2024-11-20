package org.perfmon4j.util.mbean;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface MBeanServerFinder {
	MBeanServer getMBeanServer() throws MBeanQueryException;
	MBeanInfo getMBeanInfo(ObjectName objectName) throws MBeanQueryException;
	Object getAttribute(ObjectName objectName, String attributeName) throws MBeanQueryException;
}

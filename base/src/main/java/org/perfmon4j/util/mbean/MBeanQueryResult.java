package org.perfmon4j.util.mbean;

public interface MBeanQueryResult {
	public MBeanQuery getQuery();
	public MBeanInstance[] getInstances();
}

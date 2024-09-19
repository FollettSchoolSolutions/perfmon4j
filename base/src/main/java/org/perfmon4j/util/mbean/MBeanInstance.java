package org.perfmon4j.util.mbean;

public interface MBeanInstance {
	public String getName();
	public String getInstanceName();
	public MBeanDatum<?>[] extractAttributes();
}

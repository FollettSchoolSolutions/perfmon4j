package org.perfmon4j.util.mbean;

public interface MBeanQuery extends Comparable<MBeanQuery>{
	public String getBaseJMXName();
	public String getInstanceName();
	public String[] getCounters();
	public String[] getGauges();
}

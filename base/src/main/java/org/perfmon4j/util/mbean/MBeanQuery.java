package org.perfmon4j.util.mbean;

public interface MBeanQuery extends Comparable<MBeanQuery>{
	public String getDisplayName();
	public String getBaseJMXName();
	public String getInstancePropertyKey();
	public String[] getCounters();
	public String[] getGauges();
}

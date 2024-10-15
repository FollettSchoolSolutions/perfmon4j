package org.perfmon4j.util.mbean;


public interface MBeanQuery extends Comparable<MBeanQuery>{
	public String getDomain();
	public String getDisplayName();
	public String getBaseJMXName();
	public String getInstanceKey();
	public String[] getCounters();
	public String[] getGauges();
	public SnapShotRatio[] getRatios();
	
	public String getSignature(); // Returns a unique SHA256 that represents a canonical version of this Query.
}

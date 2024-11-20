package org.perfmon4j.util.mbean;

import org.perfmon4j.util.mbean.MBeanQueryBuilder.NamedRegExFilter;
import org.perfmon4j.util.mbean.MBeanQueryBuilder.RegExFilter;

public interface MBeanQuery extends Comparable<MBeanQuery>{
	public String getDomain();
	public String getDisplayName();
	public String getBaseJMXName();
	public String getInstanceKey();
	public String[] getCounters();
	public String[] getGauges();
	public SnapShotRatio[] getRatios();
	
	public RegExFilter getInstanceValueFilter();
	public NamedRegExFilter getAttributeValueFilter();
	public String getSignature(); // Returns a unique SHA256 that represents a canonical version of this Query.
}

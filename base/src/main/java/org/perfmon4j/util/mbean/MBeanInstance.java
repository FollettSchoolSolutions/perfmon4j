package org.perfmon4j.util.mbean;

import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;

public interface MBeanInstance {
	public String getName();
	public String getInstanceName();
	public MBeanDatum<?>[] extractAttributes() throws MBeanQueryException;
	public DatumDefinition[] getDatumDefinition() throws MBeanQueryException;
}

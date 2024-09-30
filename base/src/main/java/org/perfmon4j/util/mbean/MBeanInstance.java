package org.perfmon4j.util.mbean;

import org.perfmon4j.GenericItemRegistry.OverrideClassNameForWrappedObject;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;

public interface MBeanInstance extends OverrideClassNameForWrappedObject {
	public String getName();
	public String getInstanceName();
	public MBeanDatum<?>[] extractAttributes() throws MBeanQueryException;
	public DatumDefinition[] getDatumDefinition() throws MBeanQueryException;
	
	static String buildEffectiveClassName(MBeanQuery query) {
		return MBeanInstance.class.getName() + ":" + query.getSignature();
	}
	public static boolean isMBeanInstanceEffectiveClassName(String className) {
		return className == null ? false : className.startsWith(MBeanInstance.class.getName() + ":");
	}
}

package org.perfmon4j.util.mbean;

import java.util.Objects;

import javax.management.MBeanServer;
import javax.management.ObjectName;


class MBeanInstanceImpl implements MBeanInstance {
	private final ObjectName objectName;
	private final String instanceName;
	private final MBeanQuery query;
	private final MBeanAttributeExtractor extractor;
	
	
	MBeanInstanceImpl(MBeanServer mBeanServer, ObjectName objectName, MBeanQuery query) {
		this.objectName = objectName;
		this.query = query;
		this.extractor = new MBeanAttributeExtractor(mBeanServer, objectName, query);
		
		final String instanceNameKey = query.getInstancePropertyKey();
		if (instanceNameKey != null  && !instanceNameKey.isEmpty()) {
			this.instanceName = objectName.getKeyProperty(instanceNameKey);
		} else {
			this.instanceName = null;
		}
	}

	@Override
	public String getName() {
		return query.getDisplayName();
	}

	@Override
	public String getInstanceName() {
		return instanceName;
	}
	
	@Override
	public MBeanDatum<?>[] extractAttributes() {
		return extractor.extractAttributes();
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectName, instanceName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MBeanInstanceImpl other = (MBeanInstanceImpl) obj;
		return Objects.equals(instanceName, other.instanceName) && Objects.equals(objectName, other.objectName);
	}

}


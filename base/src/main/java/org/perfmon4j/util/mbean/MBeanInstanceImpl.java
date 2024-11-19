package org.perfmon4j.util.mbean;

import java.util.Objects;

import javax.management.ObjectName;

import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;


class MBeanInstanceImpl implements MBeanInstance {
	private final String name;
	private final ObjectName objectName;
	private final String instanceName;
	private final String effectiveClassName;
	private final MBeanAttributeExtractor extractor;
	private final DatumDefinition[] datumDefinition;
	
	
	MBeanInstanceImpl(MBeanServerFinder mBeanServerFinder, ObjectName objectName, MBeanQuery query) throws MBeanQueryException {
		this.name = query.getDisplayName();
		this.objectName = objectName;
		this.extractor = new MBeanAttributeExtractor(mBeanServerFinder, objectName, query);
		this.datumDefinition = extractor.getDatumDefinition();
		this.effectiveClassName = MBeanInstance.buildEffectiveClassName(query);
		
		final String instanceNameKey = query.getInstanceKey();
		if (instanceNameKey != null  && !instanceNameKey.isEmpty()) {
			this.instanceName = objectName.getKeyProperty(instanceNameKey);
		} else {
			this.instanceName = null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getInstanceName() {
		return instanceName;
	}
	
	@Override
	public MBeanDatum<?>[] extractAttributes() throws MBeanQueryException {
		return extractor.extractAttributes();
	}

	@Override
	public int hashCode() {
		return Objects.hash(instanceName, name, objectName);
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
		return Objects.equals(instanceName, other.instanceName) && Objects.equals(name, other.name)
				&& Objects.equals(objectName, other.objectName);
	}

	@Override
	public DatumDefinition[] getDatumDefinition() {
		return datumDefinition;
	}

	@Override
	/**
	 * This is required by the POJOSnapShotRegistry
	 * Each "POJO" (Even thought the MBeanInstance is not technically a POJO)
	 * must have a unique className.  The className is used as a key by the POJSnapShotRegistry.
	 */
	public String getEffectiveClassName() {
		return effectiveClassName;
	}

	@Override
	public String toString() {
		return "MBeanInstanceImpl [name=" + name + ", objectName=\"" + objectName + "\"]";
	}
	
}


package org.perfmon4j.util.mbean;

import java.lang.reflect.Constructor;

import org.perfmon4j.GenericItemRegistry.OverrideClassNameForWrappedObject;

/**
 * This will be used as a base class by Javassist when generating a 
 * POJO class for each unique JMXQuery.
 */
public abstract class MBeanPojoBase implements OverrideClassNameForWrappedObject {
	private final long cacheDurationMillis = Long.getLong(MBeanPojoBase.class.getName() + ".cacheDurationMillis", 500);
	private final MBeanInstance mBeanInstance;
	
	private final Object cachedDataLockToken = new Object(); 
	private MBeanDatum[] cachedData = null;
	private long lastDataReadTime = -1;

	public MBeanPojoBase(MBeanInstance mBeanInstance) {
		this.mBeanInstance = mBeanInstance;
	}
	
	protected MBeanDatum<?> getData(String attributeName) throws MBeanQueryException {
		MBeanDatum<?>[] data = null;
		synchronized (cachedDataLockToken) {
			long now = System.currentTimeMillis();
			if (cachedData == null || (now > (lastDataReadTime + cacheDurationMillis))) {
				cachedData = mBeanInstance.extractAttributes();
				lastDataReadTime = now;
			} 
			data = cachedData;
		}
		for (MBeanDatum<?> datum : data) {
			if (datum.getName().equals(attributeName)) {
				return datum;
			}
		}
		
		throw new MBeanQueryException("Unable to find expected attribute in MBean. Attribute name: " + attributeName);
	}

	@Override
	public String getEffectiveClassName() {
		return mBeanInstance.getName();
	}
	
	/* Below is an example of the methods that will be
	 * generated, using javassist,for each MBeanAttribute
	 * 
	 * 	@SnapShotCounter
	 *	public Long getMyCounter() {
	 *		return (Long)getData("MyCounter");
	 *	}
	 *
	 *	@SnapShotGauge
	 *	public Float getCpuPercent() {
	 *		return (Float)getData("CpuPercent");
	 *	}
	 *
	 */
	
	static MBeanPojoBase invokeConstructor(Class<MBeanPojoBase> clazz, MBeanInstance instance) throws Exception {
		try {
			Constructor<MBeanPojoBase> constructor = clazz.getConstructor(new Class<?>[] {MBeanInstance.class});
			return constructor.newInstance(instance);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new Exception("Unable to construct MBeanPojoBase object", e);
		}
	}
	
}

package org.perfmon4j.util.mbean;

import java.lang.reflect.Constructor;

import org.perfmon4j.GenericItemRegistry.OverrideClassNameForWrappedObject;
import org.perfmon4j.util.Cacheable;

/**
 * This will be used as a base class by Javassist when generating a 
 * POJO class for each unique JMXQuery.
 */
public abstract class MBeanPojoBase implements OverrideClassNameForWrappedObject {
	private final long cacheDurationMillis = Long.getLong(MBeanPojoBase.class.getName() + ".cacheDurationMillis", 500);
	private final MBeanInstance mBeanInstance;
	private final Cacheable<MBeanDatum<?>[]> cachedData;
 	

	public MBeanPojoBase(MBeanInstance mBeanInstance) {
		this.mBeanInstance = mBeanInstance;
		cachedData = new Cacheable<>(() -> this.mBeanInstance.extractAttributes(), cacheDurationMillis);
	}
	
	protected MBeanDatum<?> getData(String attributeName) throws MBeanQueryException {
		try {
			MBeanDatum<?>[] data = cachedData.get();
			for (MBeanDatum<?> datum : data) {
				if (datum.getName().equals(attributeName)) {
					return datum;
				}
			}
		} catch (MBeanQueryException e) {
			throw e;
		} catch (Exception e) {
			throw new MBeanQueryException(e);
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
	 *	public long getMyCounter() throws org.perfmon4j.util.mbean.MBeanQueryException {
	 *		return ((Long)getData("MyCounter").getValue()).getLong();
	 *	};	
	 * 
	 *	@SnapShotGauge
	 *	public float getCpuPercent() {
	 *		return ((Float)getData("CpuPercent").getValue()).getFloat();
	 *	}
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

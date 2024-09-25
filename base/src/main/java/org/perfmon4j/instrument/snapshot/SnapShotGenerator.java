package org.perfmon4j.instrument.snapshot;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.remotemanagement.MonitorKeyWithFields;
import org.perfmon4j.util.mbean.MBeanInstance;
import org.perfmon4j.util.mbean.MBeanPojoBase;

public abstract class SnapShotGenerator {
	public static final String DELTA_FIELD_SUFFIX = "PerSecond";

	public abstract Class<MBeanPojoBase> generatePOJOClassForMBeanInstance(MBeanInstance mBeanInstance) throws GenerateSnapShotException;
	public abstract Class<?> generateSnapShotDataImpl(Class<?> dataProvider) throws GenerateSnapShotException;
	public abstract Bundle generateBundle(Class<?> provider) throws GenerateSnapShotException;
	public abstract Bundle generateBundle(Class<?> provider, String instanceName) throws GenerateSnapShotException;
	public abstract Bundle generateBundleForPOJO(Class<?> provider) throws GenerateSnapShotException;
	public abstract MonitorKeyWithFields[] generateExternalMonitorKeys (Class<?> clazz);

	
    static public interface SnapShotLifecycle {
    	public long getStartTime();
    	public long getEndTime();
    	public long getDuration();
    	
    	public void init(Object data, long timeStamp);
    	public void takeSnapShot(Object dataProvider, long timesStamp);
    }
    
    static public interface SnapShotPOJOLifecycle extends SnapShotLifecycle {
    	public void setInstanceName(String instanceName);
    	public String getInstanceName();
    }

	static public class POJOBundle {
	    protected final Class<?> dataClass;
	    private final boolean usePriorityTimer;
	    
	    public POJOBundle(Class<?> dataClass, boolean usePriorityTimer) {
	    	this.dataClass = dataClass;
	    	this.usePriorityTimer = usePriorityTimer;
	    }
	    
	    public SnapShotData newSnapShotData() {
	    	try {
				return (SnapShotData)dataClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException("Unable to instantiate SnapShotData class", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Illegal Access of SnapShotDate", e);
			}
	    }
	    
	    public boolean isUsePriorityTimer() {
	    	return usePriorityTimer;
	    }
	}
    
	static public class Bundle extends POJOBundle {
	    private final Object providerInstance;
	    
	    public Bundle(Class<?> dataClass, Object providerInstance, boolean usePriorityTimer) {
	    	super(dataClass, usePriorityTimer);
	    	this.providerInstance = providerInstance;
	    }
	    
	    public Object getProviderInstance() {
	    	return providerInstance;
	    }
	}

}
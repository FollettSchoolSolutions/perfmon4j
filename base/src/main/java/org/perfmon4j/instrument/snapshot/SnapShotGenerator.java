package org.perfmon4j.instrument.snapshot;

import org.perfmon4j.SnapShotData;
import org.perfmon4j.remotemanagement.MonitorKeyWithFields;

public abstract class SnapShotGenerator {
	public static final String DELTA_FIELD_SUFFIX = "PerSecond";

	public abstract Class<?> generateSnapShotDataImpl(Class<?> dataProvider) throws GenerateSnapShotException;
	public abstract Bundle generateBundle(Class<?> provider) throws GenerateSnapShotException;
	public abstract Bundle generateBundle(Class<?> provider, String instanceName) throws GenerateSnapShotException;
	public abstract MonitorKeyWithFields[] generateExternalMonitorKeys (Class<?> clazz);

	
    static public interface SnapShotLifecycle {
    	public long getStartTime();
    	public long getEndTime();
    	public long getDuration();
    	
    	public void init(Object data, long timeStamp);
    	public void takeSnapShot(Object dataProvider, long timesStamp);
    }
    
	static public class Bundle {
	    private final Class<?> dataClass;
	    private final Object providerInstance;
	    private final boolean usePriorityTimer;
	    
	    public Bundle(Class<?> dataClass, Object providerInstance, boolean usePriorityTimer) {
	    	this.dataClass = dataClass;
	    	this.providerInstance = providerInstance;
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
	    
	    public Object getProviderInstance() {
	    	return providerInstance;
	    }
	    
	    public boolean isUsePriorityTimer() {
	    	return usePriorityTimer;
	    }
	}
   
}
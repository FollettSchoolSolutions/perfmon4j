package org.perfmon4j.snapshot.emitter;

public interface EmitterController {
	public void emit(EmitterData data);
	
	public EmitterData initData();
	public EmitterData initData(String instanceName);
	public EmitterData initData(long timestamp);
	public EmitterData initData(String instanceName, long timeStamp);
	
	public boolean isAppenderAttached();
}

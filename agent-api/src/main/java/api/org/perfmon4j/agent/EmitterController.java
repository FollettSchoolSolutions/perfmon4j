package api.org.perfmon4j.agent;

public interface EmitterController {
	public void emit(EmitterData data);
	
	public EmitterData initData();
	public EmitterData initData(String instanceName);
	public EmitterData initData(long timestamp);
	public EmitterData initData(String instanceName, long timeStamp);
	
	public boolean isActive();
}

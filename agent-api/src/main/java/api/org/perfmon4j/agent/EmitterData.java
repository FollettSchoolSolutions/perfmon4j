package api.org.perfmon4j.agent;

public interface EmitterData {
	public void addData(String fieldName, long value);
	public void addData(String fieldName, int value);
	public void addData(String fieldName, double value);
	public void addData(String fieldName, float value);
	public void addData(String fieldName, boolean value);
	public void addData(String fieldName, String value);
}

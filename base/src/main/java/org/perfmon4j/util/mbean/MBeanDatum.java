package org.perfmon4j.util.mbean;

public interface MBeanDatum<T> {
	public enum Type {
		GAUGE,
		COUNTER
	};
	
	public String getName();
	public Type getType();
	public T getValue();
}

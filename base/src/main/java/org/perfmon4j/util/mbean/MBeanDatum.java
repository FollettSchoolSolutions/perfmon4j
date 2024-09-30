package org.perfmon4j.util.mbean;

import javax.management.MBeanAttributeInfo;

import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.snapshot.Delta;

public interface MBeanDatum<T> {
	public enum OutputType {
		GAUGE(SnapShotGauge.class),
		COUNTER(SnapShotCounter.class),
		STRING(SnapShotString.class);
		
		final String annotationClassName;

		private OutputType(Class<?> annotationClass) {
			this.annotationClassName = annotationClass.getName(); 
		}
		
		public String getAnnotationClassName() {
			return annotationClassName;
		}
	};
	
	public enum AttributeType {
		NATIVE_SHORT("short", true, false),
		NATIVE_INTEGER("int", true, true),
		NATIVE_LONG("long", true, true),
		NATIVE_FLOAT("float", true, false),
		NATIVE_DOUBLE("double", true, false),
		NATIVE_BOOLEAN("boolean", true, false),
		NATIVE_CHARACTER("char", true, false),
		NATIVE_BYTE("byte", true, false),
		SHORT("java.lang.Short", true, false),
		INTEGER("java.lang.Integer", true, true),
		LONG("java.lang.Long", true, true),
		FLOAT("java.lang.Float", true, false),
		DOUBLE("java.lang.Double", true, false),
		BOOLEAN("java.lang.Boolean", true, false),
		CHARACTER("java.lang.Character", true, false),
		BYTE("java.lang.Byte", true, false),
		STRING("java.lang.String", false, false);
		
		private final String jmxType;
		private final boolean supportsGauge;
		private final boolean supportsCounter;
		
		private AttributeType(String jmxType, boolean supportsGauge, boolean supportsCounter) {
			this.jmxType = jmxType;
			this.supportsGauge = supportsGauge;
			this.supportsCounter = supportsCounter;
		}
		
		public String getJmxType() {
			return jmxType;
		}
		
		public boolean isSupportsGauge() {
			return supportsGauge;
		}
		
		public boolean isSupportsCounter() {
			return supportsCounter;
		}
		
		public OutputType getValidOutputType(OutputType desiredOutputType) {
			OutputType result = desiredOutputType;
			
			if (result.equals(OutputType.COUNTER) && !isSupportsCounter()) {
				result = OutputType.GAUGE;
			}
			
			if (result.equals(OutputType.GAUGE) && !isSupportsGauge()) {
				result = OutputType.STRING;
			}
			
			return result;
		}
		
		public static AttributeType getAttributeType(MBeanAttributeInfo info) {
			for (AttributeType a : AttributeType.values()) {
				if (a.getJmxType().equals(info.getType())) {
					return a;
				}
			}
			return STRING;
		}
	};
	
	public String getName();
	public OutputType getOutputType();
	public AttributeType getAttributeType();
	public T getValue();
	public PerfMonObservableDatum<?> toPerfMonObservableDatum();
	public PerfMonObservableDatum<Delta> toPerfMonObservableDatum(MBeanDatum<?> initialDatum, long durationMills);
}

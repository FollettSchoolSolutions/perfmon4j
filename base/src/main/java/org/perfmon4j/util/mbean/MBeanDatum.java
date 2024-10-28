package org.perfmon4j.util.mbean;

import javax.management.MBeanAttributeInfo;

import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.util.mbean.MBeanAttributeExtractor.DatumDefinition;

public interface MBeanDatum<T> {
	public enum OutputType {
		GAUGE,
		COUNTER,
		STRING,
		RATIO,
		VOID; // Used to retrieve numerator and denominator for a Ratio - Should not be directly written to an appender.
		
		public boolean isOutputToAppender() {
			return !this.equals(VOID);
		}
	};
	
	public enum AttributeType {
		NATIVE_SHORT("short", true, false, true),
		NATIVE_INTEGER("int", true, true, true),
		NATIVE_LONG("long", true, true, true),
		NATIVE_FLOAT("float", true, false, true),
		NATIVE_DOUBLE("double", true, false, true),
		NATIVE_BOOLEAN("boolean", true, false, false),
		NATIVE_CHARACTER("char", true, false, false),
		NATIVE_BYTE("byte", true, false, false),
		SHORT("java.lang.Short", true, false, true),
		INTEGER("java.lang.Integer", true, true, true),
		LONG("java.lang.Long", true, true, true),
		FLOAT("java.lang.Float", true, false, true),
		DOUBLE("java.lang.Double", true, false, true),
		BOOLEAN("java.lang.Boolean", true, false, false),
		CHARACTER("java.lang.Character", true, false, false),
		BYTE("java.lang.Byte", true, false, false),
		STRING("java.lang.String", false, false, false);
		
		private final String jmxType;
		private final boolean supportsGauge;
		private final boolean supportsCounter;
		private final boolean supportsRatioComponent; // Can be used as a numerator/denominator in a ratio,
		
		private AttributeType(String jmxType, boolean supportsGauge, boolean supportsCounter, boolean supportsRatioComponent) {
			this.jmxType = jmxType;
			this.supportsGauge = supportsGauge;
			this.supportsCounter = supportsCounter;
			this.supportsRatioComponent = supportsRatioComponent;
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
		
		public boolean isSupportsRatioComponent() {
			return supportsRatioComponent;
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
	
	public DatumDefinition getDatumDefinition();
	public String getName();
	public String getDisplayName();
	public OutputType getOutputType();
	public AttributeType getAttributeType();
	public T getValue();
	public PerfMonObservableDatum<?> toPerfMonObservableDatum();
	public PerfMonObservableDatum<Delta> toPerfMonObservableDatum(MBeanDatum<?> initialDatum, long durationMills);
}

package org.perfmon4j.util.mbean;

import javax.management.MBeanAttributeInfo;

public interface MBeanDatum<T> {
	public enum OutputType {
		GAUGE,
		COUNTER,
		STRING
	};
	
	public enum AttributeType {
		NATIVE_SHORT(true, false),
		NATIVE_INTEGER(true, true),
		NATIVE_LONG(true, true),
		NATIVE_FLOAT(true, false),
		NATIVE_DOUBLE(true, false),
		NATIVE_BOOLEAN(true, false),
		NATIVE_CHARACTER(true, false),
		NATIVE_BYTE(true, false),
		SHORT(true, false),
		INTEGER(true, true),
		LONG(true, true),
		FLOAT(true, false),
		DOUBLE(true, false),
		BOOLEAN(true, false),
		CHARACTER(true, false),
		BYTE(true, false),
		STRING(false, false);
		
		private final boolean supportsGauge;
		private final boolean supportsCounter;
		
		private AttributeType(boolean supportsGauge, boolean supportsCounter) {
			this.supportsGauge = supportsGauge;
			this.supportsCounter = supportsCounter;
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
			AttributeType result = STRING;
			
			if ("short".equals(info.getType())) {
				result = NATIVE_SHORT;
			} else if ("int".equals(info.getType())) {
				result = NATIVE_INTEGER;
			} else if ("long".equals(info.getType())) {
				result = NATIVE_LONG;
			} else if ("float".equals(info.getType())) {
				result = NATIVE_FLOAT;
			} else if ("double".equals(info.getType())) {
				result = NATIVE_DOUBLE;
			} else if ("char".equals(info.getType())) {
				result = NATIVE_CHARACTER;
			} else if ("byte".equals(info.getType())) {
				result = NATIVE_BYTE;
			} else if ("boolean".equals(info.getType())) {
				result = NATIVE_BOOLEAN;
			} else if ("java.lang.Short".equals(info.getType())) {
				result = SHORT;
			} else if ("java.lang.Integer".equals(info.getType())) {
				result = INTEGER;
			} else if ("java.lang.Long".equals(info.getType())) {
				result = LONG;
			} else if ("java.lang.Float".equals(info.getType())) {
				result = FLOAT;
			} else if ("java.lang.Double".equals(info.getType())) {
				result = DOUBLE;
			} else if ("java.lang.Boolean".equals(info.getType())) {
				result = BOOLEAN;
			} else if ("java.lang.Character".equals(info.getType())) {
				result = CHARACTER;
			} else if ("java.lang.Byte".equals(info.getType())) {
				result = BYTE;
			} 
			return result;
		}
	};
	
	public String getName();
	public OutputType getOutputType();
	public AttributeType getAttributeType();
	public T getValue();
}

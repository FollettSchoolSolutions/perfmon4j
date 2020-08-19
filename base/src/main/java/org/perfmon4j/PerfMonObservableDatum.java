package org.perfmon4j;

import java.math.BigDecimal;
import java.util.Set;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;
import org.perfmon4j.util.MiscHelper;


public class PerfMonObservableDatum<T> {
	public static final Ratio NULL_RATIO = new Ratio(0, 0);
	public static final Delta NULL_DELTA = new Delta(0, 0, 0);
	public static final Number NULL_NUMBER = Integer.valueOf(-1);
	
	private final String fieldName;
	private final String defaultDisplayName;
	private final boolean ratio;
	private final boolean delta;
	private final boolean inputWasNull;
	private final Number value;
	private final T complexObject;
	private final String stringValue;
	private final boolean isNumeric;
	private final boolean isDateTime;
	

	static public PerfMonObservableDatum<Boolean> newDatum(String fieldName, boolean value) {
		return newDatum(fieldName, Boolean.valueOf(value));
	}

	static public PerfMonObservableDatum<Boolean> newDatum(String fieldName, Boolean value) {
		return new PerfMonObservableDatum<Boolean>(fieldName, value);
	}
	
	static public PerfMonObservableDatum<Short> newDatum(String fieldName, short value) {
		return newDatum(fieldName, Short.valueOf(value));
	}
	
	static public PerfMonObservableDatum<Integer> newDatum(String fieldName, int value) {
		return newDatum(fieldName, Integer.valueOf(value));
	}

	static public PerfMonObservableDatum<Long> newDatum(String fieldName, long value) {
		return newDatum(fieldName, Long.valueOf(value));
	}
	
	static public PerfMonObservableDatum<Float> newDatum(String fieldName, float value) {
		return newDatum(fieldName, Float.valueOf(value));
	}

	static public PerfMonObservableDatum<Double> newDatum(String fieldName, double value) {
		return newDatum(fieldName, Double.valueOf(value));
	}
	
	static public <X extends Number> PerfMonObservableDatum<X> newDatum(String fieldName, X number) {
		return new PerfMonObservableDatum<X>(fieldName, number);
	}

	static public <X extends Ratio> PerfMonObservableDatum<X> newDatum(String fieldName, X ratio) {
		return newDatum(fieldName, ratio, false);
	}

	static public <X extends Ratio> PerfMonObservableDatum<X> newDatum(String fieldName, X ratio, boolean formatAsPercent) {
		return new PerfMonObservableDatum<X>(fieldName, ratio, formatAsPercent);
	}

	static public <X extends Delta> PerfMonObservableDatum<X> newDatum(String fieldName, X delta) {
		return new PerfMonObservableDatum<X>(fieldName, delta, false);
	}
	
	static public <X extends Delta> PerfMonObservableDatum<X> newDatum(String fieldName, X delta, boolean formatAsPerSecond) {
		return new PerfMonObservableDatum<X>(fieldName, delta, formatAsPerSecond);
	}

	static public PerfMonObservableDatum<String> newDatum(String fieldName, String value) {
		return new PerfMonObservableDatum<String>(fieldName, value);
	}

	static public PerfMonObservableDatum<String> newDateTimeDatumIfSet(String fieldName, long value) {
		if (value != PerfMon.NOT_SET) {
			return new PerfMonObservableDatum<String>(fieldName, MiscHelper.formatTimeAsISO8601(value), true);
		} else {
			return null;
		}
	}
	
	
	static public <X extends Object> PerfMonObservableDatum<X> newDatum(String fieldName, X value) {
		return new PerfMonObservableDatum<X>(fieldName, value);
	}

	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String fieldName, Boolean booleanValue) {
		super();
		if (booleanValue == null) {
			booleanValue = Boolean.FALSE;
			this.inputWasNull = true;
		} else {
			this.inputWasNull = false;
		}
		this.fieldName = this.defaultDisplayName = fieldName;
	
		this.ratio = false;
		this.delta = false;
		this.complexObject = (T)booleanValue;
		this.value = Short.valueOf((short)(booleanValue.booleanValue() ? 1 : 0));
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
		this.isDateTime = false;
	}

	private PerfMonObservableDatum(String fieldName, Number value) {
		super();
		if (value == null) {
			value = NULL_NUMBER;
			this.inputWasNull = true;
		} else {
			this.inputWasNull = false;
		}
		this.fieldName = this.defaultDisplayName = fieldName;
		this.ratio = false;
		this.delta = false;
		this.complexObject = null;
		this.value = value;
		this.stringValue = buildStringValue(value);
		this.isNumeric = true;
		this.isDateTime = false;
	}

	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String fieldName, String value) {
		this(fieldName, value, false);
	}
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String fieldName, String value, boolean isDateTime) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			this.stringValue = "null";
		} else {
			this.inputWasNull = false;
			this.stringValue = value;
		}
		this.fieldName = this.defaultDisplayName = fieldName;
		this.ratio = false;
		this.delta = false;
		this.value = null;
		this.complexObject = (T)value;
		this.isNumeric = false;
		this.isDateTime = isDateTime;
	}

	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String fieldName, Ratio value, boolean formatAsPercent) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			value = NULL_RATIO;
		} else {
			this.inputWasNull = false;
		}
		this.fieldName = fieldName;
		if (formatAsPercent) {
			this.defaultDisplayName = fieldName + "%";
		} else {
			this.defaultDisplayName = fieldName;
		}
		this.ratio = true;
		this.delta = false;
		this.value = Float.valueOf(value.getRatio() * (formatAsPercent ? 100 : 1));
		this.complexObject = (T)value;
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
		this.isDateTime = false;
	}
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String fieldName, Delta value, boolean formatAsPerSecond) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			value = NULL_DELTA;
		} else {
			this.inputWasNull = false;
		}
		this.fieldName = fieldName;
		if (formatAsPerSecond) {
			this.defaultDisplayName = fieldName + "PerSec";
		} else {
			this.defaultDisplayName = fieldName;
		}
		this.ratio = false;
		this.delta = true;
		if (formatAsPerSecond) {
			this.value = value.getDeltaPerSecond_object();
		} else {
			this.value = value.getDelta_object();
		}
		this.complexObject = (T)value;
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
		this.isDateTime = false;
	}
	
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String fieldName, Object value) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			this.stringValue = "null";
		} else {
			this.inputWasNull = false;
			this.stringValue = value.toString();
		}
		this.fieldName = this.defaultDisplayName = fieldName;
		this.ratio = false;
		this.delta = false;
		this.value = null;
		this.complexObject = (T)value;
		this.isNumeric = false;
		this.isDateTime = false;
	}

	
	public boolean isRatio() {
		return ratio;
	}
	
	public boolean isDelta() {
		return delta;
	}
	
	public Number getValue() {
		return value;
	}
	
	public T getComplexObject() {
		return complexObject;
	}
	
	public boolean isNumeric() {
		return isNumeric;
	}
	
	public boolean getInputValueWasNull() {
		return inputWasNull;
	}

	static private String buildStringValue(Number n) {
		String result;
		
		if ((n instanceof Double) || (n instanceof Float)) {
			BigDecimal bd = new BigDecimal(Double.toString(n.doubleValue()));
			bd = bd.setScale(3, BigDecimal.ROUND_HALF_UP);       
			result = bd.toPlainString();
		} else if (n != null) {
			result = n.toString();
		} else {
			result = "";
		}
		
		return result;
	}

	@Override
	public String toString() {
		return stringValue;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getDefaultDisplayName() {
		return defaultDisplayName;
	}

	public boolean isInputWasNull() {
		return inputWasNull;
	}

	public boolean isDateTime() {
		return isDateTime;
	}

	/**
	 * IMPORTANT: hashCode ONLY considers the fieldName.  This is to ensure that any set of 
	 * data includes only one datum for each unique field.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		return result;
	}

	/**
	 * IMPORTANT: equals ONLY considers the fieldName.  This is to ensure that any set of 
	 * data includes only one datum for each unique field.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PerfMonObservableDatum<?> other = (PerfMonObservableDatum<?>) obj;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		return true;
	}
	
	static public PerfMonObservableDatum<?> findObservationByFieldName(String fieldName, Set<PerfMonObservableDatum<?>> observations) {
		PerfMonObservableDatum<?> observation = null;
 		
		for (PerfMonObservableDatum<?> obv: observations) {
			if (obv.getFieldName().equals(fieldName)) {
				observation = obv;
				break;
			}
		}
		
		return observation;
	}

	static public PerfMonObservableDatum<?> findObservationByDefaultDisplayName(String defaultDisplayName, Set<PerfMonObservableDatum<?>> observations) {
		PerfMonObservableDatum<?> observation = null;
 		
		for (PerfMonObservableDatum<?> obv: observations) {
			if (obv.getDefaultDisplayName().equals(defaultDisplayName)) {
				observation = obv;
				break;
			}
		}
		
		return observation;
	}
	
}

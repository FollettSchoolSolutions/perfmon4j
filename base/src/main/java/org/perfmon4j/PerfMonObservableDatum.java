package org.perfmon4j;

import java.math.BigDecimal;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;


public class PerfMonObservableDatum<T> {
	public static final Ratio NULL_RATIO = new Ratio(0, 0);
	public static final Delta NULL_DELTA = new Delta(0, 0, 0);
	public static final Number NULL_NUMBER = new Integer(-1);
	
	private final boolean ratio;
	private final boolean delta;
	private final boolean inputWasNull;
	private final Number value;
	private final T complexObject;
	private final String stringValue;
	private final boolean isNumeric;
	

	static public PerfMonObservableDatum<Boolean> newDatum(boolean value) {
		return newDatum(Boolean.valueOf(value));
	}

	static public PerfMonObservableDatum<Boolean> newDatum(Boolean value) {
		return new PerfMonObservableDatum<Boolean>(value);
	}
	
	static public PerfMonObservableDatum<Short> newDatum(short value) {
		return newDatum(Short.valueOf(value));
	}
	
	static public PerfMonObservableDatum<Integer> newDatum(int value) {
		return newDatum(Integer.valueOf(value));
	}

	static public PerfMonObservableDatum<Long> newDatum(long value) {
		return newDatum(Long.valueOf(value));
	}
	
	static public PerfMonObservableDatum<Float> newDatum(float value) {
		return newDatum(Float.valueOf(value));
	}

	static public PerfMonObservableDatum<Double> newDatum(double value) {
		return newDatum(Double.valueOf(value));
	}
	
	static public <X extends Number> PerfMonObservableDatum<X> newDatum(X number) {
		return new PerfMonObservableDatum<X>(number);
	}

	static public <X extends Ratio> PerfMonObservableDatum<X> newDatum(X ratio) {
		return newDatum(ratio, false);
	}

	static public <X extends Ratio> PerfMonObservableDatum<X> newDatum(X ratio, boolean formatAsPercent) {
		return new PerfMonObservableDatum<X>(ratio, formatAsPercent);
	}
	
	static public <X extends Delta> PerfMonObservableDatum<X> newDatum(X delta) {
		return new PerfMonObservableDatum<X>(delta);
	}

	static public PerfMonObservableDatum<String> newDatum(String value) {
		return new PerfMonObservableDatum<String>(value);
	}

	static public <X extends Object> PerfMonObservableDatum<X> newDatum(X value) {
		return new PerfMonObservableDatum<X>(value);
	}

	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(Boolean booleanValue) {
		super();
		if (booleanValue == null) {
			booleanValue = Boolean.FALSE;
			this.inputWasNull = true;
		} else {
			this.inputWasNull = false;
		}
		
		this.ratio = false;
		this.delta = false;
		this.complexObject = (T)booleanValue;
		this.value = Short.valueOf((short)(booleanValue.booleanValue() ? 1 : 0));
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
	}

	private PerfMonObservableDatum(Number value) {
		super();
		if (value == null) {
			value = NULL_NUMBER;
			this.inputWasNull = true;
		} else {
			this.inputWasNull = false;
		}
		this.ratio = false;
		this.delta = false;
		this.complexObject = null;
		this.value = value;
		this.stringValue = buildStringValue(value);
		this.isNumeric = true;
	}
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String value) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			this.stringValue = "null";
		} else {
			this.inputWasNull = false;
			this.stringValue = value;
		}
		this.ratio = false;
		this.delta = false;
		this.value = null;
		this.complexObject = (T)value;
		this.isNumeric = false;
	}

	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(Ratio value, boolean formatAsPercent) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			value = NULL_RATIO;
		} else {
			this.inputWasNull = false;
		}
		this.ratio = true;
		this.delta = false;
		this.value = Float.valueOf(value.getRatio() * (formatAsPercent ? 100 : 1));
		this.complexObject = (T)value;
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
	}

	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(Delta value) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			value = NULL_DELTA;
		} else {
			this.inputWasNull = false;
		}
		this.ratio = false;
		this.delta = true;
		this.value = value.getDeltaPerSecond_object();
		this.complexObject = (T)value;
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
	}
	
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(Object value) {
		super();
		if (value == null) {
			this.inputWasNull = true;
			this.stringValue = "null";
		} else {
			this.inputWasNull = false;
			this.stringValue = value.toString();
		}
		this.ratio = false;
		this.delta = false;
		this.value = null;
		this.complexObject = (T)value;
		this.isNumeric = false;
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
}

package org.perfmon4j;

import java.math.BigDecimal;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;


public class PerfMonObservableDatum<T> {
	private final boolean ratio;
	private final boolean delta;
	private final Number value;
	private final T complexObject;
	private final String stringValue;
	private final boolean isNumeric;
	
	static public PerfMonObservableDatum<Boolean> newDatum(boolean value) {
		return new PerfMonObservableDatum<Boolean>(Boolean.valueOf(value));
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
		float multiplier = formatAsPercent ? 100 : 1;
		return new PerfMonObservableDatum<X>(true, false, Float.valueOf(ratio.getRatio()*multiplier), ratio);
	}
	
	static public <X extends Delta> PerfMonObservableDatum<X> newDatum(X delta) {
		return new PerfMonObservableDatum<X>(false, true, delta.getDeltaPerSecond_object(), delta);
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
		this.ratio = false;
		this.delta = false;
		this.complexObject = (T)booleanValue;
		this.value = Short.valueOf((short)(booleanValue.booleanValue() ? 1 : 0));
		this.stringValue = buildStringValue(this.value);
		this.isNumeric = true;
	}

	
	private PerfMonObservableDatum(Number value) {
		super();
		this.ratio = false;
		this.delta = false;
		this.complexObject = null;
		this.value = value;
		this.stringValue = buildStringValue(value);
		this.isNumeric = true;
	}
	
	private PerfMonObservableDatum(boolean ratio,
			boolean delta, Number value, T complexObject) {
		super();
		this.ratio = ratio;
		this.delta = delta;
		this.value = value;
		this.stringValue = buildStringValue(value);
		this.complexObject = complexObject;
		this.isNumeric = true;
	}
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(String value) {
		super();
		this.ratio = false;
		this.delta = false;
		this.value = null;
		this.stringValue = value;
		this.complexObject = (T)value;
		this.isNumeric = false;
	}
	
	@SuppressWarnings("unchecked")
	private PerfMonObservableDatum(Object value) {
		super();
		this.ratio = false;
		this.delta = false;
		this.value = null;
		this.stringValue = value.toString();
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
	

	static private String buildStringValue(Number n) {
		String result;
		
		if ((n instanceof Double) || (n instanceof Float)) {
			BigDecimal bd = new BigDecimal(Double.toString(n.doubleValue()));
			bd = bd.setScale(3, BigDecimal.ROUND_HALF_UP);       
			result = bd.toPlainString();
		} else {
			result = n.toString();
		}
		
		return result;
	}

	@Override
	public String toString() {
		return stringValue;
	}
}

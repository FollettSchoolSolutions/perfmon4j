package org.perfmon4j;

import java.math.BigDecimal;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;


public class PerfMonObservableDatum<T> {
	private final boolean ratio;
	private final boolean delta;
	private final Number value;
	private final T complexObject;
	

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
		return new PerfMonObservableDatum<X>(true, false, Float.valueOf(ratio.getRatio()), ratio);
	}
	
	static public <X extends Delta> PerfMonObservableDatum<X> newDatum(X delta) {
		return new PerfMonObservableDatum<X>(false, true, delta.getDeltaPerSecond_object(), delta);
	}
	
	private PerfMonObservableDatum(Number value) {
		super();
		this.ratio = false;
		this.delta = false;
		this.complexObject = null;
		this.value = value;
	}
	
	private PerfMonObservableDatum(boolean ratio,
			boolean delta, Number value, T complexObject) {
		super();
		this.ratio = ratio;
		this.delta = delta;
		this.value = value;
		this.complexObject = complexObject;
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
	
	@Override
	public String toString() {
		String result = "";
		
		Number n = getValue();
		if (n != null) {
			if ((n instanceof Double) || (n instanceof Float)) {
				BigDecimal bd = new BigDecimal(Double.toString(n.doubleValue()));
				bd = bd.setScale(3, BigDecimal.ROUND_HALF_UP);       
				result = bd.toPlainString();
			} else {
				result = n.toString();
			}
		}
		
		return result;
	}
}

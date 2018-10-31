package org.perfmon4j;

import java.math.BigDecimal;

import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.instrument.snapshot.Ratio;


public class PerfMonObservableDatum<T> {
	private final String fieldName;
	private final boolean ratio;
	private final boolean delta;
	private final Number value;
	private final T complexObject;
	
	static public <X extends Number> PerfMonObservableDatum<X> newDatum(String fieldName, X number) {
		return new PerfMonObservableDatum<X>(fieldName, number);
	}

	static public <X extends Ratio> PerfMonObservableDatum<X> newDatum(String fieldName, X ratio) {
		return new PerfMonObservableDatum<X>(fieldName, true, false, Float.valueOf(ratio.getRatio()), ratio);
	}
	
	static public <X extends Delta> PerfMonObservableDatum<X> newDatum(String fieldName, X delta) {
		return new PerfMonObservableDatum<X>(fieldName, false, true, delta.getDeltaPerSecond_object(), delta);
	}
	
	private PerfMonObservableDatum(String fieldName, Number value) {
		super();
		this.fieldName = fieldName;
		this.ratio = false;
		this.delta = false;
		this.complexObject = null;
		this.value = value;
	}
	
	private PerfMonObservableDatum(String fieldName, boolean ratio,
			boolean delta, Number value, T complexObject) {
		super();
		this.fieldName = fieldName;
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
	
	public String getFieldName() {
		return fieldName;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fieldName == null) ? 0 : fieldName.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

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
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}

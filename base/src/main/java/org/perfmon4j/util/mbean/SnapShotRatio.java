package org.perfmon4j.util.mbean;


public interface SnapShotRatio {
	public String getName();
	public String getDenominator();
	public String getNumerator();
	public boolean isFormatAsPercent();
}

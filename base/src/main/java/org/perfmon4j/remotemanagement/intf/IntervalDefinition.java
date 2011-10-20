/*
 *	Copyright 2011 Follett Software Company 
 *
 *	This file is part of PerfMon4j(tm).
 *
 * 	Perfmon4j is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU Lesser General Public License, version 3,
 * 	as published by the Free Software Foundation.  This program is distributed
 * 	WITHOUT ANY WARRANTY OF ANY KIND, WITHOUT AN IMPLIED WARRANTY OF MERCHANTIBILITY,
 * 	OR FITNESS FOR A PARTICULAR PURPOSE.  You should have received a copy of the GNU Lesser General Public 
 * 	License, Version 3, along with this program.  If not, you can obtain the LGPL v.s at 
 * 	http://www.gnu.org/licenses/
 * 	
 * 	perfmon4j@fsc.follett.com
 * 	David Deuchert
 * 	Follett Software Company
 * 	1391 Corporate Drive
 * 	McHenry, IL 60050
 * 
*/
package org.perfmon4j.remotemanagement.intf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntervalDefinition extends MonitorDefinition {
	private static final long serialVersionUID = ManagementVersion.MAJOR_VERSION;

	private final List<FieldDefinition> fields = new ArrayList<FieldDefinition>();
	
	public IntervalDefinition() {
		super("Interval", MonitorDefinition.INTERVAL_TYPE);
	
		fields.add(new FieldDefinition(this.getType(), "MaxActiveThreadCount", FieldDefinition.INTEGER_TYPE));
		fields.add(new FieldDefinition(this.getType(), "MaxDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "MaxSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "MinDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "MinSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TotalDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TotalSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "AverageDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "AverageSQLDuration", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "SumOfSquares", FieldDefinition.LONG_TYPE));
		fields.add(new FieldDefinition(this.getType(), "SumOfSQLSquares", FieldDefinition.LONG_TYPE));

		fields.add(new FieldDefinition(this.getType(), "TotalCompletions", FieldDefinition.INTEGER_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TotalHits", FieldDefinition.INTEGER_TYPE));

		fields.add(new FieldDefinition(this.getType(), "TimeStart", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TimeStop", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TimeMaxActiveThreadCountSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TimeMaxDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TimeMinDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TimeMaxSQLDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		fields.add(new FieldDefinition(this.getType(), "TimeMinSQLDurationSet", FieldDefinition.TIMESTAMP_TYPE));
		
		fields.add(new FieldDefinition(this.getType(), "ThroughputPerMinute", FieldDefinition.DOUBLE_TYPE));
		fields.add(new FieldDefinition(this.getType(), "StdDeviation", FieldDefinition.DOUBLE_TYPE));
	}

	@Override
	public Iterator<FieldDefinition> getFieldItr() {
		return fields.iterator();
	}
	
	
	/**
    public long getLifetimeStartTime()
    public double getLifetimeStdDeviation()
    public double getLifetimeSQLStdDeviation() {
    public int getLifetimeMaxThreadCount() {
    public long getLifetimeMinDuration() {
    public long getLifetimeAverageDuration() {
    private long getLifetimeMaxSQLDuration() {
    public long getLifetimeMinSQLDuration() {
    public long getLifetimeAverageSQLDuration() {
    public long getTimeLifetimeMaxActiveThreadCountSet() {
    public long getTimeLifetimeMaxDurationSet() {
    public long getTimeLifetimeMinDurationSet() {
    public long getTimeLifetimeMaxSQLDurationSet() {
    public long getTimeLifetimeMinSQLDurationSet() {
	 */

	static public void main(String args[]) {
		IntervalDefinition d = new IntervalDefinition();
		System.out.println(new IntervalDefinition());
		Iterator<FieldDefinition> defItr = d.getFieldItr();
		while (defItr.hasNext()) {
			System.out.println(defItr.next());
		}
	}

}

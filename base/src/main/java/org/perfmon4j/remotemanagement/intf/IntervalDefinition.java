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
	
	protected IntervalDefinition() {
		super("Interval", MonitorDefinition.Type.INTERVAL);
	
		fields.add(new FieldDefinition(this, "MaxActiveThreadCount", FieldDefinition.Type.INTEGER));
		fields.add(new FieldDefinition(this, "MaxDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "MaxSQLDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "MinDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "MinSQLDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "TotalDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "TotalSQLDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "AverageDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "AverageSQLDuration", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "SumOfSquares", FieldDefinition.Type.LONG));
		fields.add(new FieldDefinition(this, "SumOfSQLSquares", FieldDefinition.Type.LONG));

		fields.add(new FieldDefinition(this, "TotalCompletions", FieldDefinition.Type.INTEGER));
		fields.add(new FieldDefinition(this, "TotalHits", FieldDefinition.Type.INTEGER));

		fields.add(new FieldDefinition(this, "TimeStart", FieldDefinition.Type.TIMESTAMP));
		fields.add(new FieldDefinition(this, "TimeStop", FieldDefinition.Type.TIMESTAMP));
		fields.add(new FieldDefinition(this, "TimeMaxActiveThreadCountSet", FieldDefinition.Type.TIMESTAMP));
		fields.add(new FieldDefinition(this, "TimeMaxDurationSet", FieldDefinition.Type.TIMESTAMP));
		fields.add(new FieldDefinition(this, "TimeMinDurationSet", FieldDefinition.Type.TIMESTAMP));
		fields.add(new FieldDefinition(this, "TimeMaxSQLDurationSet", FieldDefinition.Type.TIMESTAMP));
		fields.add(new FieldDefinition(this, "TimeMinSQLDurationSet", FieldDefinition.Type.TIMESTAMP));
		
		fields.add(new FieldDefinition(this, "ThroughputPerMinute", FieldDefinition.Type.DOUBLE));
		fields.add(new FieldDefinition(this, "StdDeviation", FieldDefinition.Type.DOUBLE));
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

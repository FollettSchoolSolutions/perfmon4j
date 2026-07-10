/*
 *	Copyright 2019 Follett School Solutions 
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
 * 	ddeuchert@follett.com
 * 	David Deuchert
 * 	Follett School Solutions
*/
package api.org.perfmon4j.agent.instrument;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



/**
 * This class serves as an alias for the SnapShotString Annotation
 * implemented in the perfmon4j agent.
 *
 * The alias provides a simplified implementation using only default
 * values for the following attributes:
 * 	formatter = SnapShotStringFormatter.class
 *
 * outputAsTag is passed through to the base annotation as-is; it is not
 * defaulted away like formatter.
 *
 * @author perfmon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SnapShotString {
	public boolean isInstanceName() default false;

	/**
	 * When true, appenders that support tag semantics (e.g. InfluxAppender)
	 * will serialize this value as a tag rather than a field. Appenders
	 * without tag support ignore this and treat the value as a normal
	 * string. Defaults to false, preserving existing behavior.
	 */
	public boolean outputAsTag() default false;
}

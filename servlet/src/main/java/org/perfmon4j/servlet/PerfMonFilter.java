/*
 *	Copyright 2008, 2009, 2010 Follett Software Company 
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
package org.perfmon4j.servlet;


@Deprecated
/** 
 * SEE web.org.perfmon4j.servlet.PerfmonServlet
 *
 */
public class PerfMonFilter extends web.org.perfmon4j.servlet.PerfMonFilter {
	/** This class exists for backward compatability issues
	 *  However be aware that you should use "web.org.perfmon4j.servlet packaging"
	 *  as perfmon4j puts special meaning on the "org.perfmon4j" package and how 
	 *  classloading is handled.  If you cannot get your class to be found despite
	 *  your best efforts, you might have that problem.
	 */
    public PerfMonFilter(boolean childOfPerfmonValve) {
    	super(childOfPerfmonValve); 
    }	
}

/*
 *	Copyright 2012 Follett Software Company 
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

package org.perfmon4j;

import java.util.ArrayList;
import java.util.List;

public class NoWrapTimerContainer {
	public PerfMonTimer extremeTimer;
	public PerfMonTimer annotationTimer;
	public PerfMonTimer extremeSQLTimer;

	public static class ArrayStack  {
		@SuppressWarnings("unchecked")
		private List list = new ArrayList(128); 
		
		@SuppressWarnings("unchecked")
		public void push(NoWrapTimerContainer value) {
			list.add(value);
		}
		
		public NoWrapTimerContainer pop() {
			return (NoWrapTimerContainer)list.remove(list.size()-1);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static ThreadLocal callStack = new ThreadLocal() {
        protected synchronized ArrayStack initialValue() {
            return new ArrayStack();
        }			
	};	
}


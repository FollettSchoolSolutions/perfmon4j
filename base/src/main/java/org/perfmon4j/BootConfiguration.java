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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.perfmon4j.util.BeanHelper;
import org.perfmon4j.util.BeanHelper.UnableToSetAttributeException;


public class BootConfiguration {
	private ServletValveConfig servletValveConfig = null;
	private ExceptionTrackerConfig exceptionTrackerConfig = null;
	
	public ServletValveConfig getServletValveConfig() {
		return servletValveConfig;
	}

	public void setServletValveConfig(ServletValveConfig servletValveConfig) {
		this.servletValveConfig = servletValveConfig;
	}
	
	public ExceptionTrackerConfig getExceptionTrackerConfig() {
		return exceptionTrackerConfig;
	}

	public void setExceptionTrackerConfig(ExceptionTrackerConfig exceptionTrackerConfig) {
		this.exceptionTrackerConfig = exceptionTrackerConfig;
	}

	public static final class ServletValveConfig {
		private String baseFilterCategory = "WebRequest";
	    private boolean abortTimerOnRedirect = false;
	    private boolean abortTimerOnImageResponse = false;
	    private String abortTimerOnURLPattern = null;
	    private String skipTimerOnURLPattern = null;
	    private boolean outputRequestAndDuration = false;
	    private String pushCookiesOnNDC = null;
	    private String pushSessionAttributesOnNDC = null;
	    private boolean pushClientInfoOnNDC = false;
	    private boolean pushURLOnNDC = false;
	    private String servletPathTransformationPattern = null;
	    
		public String getBaseFilterCategory() {
			return baseFilterCategory;
		}
		public void setBaseFilterCategory(String baseFilterCategory) {
			this.baseFilterCategory = baseFilterCategory;
		}
		public boolean isAbortTimerOnRedirect() {
			return abortTimerOnRedirect;
		}
		public void setAbortTimerOnRedirect(boolean abortTimerOnRedirect) {
			this.abortTimerOnRedirect = abortTimerOnRedirect;
		}
		public boolean isAbortTimerOnImageResponse() {
			return abortTimerOnImageResponse;
		}
		public void setAbortTimerOnImageResponse(boolean abortTimerOnImageResponse) {
			this.abortTimerOnImageResponse = abortTimerOnImageResponse;
		}
		public String getAbortTimerOnURLPattern() {
			return abortTimerOnURLPattern;
		}
		public void setAbortTimerOnURLPattern(String abortTimerOnURLPattern) {
			this.abortTimerOnURLPattern = abortTimerOnURLPattern;
		}
		public String getSkipTimerOnURLPattern() {
			return skipTimerOnURLPattern;
		}
		public void setSkipTimerOnURLPattern(String skipTimerOnURLPattern) {
			this.skipTimerOnURLPattern = skipTimerOnURLPattern;
		}
		public boolean isOutputRequestAndDuration() {
			return outputRequestAndDuration;
		}
		public void setOutputRequestAndDuration(boolean outputRequestAndDuration) {
			this.outputRequestAndDuration = outputRequestAndDuration;
		}
		public String getPushCookiesOnNDC() {
			return pushCookiesOnNDC;
		}
		public void setPushCookiesOnNDC(String pushCookiesOnNDC) {
			this.pushCookiesOnNDC = pushCookiesOnNDC;
		}
		public String getPushSessionAttributesOnNDC() {
			return pushSessionAttributesOnNDC;
		}
		public void setPushSessionAttributesOnNDC(String pushSessionAttributesOnNDC) {
			this.pushSessionAttributesOnNDC = pushSessionAttributesOnNDC;
		}
		public boolean isPushClientInfoOnNDC() {
			return pushClientInfoOnNDC;
		}
		public void setPushClientInfoOnNDC(boolean pushClientInfoOnNDC) {
			this.pushClientInfoOnNDC = pushClientInfoOnNDC;
		}
		
		public boolean isPushURLOnNDC() {
			return pushURLOnNDC;
		}
		public void setPushURLOnNDC(boolean pushURLOnNDC) {
			this.pushURLOnNDC = pushURLOnNDC;
		}
		
		public String getServletPathTransformationPattern() {
			return servletPathTransformationPattern;
		}
		public void setServletPathTransformationPattern(String servletPathTransformationPattern) {
			this.servletPathTransformationPattern = servletPathTransformationPattern;
		}
		
		private void setValue(Object valve, String attributeName, Object value ) {
			try {
				if (value != null) {
					BeanHelper.setValue(valve, attributeName, value);
				}
			} catch (UnableToSetAttributeException e) {
				// Nothing todo...
			}
			
		}
		
		
		public void copyProperties(Object valve) throws UnableToSetAttributeException {
			setValue(valve, "baseFilterCategory", getBaseFilterCategory());
			setValue(valve, "abortTimerOnURLPattern", getAbortTimerOnURLPattern());
			setValue(valve, "skipTimerOnURLPattern", getSkipTimerOnURLPattern());
			setValue(valve, "pushCookiesOnNDC", getPushCookiesOnNDC());
			setValue(valve, "pushSessionAttributesOnNDC", getPushSessionAttributesOnNDC());
			setValue(valve, "servletPathTransformationPattern", getServletPathTransformationPattern());
			
			setValue(valve, "abortTimerOnRedirect", Boolean.valueOf(isAbortTimerOnRedirect()));
			setValue(valve, "abortTimerOnImageResponse", Boolean.valueOf(isAbortTimerOnImageResponse()));
			setValue(valve, "outputRequestAndDuration", Boolean.valueOf(isOutputRequestAndDuration()));
			setValue(valve, "pushClientInfoOnNDC", Boolean.valueOf(isPushClientInfoOnNDC()));
			setValue(valve, "pushURLOnNDC", Boolean.valueOf(isPushURLOnNDC()));
		}	    
	}
	
	public static final class ExceptionTrackerConfig {
		private final Set<ExceptionElement> elements = new HashSet<BootConfiguration.ExceptionElement>();

		public void addElement(ExceptionElement element) {
			elements.add(element);
		}
		
		public ExceptionElement[] getElements() {
			return elements.toArray(new ExceptionElement[] {});
		}
	}
	
	public static final class ExceptionElement {
		private final String className;
		private final String displayName;

		public ExceptionElement(String className) {
			this(className, className);
			
		}
		
		public ExceptionElement(String className, String displayName) {
			this.className = className;
			this.displayName = displayName;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public String getClassName() {
			return className;
		}

		@Override
		public int hashCode() {
			return Objects.hash(className);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ExceptionElement other = (ExceptionElement) obj;
			return Objects.equals(className, other.className);
		}
	}

}

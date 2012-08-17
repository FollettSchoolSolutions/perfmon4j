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


public class BootConfiguration {
	private ServletValveConfig servletValveConfig = null;
	
	public ServletValveConfig getServletValveConfig() {
		return servletValveConfig;
	}

	public void setServletValveConfig(ServletValveConfig servletValveConfig) {
		this.servletValveConfig = servletValveConfig;
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
	}
}

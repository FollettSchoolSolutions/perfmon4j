/*
 *	Copyright 2008,2009 Follett Software Company 
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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.NDC;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.UserAgentSnapShotMonitor;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public class PerfMonNDCFilter extends PerfMonFilter {
    private static final Logger logger = LoggerFactory.initLogger(PerfMonNDCFilter.class);
    
    final static public String PROPERTY_PUSH_URL_ON_NDC = "PUSH_URL_ON_NDC";
    private boolean pushURLOnNDC = false;
 
    
/*----------------------------------------------------------------------------*/    
    public void init(FilterConfig filterConfig) throws ServletException {
    	super.init(filterConfig);
        pushURLOnNDC = Boolean.parseBoolean(getInitParameter(filterConfig, PROPERTY_PUSH_URL_ON_NDC, Boolean.FALSE.toString()));
    }
    
/*----------------------------------------------------------------------------*/    
    protected void doFilterHttpRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {    
        boolean pushedNDC = false;
        try {
            if (pushURLOnNDC) {
                NDC.push(buildRequestDescription(request));
                pushedNDC = true;
            }
            super.doFilterHttpRequest(request, response, chain);
        } finally {
            if (pushedNDC) {
                NDC.pop();
            }
        }
    }
}

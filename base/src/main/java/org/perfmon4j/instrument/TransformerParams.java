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

package org.perfmon4j.instrument;

import java.util.ArrayList;
import java.util.List;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

class TransformerParams {
	private static final Logger logger = LoggerFactory.initLogger(TransformerParams.class);
	
    static final int MODE_NONE = 0;
    static final int MODE_ANNOTATE = 1;
    static final int MODE_EXTREME = 2;
    static final int MODE_BOTH = 3;
    
    List<String> annotateList = new ArrayList();
    List<ExtremeListElement> extremeList = new ArrayList();
    List<String> ignoreList = new ArrayList();
    
 
    /**
     * Some classes are problematic for instrumentation...  As we find the
     * add it to the BLACKLIST...
     * Note: As a workaround the user can exclude them via the -i
     * parameter.
     * 
     * Unlike the ignoreList the
     * 
     */
    private final static String BLACKLIST[] = {
    		"org.apache.log4j",  	// Skip logging classes... Good way to end up in an infinite loop...
			"java.util.logging",  	// Skip logging classes... Good way to end up in an infinite loop...
    		"org.jboss.security",  	// Instrumenting these classes fail on JBoss 5.1.x
    		"org.jboss.aop" 	 	// Instrumenting another instrumentation framework seems like a bad idea
    };
    
    
    String xmlFileToConfig = null;
    private boolean bootStrapInstrumentationEnabled = false;
    private int reloadConfigSeconds = 60;
    private boolean debugEnabled = Boolean.getBoolean("PerfMon4j.debugEnabled");
    private boolean verboseEnabled = false;
    
    
	TransformerParams() {
        this("");
    }
    
	/**
	 * Want to replace commas unless they are nested inside a ()
	 * @param in
	 * @return
	 */
	private String replaceCommas(String in) {
		StringBuilder result = new StringBuilder();
		boolean inParens = false;
		
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			
			if (c == ',' && !inParens) {
				c = ' ';
			} else if (c == '(') {
				inParens = true;
			} else if (c == ')') {
				inParens = false;
			}
			result.append(c);
		}
		return result.toString();
	}
	
	
    TransformerParams(String params) {
        boolean paramsAreLegal = true;
        
        if (params != null && params.length() > 0) {
        	params = params.replaceAll("&nbsp;", " ");
        	params = replaceCommas(params);
            do {
            	params = params.trim();
                
                if (params.indexOf('"') == 0) {
                    params = params.substring(1);
                }
                
                if (params.indexOf('"') == params.length() - 1) {
                    params = params.substring(0, params.length() - 1);
                }
                
                NextParam nextParam = null;
                if (isParam('a', params)) {
                    nextParam = getNextParam(params);
                    annotateList.add(nextParam.parameter);
                } else if (isParam('e', params)) {
                    nextParam = getNextParam(params);
                    
                     TransformOptions o = parseOptions(nextParam);
                     if (o == null) {
                    	 throw new RuntimeException ("Illegal javassist params: " + nextParam);
                     }
                     
                    extremeList.add(new ExtremeListElement(o,nextParam.parameter));
                } else if (isParam('f', params)) {
                    nextParam = getNextParam(params);
                    xmlFileToConfig = nextParam.parameter;
                } else if (isParam('b', params)) {
                	nextParam = getNextParam(params);
                	bootStrapInstrumentationEnabled = Boolean.parseBoolean(nextParam.parameter);
                } else if (isParam('d', params)) {
                	nextParam = getNextParam(params);
                	debugEnabled = Boolean.parseBoolean(nextParam.parameter);
                } else if (isParam('v', params)) {
                	nextParam = getNextParam(params);
                	verboseEnabled = Boolean.parseBoolean(nextParam.parameter);
                } else if (isParam('i', params)) {
                	nextParam = getNextParam(params);
                    ignoreList.add(nextParam.parameter);
                } else if (isParam('r', params)) {
                	nextParam = getNextParam(params);
                	String val = nextParam.parameter;
                	try {
                		reloadConfigSeconds = Integer.parseInt(val);
                	} catch (NumberFormatException nfe) {
                		logger.logWarn("Unable to parse (reloadConfigSeconds) parameter from -r" + val);
                	}
                	if (reloadConfigSeconds <= 0) {
                		reloadConfigSeconds = 0;
                		logger.logInfo("Configuration file will NOT be checked for modifications");
                	} else if (reloadConfigSeconds < 10) {
                		reloadConfigSeconds = 10;
                		logger.logWarn("Minimum reloadConfigSeconds allowed is 10 seconds");
                	}
                	
                }
                if (nextParam != null) {
                    params = nextParam.remainingParamString;
                } else {
                    paramsAreLegal = false;
                }
            } while (paramsAreLegal && params.length() > 0);
        }
        
        if (!paramsAreLegal) {
            throw new RuntimeException ("Illegal javassist params: " + params);
        }
    }

    final private TransformOptions parseOptions(NextParam nextParam) {
    	TransformOptions result = null;
    	String param = nextParam.parameter;
    	
    	if (param.startsWith("(")) {
    		int offset = param.indexOf(")");
    		if (offset > 0) {
    			String options = param.substring(0, offset+1);
    			nextParam.parameter = param.substring(offset+1);
    			
    			result = new TransformOptions(options.contains("+getter"), options.contains("+setter"));
    		}
    	} else {
    		result = new TransformOptions(false, false);
    	}
    	
    	return result;
    }
    
    
    final private static boolean isParam(char paramValue, String param) {
    	return param.startsWith(paramValue + "=") || param.startsWith("-" + paramValue);
    }
    
    final private static class NextParam {
        private String remainingParamString;
        private String parameter;
    }

    private NextParam getNextParam(String params) {
        NextParam next = new NextParam();
        
        int endIndex = params.indexOf(" ");
        if (endIndex < 0) {
            endIndex = params.length();
        }
        next.parameter = params.substring(2, endIndex);
        next.remainingParamString = params.substring(endIndex);
        
        return next;
    }   

    
    int getTransformMode(String className) {
        int mode = MODE_ANNOTATE;
        int includeMatchLength = 0;
        
        if ((annotateList.size() > 0) || (extremeList.size() > 0)) {
            int annotateMatchLength = getMatchElement(annotateList, className).matchLength;
            int extremeMatchLength = getMatchElement(extremeList, className).matchLength;
            
            includeMatchLength = Math.max(annotateMatchLength, extremeMatchLength);
           
            if (annotateMatchLength > 0) {
                mode = extremeMatchLength > 0 ? MODE_BOTH : MODE_ANNOTATE;
            } else if (extremeMatchLength > 0) {
                mode = MODE_EXTREME;
            } else {
                mode = MODE_NONE;
            }
        }
        if ((mode != MODE_NONE) && (ignoreList.size() > 0)) {
        	int len = getMatchElement(ignoreList, className).matchLength;
        	if (len > includeMatchLength) {
        		mode = MODE_NONE;
        	}
        }
        
        // Finally check the BLACKLIST for problamatic classes..
        if (mode != MODE_NONE) {
        	for (int i = 0; i < BLACKLIST.length; i++) {
        		if (className.startsWith(BLACKLIST[i])) {
        			logger.logDebug("Skipping instrumentation for blacklisted class: " + className);
        			mode = MODE_NONE;
        		}
			}
        }
        return mode;
    }
    
    private MatchElement getMatchElement(List list, String className) {
        MatchElement matchElement = new MatchElement(0, null);
        int listSize = list.size();
        
        for (int i = 0; i < listSize; i++) {
            String element = list.get(i).toString();
            
            if (className.equals(element) || className.startsWith(element + ".")) {
                int currentLength = element.length();
                
                if (currentLength > matchElement.matchLength) {
                	matchElement = new MatchElement(currentLength, list.get(i));
                }
            }
        }
        
        return matchElement;
    }
    
    int getTransformMode(Class clazz) {
        return getTransformMode(clazz.getName());
    }

    public String getXmlFileToConfig() {
        return xmlFileToConfig;
    }

    public boolean isBootStrapInstrumentationEnabled() {
		return bootStrapInstrumentationEnabled;
	}

	public int getReloadConfigSeconds() {
		return reloadConfigSeconds;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}
	
	public TransformOptions getTransformOptions(Class clazz) {
		return getTransformOptions(clazz.getName());
	}
	
	
	public TransformOptions getTransformOptions(String className) {
		TransformOptions result = TransformOptions.DEFAULT;
		ExtremeListElement element = (ExtremeListElement)getMatchElement(extremeList, className).matchElement;
		if (element != null) {
			result = element.getOptions();
		}
		return result;
	}
	
	public static class TransformOptions {
		public static final TransformOptions DEFAULT = new TransformOptions(false, false);
		private final boolean instrumentSetters;
		private final boolean instrumentGetters;
	
		private TransformOptions(boolean instrumentGetters, boolean instrumentSetters) {
			this.instrumentGetters = instrumentGetters;
			this.instrumentSetters = instrumentSetters;
		}

		public boolean isInstrumentSetters() {
			return instrumentSetters;
		}

		public boolean isInstrumentGetters() {
			return instrumentGetters;
		}
		
	}
	
	private static class ExtremeListElement {
		private final TransformOptions options;
		private final String value;
		
		private ExtremeListElement(TransformOptions options, String value) {
			this.options = options;
			this.value = value;
		}

		public TransformOptions getOptions() {
			return options;
		}

		public String getValue() {
			return value;
		}
		
		public String toString() {
			return value;
		}
	}
	
	private static class MatchElement {
		private final int matchLength;
		private final Object matchElement;
		
		private MatchElement(int matchLength, Object matchElement) {
			this.matchLength = matchLength;
			this.matchElement = matchElement;
		}
	}

	public boolean isVerboseInstrumentationEnabled() {
		return isDebugEnabled() || verboseEnabled;
	}
	
	public boolean isExtremeInstrumentationEnabled() {
		return !extremeList.isEmpty();
	}

	public boolean isAnnotationInstrumentationEnabled() {
		return !annotateList.isEmpty();
	}
}

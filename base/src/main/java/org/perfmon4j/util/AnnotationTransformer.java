/*
 *	Copyright 2008-2011 Follett Software Company 
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

package org.perfmon4j.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.SnapShotStringFormatter;



public class AnnotationTransformer {
    static private final Logger logger = LoggerFactory.initLogger(AnnotationTransformer.class);
    private final Map<Class<? extends Annotation>, Worker<?>> workers = new HashMap<Class<? extends Annotation>, Worker<?>>(); 

    private static final String API_SNAP_SHOT_COUNTER = "org.perfmon4j.agent.api.instrument.SnapShotCounter";
    private static final String API_SNAP_SHOT_PROVIDER = "org.perfmon4j.agent.api.instrument.SnapShotProvider";
    private static final String API_SNAP_SHOT_GAUGE = "org.perfmon4j.agent.api.instrument.SnapShotGauge";
    private static final String API_SNAP_SHOT_STRING = "org.perfmon4j.agent.api.instrument.SnapShotString";
    private static final String API_SNAP_SHOT_RATIO = "org.perfmon4j.agent.api.instrument.SnapShotRatio";
    
    
    public AnnotationTransformer() {
    	workers.put(SnapShotCounter.class, new CounterWorker());
    	workers.put(SnapShotProvider.class, new ProviderWorker());
    	workers.put(SnapShotGauge.class, new GaugeWorker());
    	workers.put(SnapShotString.class, new StringWorker());
    	workers.put(SnapShotRatio.class, new RatioWorker());
    }

    public <T extends Annotation> T transform(Class<T> annotationClass, Annotation an) {
    	T result = null;
    	if (an != null) {
    		if (annotationClass.isInstance(an)) {
    			result = annotationClass.cast(an);
    		}
    		if (result == null) {
    			Worker<?> worker = workers.get(annotationClass);
    			if (worker != null) {
    				Annotation tmp = worker.transform(an);
    				if (tmp != null) {
    					result = annotationClass.cast(tmp);
    				}
    			}
    		}
    	}
    	
    	return result;
    }
    
    public <T extends Annotation> T findAnotation(Class<T> annotationClass, Class<?> clazz) {
    	T result = clazz.getAnnotation(annotationClass);
    	
    	if (result == null) {
	    	for (Annotation an : clazz.getAnnotations()) {
	    		result = transform(annotationClass, an);
	    		if (result != null) {
	    			break;
	    		}
	    	}
    	}
    	
    	return result;
    }
    
    public <T extends Annotation> T findAnotation(Class<T> annotationClass, Method method) {
    	T result = method.getAnnotation(annotationClass);
    	
    	if (result == null) {
	    	for (Annotation an : method.getAnnotations()) {
	    		result = transform(annotationClass, an);
	    		if (result != null) {
	    			break;
	    		}
	    	}
    	}
    	
    	return result;
    }

    
    private static <T> T getNamedProperty(Annotation an, String propertyName, T defaultValue) {
    	T result = defaultValue;
    	
    	try {
			Method m = an.getClass().getMethod(propertyName, new Class[]{});
			result = (T)m.invoke(an, new Object[]{});
		} catch (NoSuchMethodException e) {
			// Ignore
		} catch (SecurityException e) {
			// Ignore
		} catch (IllegalAccessException e) {
			// Ignore
		} catch (IllegalArgumentException e) {
			// Ignore
		} catch (InvocationTargetException e) {
			// Ignore
		}
    	
    	
    	return result;
    }
    
    
    private static interface Worker<T extends Annotation> {
    	public T transform(Annotation an);
    }

    private static class CounterWorker implements Worker<SnapShotCounter> {
		public SnapShotCounter transform(Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_COUNTER)) {
	    		return new SnapShotCounter() {
					
					public Class<? extends Annotation> annotationType() {
						return SnapShotCounter.class;
					}
					
					public String suffix() {
						return "";
					}
					
					public Display preferredDisplay() {
						return SnapShotCounter.Display.DELTA;
					}
					
					public Class<? extends NumberFormatter> formatter() {
						return NumberFormatter.class;
					}
				};
	    	}
	    	return null;
		}
    }
    
    private static class ProviderWorker implements Worker<SnapShotProvider> {
		public SnapShotProvider transform(Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_PROVIDER)) {
	    		return new SnapShotProvider() {
					
					public Class<? extends Annotation> annotationType() {
						return SnapShotProvider.class;
					}
					
					public boolean usePriorityTimer() {
						return false;
					}
					
					public Type type() {
						return SnapShotProvider.Type.INSTANCE_PER_MONITOR;
					}
					
					public Class<?> dataInterface() {
						return void.class;
					}

					public Class<? extends SnapShotSQLWriter> sqlWriter() {
						return null;
					}
				};
	    	}
	    	
	    	return null;
		}
    }
    
    
    private static class GaugeWorker implements Worker<SnapShotGauge> {
		public SnapShotGauge transform(Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_GAUGE)) {
	    		return new SnapShotGauge() {
	
					public Class<? extends Annotation> annotationType() {
						return SnapShotGauge.class;
					}
	
					public Class<? extends NumberFormatter> formatter() {
						return NumberFormatter.class;
					}
				};
	    	}
	    	return null;
		}
    }
    
    private static class StringWorker implements Worker<SnapShotString> {
		public SnapShotString transform(Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_STRING)) {
	    		return new SnapShotString() {

					public Class<? extends Annotation> annotationType() {
						return SnapShotString.class;
					}

					public Class<? extends SnapShotStringFormatter> formatter() {
						return SnapShotStringFormatter.class;
					}

					public boolean isInstanceName() {
						return false;
					}
				};
	    	}
	    	return null;
		}
    }
    
    
   
    
    
    private static class RatioWorker implements Worker<SnapShotRatio> {
		public SnapShotRatio transform(final Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_RATIO)) {
	    		return new SnapShotRatio() {

					public Class<? extends Annotation> annotationType() {
						return SnapShotString.class;
					}

					public String name() {
						return getNamedProperty(an, "name", (String)null);
					}

					public String denominator() {
						return getNamedProperty(an, "denominator", (String)null);
					}

					public String numerator() {
						return getNamedProperty(an, "numerator", (String)null);
					}

					public boolean displayAsPercentage() {
						return getNamedProperty(an, "displayAsPercentage", Boolean.FALSE).booleanValue();
					}

					public boolean displayAsDuration() {
						return getNamedProperty(an, "displayAsDuration", Boolean.FALSE).booleanValue();
					}
				};
	    	}
	    	return null;
		}
    }


} 

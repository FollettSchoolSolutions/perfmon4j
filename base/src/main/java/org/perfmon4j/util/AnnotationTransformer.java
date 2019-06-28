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
package org.perfmon4j.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotCounter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotRatio;
import org.perfmon4j.instrument.SnapShotRatios;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.instrument.SnapShotStringFormatter;



public class AnnotationTransformer {
    static private final Logger logger = LoggerFactory.initLogger(AnnotationTransformer.class);
    private final Map<Class<? extends Annotation>, Worker<?>> workers = new HashMap<Class<? extends Annotation>, Worker<?>>(); 

    private static final String API_SNAP_SHOT_COUNTER = "api.org.perfmon4j.agent.instrument.SnapShotCounter";
    private static final String API_SNAP_SHOT_PROVIDER = "api.org.perfmon4j.agent.instrument.SnapShotProvider";
    private static final String API_SNAP_SHOT_GAUGE = "api.org.perfmon4j.agent.instrument.SnapShotGauge";
    private static final String API_SNAP_SHOT_STRING = "api.org.perfmon4j.agent.instrument.SnapShotString";
    private static final String API_SNAP_SHOT_RATIO = "api.org.perfmon4j.agent.instrument.SnapShotRatio";
    private static final String API_SNAP_SHOT_RATIOS = "api.org.perfmon4j.agent.instrument.SnapShotRatios";
    private static final String API_SNAP_SHOT_INSTANCE_DEFINITION = "api.org.perfmon4j.agent.instrument.SnapShotInstanceDefinition";
    
    
    public AnnotationTransformer() {
    	workers.put(SnapShotCounter.class, new CounterWorker());
    	workers.put(SnapShotProvider.class, new ProviderWorker());
    	workers.put(SnapShotGauge.class, new GaugeWorker());
    	workers.put(SnapShotString.class, new StringWorker());
    	workers.put(SnapShotRatio.class, new RatioWorker());
    	workers.put(SnapShotRatios.class, new RatiosWorker());
    	workers.put(SnapShotInstanceDefinition.class, new InstanceDefinitionWorker());
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
    
    /* package level for testing */ static <T extends Enum<T>> T transformEnumValue(Class<T> enumType, Enum<?>sourceValue, T defaultValue) {
    	T result = defaultValue;
    	
    	if (sourceValue != null) {
	    	try {
	        	result = Enum.valueOf(enumType, sourceValue.name()) ;
	    	} catch (IllegalArgumentException ex) {
	    		logger.logWarn(enumType + " does not contain a matching value for \"" 
	    				+ sourceValue.name() + "\". Returning default: \"" + defaultValue.name() + "\"");
	    	}
    	} else {
    		logger.logWarn("null passed in for sourceValue. Returning default: \"" + enumType + "." + defaultValue.name() + "\"");
    	}
    	return result;
    }

    @SuppressWarnings("unchecked")
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
		} catch (ClassCastException e) {
			// Ignore
		}
    	
    	
    	return result;
    }
    

	private static Enum<?> getNamedEnumProperty(Annotation an, String enumPropertyName) {
    	Enum<?> result = null;
    	
    	try {
			Method m = an.getClass().getMethod(enumPropertyName, new Class[]{});
			result = (Enum<?>)m.invoke(an, new Object[]{});
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
		} catch (ClassCastException e) {
			// Ignore
		}
    	
    	
    	return result;
    }
    
    
    private static interface Worker<T extends Annotation> {
    	public T transform(Annotation an);
    }

    private static class CounterWorker implements Worker<SnapShotCounter> {
		public SnapShotCounter transform(final Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_COUNTER)) {
	    		return new SnapShotCounter() {
					public Class<? extends Annotation> annotationType() {
						return SnapShotCounter.class;
					}
					
					public String suffix() {
						return "";
					}
					
					public Display preferredDisplay() {
						return AnnotationTransformer.transformEnumValue(SnapShotCounter.Display.class, 
								AnnotationTransformer.getNamedEnumProperty(an, "preferredDisplay"),
								SnapShotCounter.Display.DELTA);
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
		public SnapShotProvider transform(final Annotation an) {
			if (an.annotationType().getName().equals(API_SNAP_SHOT_PROVIDER)) {
	    		return new SnapShotProvider() {
					public Class<? extends Annotation> annotationType() {
						return SnapShotProvider.class;
					}
					
					public boolean usePriorityTimer() {
						return false;
					}
					
					public Type type() {
						return AnnotationTransformer.transformEnumValue(SnapShotProvider.Type.class, 
								AnnotationTransformer.getNamedEnumProperty(an, "type"),
								SnapShotProvider.Type.INSTANCE_PER_MONITOR);
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
		public SnapShotString transform(final Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_STRING)) {
	    		return new SnapShotString() {
					public Class<? extends Annotation> annotationType() {
						return SnapShotString.class;
					}

					public Class<? extends SnapShotStringFormatter> formatter() {
						return SnapShotStringFormatter.class;
					}

					public boolean isInstanceName() {
						return AnnotationTransformer.getNamedProperty(an, "isInstanceName", Boolean.FALSE).booleanValue();
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
						return SnapShotRatio.class;
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


    private static class RatiosWorker implements Worker<SnapShotRatios> {
		public SnapShotRatios transform(final Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_RATIOS)) {
	    		return new SnapShotRatios() {
	    			private final AnnotationTransformer transformer = new AnnotationTransformer();

					public Class<? extends Annotation> annotationType() {
						return SnapShotRatios.class;
					}

					public SnapShotRatio[] value() {
					
						List<SnapShotRatio> ratios = new ArrayList<SnapShotRatio>();
						Annotation[] vArray = getNamedProperty(an, "values", new Annotation[]{});
						
						for (Annotation v : vArray) {
							ratios.add(transformer.transform(SnapShotRatio.class, v));
						}
						
						return ratios.toArray(new SnapShotRatio[]{});
					}

				};
	    	}
	    	return null;
		}
    }

    private static class InstanceDefinitionWorker implements Worker<SnapShotInstanceDefinition> {
		public SnapShotInstanceDefinition transform(Annotation an) {
	    	if (an.annotationType().getName().equals(API_SNAP_SHOT_INSTANCE_DEFINITION)) {
	    		return new SnapShotInstanceDefinition() {

					public Class<? extends Annotation> annotationType() {
						return SnapShotInstanceDefinition.class;
					}
				};
	    	}
	    	return null;
		}
    }
    
    
} 

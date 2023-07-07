/*
 *	Copyright 2008 Follett Software Company 
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
 * 	1391 Corparate Drive
 * 	McHenry, IL 60050
 * 
*/

package org.perfmon4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.reactive.ReactiveContextManager;

public class ThreadTraceConfig {
    private int maxDepth = 0;
    private int minDurationToCapture = 0;
    private int randomSamplingFactor = 0;
    private Trigger[] triggers = null;
    private final Set<AppenderID> appenders = new HashSet<AppenderID>();
    private final Random random = new Random();
    
    public ThreadTraceConfig() {
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMinDurationToCapture() {
        return minDurationToCapture;
    }

    public void setMinDurationToCapture(int minDurationToCapture) {
        this.minDurationToCapture = minDurationToCapture;
    }
    
    public void setTriggers(Trigger[] triggers) {
    	this.triggers = triggers;
    }
    
    public void addAppender(AppenderID appenderID) {
        appenders.add(appenderID);
    }
    
    public void removeAppender(AppenderID appenderID) {
        appenders.remove(appenderID);
    }
    
    public AppenderID[] getAppenders() {
        return appenders.toArray(new AppenderID[]{});
    }

    public void setRandomSamplingFactor(int randomSamplingFactor) {
        this.randomSamplingFactor = randomSamplingFactor;
    }
    
    public int getRandomSamplingFactor() {
        return randomSamplingFactor;
    }
    
    public Trigger[] getTriggers() {
    	return triggers;
    }
    
    public boolean shouldTrace() {
    	boolean result = true;
    	
    	if (triggers != null) {
    		TriggerValidator reactiveContextValidators[] = null;
    		result = false;
    		for (int i = 0; i < triggers.length && !result; i++) {
    			Trigger t = triggers[i]; 
    			if (t instanceof ThreadNameTrigger) {
    				result = ((ThreadNameTrigger)t).matchesCurrentThread();
    			} else {
    				Iterator<TriggerValidator> itr = validatorsOnThread.get().iterator();
    				while (itr.hasNext() && !result) {
    					result = itr.next().isValid(t);
    				}
    				if (!result && ReactiveContextManager.areReactiveContextsActiveInJVM()) {
    					if (reactiveContextValidators == null) {
    						reactiveContextValidators = ReactiveContextManager
    								.getContextManagerForThread().getActiveContextTriggerValidatorsOnThread();
    					}
    					for (TriggerValidator validator : reactiveContextValidators) {
    						result = validator.isValid(t);
    						if (result) {
    							break;
    						}
    					}
    				}
    			}
			}
    	}
    	return result && shouldTraceBasedOnRandomSamplingFactor();
    }
    
    public boolean shouldTraceBasedOnRandomSamplingFactor() {
        boolean result = true;
        
        if (randomSamplingFactor > 1) {
            result = random.nextInt(randomSamplingFactor-1) == 0;
        }
        return result;
    }
	
	public static enum TriggerType {
		HTTP_REQUEST_PARAM("HTTP"),
		HTTP_SESSION_PARAM("HTTP_SESSION"),
		HTTP_COOKIE_PARAM("HTTP_COOKIE"),
		THREAD_NAME("THREAD_NAME"),
		THREAD_PROPERTY("THREAD_PROPERTY");
		
		final private String prefix;
		
		private TriggerType(String prefix) {
			this.prefix = prefix;
		}
		
		public String getPrefix() {
			return prefix;
		}
	}
	
	public static class Trigger {
		private final TriggerType type;
		private final String triggerString;
		
		protected Trigger(TriggerType type, String suffix) {
			this.type = type;
			triggerString = type.getPrefix() + ":" + suffix;
		}
		
		public TriggerType getType() {
			return type;
		}
		
		public String getTriggerString() {
			return triggerString;
		}
	}
	
	protected abstract static class PropertyTrigger extends Trigger {
		private final String name;
		private final String value;
		
		public PropertyTrigger(TriggerType type, String name, String value) {
			super(type, name + "=" + value);
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}

	public static class ThreadPropertytTrigger extends PropertyTrigger {
		public ThreadPropertytTrigger(String name, String value) {
			super(TriggerType.THREAD_PROPERTY, name, value);
		}
	}
	
	
	public static class HTTPRequestTrigger extends PropertyTrigger {
		public HTTPRequestTrigger(String name, String value) {
			super(TriggerType.HTTP_REQUEST_PARAM, name, value);
		}
	}
	
	public static class HTTPSessionTrigger extends PropertyTrigger {
		public HTTPSessionTrigger(String name, String value) {
			super(TriggerType.HTTP_SESSION_PARAM, name, value);
		}
	}

	public static class HTTPCookieTrigger extends PropertyTrigger {
		public HTTPCookieTrigger(String name, String value) {
			super(TriggerType.HTTP_COOKIE_PARAM, name, value);
		}
	}
	
	public static class ThreadNameTrigger extends Trigger {
		final private String threadName;
		
		public ThreadNameTrigger(String threadName) {
			super(TriggerType.THREAD_NAME, threadName);
			this.threadName = threadName;
		}
		
		public String getThreadName() {
			return threadName;
		}
		
		private boolean matchesCurrentThread() {
			return Thread.currentThread().getName().equals(threadName);
		}
	}
	
	public static interface TriggerValidator {
		public boolean isValid(Trigger trigger);
	}
	
	public static class ThreadPropertyValidator implements TriggerValidator {
		private final String value;
		
		public ThreadPropertyValidator(String propertyName, String value) {
			this.value = TriggerType.THREAD_PROPERTY + ":" + propertyName +  "=" + value;
		}
		
		public boolean isValid(Trigger trigger) {
			return value.equals(trigger.getTriggerString());
		}
	}
	
	private final static ThreadLocal<Stack<TriggerValidator>> validatorsOnThread = new ThreadLocal<Stack<TriggerValidator>>() {
		public Stack<TriggerValidator> initialValue() {
			return new Stack<TriggerValidator>();
		}
	};
	
	public static void pushThreadProperty(String propertyName, String value) {
		pushValidator(new ThreadPropertyValidator(propertyName, value));
	}

	public static void pushThreadProperty(String propertyName, String value, String reactiveContextID) {
		pushValidator(new ThreadPropertyValidator(propertyName, value), reactiveContextID);
	}

	public static void popThreadProperty() {
		popValidator();
	}

	public static void popThreadProperty(String reactiveContextID) {
		popValidator(reactiveContextID);
	}
	
	public static void pushValidator(TriggerValidator validator) {
		pushValidator(validator, null);
	}
	
	public static void pushValidator(TriggerValidator validator, String reactiveContextID) {
		if (reactiveContextID != null) {	
			ReactiveContextManager.getContextManagerForThread().pushValidator(validator, reactiveContextID);
		} else {
			validatorsOnThread.get().push(validator);
		}
	}
	
	public static void popValidator() {
		popValidator(null);
	}
	
	public static void popValidator(String reactiveContextID) {
		if (reactiveContextID != null) {	
			ReactiveContextManager.getContextManagerForThread().popValidator(reactiveContextID);
		} else {
			validatorsOnThread.get().pop();
		}
	}
	
	public static TriggerValidator[] getValidatorsOnThread() {
		TriggerValidator validatorsFromContext[] = null;
		
		if (ReactiveContextManager.areReactiveContextsActiveInJVM()) {
			validatorsFromContext = ReactiveContextManager.getContextManagerForThread().getActiveContextTriggerValidatorsOnThread();
		}

		Stack<TriggerValidator> v = validatorsOnThread.get();
		if (validatorsFromContext != null && validatorsFromContext.length > 0) {
			if (!v.isEmpty()) {
				// We have both validators associated with the thread, and with one
				// or more active reactiveContexts associated with the thread.  Must
				// merge list and return both.
				Set<TriggerValidator> combindedValidators = new HashSet<ThreadTraceConfig.TriggerValidator>();
				combindedValidators.addAll(v);
				combindedValidators.addAll(Arrays.asList(validatorsFromContext));
			
				return combindedValidators.toArray(new TriggerValidator[] {});
			} else {
				return validatorsFromContext;
			}
		} else {
			// No validators on any context associated with thread.  Simply need to return 
			// validators directly on thread (if any).
			return v.toArray(new TriggerValidator[] {});
		}
	}
}

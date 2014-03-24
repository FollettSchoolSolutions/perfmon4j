package org.perfmon4j.config.xml;

public class TriggerConfigElement {
	private final Type type;
	private final String name;
	private final String value;
	
	public TriggerConfigElement(Type type, String name) {
		this(type, name, null);
	}
	
	public TriggerConfigElement(Type type, String name, String value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}
	
	public Type getType() {
		return type;
	}
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}

	public static enum Type {
	    REQUEST_TRIGGER,
	    SESSION_TRIGGER,
	    COOKIE_TRIGGER,
	    THREAD_TRIGGER,
	    THREAD_PROPERTY_TRIGGER
	}
}

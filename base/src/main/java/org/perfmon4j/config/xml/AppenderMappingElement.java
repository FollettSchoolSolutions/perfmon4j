package org.perfmon4j.config.xml;

public class AppenderMappingElement {
	private String name = null;
	private String pattern = null;

	AppenderMappingElement() {
	}
	
	AppenderMappingElement(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPattern() {
		return pattern;
	}
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}

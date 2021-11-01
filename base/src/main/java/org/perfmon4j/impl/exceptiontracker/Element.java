package org.perfmon4j.impl.exceptiontracker;

import java.util.Objects;

public class Element implements Comparable<Element> {
	private final String fieldName;
	
	protected Element(String fieldName) {
		this.fieldName = fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public int compareTo(Element other) {
		return this.fieldName.compareTo(other.fieldName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fieldName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Element other = (Element) obj;
		return Objects.equals(fieldName, other.fieldName);
	}
}

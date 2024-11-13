package org.perfmon4j.util.mbean;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

public class TestExample implements TestExampleMBean {
	private static final AtomicLong nextValue = new AtomicLong(0);
	
	private final String type;

	public TestExample() {
		this("defaultType");
	}
	
	public TestExample(String type) {
		this.type = type;
	}

	@Override
	public long getNextValue() {
		return nextValue.getAndIncrement();
	}

	@Override
	public long getNativeLong() {
		return 1;
	}

	@Override
	public Long getLong() {
		return Long.valueOf(2);
	}

	@Override
	public int getNativeInteger() {
		return 3;
	}

	@Override
	public Integer getInteger() {
		return Integer.valueOf(4);
	}

	@Override
	public short getNativeShort() {
		return 5;
	}

	@Override
	public Short getShort() {
		return Short.valueOf((short)6);
	}

	@Override
	public double getNativeDouble() {
		return 7.7d;
	}

	@Override
	public Double getDouble() {
		return Double.valueOf(8.8d);
	}

	@Override
	public float getNativeFloat() {
		return 9.9f;
	}

	@Override
	public Float getFloat() {
		return Float.valueOf(10.10f);
	}

	@Override
	public boolean getNativeBoolean() {
		return true;
	}

	@Override
	public Boolean getBoolean() {
		return Boolean.TRUE;
	}

	@Override
	public char getNativeCharacter() {
		return 'a';
	}

	@Override
	public Character getCharacter() {
		return Character.valueOf('b');
	}

	@Override
	public byte getNativeByte() {
		return 11;
	}

	@Override
	public Byte getByte() {
		return Byte.valueOf((byte)12);
	}
	
	@Override
	public String getString() {
		return "13";
	}
	
	@Override
	public Object getObject() {
		return new StringBuilder("14");
	}
	
	@Override 
	public String getType() {
		return type;
	}
	
	@Override
	public CompositeData getCompositeData() throws OpenDataException {
		String[] itemNames = {"completed", "failed", "active", "status", "bigDecimal"}; 
		OpenType[] itemTypes = {SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.STRING, SimpleType.BIGDECIMAL};
		Object[] itemValues = {Long.valueOf(5), Long.valueOf(1), Long.valueOf(3), "Active", BigDecimal.valueOf(1000)};
		CompositeType type = new CompositeType("Request Count", "asdf", itemNames, itemNames, itemTypes);
		return new CompositeDataSupport(type, itemNames, itemValues);
	}
}

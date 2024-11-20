package org.perfmon4j.util.mbean;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

public interface TestExampleMBean {
	public long getNextValue();
	
	public short getNativeShort();
	public Short getShort();
	
	public int getNativeInteger();
	public Integer getInteger();
	
	public long getNativeLong();
	public Long getLong();
	
	public float getNativeFloat();
	public Float getFloat();
	
	public double getNativeDouble();
	public Double getDouble();

	public boolean getNativeBoolean();
	public Boolean getBoolean();
	
	public char getNativeCharacter();
	public Character getCharacter();
	
	public byte getNativeByte();
	public Byte getByte();
	
	public String getString();
	public Object getObject();
	
	public String getType();
	
	public CompositeData getCompositeData() throws OpenDataException;
}

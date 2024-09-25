package org.perfmon4j.util.mbean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.mbean.MBeanDatum.AttributeType;
import org.perfmon4j.util.mbean.MBeanDatum.OutputType;

class MBeanAttributeExtractor {
	private final MBeanServerFinder mBeanServerFinder;
	private final ObjectName objectName;
	private final MBeanQuery query;
	private final DatumDefinition datumDefinition[];

	private static final Logger logger = LoggerFactory.initLogger(MBeanAttributeExtractor.class);

	MBeanAttributeExtractor(MBeanServerFinder mBeanServerFinder, ObjectName objectName,
			MBeanQuery query) throws MBeanQueryException {
		super();
		this.mBeanServerFinder = mBeanServerFinder;
		this.objectName = objectName;
		this.query = query;
		try {
			datumDefinition = buildDataDefinitionArray(mBeanServerFinder.getMBeanServer().getMBeanInfo(objectName), query);
		} catch (IntrospectionException | InstanceNotFoundException | ReflectionException e) {
			throw new MBeanQueryException("Unabled to retrieve data from mbean: " + objectName.getCanonicalName(), e);
		}
	}
	
    private static final Function<String, String> toggleCaseOfFirstLetter = s -> {
        if (s == null || s.isEmpty()) {
            return s;
        }
        
        // Get the first character
        char firstChar = s.charAt(0);
        
        // Change the case of the first character
        char newFirstChar = Character.isUpperCase(firstChar) 
            ? Character.toLowerCase(firstChar) 
            : Character.toUpperCase(firstChar);
        
        // Combine the new first character with the rest of the string
        return newFirstChar + s.substring(1);
    };
	
	static DatumDefinition[] buildDataDefinitionArray(MBeanInfo mBeanInfo, MBeanQuery query) {
		Set<DatumDefinition> result = new HashSet<>();
		Map<String, MBeanAttributeInfo> attributes = new HashMap<String, MBeanAttributeInfo>();
		Set<String> gaugeNames = new HashSet<String>(Arrays.asList(query.getGauges()));
		Set<String> counterNames = new HashSet<String>(Arrays.asList(query.getCounters()));
		
		for (MBeanAttributeInfo aInfo : mBeanInfo.getAttributes()) {
			attributes.put(aInfo.getName(), aInfo);
		}
		
		for (int i = 0; i < 2; i++) {
			Function<String, String> transform;
			switch (i) {
			case 1:
				transform = toggleCaseOfFirstLetter;
				break;
			default:
				transform = s -> s; // do nothing.
				break;
			}
			
			// First get Counters
			for (String counter : counterNames.toArray(new String[] {})) {
				MBeanAttributeInfo info = attributes.get(transform.apply(counter));
				if (info != null) {
					OutputType outputType = AttributeType.getAttributeType(info).getValidOutputType(OutputType.COUNTER);
					counterNames.remove(counter);
					result.add(new DatumDefinition(info, outputType));
					attributes.remove(info.getName());
				}
			}
			
			// Then try gauges
			for (String gauge : gaugeNames.toArray(new String[] {})) {
				MBeanAttributeInfo info = attributes.get(transform.apply(gauge));
				if (info != null) {
					OutputType outputType = AttributeType.getAttributeType(info).getValidOutputType(OutputType.GAUGE);
					gaugeNames.remove(gauge);
					attributes.remove(info.getName());
					result.add(new DatumDefinition(info, outputType));
				}
			}
		}
		return result.toArray(new DatumDefinition[] {});
	}
	
	
	static final class MBeanDatumImpl<T> implements MBeanDatum<T> {
		private final String name;
		private final OutputType type;
		private final AttributeType attributeType;
		private final T value;
		
		MBeanDatumImpl(DatumDefinition dd, T value) {
			this.name = dd.getName();
			this.type = dd.getOutputType();
			this.attributeType = dd.getAttributeType();
			this.value = value;
		}
		
		@Override
		public AttributeType getAttributeType() {
			return attributeType;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public OutputType getOutputType() {
			return type;
		}

		@Override
		public T getValue() {
			return value;
		}
	}
	
	static final class DatumDefinition {
		private final MBeanDatum.OutputType type;
		private final MBeanDatum.AttributeType attributeType;
		private final String name;
		
		public DatumDefinition(MBeanAttributeInfo attributeInfo, OutputType type) {
			this.name = attributeInfo.getName();
			this.type = type;
			this.attributeType = MBeanDatum.AttributeType.getAttributeType(attributeInfo);
		}

		public MBeanDatum.OutputType getOutputType() {
			return type;
		}
		
		public MBeanDatum.AttributeType getAttributeType() {
			return attributeType;
		}

		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DatumDefinition other = (DatumDefinition) obj;
			return Objects.equals(name, other.name) && type == other.type;
		}
	}
	
	MBeanDatum<?>[] extractAttributes() throws MBeanQueryException {
		List<MBeanDatum<?>> result = new ArrayList<>();
		for (DatumDefinition d : datumDefinition) {
			try {
				Object value = mBeanServerFinder.getMBeanServer().getAttribute(objectName, d.getName());
				
				switch (d.getAttributeType()) {
					case NATIVE_SHORT:
					case SHORT:
						result.add(new MBeanDatumImpl<>(d, (Short)value));
						break;
						
					case NATIVE_INTEGER:
					case INTEGER:
						result.add(new MBeanDatumImpl<>(d, (Integer)value));
						break;
						
					case NATIVE_LONG:
					case LONG:
						result.add(new MBeanDatumImpl<>(d, (Long)value));
						break;

					case NATIVE_FLOAT:
					case FLOAT:
						result.add(new MBeanDatumImpl<>(d, (Float)value));
						break;

					case NATIVE_DOUBLE:
					case DOUBLE:
						result.add(new MBeanDatumImpl<>(d, (Double)value));
						break;
	
					case NATIVE_BOOLEAN:
					case BOOLEAN:
						result.add(new MBeanDatumImpl<>(d, (Boolean)value));
						break;

					case NATIVE_CHARACTER:
					case CHARACTER:
						result.add(new MBeanDatumImpl<>(d, (Character)value));
						break;
						
					case NATIVE_BYTE:
					case BYTE:
						result.add(new MBeanDatumImpl<>(d, (Byte)value));
						break;
						
					case STRING:
					default:
						if (value == null || value instanceof String) {
							result.add(new MBeanDatumImpl<>(d, (String)value));
						} else {
							result.add(new MBeanDatumImpl<>(d, value.toString()));
						}
				}
			} catch (InstanceNotFoundException | AttributeNotFoundException | ReflectionException
					| MBeanException e) {
				logger.logWarn("Unabled to retrieve attribute", e);
			}
		}
		return result.toArray(new MBeanDatum<?>[]{});
	}
	
	DatumDefinition[] getDatumDefinition() {
		return Arrays.copyOf(datumDefinition, datumDefinition.length);
	}
}

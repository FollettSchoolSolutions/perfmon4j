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
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.perfmon4j.PerfMonObservableDatum;
import org.perfmon4j.instrument.snapshot.Delta;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;
import org.perfmon4j.util.mbean.MBeanDatum.AttributeType;
import org.perfmon4j.util.mbean.MBeanDatum.OutputType;

public class MBeanAttributeExtractor {
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
		this.datumDefinition = buildDataDefinitionArray(mBeanServerFinder, objectName, query);
	}
	
    private static final Function<String, String> toggleCaseOfFirstLetter = s -> {
    	return MiscHelper.toggleCaseOfFirstLetter(s);
    };
	
    
    /**
     * Removes attributes Strings that match the "complex object" pattern (i.e. "Usage.initial")
     * from the passed in set.  These attributes are returned in a new set.
     * @param set
     * @return
     */
    private static Set<String> extractComplexObjectAttributes(Set<String> set) {
    	Set<String> result = new HashSet<String>();
    	
    	for (String attribute : set.toArray(new String[] {})) {
    		if (CompositeDataManager.isCompositeAttributeName(attribute)) {
    			set.remove(attribute);
    			result.add(attribute);
    		}
    	}
    	return result;
    }
    
	static DatumDefinition[] buildDataDefinitionArray(MBeanServerFinder serverFinder, ObjectName objectName, MBeanQuery query) throws MBeanQueryException {
		CompositeDataManager dataManager = new CompositeDataManager(serverFinder, objectName);
		
		Map<String, MBeanAttributeInfo> attributes = new HashMap<String, MBeanAttributeInfo>();
		Set<String> gaugeNames = new HashSet<String>(Arrays.asList(query.getGauges()));
		Set<String> counterNames = new HashSet<String>(Arrays.asList(query.getCounters()));

		// First get any DatumDefinitions associated with any Composite Data attributes (i.e. "Usage.used")
		Set<DatumDefinition> result = dataManager.buildCompositeDatumDefinitionArray(query);
		
		MBeanInfo mBeanInfo = serverFinder.getMBeanInfo(objectName);
		
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
				if (!CompositeDataManager.isCompositeAttributeName(counter)) {
					MBeanAttributeInfo info = attributes.get(transform.apply(counter));
					if (info != null) {
						OutputType outputType = AttributeType.getAttributeType(info).getValidOutputType(OutputType.COUNTER);
						counterNames.remove(counter);
						result.add(new DatumDefinition(info, outputType));
						attributes.remove(info.getName());
					}
				}	
			}
			
			// Then try gauges
			for (String gauge : gaugeNames.toArray(new String[] {})) {
				if (!CompositeDataManager.isCompositeAttributeName(gauge)) {
					MBeanAttributeInfo info = attributes.get(transform.apply(gauge));
					if (info != null) {
						OutputType outputType = AttributeType.getAttributeType(info).getValidOutputType(OutputType.GAUGE);
						gaugeNames.remove(gauge);
						attributes.remove(info.getName());
						result.add(new DatumDefinition(info, outputType));
					}
				}
			}
		}
		return result.toArray(new DatumDefinition[] {});
	}
	
	
	public static final class MBeanDatumImpl<T> implements MBeanDatum<T> {
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

		@Override
		public PerfMonObservableDatum<?> toPerfMonObservableDatum() {
			PerfMonObservableDatum<?> result = null;
			
			switch (getAttributeType()) {
				case NATIVE_SHORT:
				case SHORT:
					result = PerfMonObservableDatum.newDatum(getName(), (Short)getValue());
					break;
				
				case NATIVE_INTEGER:
				case INTEGER:
					result = PerfMonObservableDatum.newDatum(getName(), (Integer)getValue());
					break;
					
				case NATIVE_LONG:
				case LONG:
					result = PerfMonObservableDatum.newDatum(getName(), (Long)getValue());
					break;
	
				case NATIVE_FLOAT:
				case FLOAT:
					result = PerfMonObservableDatum.newDatum(getName(), (Float)getValue());
					break;
	
				case NATIVE_DOUBLE:
				case DOUBLE:
					result = PerfMonObservableDatum.newDatum(getName(), (Double)getValue());
					break;
	
				case NATIVE_BOOLEAN:
				case BOOLEAN:
					result = PerfMonObservableDatum.newDatum(getName(), (Boolean)getValue());
					break;
	
				case NATIVE_CHARACTER:
				case CHARACTER:
					result = PerfMonObservableDatum.newDatum(getName(), (Character)getValue());
					break;
					
				case NATIVE_BYTE:
				case BYTE:
					result = PerfMonObservableDatum.newDatum(getName(), (Byte)getValue());
					break;
					
				case STRING:
				default:
					if (value == null || value instanceof String) {
						result = PerfMonObservableDatum.newDatum(getName(), (String)getValue());
					} else {
						result = PerfMonObservableDatum.newDatum(getName(), (String)getValue().toString());
					}
			}
			return result;
		}

		@Override
		public PerfMonObservableDatum<Delta> toPerfMonObservableDatum(MBeanDatum<?> initialDatum, long durationMillis) {
			// Make sure we have a before and after value and they are the right type.
			Delta deltaValue = null;
			if (initialDatum != null) {
				Object before = initialDatum.getValue();
				Object after = getValue();
	
				if (before != null && (before instanceof Number) 
					&& after != null && (after instanceof Number)) {
					deltaValue = new Delta(((Number)before).longValue(), ((Number)after).longValue(), durationMillis);
				}
			}
			return PerfMonObservableDatum.newDatum(getName(), deltaValue);
		}
	}
	
	public static final class DatumDefinition {
		private final MBeanDatum.OutputType type;
		private final MBeanDatum.AttributeType attributeType;
		private final String name;
		private final String parentName;
		
		public DatumDefinition(MBeanAttributeInfo attributeInfo, OutputType type) {
			this.name = attributeInfo.getName();
			this.type = type;
			this.attributeType = MBeanDatum.AttributeType.getAttributeType(attributeInfo);
			this.parentName = null;
		}

		public DatumDefinition(String name, AttributeType attributeType, OutputType type) {
			this.name = name;
			this.type = type;
			this.attributeType = attributeType;
			
			String split[] = CompositeDataManager.splitAtFirstPeriod(name);
			if (split.length > 1) {
				this.parentName = split[0];
			} else {
				this.parentName = null;
			}
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
		
		public boolean isCompositeAttribute() {
			return parentName != null;
		}
		
		public String getParentName() {
			return parentName;
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

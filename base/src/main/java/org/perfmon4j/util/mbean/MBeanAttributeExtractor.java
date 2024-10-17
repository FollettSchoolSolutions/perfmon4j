package org.perfmon4j.util.mbean;

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
import org.perfmon4j.util.mbean.GaugeCounterArgumentParser.AttributeSpec;
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
		this.datumDefinition = buildDataDefinitionArray(mBeanServerFinder, objectName, query);
	}
	
    private static final Function<String, String> toggleCaseOfFirstLetter = s -> {
    	return MiscHelper.toggleCaseOfFirstLetter(s);
    };
	
	static DatumDefinition[] buildDataDefinitionArray(MBeanServerFinder serverFinder, ObjectName objectName, MBeanQuery query) throws MBeanQueryException {
		Map<String, MBeanAttributeInfo> attributes = new HashMap<String, MBeanAttributeInfo>();
		
		GaugeCounterArgumentParser parser = new GaugeCounterArgumentParser(query);
		Set<AttributeSpec> counters = parser.getCounters();
		Set<AttributeSpec> gauges = parser.getGauges();
		Set<AttributeSpec> ratioComponents = parser.getRatioComponents();
		Set<String> foundRatioComponents = new HashSet<String>();

		CompositeDataManager dataManager = new CompositeDataManager(serverFinder, objectName);
		
		// First get any DatumDefinitions associated with any Composite Data attributes (i.e. "Usage.used")
		Set<DatumDefinition> result = dataManager.buildCompositeDatumDefinitionArray(parser, foundRatioComponents);
		
		MBeanInfo mBeanInfo = serverFinder.getMBeanInfo(objectName);
		
		for (MBeanAttributeInfo aInfo : mBeanInfo.getAttributes()) {
			attributes.put(aInfo.getName(), aInfo);
		}
		
		// First walk through and get all the ratio components.
		// Ratio components can duplicate defined counters/gauges.
		for (AttributeSpec ratioComponent : ratioComponents) {
			if (!ratioComponent.isCompositeName()) {
				MBeanAttributeInfo info = attributes.get(ratioComponent.getName());
				if (info == null) {
					// Try again after toggling case of first letter.
					info = attributes.get(MiscHelper.toggleCaseOfFirstLetter(ratioComponent.getName()));
				}
				
				if (info != null) {
					if (AttributeType.getAttributeType(info).isSupportsRatioComponent()) {
						result.add(new DatumDefinition(info, OutputType.VOID, ratioComponent));
						foundRatioComponents.add(ratioComponent.getName());
					}
				}
			}	
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
			for (AttributeSpec counter : counters.toArray(new AttributeSpec[] {})) {
				if (!counter.isCompositeName()) {
					MBeanAttributeInfo info = attributes.get(transform.apply(counter.getName()));
					if (info != null) {
						OutputType outputType = AttributeType.getAttributeType(info).getValidOutputType(OutputType.COUNTER);
						counters.remove(counter);
						result.add(new DatumDefinition(info, outputType, counter));
						attributes.remove(info.getName());
					}
				}	
			}
			
			// Then try gauges
			for (AttributeSpec gauge : gauges.toArray(new AttributeSpec[] {})) {
				if (!gauge.isCompositeName()) {
					MBeanAttributeInfo info = attributes.get(transform.apply(gauge.getName()));
					if (info != null) {
						OutputType outputType = AttributeType.getAttributeType(info).getValidOutputType(OutputType.GAUGE);
						gauges.remove(gauge);
						attributes.remove(info.getName());
						result.add(new DatumDefinition(info, outputType, gauge));
					}
				}
			}
		}
		
		// Finally add the ratios.  Only add ratios where a data mapping is found for both
		// the numerator and denominator.
		for (SnapShotRatio ratio : query.getRatios()) {
			if (validateRatioComponents(ratio, foundRatioComponents)) {
				result.add(new DatumDefinition(ratio.getName(), AttributeType.DOUBLE, OutputType.RATIO));
			} else {
				logger.logWarn("Skipping ratio: " + ratio.getName()  + ". Numerator or denominator not found.");
			}
		}
		
		return result.toArray(new DatumDefinition[] {});
	}
	
	private static boolean validateRatioComponents(SnapShotRatio snapShotRatio, Set<String> ratioComponents) {
		return ratioComponents.contains(snapShotRatio.getNumerator()) 
			&& ratioComponents.contains(snapShotRatio.getDenominator());
	}
	
	public static final class MBeanDatumImpl<T> implements MBeanDatum<T> {
		private final String name;
		private final String displayName;
		private final OutputType type;
		private final AttributeType attributeType;
		private final T value;
		
		MBeanDatumImpl(DatumDefinition dd, T value) {
			this.name = dd.getName();
			this.displayName = dd.getDisplayName();
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
		public String getDisplayName() {
			return displayName;
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
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Short)getValue());
					break;
				
				case NATIVE_INTEGER:
				case INTEGER:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Integer)getValue());
					break;
					
				case NATIVE_LONG:
				case LONG:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Long)getValue());
					break;
	
				case NATIVE_FLOAT:
				case FLOAT:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Float)getValue());
					break;
	
				case NATIVE_DOUBLE:
				case DOUBLE:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Double)getValue());
					break;
	
				case NATIVE_BOOLEAN:
				case BOOLEAN:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Boolean)getValue());
					break;
	
				case NATIVE_CHARACTER:
				case CHARACTER:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Character)getValue());
					break;
					
				case NATIVE_BYTE:
				case BYTE:
					result = PerfMonObservableDatum.newDatum(getDisplayName(), (Byte)getValue());
					break;
					
				case STRING:
				default:
					if (value == null || value instanceof String) {
						result = PerfMonObservableDatum.newDatum(getDisplayName(), (String)getValue());
					} else {
						result = PerfMonObservableDatum.newDatum(getDisplayName(), (String)getValue().toString());
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
			return PerfMonObservableDatum.newDatum(getDisplayName(), deltaValue);
		}
	}
	
	static final class DatumDefinition {
		private final MBeanDatum.OutputType type;
		private final MBeanDatum.AttributeType attributeType;
		private final String displayName;
		private final String name;
		private final String parentName;
		
		DatumDefinition(MBeanAttributeInfo attributeInfo, OutputType type) {
			this(attributeInfo, type, null);
		}
		
		DatumDefinition(MBeanAttributeInfo attributeInfo, OutputType type, AttributeSpec spec) {
			this.name = attributeInfo.getName();
			this.displayName = spec != null ? spec.getPreferredDisplayName(name) : name;
			this.type = type;
			this.attributeType = MBeanDatum.AttributeType.getAttributeType(attributeInfo);
			this.parentName = null;
		}
		
		DatumDefinition(String name, AttributeType attributeType, OutputType type) {
			this(name, attributeType, type, null);
		}

		DatumDefinition(String name, AttributeType attributeType, OutputType type, AttributeSpec spec) {
			this.name = name;
			this.displayName = spec != null ? spec.getPreferredDisplayName(name) : name;
			this.type = type;
			this.attributeType = attributeType;
			
			String split[] = CompositeDataManager.splitAtFirstPeriod(name);
			if (split.length > 1) {
				this.parentName = split[0];
			} else {
				this.parentName = null;
			}
		}
		
		MBeanDatum.OutputType getOutputType() {
			return type;
		}
		
		MBeanDatum.AttributeType getAttributeType() {
			return attributeType;
		}

		String getName() {
			return name;
		}
		
		boolean isCompositeAttribute() {
			return parentName != null;
		}
		
		String getParentName() {
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

		public String getDisplayName() {
			return displayName;
		}

		@Override
		public String toString() {
			return "DatumDefinition [type=" + type + ", attributeType=" + attributeType + ", displayName=" + displayName
					+ ", name=" + name + ", parentName=" + parentName + "]";
		}
	}
	
	MBeanDatum<?>[] extractAttributes() throws MBeanQueryException {
		CompositeDataManager dataManager = new CompositeDataManager(mBeanServerFinder, objectName);

		List<MBeanDatum<?>> result = dataManager.extractCompositeDataAttributes(datumDefinition);
		for (DatumDefinition d : datumDefinition) {
			if (!d.isCompositeAttribute()) {
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
					logger.logWarn("Unabled to retrieve attribute: " + d);
				}
			}
		}
		return result.toArray(new MBeanDatum<?>[]{});
	}

	
	DatumDefinition[] getDatumDefinition() {
		return Arrays.copyOf(datumDefinition, datumDefinition.length);
	}
}

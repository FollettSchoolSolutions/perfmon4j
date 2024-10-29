package org.perfmon4j.util.mbean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
		
		// key=<attribute name from SnapShotDefinition>, value=<attributeName from JMXObject> - there many minor differences in case. 
		Map<String, String> foundRatioComponents = new HashMap<String, String>();

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
					AttributeType attributeType = AttributeType.getAttributeType(info);
					if (attributeType.isSupportsRatioComponent()) {
						result.add(new DatumDefinition(info.getName(), attributeType, OutputType.VOID));
						foundRatioComponents.put(ratioComponent.getName(), info.getName());
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
			SnapShotRatio normalizedRatio = normalizeRatio(ratio, foundRatioComponents);
			if (normalizedRatio != null) {
				result.add(new RatioDatumDefinition(normalizedRatio));
			} else {
				logger.logWarn("Skipping ratio: " + ratio.getName()  + ". Numerator and/or denominator not found.");
			}
		}
		return result.toArray(new DatumDefinition[] {});
	}
	
	
	/** This method will make a copy of the SnapShotRatio that ensures the numerator and
	 * denominator matches the JMXAttribute name's case.
	 * 
	 * Will return null if the required numerator and denominator are not found
	 * @param snapShotRatio
	 * @param ratioComponents
	 * @return
	 */
	private static SnapShotRatio normalizeRatio(SnapShotRatio snapShotRatio, Map<String,String> ratioComponents) {
		SnapShotRatio result = null;
		
		String numerator = ratioComponents.get(snapShotRatio.getNumerator());
		String denominator = ratioComponents.get(snapShotRatio.getDenominator());

		if (numerator != null && denominator != null) {
			result = MBeanQueryBuilder.normalize(snapShotRatio, numerator, denominator);
		}
	
		return result;
	}
	
	public static final class MBeanDatumImpl<T> implements MBeanDatum<T> {
		private final DatumDefinition datumDefinition;
		private final T value;
		
		MBeanDatumImpl(DatumDefinition datumDefinition, T value) {
			this.datumDefinition = datumDefinition;
			this.value = value;
		}

		@Override
		public DatumDefinition getDatumDefinition() {
			return datumDefinition;
		}		
		
		@Override
		public AttributeType getAttributeType() {
			return datumDefinition.getAttributeType();
		}
		
		@Override
		public String getName() {
			return datumDefinition.getName();
		}

		@Override
		public String getDisplayName() {
			return datumDefinition.getDisplayName();
		}
		
		@Override
		public OutputType getOutputType() {
			return datumDefinition.getOutputType();
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

		@Override
		public String toString() {
			return "MBeanDatumImpl [datumDefinition=" + datumDefinition + ", value=" + value + "]";
		}
	}
	
	static class DatumDefinition {
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
			return this.getClass().getSimpleName() + " [type=" + type + ", attributeType=" + attributeType + ", displayName=" + displayName
					+ ", name=" + name + ", parentName=" + parentName + "]";
		}
	}
	
	static final class RatioDatumDefinition extends DatumDefinition {
		private final SnapShotRatio snapShotRatio;
		
		RatioDatumDefinition(SnapShotRatio snapShotRatio) {
			super(snapShotRatio.getName(), AttributeType.DOUBLE, OutputType.RATIO);
			this.snapShotRatio = snapShotRatio;
		}

		SnapShotRatio getSnapShotRatio() {
			return snapShotRatio;
		}
	}
	
	MBeanDatum<?>[] extractAttributes() throws MBeanQueryException {
		CompositeDataManager dataManager = new CompositeDataManager(mBeanServerFinder, objectName);

		Map<DatumDefinition, MBeanDatum<?>> allData = dataManager.extractCompositeDataAttributes(datumDefinition);
		for (DatumDefinition d : datumDefinition) {
			if (!d.isCompositeAttribute() && !d.getOutputType().equals(OutputType.RATIO)) {
				try {
					Object value = mBeanServerFinder.getMBeanServer().getAttribute(objectName, d.getName());
					
					switch (d.getAttributeType()) {
						case NATIVE_SHORT:
						case SHORT:
							allData.put(d, new MBeanDatumImpl<>(d, (Short)value));
							break;
							
						case NATIVE_INTEGER:
						case INTEGER:
							allData.put(d, new MBeanDatumImpl<>(d, (Integer)value));
							break;
							
						case NATIVE_LONG:
						case LONG:
							allData.put(d, new MBeanDatumImpl<>(d, (Long)value));
							break;
	
						case NATIVE_FLOAT:
						case FLOAT:
							allData.put(d, new MBeanDatumImpl<>(d, (Float)value));
							break;
	
						case NATIVE_DOUBLE:
						case DOUBLE:
							allData.put(d, new MBeanDatumImpl<>(d, (Double)value));
							break;
		
						case NATIVE_BOOLEAN:
						case BOOLEAN:
							allData.put(d, new MBeanDatumImpl<>(d, (Boolean)value));
							break;
	
						case NATIVE_CHARACTER:
						case CHARACTER:
							allData.put(d, new MBeanDatumImpl<>(d, (Character)value));
							break;
							
						case NATIVE_BYTE:
						case BYTE:
							allData.put(d, new MBeanDatumImpl<>(d, (Byte)value));
							break;
							
						case STRING:
						default:
							if (value == null || value instanceof String) {
								allData.put(d, new MBeanDatumImpl<>(d, (String)value));
							} else {
								allData.put(d, new MBeanDatumImpl<>(d, value.toString()));
							}
					}
				} catch (InstanceNotFoundException | AttributeNotFoundException | ReflectionException
						| MBeanException e) {
					logger.logWarn("Unabled to retrieve attribute: " + d);
				}
			}
		}
		
		// Now process all ratio definitions
		for (DatumDefinition d : datumDefinition) {
			if (d.getOutputType().equals(OutputType.RATIO)) {
				SnapShotRatio ratio = ((RatioDatumDefinition)d).getSnapShotRatio();
				
				allData.put(d, resolveRatio(d,
						findElementForRatioCalculation(allData, ratio.getNumerator()),
						findElementForRatioCalculation(allData, ratio.getDenominator()),
						ratio.isFormatAsPercent()));
			}
		}

		// Finally remove any data elements that are not
		// intended for output to an appender and add to the return 
		// list.
		List<MBeanDatum<?>> result = new ArrayList<MBeanDatum<?>>();

		for (MBeanDatum<?> datum : allData.values()) {
			if (datum.getOutputType().isOutputToAppender()) {
				result.add(datum);
			}
		}
		
		return result.toArray(new MBeanDatum<?>[]{});		
		
	}
	
	
	/**
	 * A OutputType.VOID indicate a data element that is used for calculation.  For
	 * example a numerator or denominator or a Ratio.
	 * 
	 * @param allData
	 * @param name
	 * @return
	 */
	MBeanDatum<?> findElementForRatioCalculation(Map<DatumDefinition, MBeanDatum<?>> allData, String name) {
		for (MBeanDatum<?> d : allData.values()) {
			if (d.getName().equals(name) && d.getOutputType().equals(OutputType.VOID)) {
				return d;
			}
		}
		return null;
	}
	
	MBeanDatum<Double> resolveRatio(DatumDefinition def, MBeanDatum<?> numerator, MBeanDatum<?> denominator, boolean formatAsPercent) {
		Double ratio = null;
		
		if (numerator != null && denominator != null) {
			Number n = (Number)numerator.getValue();
			Number d = (Number)denominator.getValue();
			
			if (n != null && d != null) {
				ratio = Double.valueOf(MiscHelper.safeDivide(n.longValue(), d.longValue()));
				if (formatAsPercent) {
					ratio *= 100;
				}
			} 
		}
		return new MBeanDatumImpl<>(def, ratio);
	}
	

	
	DatumDefinition[] getDatumDefinition() {
		return Arrays.copyOf(datumDefinition, datumDefinition.length);
	}
}

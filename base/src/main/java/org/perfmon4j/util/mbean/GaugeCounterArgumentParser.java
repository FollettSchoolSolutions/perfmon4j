package org.perfmon4j.util.mbean;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * When defining a guage or counter for a mBeanSnapShotMonitor you can
 * pass additional attributes in parameters after the attribute name.
 * 
 * Currently the only parameter supported is displayName, but more could be
 * added later.
 * 
 * example:
 *    <mBeanSnapshotMonitor name='JVMThreading' 
 * 		jmxName='java.lang:type=Threading'
 *       counters='TotalStartedThreadCount(displayName="threadsStarted")'
 *       gauges='ThreadCount(displayName="currentThreadCount"),DaemonThreadCount(displayName="currentDaemonCount")'
 *       ratios='memInUsePercent=bytesUsed/bytesAvailable'>
 *       <appender name='inMemory'/>
 *     </mBeanSnapShotMonitor>
 */

class GaugeCounterArgumentParser {
	private static final Pattern attributeExtractor = Pattern.compile("(\\S+)\\s*\\(displayName\\s*=\\s*['\"]([^'\"]+)['\"]\\)"); 
	private final Set<AttributeSpec> counters = new HashSet<GaugeCounterArgumentParser.AttributeSpec>();	
	private final Set<AttributeSpec> gauges = new HashSet<GaugeCounterArgumentParser.AttributeSpec>();
	
	// The ratioComponents are the numerator and denominator fields included in ratio definition(s)
	//  For example if a query had the following ratio defined "memInUsePercent=bytesUsed/bytesAvailable"
	// bytesUsed and bytesAvailable would be the components.
	private final Set<AttributeSpec> ratioComponents = new HashSet<AttributeSpec>();
	
	GaugeCounterArgumentParser(MBeanQuery query) {
		for (String counter : query.getCounters()) {
			counters.add(new AttributeSpec(counter));
		}
		for (String gauge : query.getGauges()) {
			gauges.add(new AttributeSpec(gauge));
		}
		for (SnapShotRatio ratio : query.getRatios()) {
			ratioComponents.add(new AttributeSpec(ratio.getNumerator()));
			ratioComponents.add(new AttributeSpec(ratio.getDenominator()));
		}
	}
	
	Set<AttributeSpec> getCounters() {
		return counters;
	}
	
	Set<AttributeSpec> getGauges() {
		return gauges;
	}
	
	Set<AttributeSpec> getRatioComponents() {
		return ratioComponents;
	}

	static class AttributeSpec {
		private final String name;
		private final String displayName;
		private final boolean compositeName; // Composite name is for an attribute that is part of a JMX Composite Object (i.e. "Usage.current");
		
		AttributeSpec(String attribute) {
			Matcher m = attributeExtractor.matcher(attribute); 
			if (m.find()) {
				name = m.group(1);
				displayName = m.group(2);
			} else {
				name = attribute;
				displayName = null;
			}
			this.compositeName = CompositeDataManager.isCompositeAttributeName(name);
		}

		String getName() {
			return name;
		}

		String getPreferredDisplayName() {
			return getPreferredDisplayName(name);
		}
		
		String getPreferredDisplayName(String defaultName) {
			return displayName != null ? displayName : defaultName;
		}
		
		public boolean isCompositeName() {
			return compositeName;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AttributeSpec other = (AttributeSpec) obj;
			return Objects.equals(name, other.name);
		}

		@Override
		public String toString() {
			return "AttributeSpec [name=" + name + ", displayName=" + displayName + ", compositeName=" + compositeName
					+ "]";
		}
	}
}

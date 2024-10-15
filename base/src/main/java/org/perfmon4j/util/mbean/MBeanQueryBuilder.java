package org.perfmon4j.util.mbean;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.perfmon4j.util.MiscHelper;

public class MBeanQueryBuilder {
	private final String baseJMXName;
	private String domain = null;
	private String instanceKey = null;
	private String displayName = null;
	private final Set<String> counters = new TreeSet<String>();
	private final Set<String> gauges = new TreeSet<String>();
	private final Set<SnapShotRatioImpl> ratios = new TreeSet<SnapShotRatioImpl>();
	
	public MBeanQueryBuilder(String baseJMXName) {
		this.baseJMXName = baseJMXName;
	}
	
	public MBeanQuery build() throws Exception {
		return new MBeanQueryImpl(domain, baseJMXName, displayName, instanceKey, counters.toArray(new String[] {}),
			gauges.toArray(new String[] {}));
	}

	public String getDomain() {
		return domain;
	}
	
	public MBeanQueryBuilder setDomain(String domain) {
		this.domain = domain;
		return this;
	}
	
	public String getInstanceKey() {
		return instanceKey;
	}

	public MBeanQueryBuilder setInstanceKey(String instanceKey) {
		this.instanceKey = instanceKey;
		return this;
	}

	public String getBaseJMXName() {
		return baseJMXName;
	}

	public String[] getCounters() {
		return counters.toArray(new String[] {});
	}
	
	public MBeanQueryBuilder setCounters(String counterCSV) {
		synchronized (counters) {
			counters.clear();
			if (!MiscHelper.isBlankOrNull(counterCSV)) {
				for (String counter : counterCSV.split(",")) {
					counters.add(counter.trim());
				}
			}
		}
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public MBeanQueryBuilder setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String[] getGauges() {
		return gauges.toArray(new String[] {});
	}
	
	public MBeanQueryBuilder setGauges(String gaugesCSV) {
		synchronized (gauges) {
			gauges.clear();
			if (!MiscHelper.isBlankOrNull(gaugesCSV)) {
				for (String gauge : gaugesCSV.split(",")) {
					gauges.add(gauge.trim());
				}
			}
		}		
		return this;
	}

	public SnapShotRatio[] getRatios() {
		return ratios.toArray(new SnapShotRatio[] {});
	}
	
	public MBeanQueryBuilder setRatios(String ratiosCSV) {
		synchronized (ratios) {
			ratios.clear();
//			if (!MiscHelper.isBlankOrNull(gaugesCSV)) {
//				for (String gauge : gaugesCSV.split(",")) {
//					gauges.add(gauge.trim());
//				}
//			}
		}		
		return this;
	}
	
	private static class MBeanQueryImpl implements MBeanQuery {
		private final String domain;
		private final String baseJMXName;
		private final String displayName;
		private final String instanceKey;
		private final String[] counters;
		private final String[] gauges;
		private final SnapShotRatio[] ratios;
		private final String signature;
		
		MBeanQueryImpl(String domain, String baseJMXName, String displayName, String instanceKey, String[] counters, String[] gauges) throws Exception {
			this.domain = domain;
			this.baseJMXName = baseJMXName;
			this.displayName = displayName == null || displayName.isBlank() ? baseJMXName : displayName;
			this.instanceKey = instanceKey;
			this.counters = counters;
			this.gauges = gauges;
			this.ratios = new SnapShotRatio[] {};
			this.signature =  MiscHelper.generateSHA256(buildComparableKey(this.domain, this.baseJMXName, this.displayName, this.instanceKey, counters, gauges));
		}
		
		private static String buildComparableKey(String domain, String baseJMXName, String displayName, String instanceKey, String[] counters, String[] gauges) {
			String result = MBeanQuery.class.getName() + "|" + baseJMXName + "|" + displayName;
			
			if (domain != null) {
				result += "|" + domain;
			}
			
			if (instanceKey != null) {
				result += "|" + instanceKey;
			}
			
			result += "|counters=" + MiscHelper.toString(counters);
			result += "|gauges=" + MiscHelper.toString(gauges);
			
			return result;
		}
		
		@Override
		public String getBaseJMXName() {
			return baseJMXName;
		}

		@Override
		public String getDisplayName() {
			return displayName;
		}

		@Override
		public String getInstanceKey() {
			return instanceKey;
		}
		
		@Override
		public String[] getCounters() {
			return counters;
		}
		
		@Override
		public String[] getGauges() {
			return gauges;
		}

		public String getSignature() {
			return signature;
		}

		@Override
		public int hashCode() {
			return Objects.hash(signature);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MBeanQueryImpl other = (MBeanQueryImpl) obj;
			return Objects.equals(signature, other.signature);
		}

		@Override
		public int compareTo(MBeanQuery o) {
			int result = -1;
			if (o instanceof MBeanQueryImpl) {
				result = signature.compareTo(((MBeanQueryImpl)o).signature);
			}
			return result;
		}

		@Override
		public String getDomain() {
			return domain;
		}

		@Override
		public SnapShotRatio[] getRatios() {
			return ratios;
		}
	}
	
	private static class SnapShotRatioImpl implements SnapShotRatio, Comparable<SnapShotRatioImpl> {
		private final String name;
		private final String numerator;
		private final String denominator;
		private final boolean displayAsPercentage;
		private final boolean displayAsDuration;
		private final String sortKey;
	
		SnapShotRatioImpl(String name, String numerator, String denominator, boolean displayAsPercentage,
				boolean displayAsDuration) {
			this.name = name;
			this.numerator = numerator;
			this.denominator = denominator;
			this.displayAsPercentage = displayAsPercentage;
			this.displayAsDuration = displayAsDuration;
			this.sortKey = buildSortKey(name, numerator, denominator, displayAsPercentage, displayAsDuration);
		}
		
		private static String buildSortKey(String name, String numerator, String denominator, boolean displayAsPercentage,
			boolean displayAsDuration) {
			return name + "|" + numerator + "|" + denominator + "|" + Boolean.toString(displayAsPercentage) +
					"|" + Boolean.toString(displayAsDuration);
		}
		
//		private static SnapShotRatio
//		

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getDenominator() {
			return denominator;
		}

		@Override
		public String getNumerator() {
			return numerator;
		}

		@Override
		public boolean isDisplayAsPercentage() {
			return displayAsPercentage;
		}

		@Override
		public boolean isDisplayAsDuration() {
			return displayAsDuration;
		}

		@Override
		public int compareTo(SnapShotRatioImpl o) {
			return sortKey.compareTo(o.sortKey);
		}

		@Override
		public int hashCode() {
			return Objects.hash(sortKey);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SnapShotRatioImpl other = (SnapShotRatioImpl) obj;
			return Objects.equals(sortKey, other.sortKey);
		}

		@Override
		public String toString() {
			return "SnapShotRatioImpl [name=" + name + ", numerator=" + numerator + ", denominator=" + denominator
					+ ", displayAsPercentage=" + displayAsPercentage + ", displayAsDuration=" + displayAsDuration + "]";
		}
	}
}

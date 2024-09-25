package org.perfmon4j.util.mbean;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.perfmon4j.util.MiscHelper;

public class MBeanQueryBuilder {
	private final String baseJMXName;
	private String domain = null;
	private String instanceName = null;
	private String displayName = null;
	private final Set<String> counters = new TreeSet<String>();
	private final Set<String> gauges = new TreeSet<String>();
	
	public MBeanQueryBuilder(String baseJMXName) {
		this.baseJMXName = baseJMXName;
	}
	
	public MBeanQuery build() throws Exception {
		return new MBeanQueryImpl(domain, baseJMXName, displayName, instanceName, counters.toArray(new String[] {}),
			gauges.toArray(new String[] {}));
	}

	public String getDomain() {
		return domain;
	}
	
	public MBeanQueryBuilder setDomain(String domain) {
		this.domain = domain;
		return this;
	}
	
	public String getInstanceName() {
		return instanceName;
	}

	public MBeanQueryBuilder setInstanceName(String instanceName) {
		this.instanceName = instanceName;
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
			for (String counter : counterCSV.split(",")) {
				counters.add(counter.trim());
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
			for (String gauge : gaugesCSV.split(",")) {
				gauges.add(gauge.trim());
			}
		}		
		return this;
	}

	private static class MBeanQueryImpl implements MBeanQuery {
		private final String domain;
		private final String baseJMXName;
		private final String displayName;
		private final String instanceName;
		private final String[] counters;
		private final String[] gauges;
		private final String signature;
		
		MBeanQueryImpl(String domain, String baseJMXName, String displayName, String instanceName, String[] counters, String[] gauges) throws Exception {
			this.domain = domain;
			this.baseJMXName = baseJMXName;
			this.displayName = displayName == null || displayName.isBlank() ? baseJMXName : displayName;
			this.instanceName = instanceName;
			this.counters = counters;
			this.gauges = gauges;
			this.signature =  MiscHelper.generateSHA256(buildComparableKey(this.domain, this.baseJMXName, this.displayName, this.instanceName, counters, gauges));
		}
		
		private static String buildComparableKey(String domain, String baseJMXName, String displayName, String instanceName, String[] counters, String[] gauges) {
			String result = MBeanQuery.class.getName() + "|" + baseJMXName + "|" + displayName;
			
			if (domain != null) {
				result += "|" + domain;
			}
			
			if (instanceName != null) {
				result += "|" + instanceName;
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
		public String getInstancePropertyKey() {
			return instanceName;
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
	}
}

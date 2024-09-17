package org.perfmon4j.util.mbean;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.perfmon4j.util.MiscHelper;

public class MBeanQueryBuilder {
	private final String baseJMXName;
	private String instanceName = null;
	private final Set<String> counters = new TreeSet<String>();
	private final Set<String> gauges = new TreeSet<String>();
	
	public MBeanQueryBuilder(String baseJMXName) {
		this.baseJMXName = baseJMXName;
	}
	
	public MBeanQuery build() {
		return new MBeanQueryImpl(baseJMXName, instanceName, counters.toArray(new String[] {}),
			gauges.toArray(new String[] {}));
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
		private final String baseJMXName;
		private final String instanceName;
		private final String[] counters;
		private final String[] gauges;
		private final String comparableKey;
		
		MBeanQueryImpl(String baseJMXName, String instanceName, String[] counters, String[] gauges) {
			this.baseJMXName = baseJMXName;
			this.instanceName = instanceName;
			this.counters = counters;
			this.gauges = gauges;
			this.comparableKey =  buildComparableKey(baseJMXName, instanceName, counters, gauges);
		}
		
		private static String buildComparableKey(String baseJMXName, String instanceName, String[] counters, String[] gauges) {
			String result = MBeanQuery.class.getName() + "|" + baseJMXName;			
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
		public String getInstanceName() {
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

		@Override
		public int hashCode() {
			return Objects.hash(comparableKey);
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
			return Objects.equals(comparableKey, other.comparableKey);
		}

		@Override
		public int compareTo(MBeanQuery o) {
			int result = -1;
			if (o instanceof MBeanQueryImpl) {
				result = comparableKey.compareTo(((MBeanQueryImpl)o).comparableKey);
			}
			return result;
		}
	}
}

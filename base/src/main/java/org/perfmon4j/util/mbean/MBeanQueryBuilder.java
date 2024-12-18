package org.perfmon4j.util.mbean;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

public class MBeanQueryBuilder {
	private static final Logger logger = LoggerFactory.initLogger(MBeanQueryImpl.class);
	private static final Pattern attributeValueFilterPattern = Pattern.compile("([^\\=]+)\\=(.*)");
	
	private final String baseJMXName;
	private String domain = null;
	private String instanceKey = null;
	private String displayName = null;
	private final Set<String> counters = new TreeSet<String>();
	private final Set<String> gauges = new TreeSet<String>();
	private final Set<SnapShotRatioImpl> ratios = new TreeSet<SnapShotRatioImpl>();
	private RegExFilter instanceValueFilter = null;
	private NamedRegExFilter attributeValueFilter = null;
	
	public MBeanQueryBuilder(String baseJMXName) {
		this.baseJMXName = baseJMXName;
	}
	
	public MBeanQuery build() throws Exception {
		return new MBeanQueryImpl(domain, baseJMXName, displayName, instanceKey, counters.toArray(new String[] {}),
			gauges.toArray(new String[] {}), ratios.toArray(new SnapShotRatio[] {}), instanceValueFilter, attributeValueFilter);
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
			if (!MiscHelper.isBlankOrNull(ratiosCSV)) {
				for (String ratioString : ratiosCSV.split(",")) {
					SnapShotRatioImpl ratio = SnapShotRatioImpl.parse(ratioString);
					if (ratio == null) {
						logger.logWarn("Unable to build SnapShotRatio from: " + ratioString);
					} else {
						ratios.add(ratio);
					}
				}
			}
		}		
		return this;
	}
	
	public MBeanQueryBuilder setInstanceValueFilter(String regEx) {
		instanceValueFilter = null;  // Clear out any previously set value.
		if (regEx != null) {
			try {
				instanceValueFilter = new RegExFilter(regEx);
			} catch (InvalidPatternSyntaxException e) {
				logger.logWarn("Skipping invalid instanceValueFilter: \"" + regEx + "\" Error: " + e.getMessage());
			}
		}
		return this;
	}	
	
	
	/**
	 * Parameter expected to be in the format <attribute name>=<regular expression to match attribute name>
	 * @param value 
	 * @return
	 */
	public MBeanQueryBuilder setAttributeValueFilter(String value) {
		attributeValueFilter = null;  // Clear out any previously set value.
		
		if (value != null) {
			Matcher m = attributeValueFilterPattern.matcher(value);
			if (m.matches()) {
				String attributeName = m.group(1);
				String regEx = m.group(2);
				try {
					attributeValueFilter = new NamedRegExFilter(attributeName, regEx);
				} catch (InvalidPatternSyntaxException e) {
					logger.logWarn("Invalid regular expression. Skipping invalid attributeValueFilter: \"" + value + "\" Error: " + e.getMessage());
				}
			} else {
				logger.logWarn("Skipping invalid value for attributeValueFilter: " + value);
			}
		}
		return this;
	}
	
	
	/** package level **/ static class MBeanQueryImpl implements MBeanQuery {
		private final String domain;
		private final String baseJMXName;
		private final String displayName;
		private final String instanceKey;
		private final String[] counters;
		private final String[] gauges;
		private final SnapShotRatio[] ratios;
		private final String signature;
		private final RegExFilter instanceValueFilter;
		private final NamedRegExFilter attributeValueFilter;

		
		MBeanQueryImpl(String domain, String baseJMXName, String displayName, String instanceKey, String[] counters, String[] gauges, 
			SnapShotRatio[] ratios,	RegExFilter instanceValueFilter, NamedRegExFilter attributeValueFilter) throws Exception {
			this.domain = domain;
			this.baseJMXName = baseJMXName;
			this.displayName = displayName == null || displayName.isBlank() ? baseJMXName : displayName;
			this.instanceKey = instanceKey;
			this.counters = (counters != null) ? counters : new String[] {};
			this.gauges = (gauges != null) ? gauges : new String[] {};
			this.ratios = (ratios != null) ? ratios : new SnapShotRatio[] {};
			this.instanceValueFilter = instanceValueFilter;
			this.attributeValueFilter = attributeValueFilter;
			this.signature =  MiscHelper.generateSHA256(buildComparableKey(this.domain, this.baseJMXName, this.displayName, this.instanceKey, counters, gauges, ratios, 
				instanceValueFilter, attributeValueFilter));
		}
		
		private static String buildComparableKey(String domain, String baseJMXName, String displayName, String instanceKey, String[] counters, String[] gauges,
			SnapShotRatio[] ratios, RegExFilter instanceValueFilter, NamedRegExFilter attributeValueFilter) {
			String result = MBeanQuery.class.getName() + "|" + baseJMXName + "|" + displayName;
			
			if (domain != null) {
				result += "|" + domain;
			}
			
			if (instanceKey != null) {
				result += "|" + instanceKey;
			}
			
			result += "|counters=" + MiscHelper.toString(counters);
			result += "|gauges=" + MiscHelper.toString(gauges);
			result += "|ratios=" + ratiosToString(ratios);
			
			if (instanceValueFilter != null) {
				result += "|instanceValueFilter=" + instanceValueFilter.toString();
			}
			if (attributeValueFilter != null) {
				result += "|attributeValueFilter=" + attributeValueFilter.toString();
			}
			
			return result;
		}
		
		private static String ratiosToString(SnapShotRatio[] ratios) {
			String[] result = new String[ratios.length];
			for (int i = 0; i < ratios.length; i++) {
				result[i] = ratios[i].getName();
			}
			return MiscHelper.toString(result);
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
		
		@Override
		public RegExFilter getInstanceValueFilter() {
			return instanceValueFilter;
		}

		@Override
		public NamedRegExFilter getAttributeValueFilter() {
			return attributeValueFilter;
		}
		
		@Override
		public String toString() {
			return "MBeanQueryImpl [domain=" + domain + ", baseJMXName=" + baseJMXName + ", displayName=" + displayName
					+ ", instanceKey=" + instanceKey + ", counters=" + MiscHelper.toString(counters) + ", gauges="
					+ MiscHelper.toString(gauges) + ", ratios=" + ratiosToString(ratios) 
					+ ", instanceValueFilter=" + (instanceValueFilter != null ? instanceValueFilter.toString() : "")
					+ ", attributeValueFilter=" + (attributeValueFilter != null ? attributeValueFilter.toString() : "") + "]";
		}

	}
	
	static SnapShotRatio normalize(SnapShotRatio ratio, String newNumerator, String newDenominator) {
		return new SnapShotRatioImpl(ratio.getName(),  newNumerator, newDenominator, ratio.isFormatAsPercent());
	}
	
	
	private static class SnapShotRatioImpl implements SnapShotRatio, Comparable<SnapShotRatioImpl> {
		private final String name;
		private final String numerator;
		private final String denominator;
		private final boolean formatAsPercent;
		private final String sortKey;
	
		SnapShotRatioImpl(String name, String numerator, String denominator, boolean formatAsPercent) {
			this.name = name;
			this.numerator = numerator;
			this.denominator = denominator;
			this.formatAsPercent = formatAsPercent;
			this.sortKey = buildSortKey(name, numerator, denominator, formatAsPercent);
		}
		
		private static String buildSortKey(String name, String numerator, String denominator, boolean formatAsPercent) {
			return name + "|" + numerator + "|" + denominator + "|" + Boolean.toString(formatAsPercent);
		}
		
		private static final Pattern RATIO_PATTERN = Pattern.compile("([^\\=]+)\\=([^\\/]+)\\/([^(]+)(.*)");
		
		private static SnapShotRatioImpl parse(String value) {
			SnapShotRatioImpl result = null;
			
			Matcher m = RATIO_PATTERN.matcher(value);
			if (m.matches()) {
				String name = m.group(1).trim();
				String numerator = m.group(2).trim();
				String denominator = m.group(3).trim();
				String extraParameters = m.group(4).trim();
				
				// For formatAsPercent allow (true) to be optionally included in quotes (single or double).
				extraParameters = extraParameters.replace("\"true\"", "true").replace("'true'", "true");
				
				if (!name.isEmpty() && !numerator.isEmpty() && !denominator.isEmpty()) {
					result = new SnapShotRatioImpl(name, numerator, denominator, 
							extraParameters.equals("(formatAsPercent=true)"));
				}
			}
			
			return result;
		}

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
		public boolean isFormatAsPercent() {
			return formatAsPercent;
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
					+ ", formatAsPercent=" + formatAsPercent +  "]";
		}
	}
	
	public static class RegExFilter {
		private final String rawPattern;
		private final Pattern compiledPattern;
		
		private RegExFilter(String rawPattern) throws InvalidPatternSyntaxException {
			this.rawPattern = rawPattern;
			try {
				this.compiledPattern = Pattern.compile(rawPattern);
			} catch (PatternSyntaxException ex) {
				throw new InvalidPatternSyntaxException(ex);
			}
		}
		
		public boolean matches(String value) {
			return compiledPattern.matcher(value).matches();
		}

		@Override
		public String toString() {
			return "\"" + rawPattern + "\"";
		}
	}
	
	/**
	 * Used to specify a filter on based on a named attribute of an MBean.
	 */
	public static class NamedRegExFilter extends RegExFilter{
		private final String name;
		
		private NamedRegExFilter(String name, String rawPattern) throws InvalidPatternSyntaxException {
			super(rawPattern);
			this.name = name;
		}
		
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name + "=" + super.toString();
		}
	}
	
	static final class InvalidPatternSyntaxException extends Exception {
		private static final long serialVersionUID = 1L;

		public InvalidPatternSyntaxException(PatternSyntaxException ex) {
			super(ex);
		}
	}
}

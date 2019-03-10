package org.perfmon4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.util.EnhancedAppenderPatternHelper;

public class AppenderToMonitorMapper {
	private final PatternMapper[] mappers;
	
	
	private AppenderToMonitorMapper(Map<?, PatternMapper> map) {
		mappers = map.values().toArray(new PatternMapper[]{});
	}

	public boolean hasAppendersForMonitor(String intervalMonitorName) {
		boolean result = false;
		
		for (PatternMapper mapper : mappers) {
			if (mapper.getPattern().matches(intervalMonitorName)) {
				result = true;
				break;
			}
		}

		return result;
	}
	
	public AppenderID[] getAppendersForMonitor(String intervalMonitorName) {
		Set<AppenderID> appenders = new HashSet<AppenderID>();
		
		for (PatternMapper mapper : mappers) {
			if (mapper.getPattern().matches(intervalMonitorName)) {
				appenders.addAll(Arrays.asList(mapper.getAppenders()));
			}
		}

		return appenders.toArray(new AppenderID[]{});
	}

	
	/**
	 * Package level for testing.
	 * @param monitorName
	 * @param pattern
	 * @return
	 */
	static HashableRegEx buildRegEx(String monitorName, String pattern) {
		HashableRegEx result = HashableRegEx.NULL_REGEX;
		
		if (!PerfMon.ROOT_MONITOR_NAME.equals(monitorName)) {
			monitorName = PerfMon.ROOT_MONITOR_NAME + "." + monitorName;
		}
		
		if (PerfMon.APPENDER_PATTERN_PARENT_ONLY.equals(pattern) || ".".equals(pattern)) {
			// Parent only.. 
			result = new HashableRegEx(Pattern.quote(monitorName));
		} else if (PerfMon.APPENDER_PATTERN_PARENT_AND_CHILDREN_ONLY.equals(pattern)) {
			result = new HashableRegEx(Pattern.quote(monitorName) + "(|\\.[^\\.]+)");
		} else if (PerfMon.APPENDER_PATTERN_CHILDREN_ONLY.equals(pattern)) {
			result = new HashableRegEx(Pattern.quote(monitorName) + "(\\.[^\\.]+)");
		} else if (PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS.equals(pattern)) {
			result = new HashableRegEx(Pattern.quote(monitorName) + "(|\\.[^\\.]+)+");
		} else if (PerfMon.APPENDER_PATTERN_ALL_DESCENDENTS.equals(pattern)) {
			result = new HashableRegEx(Pattern.quote(monitorName) + "(\\.[^\\.]+)+");
		} else {
			String regEx = EnhancedAppenderPatternHelper.buildPattern(monitorName, pattern);
			if (regEx != null) {
				result = new HashableRegEx(regEx);
			}
		}

		return result;
	}

	/**
	 * Not thread safe class...
	 * @author perfmon
	 *
	 */
	public static class Builder {
		private final Map<HashableRegEx, PatternMapper> map = new HashMap<HashableRegEx, PatternMapper>();
		
		public Builder() {
		}
		
		public Builder add(String monitorName, String pattern, AppenderID... appenders) {
			HashableRegEx regEx = AppenderToMonitorMapper.buildRegEx(monitorName, pattern);
			
			PatternMapper mapper = map.get(regEx);
			if (mapper == null) {
				mapper = new PatternMapper(regEx, appenders);
				map.put(regEx, mapper);
			} else {
				map.put(regEx, mapper.append(appenders));
			}
			
			return this;
		}
		
		public AppenderToMonitorMapper build() {
			return new AppenderToMonitorMapper(map);
		}
	}
	
	private static class PatternMapper {
		private final HashableRegEx pattern;
		private final AppenderID[] appenders;
		
		public PatternMapper(HashableRegEx pattern, AppenderID[] appenders) {
			super();
			this.pattern = pattern;
			this.appenders = appenders;
		}
		
		public HashableRegEx getPattern() {
			return pattern;
		}
		
		public AppenderID[] getAppenders() {
			return appenders;
		}
		
		PatternMapper append(AppenderID... appenderIDs) {
			Set<AppenderID> newAppenders = new HashSet<AppenderID>(Arrays.asList(this.getAppenders()));
		
			newAppenders.addAll(Arrays.asList(appenderIDs));
		
			return new PatternMapper(this.getPattern(), newAppenders.toArray(new AppenderID[]{}));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((pattern == null) ? 0 : pattern.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PatternMapper other = (PatternMapper) obj;
			if (pattern == null) {
				if (other.pattern != null)
					return false;
			} else if (!pattern.equals(other.pattern))
				return false;
			return true;
		}
	}

	/**
	 * Package level for testing.
	 * @author perfmon
	 *
	 */
	static class HashableRegEx {
		final String regEx;
		final Pattern pattern;
		
		private static final HashableRegEx NULL_REGEX = new HashableRegEx(null);

		HashableRegEx(String regEx) {
			super();
			this.regEx = (regEx == null ? "" : regEx);
			this.pattern = this.regEx.isEmpty() ? null : Pattern.compile(regEx);
		}
		
		public String getRegEx() {
			return regEx;
		}
		
		public boolean matches(String input) {
			if (!PerfMon.ROOT_MONITOR_NAME.equals(input)) {
				input = PerfMon.ROOT_MONITOR_NAME + "." + input;
			}
			
			return pattern == null ? false : pattern.matcher(input).matches();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((regEx == null) ? 0 : regEx.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HashableRegEx other = (HashableRegEx) obj;
			if (regEx == null) {
				if (other.regEx != null)
					return false;
			} else if (!regEx.equals(other.regEx))
				return false;
			return true;
		}
	}
}

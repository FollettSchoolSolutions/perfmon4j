package web.org.perfmon4j.extras.genericfilter;

import java.util.Properties;

public class FilterParamsVO implements FilterParams {
	private String baseFilterCategory;
	private boolean abortTimerOnImageResponse;
	private boolean abortTimerOnRedirect;
	private String abortTimerOnURLPattern;
	private String skipTimerOnURLPattern;
	private boolean outputRequestAndDuration;
	private String servletPathTransformationPattern;
	
	public FilterParamsVO() {
		baseFilterCategory = "WebRequest";
		abortTimerOnImageResponse = true;
		abortTimerOnRedirect = true;
		abortTimerOnURLPattern = null;
		skipTimerOnURLPattern = null;
		outputRequestAndDuration = true;
		servletPathTransformationPattern = null;
	}
	
	public FilterParamsVO(Properties props) {
		baseFilterCategory = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.baseFilterCategory", "WebRequest");
		abortTimerOnImageResponse = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.abortTimerOnImageResponse", true);
		abortTimerOnRedirect = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.abortTimerOnRedirect", true);
		abortTimerOnURLPattern = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.abortTimerOnURLPattern", null);
		skipTimerOnURLPattern = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.skipTimerOnURLPattern", null);
		outputRequestAndDuration = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.outputRequestAndDuration", true);
		servletPathTransformationPattern = getProperty(props, "perfmon4j.bootconfiguration.servlet-valve.servletPathTransformationPattern", null);
	}
	
	static private String getProperty(Properties props,
			String propertyName,
			String defaultValue) {
		String result = props.getProperty(propertyName);
		
		if (result == null) {
			result = defaultValue;
		}
		
		return result;
	}

	static private boolean getProperty(Properties props,
			String propertyName,
			boolean defaultValue) {
		String result = getProperty(props, propertyName, Boolean.toString(defaultValue));
		return Boolean.parseBoolean(result);
	}
	
	
	
	public String getBaseFilterCategory() {
		return baseFilterCategory;
	}
	public boolean isAbortTimerOnImageResponse() {
		return abortTimerOnImageResponse;
	}
	public boolean isAbortTimerOnRedirect() {
		return abortTimerOnRedirect;
	}
	public String getAbortTimerOnURLPattern() {
		return abortTimerOnURLPattern;
	}
	public String getSkipTimerOnURLPattern() {
		return skipTimerOnURLPattern;
	}
	public boolean isOutputRequestAndDuration() {
		return outputRequestAndDuration;
	}
	public String getServletPathTransformationPattern() {
		return servletPathTransformationPattern;
	}

	public void setBaseFilterCategory(String baseFilterCategory) {
		this.baseFilterCategory = baseFilterCategory;
	}

	public void setAbortTimerOnImageResponse(boolean abortTimerOnImageResponse) {
		this.abortTimerOnImageResponse = abortTimerOnImageResponse;
	}

	public void setAbortTimerOnRedirect(boolean abortTimerOnRedirect) {
		this.abortTimerOnRedirect = abortTimerOnRedirect;
	}

	public void setAbortTimerOnURLPattern(String abortTimerOnURLPattern) {
		this.abortTimerOnURLPattern = abortTimerOnURLPattern;
	}

	public void setSkipTimerOnURLPattern(String skipTimerOnURLPattern) {
		this.skipTimerOnURLPattern = skipTimerOnURLPattern;
	}

	public void setOutputRequestAndDuration(boolean outputRequestAndDuration) {
		this.outputRequestAndDuration = outputRequestAndDuration;
	}

	public void setServletPathTransformationPattern(String servletPathTransformationPattern) {
		this.servletPathTransformationPattern = servletPathTransformationPattern;
	}
}

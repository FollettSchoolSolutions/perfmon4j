package web.org.perfmon4j.extras.genericfilter;

public class FilterParamsVO implements FilterParams {
	private String baseFilterCategory = "WebRequest";
	private final boolean abortTimerOnImageResponse = true;
	private final boolean abortTimerOnRedirect = true;
	private final String abortTimerOnURLPattern = null;
	private final String skipTimerOnURLPattern = null;
	private final boolean outputRequestAndDuration = true;
	private final String servletPathTransformationPattern = null;
	
	public String getBaseFilterCategory() {
		return baseFilterCategory;
	}
	public void setBaseFilterCategory(String baseFilterCategory) {
		this.baseFilterCategory = baseFilterCategory;
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

}

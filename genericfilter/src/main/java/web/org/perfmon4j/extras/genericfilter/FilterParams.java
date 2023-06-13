package web.org.perfmon4j.extras.genericfilter;

public interface FilterParams {
	public String getBaseFilterCategory();
	public boolean isAbortTimerOnRedirect();
	public boolean isAbortTimerOnImageResponse();
	public String getAbortTimerOnURLPattern();
	public String getSkipTimerOnURLPattern();
	public boolean isOutputRequestAndDuration();
//	public String getPushCookiesOnNDC();
//	public String getPushSessionAttributesOnNDC();
//	public boolean isPushClientInfoOnNDC();
//	public boolean isPushURLOnNDC();
	public String getServletPathTransformationPattern();
}

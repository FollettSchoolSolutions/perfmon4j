package web.org.perfmon4j.extras.genericfilter;

import java.util.Properties;

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
	
	static public FilterParams fromProperties(Properties props) {
		FilterParamsVO result = new FilterParamsVO(props);
		return result;
	}
	
	static public FilterParams getDefault() {
		return new FilterParamsVO();
	}
}

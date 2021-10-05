package web.org.perfmon4j.extras.wildfly8;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;

public class PerfmonHandlerWrapper implements HandlerWrapper {
	private static final Logger logger = LoggerFactory.initLogger(PerfmonHandlerWrapper.class);
	private String baseFilterCategory = "WebRequest";
    private boolean abortTimerOnRedirect = false;
    private boolean abortTimerOnImageResponse = false;
    private String abortTimerOnURLPattern = null;
    private String skipTimerOnURLPattern = null;
    private boolean outputRequestAndDuration = false;
    private boolean pushURLOnNDC = false;
    private String pushCookiesOnNDC = null;
    private String pushSessionAttributesOnNDC = null;
    private boolean pushClientInfoOnNDC = false;
    private String servletPathTransformationPattern = null;
	static boolean announced = false;
    
    
	public HttpHandler wrap(HttpHandler handler) {
		if (!announced) {
			announced = true;
			logger.logInfo("Perfmoh4j installing Undertow HandlerWrapper");
			logger.logInfo("baseFilterCategory=" + baseFilterCategory);
			logger.logInfo("abortTimerOnRedirect=" + abortTimerOnRedirect );
			logger.logInfo("abortTimerOnImageResponse=" + abortTimerOnImageResponse);
			logger.logInfo("abortTimerOnURLPattern=" + abortTimerOnURLPattern);
			logger.logInfo("skipTimerOnURLPattern=" + skipTimerOnURLPattern);
			logger.logInfo("outputRequestAndDuration=" + outputRequestAndDuration);
			logger.logInfo("pushURLOnNDC=" + pushURLOnNDC);
			logger.logInfo("pushCookiesOnNDC=" + pushCookiesOnNDC);
			logger.logInfo("pushSessionAttributesOnNDC=" + pushSessionAttributesOnNDC);
			logger.logInfo("pushClientInfoOnNDC=" + pushClientInfoOnNDC);
			logger.logInfo("servletPathTransformationPattern=" + servletPathTransformationPattern);
		}
		return new HandlerImpl(this, handler);
	}
	
	public String getBaseFilterCategory() {
		return baseFilterCategory;
	}

	public void setBaseFilterCategory(String baseFilterCategory) {
		this.baseFilterCategory = baseFilterCategory;
	}

	public boolean isAbortTimerOnRedirect() {
		return abortTimerOnRedirect;
	}

	public void setAbortTimerOnRedirect(boolean abortTimerOnRedirect) {
		this.abortTimerOnRedirect = abortTimerOnRedirect;
	}

	public boolean isAbortTimerOnImageResponse() {
		return abortTimerOnImageResponse;
	}

	public void setAbortTimerOnImageResponse(boolean abortTimerOnImageResponse) {
		this.abortTimerOnImageResponse = abortTimerOnImageResponse;
	}

	public String getAbortTimerOnURLPattern() {
		return abortTimerOnURLPattern;
	}

	public void setAbortTimerOnURLPattern(String abortTimerOnURLPattern) {
		this.abortTimerOnURLPattern = abortTimerOnURLPattern;
	}

	public String getSkipTimerOnURLPattern() {
		return skipTimerOnURLPattern;
	}

	public void setSkipTimerOnURLPattern(String skipTimerOnURLPattern) {
		this.skipTimerOnURLPattern = skipTimerOnURLPattern;
	}

	public boolean isOutputRequestAndDuration() {
		return outputRequestAndDuration;
	}

	public void setOutputRequestAndDuration(boolean outputRequestAndDuration) {
		this.outputRequestAndDuration = outputRequestAndDuration;
	}

	public String getPushCookiesOnNDC() {
		return pushCookiesOnNDC;
	}

	public void setPushCookiesOnNDC(String pushCookiesOnNDC) {
		this.pushCookiesOnNDC = pushCookiesOnNDC;
	}

	public String getPushSessionAttributesOnNDC() {
		return pushSessionAttributesOnNDC;
	}

	public void setPushSessionAttributesOnNDC(String pushSessionAttributesOnNDC) {
		this.pushSessionAttributesOnNDC = pushSessionAttributesOnNDC;
	}

	public boolean isPushClientInfoOnNDC() {
		return pushClientInfoOnNDC;
	}

	public void setPushClientInfoOnNDC(boolean pushClientInfoOnNDC) {
		this.pushClientInfoOnNDC = pushClientInfoOnNDC;
	}

	public boolean isPushURLOnNDC() {
		return pushURLOnNDC;
	}

	public void setPushURLOnNDC(boolean pushURLOnNDC) {
		this.pushURLOnNDC = pushURLOnNDC;
	}

	public String getServletPathTransformationPattern() {
		return servletPathTransformationPattern;
	}

	public void setServletPathTransformationPattern(String servletPathTransformationPattern) {
		this.servletPathTransformationPattern = servletPathTransformationPattern;
	}
}

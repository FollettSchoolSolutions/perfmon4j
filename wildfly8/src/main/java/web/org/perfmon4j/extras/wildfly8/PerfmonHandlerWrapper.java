package web.org.perfmon4j.extras.wildfly8;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;



public class PerfmonHandlerWrapper implements HandlerWrapper {
	private static final Logger logger = LoggerFactory.initLogger(PerfmonHandlerWrapper.class);
	private String baseFilterCategory = "WebRequest";
    private boolean abortTimerOnRedirect = false;
    private boolean abortTimerOnImageResponse = false;
    private String abortTimerOnURLPattern = null;
    private String skipTimerOnURLPattern = null;
    private boolean outputRequestAndDuration = false;
    private String pushCookiesOnNDC = null;
    private String pushSessionAttributesOnNDC = null;
    private boolean pushClientInfoOnNDC = false;	
	
	public HttpHandler wrap(HttpHandler handler) {
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
}

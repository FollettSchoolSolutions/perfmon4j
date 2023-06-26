package web.org.perfmon4j.extras.genericfilter;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import api.org.perfmon4j.agent.PerfMon;

public abstract class GenericFilterAsyncLoader {
	private static final int ONE_SECOND = 1000;
	public static final int RETRY_LOAD_WAIT_MILLIS = Integer.getInteger(GenericFilterAsyncLoader.class.getName() + ".RETRY_LOAD_WAIT_MILLIS", 15 * ONE_SECOND).intValue();
	public static final int MAX_RETRY_LOAD_ATTEMPTS = Integer.getInteger(GenericFilterAsyncLoader.class.getName() + ".MAX_RETRY_LOAD_ATTEMPTS", 12).intValue();
	public static final boolean LOAD_DEFAULT_AFTER_MAX_ATTEMPTS 
		=  Boolean.parseBoolean(System.getProperty(GenericFilterAsyncLoader.class.getName() + ".LOAD_DEFAULT_AFTER_MAX_ATTEMPTS", Boolean.TRUE.toString()));
	
	private final String filterDisplayName;
	private final boolean loadDefaultAfterMaxAttempts;
	private final int retryLoadWaitMillis;
	private final int maxRetryLoadAttempts;
	private final AtomicInteger loadAttempts = new AtomicInteger(0);
	private Timer timer = null;

	private final AtomicReference<GenericFilter> genericFilter = new AtomicReference<GenericFilter>();
	
	public GenericFilterAsyncLoader(String filterDisplayName) {
		this(filterDisplayName, LOAD_DEFAULT_AFTER_MAX_ATTEMPTS);
	}
	
	public GenericFilterAsyncLoader(String filterDisplayName, boolean loadDefaultAfterMaxAttempts) {
		this(filterDisplayName, loadDefaultAfterMaxAttempts, RETRY_LOAD_WAIT_MILLIS, MAX_RETRY_LOAD_ATTEMPTS);
	}
	
	public GenericFilterAsyncLoader(String filterDisplayName, boolean loadDefaultAfterMaxAttempts, int retryLoadWaitMillis,
			int maxRetryLoadAttempts) {
		super();
		this.filterDisplayName = filterDisplayName;
		this.loadDefaultAfterMaxAttempts = loadDefaultAfterMaxAttempts;
		this.retryLoadWaitMillis = retryLoadWaitMillis;
		this.maxRetryLoadAttempts = maxRetryLoadAttempts;
	}

	public void scheduleLoad() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		timer = new Timer(filterDisplayName + " installer", true);
		scheduleTimerTask();
	}
	
	
	/**
	 * This will return the filter once it has been initialized.
	 * 
	 * Since the initialization will require that the perfmonconfig.xml
	 * file has been loaded.  The result will be  
	 * 
	 * 
	 * @return The generic filter, or null, if the filter has not 
	 * yet been initialized.
	 */
	public GenericFilter getGenericFilterIfInitialized() {
		return genericFilter.get();
	}
	
	protected Properties retrieveConfiguredSettings() {
		return PerfMon.getConfiguredSettings();
	}
	
	abstract protected GenericFilter initGenericFilter(FilterParams params);
	abstract protected void logInfo(String value);

	private final boolean attemptFilterInstall() {
		boolean installed = false;
		Properties configuredSettings = retrieveConfiguredSettings();
		
		if (Boolean.parseBoolean(configuredSettings.getProperty("perfmon4j.bootConfigSettings.loaded"))) {
			GenericFilter filter = initGenericFilter(FilterParams.fromProperties(configuredSettings));
			genericFilter.set(filter);
			installed = true;
		} else {
			int numAttempts = loadAttempts.incrementAndGet();
			final String baseMsg = String.format("Attempting to load %s. Unable to load because perfmonconfig.xml boot "
				+ "settings have not been initialized after %d attempt(s).", 
				filterDisplayName, Integer.valueOf(numAttempts));
			if (numAttempts < maxRetryLoadAttempts) {
				logInfo(String.format(baseMsg + " Will attempt to load again in %d milliseconds.",
					Integer.valueOf(maxRetryLoadAttempts)));
				scheduleTimerTask();
			} else {
				if (loadDefaultAfterMaxAttempts) {
					logInfo(baseMsg + "  Will load using default parameters.");
					
					GenericFilter filter = initGenericFilter(FilterParams.getDefault());
					genericFilter.set(filter);
					installed = true;
				} else {
					logInfo(baseMsg + "  Will not be installed.");
					cancelTimerTask();
				}
			}
		}
		
		return installed;
	}
	
	private void scheduleTimerTask() {
		 timer.schedule(new LoadFilterTask(), retryLoadWaitMillis);
	}
	
	private void cancelTimerTask() {
		timer.cancel();
		timer = null;
	}

	private class LoadFilterTask extends TimerTask {
		@Override
		public void run() {
			if (attemptFilterInstall()) {
				logInfo(filterDisplayName + " installed.");
				cancelTimerTask();
			}
		}
	}
}
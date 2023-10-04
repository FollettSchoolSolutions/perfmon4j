package org.perfmon4j.emitter;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.perfmon4j.Appender;
import org.perfmon4j.Appender.AppenderID;
import org.perfmon4j.InvalidConfigException;
import org.perfmon4j.SnapShotMonitorLifecycle;

public class EmitterMonitor implements SnapShotMonitorLifecycle, EmitterController {
	private final String name;
	private boolean active = true;
	private static final Object monitorsMapLockToken = new Object();
	private static final Map<String, WeakReference<EmitterMonitor>> monitorsMap = new HashMap<String, WeakReference<EmitterMonitor>>();

	private final Object appendersLockToken = new Object();
	private final Set<AppenderID> appenders = new HashSet<Appender.AppenderID>();
	
	public EmitterMonitor(String className, String name) {
		this.name = name;
		synchronized (monitorsMapLockToken) {
			monitorsMap.put(className, new WeakReference<EmitterMonitor>(this));
		}
	}
	
	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void deInit() {
		active = false;
	}

	@Override
	public void addAppender(AppenderID appenderID) throws InvalidConfigException {
        synchronized(appendersLockToken) {
        	appenders.add(appenderID);
        }	
	}
	
	static EmitterController lookUpEmitterMonitor(String className) {
		WeakReference<EmitterMonitor> monitorRef = null;
		
		synchronized (monitorsMapLockToken) {
			monitorRef = monitorsMap.get(className);
		}
		
		if (monitorRef != null) {
			EmitterMonitor monitor = monitorRef.get();
			return (monitor != null && monitor.isActive()) ? monitor : null;
		} else {
			return null;
		}
	}

	@Override
	public void emit(EmitterData data) {
		AppenderID[] appenderIDs = null;
		
		synchronized (appendersLockToken) {
			appenderIDs = appenders.toArray(new AppenderID[] {});
		}
		
		for (AppenderID id : appenderIDs) {
			Appender appender = Appender.getAppender(id);
			if (appender != null) {
				appender.outputData((EmitterDataImpl)data);
			}
		}
	}

	@Override
	public EmitterData initData() {
		return initData(null, System.currentTimeMillis());
	}

	@Override
	public EmitterData initData(String instanceName) {
		return initData(instanceName, System.currentTimeMillis());
	}

	@Override
	public EmitterData initData(long timestamp) {
		return initData(null, timestamp);
	}

	@Override
	public EmitterData initData(String instanceName, long timestamp) {
		return new EmitterDataImpl(name, instanceName, timestamp);
	}
}

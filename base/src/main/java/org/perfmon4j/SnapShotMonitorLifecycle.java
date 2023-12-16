package org.perfmon4j;

import org.perfmon4j.Appender.AppenderID;

public interface SnapShotMonitorLifecycle {
    public boolean isActive();
    public void deInit();
    public void addAppender(AppenderID appenderID) throws InvalidConfigException;
}

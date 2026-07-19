package org.perfmon4j;

import org.perfmon4j.POJOSnapShotRegistry.POJOInstance;
import org.perfmon4j.instrument.snapshot.GenerateSnapShotException;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.Bundle;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator.SnapShotPOJOLifecycle;

/**
 * Adapts a single POJO instance registered with the POJOSnapShotRegistry to
 * the SnapShotMonitor contract used by the ExternalAppender's per-session
 * snapshot subscriptions (the POJO analog of SnapShotProviderWrapper).
 *
 * Unlike POJOSnapShotMonitor, which samples every live instance of a POJO
 * class, this wrapper samples exactly one instance identified by
 * (pojoClassName, instanceName).
 *
 * The wrapped POJO may be weakly referenced; it can be deregistered or
 * garbage collected at any time.  When the instance is gone the wrapper
 * degrades gracefully: initSnapShot always returns a non-null SnapShotData
 * and takeSnapShot simply skips sampling, leaving the data without final
 * values.  A (re)registered instance is picked up on the next initSnapShot --
 * deliberately not mid-window, so counter-delta baselines stay valid.
 */
public class POJOSnapShotWrapper extends SnapShotMonitor {
    private final String pojoClassName;
    private final String instanceName;
    private final POJOSnapShotRegistry registry;
    private final Bundle bundle;
    /** The instance being sampled for the current window; bound in initSnapShot. */
    private POJOInstance currentInstance = null;

    public POJOSnapShotWrapper(String name, String pojoClassName, String instanceName,
        POJOSnapShotRegistry registry) throws GenerateSnapShotException {
        super(name);
        this.pojoClassName = pojoClassName;
        this.instanceName = instanceName;
        this.registry = registry;

        this.bundle = registry.lookupSnapShotBundle(pojoClassName);
        if (this.bundle == null) {
            throw new GenerateSnapShotException("No POJO snapshot class registered for: " + pojoClassName);
        }
    }

    @Override
    public SnapShotData initSnapShot(long currentTimeMillis) {
        SnapShotData result = bundle.newSnapShotData();

        currentInstance = registry.getInstance(pojoClassName, instanceName);
        Object pojo = currentInstance != null ? currentInstance.getItem() : null;
        if (pojo != null) {
            ((SnapShotGenerator.SnapShotLifecycle)result).init(pojo, currentTimeMillis);
        }
        if (instanceName != null) {
            ((SnapShotPOJOLifecycle)result).setInstanceName(instanceName);
        }

        return result;
    }

    @Override
    public SnapShotData takeSnapShot(SnapShotData data, long currentTimeMillis) {
        Object pojo = currentInstance != null ? currentInstance.getItem() : null;
        if (pojo != null) {
            ((SnapShotGenerator.SnapShotLifecycle)data).takeSnapShot(pojo, currentTimeMillis);
        }
        return data;
    }
}

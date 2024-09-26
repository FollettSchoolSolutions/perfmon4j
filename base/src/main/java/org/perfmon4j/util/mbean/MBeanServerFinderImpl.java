package org.perfmon4j.util.mbean;

import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;

import org.perfmon4j.util.Cacheable;
import org.perfmon4j.util.MiscHelper;

public class MBeanServerFinderImpl implements MBeanServerFinder {
	private static final long CACHE_MILLIS = Long.getLong(MBeanServerFinder.class.getName() + ".CACHE_MILLIS", TimeUnit.MINUTES.toMillis(5));
	private final Cacheable<MBeanServer> cachedMBeanServer;
	
	public MBeanServerFinderImpl(String jmxDomain) throws MBeanQueryException {
		cachedMBeanServer = new Cacheable<>(() -> MBeanServerFinderImpl.findMBeanServer(jmxDomain), CACHE_MILLIS);
		
		getMBeanServer(); // Call now so we fail sooner, rather than later, if we can't find mBeanServer for jmxDomain;
	}

	@Override
	public MBeanServer getMBeanServer() throws MBeanQueryException {
		try {
			return cachedMBeanServer.get();
		} catch (MBeanQueryException e) {
			throw e;
		} catch (Exception e) {
			throw new MBeanQueryException(e);
		}
	}
	
	private static MBeanServer findMBeanServer(String jmxDomain) throws MBeanQueryException {
		MBeanServer result = MiscHelper.findMBeanServer(jmxDomain);
		if (result == null) {
			throw new MBeanQueryException("Unable to find mBeanServer matching domain: " + jmxDomain);
		}
		return result;
	}
}

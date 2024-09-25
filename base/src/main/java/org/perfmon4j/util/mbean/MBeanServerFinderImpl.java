package org.perfmon4j.util.mbean;

import java.lang.ref.SoftReference;

import javax.management.MBeanServer;

import org.perfmon4j.util.MiscHelper;

public class MBeanServerFinderImpl implements MBeanServerFinder {
	private final String jmxDomain;
	private SoftReference<MBeanServer> mBeanServerReference = null; 

	public MBeanServerFinderImpl(String jmxDomain) throws MBeanQueryException {
		this.jmxDomain = jmxDomain;
		// Do this so we find out on construction that we can't find the mBeanSever.
		getMBeanServer();
	}

	@Override
	public synchronized MBeanServer getMBeanServer() throws MBeanQueryException {
		MBeanServer result = null;
		
		if (mBeanServerReference == null || (result = mBeanServerReference.get()) == null) {
			result = MiscHelper.findMBeanServer(jmxDomain);
			if (result == null) {
				throw new MBeanQueryException("Unable to find mBeanServer matching domain: " + jmxDomain);
			}
			mBeanServerReference = new SoftReference<MBeanServer>(result);
		}
		return result;
	}
}

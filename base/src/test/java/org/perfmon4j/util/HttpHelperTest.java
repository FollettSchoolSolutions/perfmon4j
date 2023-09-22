package org.perfmon4j.util;

import junit.framework.TestCase;

public class HttpHelperTest extends TestCase {

	public void testDefaultConnectTimeout() {
		HttpHelper helper = new HttpHelper();
		assertEquals(5000, helper.getConnectTimeoutMillis());
	}
	
	public void testOverrideDefaultConnectTimeout() {
		String systemPropertyKey = "org.perfmon4j.util.HttpHelper.defaultConnectTimeoutMillis";
		try {
			System.setProperty(systemPropertyKey, "10000");
			HttpHelper helper = new HttpHelper();
			assertEquals(10000, helper.getConnectTimeoutMillis());
			
		} finally {
			System.getProperties().remove(systemPropertyKey);
		}
	}

	public void testDefaultReadTimeout() {
		HttpHelper helper = new HttpHelper();
		assertEquals(5000, helper.getReadTimeoutMillis());
	}
	
	public void testOverrideDefaultReadTimeout() {
		String systemPropertyKey = "org.perfmon4j.util.HttpHelper.defaultReadTimeoutMillis";
		try {
			System.setProperty(systemPropertyKey, "10000");
			HttpHelper helper = new HttpHelper();
			assertEquals(10000, helper.getReadTimeoutMillis());
			
		} finally {
			System.getProperties().remove(systemPropertyKey);
		}
	}
}

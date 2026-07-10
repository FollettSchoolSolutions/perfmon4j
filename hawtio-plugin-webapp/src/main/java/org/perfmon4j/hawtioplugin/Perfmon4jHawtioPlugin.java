package org.perfmon4j.hawtioplugin;

public class Perfmon4jHawtioPlugin implements Perfmon4jHawtioPluginMBean {
	private final String url;

	public Perfmon4jHawtioPlugin(String url) {
		this.url = url;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public String getScope() {
		// Must match the "name" passed to ModuleFederationPlugin in hawtio-plugin/webpack.config.js
		return "perfmon4jHawtioPlugin";
	}

	@Override
	public String getModule() {
		// Must match the key under "exposes" in hawtio-plugin/webpack.config.js
		return "./plugin";
	}
}

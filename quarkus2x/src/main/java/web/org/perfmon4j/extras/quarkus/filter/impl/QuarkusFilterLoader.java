package web.org.perfmon4j.extras.quarkus.filter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import web.org.perfmon4j.extras.genericfilter.FilterParams;
import web.org.perfmon4j.extras.genericfilter.GenericFilter;
import web.org.perfmon4j.extras.genericfilter.GenericFilterAsyncLoader;

public class QuarkusFilterLoader extends GenericFilterAsyncLoader {
	public QuarkusFilterLoader() {
		super("Perfmon4j Rest Filter for Quarkus");
	}

	private static final Logger logger = LoggerFactory.getLogger("org.perfmon4j." + QuarkusFilterLoader.class.getSimpleName());

	@Override
	protected GenericFilter initGenericFilter(FilterParams params) {
		return new QuarkusFilter(params);
	}

	@Override
	protected void logInfo(String value) {
		logger.info(value);
	}

}

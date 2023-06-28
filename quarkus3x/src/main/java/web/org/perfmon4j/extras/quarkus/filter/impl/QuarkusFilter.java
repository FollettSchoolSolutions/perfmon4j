package web.org.perfmon4j.extras.quarkus.filter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import web.org.perfmon4j.extras.genericfilter.FilterParams;
import web.org.perfmon4j.extras.genericfilter.GenericFilter;

public class QuarkusFilter extends GenericFilter {
	private static final Logger logger = LoggerFactory.getLogger("org.perfmon4j." + QuarkusFilter.class.getSimpleName());
	
	protected QuarkusFilter(FilterParams params) {
		super(params);
	}

	@Override
	protected void logInfo(String value) {
		logger.info(value);
	}

	@Override
	protected void logInfo(String value, Exception ex) {
		logger.info(value, ex);
	}
}

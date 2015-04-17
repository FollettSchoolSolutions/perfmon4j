package org.perfmon4j.restdatasource;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/rest")
public class RestApp extends Application {
	private Set<Object> singletons = new HashSet<Object>();
	 
	public RestApp() {
		singletons.add(new RestImpl());
	}
 
	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}

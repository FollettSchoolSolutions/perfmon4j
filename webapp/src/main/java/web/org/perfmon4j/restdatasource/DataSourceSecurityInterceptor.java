package web.org.perfmon4j.restdatasource;

import java.io.IOException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


@Provider
@ServerInterceptor
public class DataSourceSecurityInterceptor implements ContainerRequestFilter  {
	private static final Logger logger = LoggerFactory.initLogger(DataSourceSecurityInterceptor.class);
	
	private static SecuritySettings securitySettings;
	
	static public SecuritySettings setSecuritySettings(SecuritySettings newSecuritySettings) {
		SecuritySettings result = securitySettings;
		
		securitySettings = newSecuritySettings;
		
		return result;
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
		if (methodInvoker.getResourceClass().equals(DataSourceRestImpl.class)) {
			
			SecuritySettings currentSettings = securitySettings; // Get a copy, don't want it to change out from under us.
			if (currentSettings == null) {
				logger.logError("Security settings for DataSourceSecurityInterceptor have not been initialized");
				throw new NotAuthorizedException("Perfmon4j data source access is not enabled", "OAuth2.0 token");
			} else if (!currentSettings.isEnabled()) {
				throw new NotAuthorizedException("Perfmon4j data source access is not enabled", "OAuth2.0 token");
			} else {
				if (!currentSettings.isAnonymousAllowed()) {
					throw new NotAuthorizedException("Perfmon4j data source access is not enabled", "OAuth2.0 token");
				}
			}
		}
	}

	public interface SecuritySettings {
		public boolean isEnabled();
		public boolean isAnonymousAllowed();
		public String getSecret(String oauthKey);
	}
}

package web.org.perfmon4j.restdatasource;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

import web.org.perfmon4j.restdatasource.oauth2.OauthTokenHelper;


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
				throw new NotAuthorizedException("Perfmon4j data source access is not enabled", "Bearer", "error=\"data source not initialized\"");
			} else if (!currentSettings.isEnabled()) {
				throw new NotAuthorizedException("Perfmon4j data source access is not enabled", "Bearer", "error=\"data source not enabled\"");
			} else {
				// Check to see if we have an Authorization header
				String bearerToken = getBearerToken(requestContext);
				if (bearerToken != null) {
					final String publicDeniedMessage = "Bearer token expired and/or invalid";
					final String tokenDenied = " (" + bearerToken + "), Access denied";

					String oauthKey = OauthTokenHelper.getOauthKey(bearerToken);
					if (oauthKey == null) {
						logger.logError("Invalid bearer token format:" + tokenDenied);
						throw new NotAuthorizedException(publicDeniedMessage, "Bearer", "error=\"invalid token format\"");
					}

					OauthTokenHelper helper = currentSettings.getTokenHelper(oauthKey);
					if (helper == null) {
						logger.logError("Ouath key not found for bearer token:" + tokenDenied);
						throw new NotAuthorizedException(publicDeniedMessage, "Bearer", "error=\"oauth key not found\"");
					}
					
					if (!helper.validateBasicToken(bearerToken)) {
						logger.logError("Bearer token failed validation:" + tokenDenied);
						throw new NotAuthorizedException(publicDeniedMessage, "Bearer", "error=\"token validation failed\"");
					}
					// Valid.
				} else if (!currentSettings.isAnonymousAllowed()) {
					throw new NotAuthorizedException("Perfmon4j data source access is not enabled", "OAuth2.0 token");
				}
			}
		}
	}

	private String getBearerToken(ContainerRequestContext requestContext) {
		String result = null;
		String authHeader = requestContext.getHeaderString("Authorization");
		if (authHeader != null) {
			authHeader = authHeader.trim();
			String parts[] = authHeader.split("\\s+");
			if (parts.length == 2 && "Bearer".equalsIgnoreCase(parts[0])) {
				result = parts[1];
			}
		}

		if (result == null ) {
			result = getQueryParameter(requestContext, "access_token");
		}

		return result;
	}
	
	
	private String getQueryParameter(ContainerRequestContext requestContext, String parameterName) {
		String result = null;
		
		UriInfo uriInfo = requestContext.getUriInfo();
		if (uriInfo != null) {
			MultivaluedMap<String, String> map =  uriInfo.getQueryParameters(false);
			if (map != null) {
				List<String> params = map.get(parameterName);
				if (params != null && !params.isEmpty()) {
					result = "";
					for (String s : params) {
						result += s;
					}
				}
			}
		}
		
		return result;
	}
	
	
	public interface SecuritySettings {
		public boolean isEnabled();
		public boolean isAnonymousAllowed();
		public OauthTokenHelper getTokenHelper(String oauthKey);
	}
}

package war.org.perfmon4j.restdatasource.filters;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;

public final class CorsFilter implements Filter {
    private static final Logger logger = LoggerFactory.initLogger(CorsFilter.class);
    
    protected static enum CORSRequestType {
        SIMPLE,
        ACTUAL,
        PRE_FLIGHT,
        NOT_CORS,
        INVALID_CORS
    }
    
    public static final String REQUEST_HEADER_ORIGIN = "Origin";
    public static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";    
    
    public static final String HTTP_REQUEST_ATTRIBUTE_ORIGIN = "cors.request.origin";
    public static final String HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST = "cors.isCorsRequest";
    public static final String HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE = "cors.request.type";
    public static final String HTTP_REQUEST_ATTRIBUTE_REQUEST_HEADERS = "cors.request.headers";    
    
    public static final String PARAM_CORS_ALLOWED_ORIGINS = "cors.allowed.origins";
    public static final String DEFAULT_ALLOWED_ORIGINS = "*";

    public static final String PARAM_CORS_SUPPORT_CREDENTIALS = "cors.support.credentials";
    public static final String DEFAULT_SUPPORTS_CREDENTIALS = "true";
    
    public static final String PARAM_CORS_ALLOWED_METHODS = "cors.allowed.methods";
    public static final String DEFAULT_ALLOWED_METHODS = "GET,POST,HEAD,OPTIONS";
    
    public static final String PARAM_CORS_ALLOWED_HEADERS = "cors.allowed.headers";
    public static final String DEFAULT_ALLOWED_HEADERS =
            "Origin,Accept,X-Requested-With,Content-Type," +
            "Access-Control-Request-Method,Access-Control-Request-Headers";

    public static final String PARAM_CORS_PREFLIGHT_MAXAGE = "cors.preflight.maxage";
    public static final String DEFAULT_PREFLIGHT_MAXAGE = "1800";
    
    public static final String PARAM_CORS_EXPOSED_HEADERS = "cors.exposed.headers";
    public static final String DEFAULT_EXPOSED_HEADERS = "";
    
    public static final String PARAM_CORS_REQUEST_DECORATE = "cors.request.decorate";
    public static final String DEFAULT_REQUEST_DECORATE = "true";
    
    private final Collection<String> allowedOrigins;
    private boolean anyOriginAllowed;
    private final Collection<String> allowedHttpMethods;
    private final Collection<String> allowedHttpHeaders;
    private final Collection<String> exposedHeaders;
    private boolean supportsCredentials;
    private long preflightMaxAge;
    private boolean decorateRequest;

    // -----------------------------------------------------------------------------------------------------------------------
    public CorsFilter() {
        this.allowedOrigins = new HashSet<String>();
        this.allowedHttpMethods = new HashSet<String>();
        this.allowedHttpHeaders = new HashSet<String>();
        this.exposedHeaders = new HashSet<String>();
    }

    // -----------------------------------------------------------------------------------------------------------------------
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // Initialize defaults
        parseAndStore(DEFAULT_ALLOWED_ORIGINS, DEFAULT_ALLOWED_METHODS,
                DEFAULT_ALLOWED_HEADERS, DEFAULT_EXPOSED_HEADERS,
                DEFAULT_SUPPORTS_CREDENTIALS, DEFAULT_PREFLIGHT_MAXAGE,
                DEFAULT_REQUEST_DECORATE);

        if (filterConfig != null) {
            String configAllowedOrigins = getInitParameter(filterConfig, PARAM_CORS_ALLOWED_ORIGINS);
            String configAllowedHttpMethods = getInitParameter(filterConfig, PARAM_CORS_ALLOWED_METHODS);
            String configAllowedHttpHeaders = getInitParameter(filterConfig, PARAM_CORS_ALLOWED_HEADERS);
            String configExposedHeaders = getInitParameter(filterConfig, PARAM_CORS_EXPOSED_HEADERS);
            String configSupportsCredentials = getInitParameter(filterConfig, PARAM_CORS_SUPPORT_CREDENTIALS);
            String configPreflightMaxAge = getInitParameter(filterConfig, PARAM_CORS_PREFLIGHT_MAXAGE);
            String configDecorateRequest = getInitParameter(filterConfig, PARAM_CORS_REQUEST_DECORATE);

            parseAndStore(configAllowedOrigins, configAllowedHttpMethods,
                    configAllowedHttpHeaders, configExposedHeaders,
                    configSupportsCredentials, configPreflightMaxAge,
                    configDecorateRequest);                       
        }          
    }


    // -----------------------------------------------------------------------------------------------------------------------
    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest) ||
                !(servletResponse instanceof HttpServletResponse)) {
            throw new ServletException("Only support with HTTP protocol");
        }

        // Safe to downcast at this point.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Determines the CORS request type.
        CorsFilter.CORSRequestType requestType = checkRequestType(request);

        // Adds CORS specific attributes to request.
        if (decorateRequest) {
            CorsFilter.decorateCORSProperties(request, requestType);
        }
        
        switch (requestType) {
        case SIMPLE:
            this.handleSimpleCORS(request, response, filterChain);
            break;
        case ACTUAL:
            this.handleSimpleCORS(request, response, filterChain);
            break;
        case PRE_FLIGHT:
            this.handlePreflightCORS(request, response, filterChain);
            break;
        case NOT_CORS:
            this.handleNonCORS(request, response, filterChain);
            break;
        default:
            this.handleInvalidCORS(request, response, filterChain);
            break;
        }
    }


    // -----------------------------------------------------------------------------------------------------------------------
    private String getInitParameter(FilterConfig config, String key) {
        return System.getProperty(key, config.getInitParameter(key));
    }
    
    // -----------------------------------------------------------------------------------------------------------------------
    protected void handleSimpleCORS(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws IOException, ServletException {
        CorsFilter.CORSRequestType requestType = checkRequestType(request);
        if (!(requestType == CorsFilter.CORSRequestType.SIMPLE || requestType == CorsFilter.CORSRequestType.ACTUAL)) {
            throw new IllegalArgumentException("Wrong CORS type " + requestType);
        }

        final String origin = request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);
        final String method = request.getMethod();

        // Section 6.1.2
        if (!isOriginAllowed(origin)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        if (!allowedHttpMethods.contains(method)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        // Section 6.1.3
        // Add a single Access-Control-Allow-Origin header.
        if (anyOriginAllowed && !supportsCredentials) {
            // If resource doesn't support credentials and if any origin is
            // allowed
            // to make CORS request, return header with '*'.
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        } else {
            // If the resource supports credentials add a single
            // Access-Control-Allow-Origin header, with the value of the Origin
            // header as value.
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }

        // Section 6.1.3
        // If the resource supports credentials, add a single
        // Access-Control-Allow-Credentials header with the case-sensitive
        // string "true" as value.
        if (supportsCredentials) {
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        // Section 6.1.4
        // If the list of exposed headers is not empty add one or more
        // Access-Control-Expose-Headers headers, with as values the header
        // field names given in the list of exposed headers.
        if ((exposedHeaders != null) && (exposedHeaders.size() > 0)) {
            String exposedHeadersString = join(exposedHeaders, ",");
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeadersString);
        }

        // Forward the request down the filter chain.
        filterChain.doFilter(request, response);
    }


    // -----------------------------------------------------------------------------------------------------------------------
    protected void handlePreflightCORS(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws IOException, ServletException {

        CORSRequestType requestType = checkRequestType(request);
        if (requestType != CORSRequestType.PRE_FLIGHT) {
            throw new IllegalArgumentException("Request type expected to be preflight");
        }

        final String origin = request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);

        // Section 6.2.2
        if (!isOriginAllowed(origin)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        // Section 6.2.3
        String accessControlRequestMethod = request.getHeader(CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
        if (accessControlRequestMethod == null || !HTTP_METHODS.contains(accessControlRequestMethod.trim())) {
            handleInvalidCORS(request, response, filterChain);
            return;
        } else {
            accessControlRequestMethod = accessControlRequestMethod.trim();
        }

        // Section 6.2.4
        String accessControlRequestHeadersHeader = request.getHeader(CorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
        List<String> accessControlRequestHeaders = new LinkedList<String>();
        if (accessControlRequestHeadersHeader != null && !accessControlRequestHeadersHeader.trim().isEmpty()) {
            String[] headers = accessControlRequestHeadersHeader.trim().split(",");
            for (String header : headers) {
                accessControlRequestHeaders.add(header.trim().toLowerCase());
            }
        }

        // Section 6.2.5
        if (!allowedHttpMethods.contains(accessControlRequestMethod)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        // Section 6.2.6
        if (!accessControlRequestHeaders.isEmpty()) {
            for (String header : accessControlRequestHeaders) {
                if (!allowedHttpHeaders.contains(header)) {
                    handleInvalidCORS(request, response, filterChain);
                    return;
                }
            }
        }

        // Section 6.2.7
        if (supportsCredentials) {
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            if (anyOriginAllowed) {
                response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            } else {
                response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
        }

        // Section 6.2.8
        if (preflightMaxAge > 0) {
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE, String.valueOf(preflightMaxAge));
        }

        // Section 6.2.9
        response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS, accessControlRequestMethod);

        // Section 6.2.10
        if ((allowedHttpHeaders != null) && (!allowedHttpHeaders.isEmpty())) {
            response.addHeader(CorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS, join(allowedHttpHeaders, ","));
        }

        // Do not forward the request down the filter chain.
    }

    // -----------------------------------------------------------------------------------------------------------------------
    private void handleNonCORS(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws IOException, ServletException {
        // Let request pass.
        filterChain.doFilter(request, response);
    }

    // -----------------------------------------------------------------------------------------------------------------------
    private void handleInvalidCORS(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) {
        String origin = request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);
        String method = request.getMethod();
        String accessControlRequestHeaders = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);

        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.resetBuffer();

        if (logger.isDebugEnabled()) {
            StringBuilder message = new StringBuilder("Invalid CORS request; Origin=");
            message.append(origin);
            message.append(";Method=");
            message.append(method);
            if (accessControlRequestHeaders != null) {
                message.append(";Access-Control-Request-Headers=");
                message.append(accessControlRequestHeaders);
            }
            logger.logDebug(message.toString());
        }
    }


    // -----------------------------------------------------------------------------------------------------------------------
    @Override
    public void destroy() {
        // NOOP
    }

    // -----------------------------------------------------------------------------------------------------------------------
    protected static void decorateCORSProperties( final HttpServletRequest request, final CORSRequestType corsRequestType) {
        if (request == null) {
            throw new IllegalArgumentException("Request was null");
        }

        if (corsRequestType == null) {
            throw new IllegalArgumentException("Request type was null");
       }

        switch (corsRequestType) {
        case SIMPLE:
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST, Boolean.TRUE);
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_ORIGIN, request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN));
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE, corsRequestType.name().toLowerCase());
            break;
        case ACTUAL:
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST, Boolean.TRUE);
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_ORIGIN, request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN));
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE, corsRequestType.name().toLowerCase());
            break;
        case PRE_FLIGHT:
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST, Boolean.TRUE);
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_ORIGIN, request.getHeader(CorsFilter.REQUEST_HEADER_ORIGIN));
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE,corsRequestType.name().toLowerCase());
            
            String headers = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
            if (headers == null) {
                headers = "";
            }
            
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_HEADERS, headers);
            break;
        case NOT_CORS:
            request.setAttribute(CorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST, Boolean.FALSE);
            break;
        default:
            break;
        }
    }


    // -----------------------------------------------------------------------------------------------------------------------
    protected static String join(final Collection<String> elements, final String joinSeparator) {
        String separator = ",";
        if (elements == null) {
            return null;
        }
        if (joinSeparator != null) {
            separator = joinSeparator;
        }
        StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (String element : elements) {
            if (!isFirst) {
                buffer.append(separator);
            } else {
                isFirst = false;
            }

            if (element != null) {
                buffer.append(element);
            }
        }

        return buffer.toString();
    }


    // -----------------------------------------------------------------------------------------------------------------------
    protected CORSRequestType checkRequestType(final HttpServletRequest request) {
        CORSRequestType requestType = CORSRequestType.INVALID_CORS;
        
        if (request == null) {
            throw new IllegalArgumentException("Request was null");
        }
        String originHeader = request.getHeader(REQUEST_HEADER_ORIGIN);
        
        // Section 6.1.1 and Section 6.2.1
        if (originHeader != null) {
            if (originHeader.isEmpty()) {
                requestType = CORSRequestType.INVALID_CORS;
            } else if (!isValidOrigin(originHeader)) {
                requestType = CORSRequestType.INVALID_CORS;
            } else {
                String method = request.getMethod();
                if (method != null && HTTP_METHODS.contains(method)) {
                    if ("OPTIONS".equals(method)) {
                        String accessControlRequestMethodHeader = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                        if (accessControlRequestMethodHeader != null && !accessControlRequestMethodHeader.isEmpty()) {
                            requestType = CORSRequestType.PRE_FLIGHT;
                        } else if (accessControlRequestMethodHeader != null && accessControlRequestMethodHeader.isEmpty()) {
                            requestType = CORSRequestType.INVALID_CORS;
                        } else {
                            requestType = CORSRequestType.ACTUAL;
                        }
                    } else if ("GET".equals(method) || "HEAD".equals(method)) {
                        requestType = CORSRequestType.SIMPLE;
                    } else if ("POST".equals(method)) {
                        String contentType = request.getContentType();
                        if (contentType != null) {
                            contentType = contentType.toLowerCase().trim();
                            if (SIMPLE_HTTP_REQUEST_CONTENT_TYPE_VALUES.contains(contentType)) {
                                requestType = CORSRequestType.SIMPLE;
                            } else {
                                requestType = CORSRequestType.ACTUAL;
                            }
                        }
                    } else if (COMPLEX_HTTP_METHODS.contains(method)) {
                        requestType = CORSRequestType.ACTUAL;
                    }
                }
            }
        } else {
            requestType = CORSRequestType.NOT_CORS;
        }

        return requestType;
    }


    // -----------------------------------------------------------------------------------------------------------------------
    private boolean isOriginAllowed(final String origin) {
        if (anyOriginAllowed) {
            return true;
        }

        // If 'Origin' header is a case-sensitive match of any of allowed
        // origins, then return true, else return false.
        return allowedOrigins.contains(origin);
    }


    // -----------------------------------------------------------------------------------------------------------------------
    private void parseAndStore(final String allowedOrigins, final String allowedHttpMethods, final String allowedHttpHeaders, final String exposedHeaders, final String supportsCredentials, final String preflightMaxAge, final String decorateRequest) throws ServletException {
        if (allowedOrigins != null) {
            if (allowedOrigins.trim().equals("*")) {
                this.anyOriginAllowed = true;
            } else {
                this.anyOriginAllowed = false;
                Set<String> setAllowedOrigins = parseStringToSet(allowedOrigins);
                this.allowedOrigins.clear();
                this.allowedOrigins.addAll(setAllowedOrigins);
            }
        }

        if (allowedHttpMethods != null) {
            Set<String> setAllowedHttpMethods = parseStringToSet(allowedHttpMethods);
            this.allowedHttpMethods.clear();
            this.allowedHttpMethods.addAll(setAllowedHttpMethods);
        }

        if (allowedHttpHeaders != null) {
            Set<String> setAllowedHttpHeaders = parseStringToSet(allowedHttpHeaders);
            Set<String> lowerCaseHeaders = new HashSet<String>();
            for (String header : setAllowedHttpHeaders) {
                String lowerCase = header.toLowerCase();
                lowerCaseHeaders.add(lowerCase);
            }
            this.allowedHttpHeaders.clear();
            this.allowedHttpHeaders.addAll(lowerCaseHeaders);
        }

        if (exposedHeaders != null) {
            Set<String> setExposedHeaders = parseStringToSet(exposedHeaders);
            this.exposedHeaders.clear();
            this.exposedHeaders.addAll(setExposedHeaders);
        }

        if (supportsCredentials != null) {
            // For any value other then 'true' this will be false.
            this.supportsCredentials = Boolean.parseBoolean(supportsCredentials);
        }

        if (preflightMaxAge != null) {
            try {
                if (!preflightMaxAge.isEmpty()) {
                    this.preflightMaxAge = Long.parseLong(preflightMaxAge);
                } else {
                    this.preflightMaxAge = 0L;
                }
            } catch (NumberFormatException e) {
                throw new ServletException("Invalid preflight max-age", e);
            }
        }

        if (decorateRequest != null) {
            // For any value other then 'true' this will be false.
            this.decorateRequest = Boolean.parseBoolean(decorateRequest);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------
    private Set<String> parseStringToSet(final String data) {
        String[] splits;

        if (data != null && data.length() > 0) {
            splits = data.split(",");
        } else {
            splits = new String[] {};
        }

        Set<String> set = new HashSet<String>();
        if (splits.length > 0) {
            for (String split : splits) {
                set.add(split.trim());
            }
        }

        return set;
    }


    // -----------------------------------------------------------------------------------------------------------------------
    protected static boolean isValidOrigin(String origin) {
        // Checks for encoded characters. Helps prevent CRLF injection.
        if (origin.contains("%")) {
            return false;
        }

        URI originURI;

        try {
            originURI = new URI(origin);
        } catch (URISyntaxException e) {
            return false;
        }
        // If scheme for URI is null, return false. Return true otherwise.
        return originURI.getScheme() != null;

    }


    // -----------------------------------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------------
    // Restricted values
    public static final Collection<String> HTTP_METHODS =
            new HashSet<String>(Arrays.asList("OPTIONS", "GET", "HEAD", "POST",
                    "PUT", "DELETE", "TRACE", "CONNECT"));

    public static final Collection<String> COMPLEX_HTTP_METHODS =
            new HashSet<String>(Arrays.asList("PUT", "DELETE", "TRACE",
                    "CONNECT"));

    public static final Collection<String> SIMPLE_HTTP_METHODS =
            new HashSet<String>(Arrays.asList("GET", "POST", "HEAD"));

    public static final Collection<String> SIMPLE_HTTP_REQUEST_HEADERS =
            new HashSet<String>(Arrays.asList("Accept", "Accept-Language",
                    "Content-Language"));

    public static final Collection<String> SIMPLE_HTTP_RESPONSE_HEADERS =
            new HashSet<String>(Arrays.asList("Cache-Control",
                    "Content-Language", "Content-Type", "Expires",
                    "Last-Modified", "Pragma"));

    public static final Collection<String> SIMPLE_HTTP_REQUEST_CONTENT_TYPE_VALUES =
            new HashSet<String>(Arrays.asList(
                    "application/x-www-form-urlencoded",
                    "multipart/form-data", "text/plain"));

}

package web.org.perfmon4j.extras.genericfilter;

public interface HttpRequest {
	public String getServletPath();
	public String getMethod();
	public String getQueryString();
}

package web.org.perfmon4j.extras.genericfilter;

import java.util.List;

public interface HttpRequest {
	public String getServletPath();
	public String getMethod();
	public String getQueryString();
	public List<String> getQueryParameter(String name);
	public Object getSessionAttribute(String name);
	public String getCookieValue(String name);
}

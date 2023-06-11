package web.org.perfmon4j.extras.genericfilter;

public interface HttpResponse {
	public int getStatus();
	public String getHeader(String name);
}

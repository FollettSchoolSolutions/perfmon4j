package web.org.perfmon4j.extras.genericfilter;

public interface HttpRequestChain {
	public void next(HttpRequest request, HttpResponse response, HttpRequestChain chain);
}

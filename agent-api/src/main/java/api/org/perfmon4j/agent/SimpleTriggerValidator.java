package api.org.perfmon4j.agent;

public interface SimpleTriggerValidator {
	/**
	 * @param triggerString
	 * 
	 * Will be one of the following:
	 * 	For an HTTP Request Trigger, which should be validated against query parameters:
	 * 		"HTTP:<queryParameterName>=<queryParameterValue>"
	 * 	For an HTTP Session Trigger, which should be validated against query parameters:
	 * 		"HTTP_SESSION:<sessionAttributeName>=<sessionAttributeValue>"
	 * 	For an HTTP Cookie Trigger, which should be validated against query parameters:
	 * 		"HTTP_COOKIE:<cookieName>=<cookieValue>"
	 * @return
	 */
	public boolean isValid(String triggerString);
}

package org.perfmon4j.util.mbean;

public class MBeanQueryException extends Exception {
	private static final long serialVersionUID = 7224617077790953780L;

	public MBeanQueryException() {
	}

	public MBeanQueryException(String message) {
		super(message);
	}

	public MBeanQueryException(Throwable cause) {
		super(cause);
	}

	public MBeanQueryException(String message, Throwable cause) {
		super(message, cause);
	}
}

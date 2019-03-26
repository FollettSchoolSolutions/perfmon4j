package demo.simple.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

public class EchoCommand extends HystrixCommand<String> {
	private final String whatToEcho;

	public EchoCommand(String whatToEcho) {
		super(HystrixCommand.Setter
			.withGroupKey(HystrixCommandGroupKey.Factory.asKey("simple.EchoCommandGroup"))
			.andCommandKey(HystrixCommandKey.Factory.asKey("simple.EchoCommand"))
		);
		this.whatToEcho = whatToEcho;
	}

	@Override
	protected String run() throws Exception {
		if ("bad".equals(whatToEcho)) {
			throw new RuntimeException("This is bad... It threw a runtime exception");
		}
			
		return "echo=" + whatToEcho;
	}

	@Override
	protected String getFallback() {
		return "Echo service is not available at this time.  Please try again.";
	}
	
}


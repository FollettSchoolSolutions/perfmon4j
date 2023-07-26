package web.org.perfmon4j.extras.genericfilter;

import java.util.Stack;

import org.mockito.Mockito;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;


public class GenericFilterTestImpl extends GenericFilter {
	private final Logger logger = LoggerFactory.initLogger(GenericFilterTestImpl.class);

	protected GenericFilterTestImpl() {
		super(new FilterParamsVO());
	}
	
	protected GenericFilterTestImpl(FilterParams params) {
		super(params);
	}

	@Override
	protected void logInfo(String value) {
		logger.logInfo(value);
	}

	@Override
	protected void logInfo(String value, Exception ex) {
		logger.logInfo(value, ex);
	}
	
    boolean isPerfMonConfigured() {
    	return PerfMon.isConfigured();
    }
    
//    private Map<api.org.perfmon4j.agent.PerfMonTimer, PerfMonTimer> timerMap = 
//    		new HashMap<>();
    private Stack<PerfMonTimer> timerStack = new Stack<>();

    
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
    api.org.perfmon4j.agent.PerfMonTimer startTimer(String category, boolean isDynamicTimer, String requestContext) {
    	PerfMonTimer realTimer = PerfMonTimer.start(category, true, requestContext);
    	
    	api.org.perfmon4j.agent.PerfMonTimer result = Mockito.mock(api.org.perfmon4j.agent.PerfMonTimer.class);
    	timerStack.push(realTimer);
    	
    	return result;
    }
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
	/* package */ void abortTimer( api.org.perfmon4j.agent.PerfMonTimer timer) {
    	PerfMonTimer.abort(timerStack.pop());
    }
    
    /**
     * Method will be overwritten by test class to help with unit testing.
     * @return
     */
	/* package */ void stopTimer( api.org.perfmon4j.agent.PerfMonTimer timer) {
    	PerfMonTimer.stop(timerStack.pop());
    }
}

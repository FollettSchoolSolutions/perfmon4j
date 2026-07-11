// Standalone target JVM for exercising the hawtio-plugin dev harness (`npm start`)
// against a real perfmon4j-attached JVM, without needing a WildFly/Hawtio deployment.
// See run.sh in this directory and hawtio-plugin/CLAUDE.md's "Local dev harness
// against a real JVM" section for how this fits together.
//
// Touching PerfMon.ROOT_MONITOR_NAME forces PerfMon's static initializer to run,
// which registers the org.perfmon4j.selfmanagement.SelfManagement MBean.
import org.perfmon4j.PerfMon;

public class TargetMain {
    public static void main(String[] args) throws Exception {
        System.out.println("Root monitor: " + PerfMon.ROOT_MONITOR_NAME);
        System.out.println("perfmon4j version: " + System.getProperty(PerfMon.PERFMON4J_VERSION));
        System.out.println("Target JVM ready - listening for Jolokia connections.");
        Thread.sleep(Long.MAX_VALUE);
    }
}

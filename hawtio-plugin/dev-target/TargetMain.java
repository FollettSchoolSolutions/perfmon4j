// Standalone target JVM for exercising the hawtio-plugin dev harness (`npm start`)
// against a real perfmon4j-attached JVM, without needing a WildFly/Hawtio deployment.
// See run.sh in this directory and hawtio-plugin/CLAUDE.md's "Local dev harness
// against a real JVM" section for how this fits together.
//
// Touching PerfMon.ROOT_MONITOR_NAME forces PerfMon's static initializer to run,
// which registers the org.perfmon4j.selfmanagement.SelfManagement and
// org.perfmon4j.remotemanagement.jmx.RemoteManagement MBeans.
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonTimer;

public class TargetMain {
    public static void main(String[] args) throws Exception {
        System.out.println("Root monitor: " + PerfMon.ROOT_MONITOR_NAME);
        System.out.println("perfmon4j version: " + System.getProperty(PerfMon.PERFMON4J_VERSION));

        // Drives a real INTERVAL monitor so the perfmon4j Chart nav item's monitor
        // picker (RemoteManagement.getMonitors()/getFieldsForMonitor()) has something
        // chartable to select. Deliberately does NOT touch
        // org.perfmon4j.remotemanagement.RemoteImpl.getSingleton() (which would
        // register SNAPSHOT-type monitors like JVMSnapShot) - doing so requires
        // Javassist on the classpath to generate snapshot bundles
        // (PerfMonTimerTransformer.snapShotGenerator), which this bare classes-only
        // classpath doesn't have; RemoteManagement.getMonitors() merges INTERVAL and
        // SNAPSHOT keys in one call with no per-source error isolation, so touching
        // that path here would break monitor listing entirely instead of just
        // omitting SNAPSHOT monitors. INTERVAL-only is sufficient to manually verify
        // the chart feature end-to-end.
        Thread loadThread = new Thread(() -> {
            while (true) {
                PerfMonTimer t = PerfMonTimer.start("dev.target.demo");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                } finally {
                    PerfMonTimer.stop(t);
                }
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();

        System.out.println("Target JVM ready - listening for Jolokia connections.");
        Thread.sleep(Long.MAX_VALUE);
    }
}

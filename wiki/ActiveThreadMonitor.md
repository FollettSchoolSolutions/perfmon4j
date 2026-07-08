ActiveThreadMonitor
===================

**Version Introduced:** 1.6.0-SNAPSHOT
**Class Name:** org.perfmon4j.util.ActiveThreadMonitor

Active thread monitoring provides an extension to Perfmon4j's existing interval monitoring. It allows you to track any currently outstanding threads that are currently active that have exceeded a defined duration. While most useful for WebRequest, it can be added to any interval duration.

## Configure the ActiveThreadMonitor in perfmonconfig.xml

Below is an example configuration file that will output information on currently active WebRequest threads that exceed 5 minutes, 30 minutes and 1 hour in duration. Both a count of threads, that exceed each of these durations, and the specific thread names will be output to the appender.

```xml
<Perfmon4JConfig enabled='true'>
	<appender name='text-appender' className='org.perfmon4j.TextAppender' interval='1 second'/>

	<monitor name='WebRequest'>
		<appender name='text-appender' pattern="."/>
		<attribute name='activeThreadMonitor'>5 minutes, 30 minutes, 1 hour</attribute>
	</monitor>
</Perfmon4JConfig>
```

## Notes

1. By providing the thread names of active, long running, threads you are provided an opportunity to perform a java thread dump to examine the running status of the thread. See [Capturing a Java Thread Dump](https://www.baeldung.com/java-thread-dump) for options to capture a thread dump.
2. Currently Perfmon4j's TextAppender, InfluxAppender, and Azure LogAnalyticsAppender support output of the activeThreadMonitor data. The Perfmon4j SQLAppender does not.
3. The overhead introduced to monitor active threads is minimal. However you can disable the functionality by starting your application with the following system property: `-Dorg.perfmon4j.MonitorThreadTracker.DisableThreadTracking=true`

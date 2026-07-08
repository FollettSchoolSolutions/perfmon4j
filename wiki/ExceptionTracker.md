ExceptionTracker
================

**Version Introduced:** 1.6.0-SNAPSHOT
**Class Name:** org.perfmon4j.ExceptionTracker

The ExceptionTracker provides monitoring of any Exception classes (actually any Class derived from java.lang.Throwable). To implement this you must do the following steps:

1. Launch your java program with the perfmon4j agent.
2. Configure the agent to load a perfmonconfig.xml configuration file.
3. Include an ExceptionTracker configuration in the `<boot>` element of the XML file.
4. Configure a SnapShot monitor to output the Exception Tracker data using your choice of Perfmon4j appender(s).

## Configure the ExceptionTracker in perfmonconfig.xml

Below is an example configuration file that will output exception counts to a TextAppender:

```xml
<Perfmon4JConfig enabled='true'>
	<boot>
                <!-- First select the Exception(s) that you want to track.  This is initialized at
                at system startup and can't be changed without a restart -->
		<exceptionTracker>
			<exception displayName='NullPointerEx' className='java.lang.NullPointerException'/>
		</exceptionTracker>
	</boot>
	<appender name='text-appender' className='org.perfmon4j.TextAppender' interval='1 second'/>

        <!-- Second configure the Snapshot monitor to output exceptions to one or more appenders
        If changed this will be dynamically updated during Runtime -->
	<snapShotMonitor name='ExceptionTracker' className='org.perfmon4j.ExceptionTracker'>
		<appender name='text-appender'/>
	</snapShotMonitor>
</Perfmon4JConfig>
```

## Notes

1. Perfmon4j can instrument any class derived from Throwable, including Throwable itself. While this can be done it is probably not wise. It is best to instrument known exceptions that are used to indicate failure conditions that you would benefit from tracking (i.e. java.net.ConnectException or com.microsoft.sqlserver.jdbc.SQLServerException).
2. The instrumentation of Exceptions occurs when a tracked exception object is created, not when it is thrown. This means constructing an Exception, but not throwing it, will still result in an occurrence count.

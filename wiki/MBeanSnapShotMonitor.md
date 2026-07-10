MBeanSnapShotMonitor
====================

**Version Introduced:** 2.1.1

The MBeanSnapShotMonitor can be used to monitor attributes on any JMX registered object. These monitors can be configured at runtime and do not require any code.

Syntax
------

Examples of mBeanSnapShotMonitor definitions in a perfmonconfig.xml file:

```xml
<mBeanSnapshotMonitor name='JVMThreading'
  jmxName='java.lang:type=ClassLoading'
  counters='TotalLoadedClassCount,UnloadedClassCount'
  gauges='LoadedClassCount'>
  <appender name='text-appender'/>
</mBeanSnapshotMonitor>

<mBeanSnapshotMonitor name='MemoryPool'
  jmxName='java.lang:type=MemoryPool'
  instanceKey='name'
  gauges='Usage.max,Usage.committed,Usage.used,Type'
  ratios='percentInUse=Usage.used/Usage.max'>
    <appender name='text-appender'/>
</mBeanSnapshotMonitor>
```

> More examples can be found [here](https://gist.github.com/ddeuchert/9c2adbfe4f64f5d5afa30c8b1b4dda96)

### Attributes

#### name
Required - Specify a unique name for each monitor. This name will be used to identify the measurements when they are collected and written to an appender.

#### jmxName
Required - Specify the full (or partial) JMX ObjectName used to identify the MBean. When a partial object name is specified the instanceKey must be provided.

#### instanceKey
Optional - Required when specifying a partial ObjectName. The instanceKey indicates the property key used to identify each instance. When this is used all other property keys _must_ be defined in the jmxName.

#### instanceValueFilter
Optional - Used along with instanceKey value to filter MBeans based on the ObjectName. For example, if you had the following MBeans: (`java.lang:name=G1 Eden Space,type=MemoryPool`, `java.lang:name=G1 Old Gen,type=MemoryPool`, `java.lang:name=G1 Survivor Space,type=MemoryPool`) and only want to monitor the OldGen and Eden memory spaces you could provide the following regular expression: `instanceValueFilter='G1 Eden Space|G1 Old Gen'`

#### attributeValueFilter
Optional - Used along with instanceKey value to filter MBeans based on the value of a specified MBean attribute. For example, the `java.lang:name=*,type=MemoryPool` MBeans have an attribute named `type`. The value of type is either `HEAP` or `NON_HEAP`. If you only want to monitor the HEAP instances you could include the following: `attributeValueFilter='type=HEAP'`

> The format of this attributeValueFilter is `<MBean Attribute Name>=<Regular expression used to match attribute value>`

#### gauges
Optional - A comma separated list with attributes associated with the mBean to be collected. Gauges represent simple 'one time' values. For example: current temperature. Gauges can be any numeric, string or object type.

#### counters
Optional - A comma separated list of mBean attributes to be collected and treated as an ever-increasing value. For example: total number of thread created since the JVM was started. Perfmon4j will display these measurements as a delta (current value - previous value). Although counters can be any numeric value they are typically defined as `long` values in mBeans.

#### ratios
Optional - Ratios represent simple math calculations of two attributes (one attribute divided by another attribute). Multiple ratios can be specified through a comma separated list. The format is: `<name>=<numerator>/<denominator>`. Example: `threadsInUse=activeTheads/totalThreads`.

Examples
--------

### Configure monitoring on a single JMX Object

The following example demonstrates how to monitor the ClassLoadingMBean provided by the JVM. Details regarding the MBean interface for this object can be found [here](https://docs.oracle.com/javase/8/docs/api/java/lang/management/ClassLoadingMXBean.html).

The JMX Object name of this mBean is: `java.lang:type=ClassLoading`

| Attribute | Type | Description |
|-----------|------|-------------|
| LoadedClassCount | int | Number of active classes currently loaded within the JVM |
| TotalLoadedClassCount | long | Total number of classes loaded since the JVM was started |
| UnloadedClassCount | long | Total number of classes unloaded since the JVM was started |

To monitor this object add the following to your perfmonconfig.xml:

```xml
<Perfmon4JConfig enabled='true'>
	<appender name='text-appender' className='org.perfmon4j.TextAppender' interval='1 minute'/>

	<mBeanSnapshotMonitor name='JVMThreading'
		jmxName='java.lang:type=ClassLoading'
		counters='TotalLoadedClassCount,UnloadedClassCount'
		gauges='LoadedClassCount'>
		<appender name='text-appender'/>
	</mBeanSnapshotMonitor>

</Perfmon4JConfig>
```

Below is example output that was written to stdout:

```
********************************************************************************
JVMThreading
14:36:00:000 -> 14:37:00:000
 UnloadedClassCount....... 0
 TotalLoadedClassCount.... 0
 LoadedClassCount......... 19104
********************************************************************************
```

### Configure monitoring on Multiple Associated JMX Objects

The following example demonstrates how to monitor instances of the MemoryPoolMXBean provided by the JVM. Details regarding the MBean interface for this object can be found [here](https://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryPoolMXBean.html).

There are many examples where there are multiple instances of JMX objects associated with a single class. One common example is `java.lang.management.MemoryPoolMXBean`. Examples of object names associated with these instances are:

* `java.lang:name=G1 Eden Space,type=MemoryPool`
* `java.lang:name=G1 Old Gen,type=MemoryPool`
* `java.lang:name=G1 Survivor Space,type=MemoryPool`

MBean names in Java are defined by a domain and a key property list. In the example above the domain is: `java.lang` and the key property list contains two keys/properties. The first key/property is shared by all instances: `type=MemoryPool`. The property for the second key `name` is unique for each instance (i.e. `name=G1 Eden Space`).

While you can individually specify each of these instances independently using the method described above, you can also specify multiple instances by separating the common part of the instance (domain + common key/property values) and defining the variable property (name) as the `instanceKey`.

The following configuration will capture data from each instance of `java.lang:type=MemoryPool`:

```xml
<Perfmon4JConfig enabled='true'>

	<appender name='text-appender' className='org.perfmon4j.TextAppender' interval='1 minute'/>

	<mBeanSnapshotMonitor name='MemoryPool'
		jmxName='java.lang:type=MemoryPool'
		instanceKey='name'
                attributeValueFilter='type=HEAP'
		gauges='Usage.max,Usage.committed,Usage.used,Type'
		ratios='percentInUse=Usage.used/Usage.max'>
		<appender name='text-appender'/>
	</mBeanSnapshotMonitor>

</Perfmon4JConfig>
```

Below is example output that was written to stdout:

```
********************************************************************************
MemoryPool
14:23:00:001 -> 14:24:00:006
 instanceName............. G1 Old Gen
 percentInUse............. 0.122
 Type..................... HEAP
 Usage.max................ 536870912
 Usage.used............... 65359104
 Usage.committed.......... 88080384
********************************************************************************
...

********************************************************************************
MemoryPool
14:23:00:001 -> 14:24:00:006
 instanceName............. G1 Survivor Space
 percentInUse............. 0.000
 Type..................... HEAP
 Usage.max................ -1
 Usage.used............... 1048576
 Usage.committed.......... 1048576
********************************************************************************
```

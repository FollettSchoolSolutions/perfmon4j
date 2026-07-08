# Appender Tag Fields

### Available in Perfmon4j v2.0.0 and greater.

When storing and querying time series data, some repositories differentiate between fields and tags. InfluxDB is an example of a provider that does this. You can read a little about how InfluxDB receives these attributes in the definition of their [Line Protocol](https://docs.influxdata.com/influxdb/v1.3/write_protocols/line_protocol_tutorial/).

Data elements are roughly divided into two categories: Tags and Fields. Tags are used to group and select measurements that will be included in output, while tags are the measurements themselves.

Typically when perfmon4j sends data to InfluxDB the category, subCategory, system name, group, and instanceName are considered tags, and all of the other elements are considered Fields.

To indicate a specific field should be treated as a Tag, rather than a field, you can use the TagFields attribute on the appender definition.

For example consider a Perfmon4j SnapShotMonitor that collects the following 4 elements of data:

* totalCapacity,
* activeWorkload,
* systemStatus (for this example a string value of "Green", "Yellow" or "Red")
* subSystemName

In this example you may prefer subSystemName to be considered a tag -- so you can build individual visualizations for each subsystem.

The following is an example of how you would inform the InfluxAppender to consider subSustemName a tag, rather than a field.

Perfmonconfig.xml fragment:

```xml
...
<appender name='influxdb-appender' className='org.perfmon4j.influxdb.InfluxAppender' interval='1 minute'> 
   <attribute name='baseURL'>http:\\...</attribute>
...
   <attribute name='tagFields'>subSystemName</attribute>
 </appender> 
...
<snapShotMonitor name='SubSystemStatus' className='org.myorg.MySubSystemStatusSnapShot'>
   <appender name='influxdb-appender'/>
</snapShotMonitor>
```

## Options

* To specify multiple fields use separate with a ',' (i.e. 'subSystemName,dataCenterName')
* To limit to a field to a specific monitor, you can prefix the field name with the monitor name, separated by a '|' (i.e. SubSystemStatus|subSystemName.

## As of Perfmon4j 2.2.2

A field can also be flagged as a tag directly from code, without any appender
configuration, via the `outputAsTag` attribute on `@SnapShotString`:

```java
@SnapShotString(outputAsTag=true)
public String getSubSystemName() {
    return subSystemName;
}
```

This declares the intent once, at the monitor definition, rather than
requiring every deployment to know the field name in advance via
`tagFields`. The two mechanisms compose: a field is written as a tag if
either the annotation or the appender's `tagFields` configuration says so.
Appenders without a concept of tags (e.g. `TextAppender`, `SQLAppender`)
ignore the flag and continue to treat the value as an ordinary string.

## See also
- [Perfmon4j Influx Appender](Perfmon4j-Influx-Appender.md)
- [Perfmon4j API and Agent Architecture](Perfmon4j-API-and-Agent-Architecture.md)

InfluxAppender
==============

**Version Introduced:** 1.3.5
**Support for InfluxDb 2 API added:** 1.6.1
**Class Name:** org.perfmon4j.influxdb.InfluxAppender

The InfluxAppender will write data from Perfmon4j Monitors and SnapShotMonitors to an InfluxDb instance. For information on InfluxDb click [here](https://www.influxdata.com/time-series-platform/influxdb/).

Example configuration (1.x):

```xml
<appender name='influxdb' className='org.perfmon4j.influxdb.InfluxAppender' interval='5 seconds'>
   <attribute name='baseURL'>http://myinfluxdb.my.org:8086</attribute>
   <attribute name='database'>perfmon4j</attribute>
</appender>
```

Example configuration (2.x API - Perfmon4j 1.6.1 or greater):

```xml
<appender name='influxdb' className='org.perfmon4j.influxdb.InfluxAppender' interval='5 seconds'>
   <attribute name='baseURL'>http://myinfluxdb.my.org:8086</attribute>
   <attribute name='org'>perfmon4j.org</attribute>
   <attribute name='bucket'>perfmon4j/one_week</attribute>
   <attribute name='token'>REPLACE_WITH_YOUR_API_TOKEN==</attribute>
</appender>
```

Common Attributes
-----------------

| Attribute | Description | Required | Notes |
|-----------|-------------|----------|-------|
| baseURL | The url of the InfluxDb server including protocol, domain and port | Yes | Example: http://172.28.2.112:8086 |
| groups* | Stored as a key attribute pair with associated with each data element | No | The group specified will be recorded as a key value pair along with each measurement (i.e "group=my group name") |
| connectTimeoutMillis | The connection timeout used on the post request | No | Default value: 2500 |
| subCategorySplitter | Define a regular expression to break output into category/subCategory. | No | See: [Sub Category Splitter](Sub-Category-Splitter) |
| tagFields | Define monitored fields that should be treated as tags instead of fields when being written to certain Time-Series data stores | No | See: [Appender Tag Fields](appenderTagFields) - Added in Perfmon4j 2.0.0 |

Legacy Attributes (InfluxDb 1.x)
--------------------------------

| Attribute | Description | Required | Notes |
|-----------|-------------|----------|-------|
| database | The Influx database name | Yes*** | |
| retentionPolicy | The Influx data retention policy | No | |
| userName | The InfluxDb user account | ** | |
| password | The InfluxDb user account password | ** | |

InfluxDb 2.x API Attributes (Perfmon4j 1.6.1 or greater)
---------------------------------------------------------

| Attribute | Description | Required | Notes |
|-----------|-------------|----------|-------|
| org | Your organization name defined in InfluxDb | Yes*** | |
| bucket | A bucket name associated with your organization | Yes*** | |
| token | API key with write access to the bucket | Yes*** | |

\* Influx Appender only supports a single group value. If multiple groups are specified (comma separated) only the first group is used.

\*\* Only required if your InfluxDb 1.x is password protected.

\*\*\* Either the 1.x or 2.x API attributes must be specified.

## As of Perfmon4j 2.2.2

`@SnapShotString` fields can be flagged `outputAsTag=true` so InfluxAppender
writes them as InfluxDB tags without needing a `tagFields` appender
attribute. See [Appender Tag Fields](appenderTagFields) for details.

## See also
- [Appender Tag Fields](appenderTagFields)
- [Sub Category Splitter](Sub-Category-Splitter)

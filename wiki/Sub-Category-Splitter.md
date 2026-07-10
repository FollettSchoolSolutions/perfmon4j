Sub Category Splitter
======================

**Version Introduced:** 1.6.1
**Class Name:** org.perfmon4j.SubCategorySplitter

The SubCategorySplitter provides an option to group several interval categories into a single category by defining a SubCategory. This feature is currently supported in the `org.perfmon4j.influxdb.InfluxAppender` and `org.perfmon4j.azure.LogAnalyticsAppender`.

To configure you must add a regular expression that is used to: 1) Match Interval or Snapshot categories that should be modified; and 2) Define the subcategory.

## Configure subCategorySplitter in an InfluxAppender

Below is a partial example configuration file that shows how to apply the sub category splitter.

```xml
<Perfmon4JConfig enabled='true'>
   <appender name='influxdb' className='org.perfmon4j.influxdb.InfluxAppender' interval='5 seconds'>
      <attribute name='baseURL'>http://myinfluxdb.my.org:8086</attribute>
      <attribute name='database'>perfmon4j</attribute>
      <attribute name='subCategorySplitter'>DistrictResource\.(.*)</attribute>
   </appender>

   <monitor name='DistrictResource'>
     <attribute name='influxdb' pattern='/*'>
   </monitor>
</Perfmon4JConfig>
```

## Notes

1. The pattern must contain at least one group definition to define the subCategory.
2. If the pattern contains multiple group definitions the last definition (or more specifically the last non-null captured group) will be used to define the sub category.
3. Perfmon4j automatically prepends "Interval." or "Snapshot." to each category. This does not need to be included in the regular expression.
4. If the regular expression can not be successfully parsed a warning will be written to the log and the regex will be ignored.
5. You can define multiple patterns in the supplied regular expression. For example the following pattern would match categories that start with `WebRequest.*` OR `DistrictResource.*`: `(DistrictResource\.(.*))|(WebRequest\.(.*))`

## See also
- [Perfmon4j Influx Appender](Perfmon4j-Influx-Appender)
- [Azure LogAnalytics Appender](Azure-LogAnalytics-Appender)

Azure LogAnalyticsAppender
==========================

**Version Introduced:** 1.5.1-SNAPSHOT
**Class Name:** org.perfmon4j.azure.LogAnalyticsAppender

The LogAnalyticsAppender will write data from Perfmon4j Monitors and SnapShotMonitors to a Log Analytics workspace in Azure Monitor. For information on the Microsoft REST API used by this appender click [here](https://docs.microsoft.com/en-us/azure/azure-monitor/platform/data-collector-api).

Example configuration:

```xml
<appender name='loganalytics' className='org.perfmon4j.azure.LogAnalyticsAppender' interval='1 minute'>
   <attribute name='customerID'>00000000-0000-0000-0000-000000000000</attribute>
   <attribute name='sharedKey'>REPLACE_WITH_YOUR_WORKSPACE_SHARED_KEY==</attribute>
</appender>
```

Common Attributes
-----------------

| Attribute | Description | Required | Notes |
|-----------|-------------|----------|-------|
| customerID | The workspace ID of the Log Analytics Workspace | Yes | |
| sharedKey | See this [Microsoft Page](https://docs.microsoft.com/en-us/rest/api/loganalytics/workspace%20shared%20keys/getsharedkeys) for information on how to retrieve a shared key using OAuth2 | Yes | |
| azureResourceID | An azure resource identifier to be associated with each log event | No | This could be an azure Identifier associated with the Virtual Machine or Container hosting the JVM |
| groups* | Stored as a key attribute pair with associated with each data element | No | |
| subCategorySplitter | Define a regular expression to break output into category/subCategory. See: [Sub Category Splitter](Sub-Category-Splitter) | No | |

\* The LogAnalyticsAppender only supports a single group value. If multiple groups are specified (comma separated) only the first group is used.

## See also
- [Sub Category Splitter](Sub-Category-Splitter)
- [Perfmon4j Influx Appender](Perfmon4j-Influx-Appender)

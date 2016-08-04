---
layout: page
title: JBoss 7 Windows Install Guide
permalink: /documentation/quickstart/jboss7windows/
showInHeader: false
---
This guide details how to install the Perfmon4j agent into an JBoss 7 application server under running on Windows. The instructions require perfomn4j 1.2.0-RC_2 or later.

## Copy files

Copy the following files from your perfmon4j distribution:

 * Copy the **perfmon4.jar** file into **jboss\standalone\lib\ext** folder.

 * Copy the **perfmon4j-jbossweb7.jar** file into **jboss\modules\org\jboss\as\web\main** folder.

 * Copy the **perfmonconfig.xml** (located in the pefmon4j/doc folder of the perfmon4j distribution) file into **jboss\bin** folder.

Copy the following following file from within your jboss server distribution:

* Copy the file **jboss\modules\org\javassist\main\javassist-3.15.0-GA.jar** to file **'jboss\standalone\lib\ext\javassist.jar'** (make sure when you copy the file is renamed to javassist.jar) -- Note: do not use the javassist.jar included with the perfmon4j distribution.

## Configure

* Append the following line to **jboss\bin\standalone.conf.bat** file (insert right above the line :JAVA_OPTS_SET):
	* set "JAVA_OPTS=%JAVA_OPTS% -javaagent:..\standalone\lib\ext\perfmon4j.jar=-eorg.apache,-eSQL,-pAUTO,-eVALVE,-f../bin/perfmonconfig.xml"

Details on javaagent [command line parameters](../../userguide/#javaagent-config).

Edit file **jboss\modules\org\jboss\as\web\main\module.xml** and add the following line to the <resources> section of the file:
 
* &lt;resource-root path="perfmon4j-jbossweb7.jar"/&gt;

## Test

Start JBoss server using jboss\bin\standalone.sh shell script.
You should see Perfmon4j ascii art when the agent is launched.

Once the jboss server is started you should see something that looks like the following:
<pre>
Max Active Threads. 0 
Throughput......... 0.00 per minute
Average Duration... 0.00
Median Duration.... NA
> 2 seconds........ 0.00%
> 5 seconds........ 0.00%
> 10 seconds....... 0.00%
Standard Deviation. 0.00
Max Duration....... 0 
Min Duration....... 0 
Total Hits......... 0
Total Completions.. 0
(SQL)Avg. Duration. 0.00
(SQL)Std. Dev...... 0.00
(SQL)Max Duration.. 0 
(SQL)Min Duration.. 0 
</pre>

This indicates that perfmon4j is monitoring each web request. Important: The JBoss console application does not route through the default web application container and request through the console will not be monitored by Perfmon4j. To evaluate request you can deploy any WAR file to the jboss/standalone/deployment folder. 

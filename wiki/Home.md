# Perfmon4j Documentation

## Overview

PerfMon4j™ is a software API designed to diagnose and monitor application performance while the software is under load. It allows monitoring application on many levels, including:

* Method via declarative programming using Java Annotations.
* Method level via the Java Agent Instrumentation API.
* Servlet Request level via a Servlet Filter or Apache Tomcat Valve.
* Arbitrary code execution point via inserted timers.
* Additional facilities enable monitoring web request, client browser information and arbitrary system health information

Perfmon4j should not be confused with a profiler. While profilers are typically used to snapshot performance in test environments, Perfmon4j provides an API to build performance monitors into your application. Like a profiler, Perfmon4j can monitor Java classes/methods at the byte code level without inserting custom code. Perfmon4j provides features beyond those typically found in profilers to enable the creation of a comprehensive monitoring system that enables detailed metrics on the usage and performance of your deployed system under load. These features include:

* Random Sampling
* Arbitrary timings based on code insertion
* Nested timings
* Monitor throughput and maximum concurrent threads
* Monitor arbitrary server metrics.
* Extremely low overhead when monitoring is disabled
* Extensible appender can be used to write data metrics to a variety of output formats, including writing to the server log or an SQL database.
* Arbitrary timings based on method parameters
* Perfmon4j is an API designed to integrate performance monitoring and logging into your enterprise application. It is intended to be deployed in production environment to monitor detailed system metrics and usage pattern.

## Getting Started

* [Configuring the Java Agent](Configuring-the-Java-Agent.md)
* [Configuration Properties and Conditional Activation](Configuration-Properties-and-Conditional-Activation.md)
* [Perfmon4j Configuration Examples](Perfmon4j-Configuration-Examples)
* [Perfmon4j Demo](Perfmon4j-Demo)

## Architecture

* [Perfmon4j API and Agent Architecture](Perfmon4j-API-and-Agent-Architecture.md) — how the `agent-api` stub classes and the `base` agent implementation connect at runtime
* [Virtual Threads and Perfmon4j](Virtual-Threads-and-Perfmon4j.md)

## Appenders

* [Appender Configuration](Appender-Configuration)
* [Appender Tag Fields](appenderTagFields.md)
* [Perfmon4j Influx Appender](Perfmon4j-Influx-Appender.md)
* [Azure LogAnalytics Appender](Azure-LogAnalytics-Appender.md)
* [SQL Appender Configuration](SQL-Appender-Configuration)
* [Sub Category Splitter](Sub-Category-Splitter.md)

## Monitoring Components

* [ActiveThreadMonitor](ActiveThreadMonitor.md)
* [ExceptionTracker](ExceptionTracker.md)
* [MBeanSnapShotMonitor](MBeanSnapShotMonitor.md)
* [Interval Monitor Pattern Definition](Interval-Monitor-Pattern-Definition.md)
* [Thread Trace Configuration Samples](Thread-Trace-Configuration-Samples)

## SQL / Database

* [Create or Upgrade Perfmon4j SQL Database](Create-or-Upgrade-Perfmon4j-SQL-Database)
* [How to Deploy Perfmon4j Datasource](How-to-Deploy-Perfmon4j-Datasource)
* [How to Monitor individual methods within the JDBC driver with -eSQL](How-to-Monitor-individual-methods-within-the-JDBC-driver)

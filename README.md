# Perfmon4j
[![Build](https://github.com/FollettSchoolSolutions/perfmon4j/actions/workflows/maven.yml/badge.svg?branch=develop)](https://github.com/FollettSchoolSolutions/perfmon4j/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.perfmon4j/perfmon4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.perfmon4j/perfmon4j)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL_v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)
[![Wiki](https://img.shields.io/badge/docs-wiki-blue.svg)](https://github.com/FollettSchoolSolutions/perfmon4j/wiki)

Perfmon4j is a Java instrumentation agent for monitoring performance in production
application servers. It uses bytecode instrumentation to inject timers into your code and
reports interval metrics — throughput, response time, active threads, SQL time, and JVM/JMX
snapshots — to pluggable appenders (text log, JDBC databases, InfluxDB, Azure Log Analytics).

## Installation

Perfmon4j is published to [Maven Central](https://central.sonatype.com/namespace/org.perfmon4j)
under the `org.perfmon4j` group id.

Maven:
```xml
<dependency>
    <groupId>org.perfmon4j</groupId>
    <artifactId>perfmon4j</artifactId>
    <version>2.2.1</version>
</dependency>
```

Gradle:
```groovy
implementation 'org.perfmon4j:perfmon4j:2.2.1'
```

Application-server integration modules are published under the same group id — for example
`perfmon4j-servlet`, `perfmon4j-wildfly8`, `perfmon4j-tomcat7`, and `perfmon4j-quarkus3x`.

## Running the agent

Perfmon4j attaches to any JVM as a standard `-javaagent`:

```
java -javaagent:/path/to/perfmon4j.jar="-ecom.example.app,-eSQL" com.example.Main
```

Agent parameters are comma-separated and optional; by default the agent looks for a
`perfmonconfig.xml` on the classpath (override with `-f<path>` for a file or `-c<resource>`
for a classpath resource). See
[Configuring the Java Agent](https://github.com/FollettSchoolSolutions/perfmon4j/wiki/Configuring-the-Java-Agent)
for the full list of parameters and configuration options.

## Documentation
- [Project Wiki](https://github.com/FollettSchoolSolutions/perfmon4j/wiki)
- [Change log](readme.txt)

## Building & releasing
- Build and test locally: `mvn clean install`
- CI (build, test, and publish) runs via GitHub Actions
  ([`.github/workflows/maven.yml`](.github/workflows/maven.yml)).
- Releases are published to Maven Central from CI — see the
  [Maven Central Publishing Guide](.github/maven/MAVEN_CENTRAL_PUBLISHING.md).

## License
Perfmon4j is released under the GNU Lesser General Public License,
[version 3.0](https://www.gnu.org/licenses/lgpl-3.0).

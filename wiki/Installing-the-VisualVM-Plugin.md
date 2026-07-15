# Installing the VisualVM Plugin

The perfmon4j VisualVM plugin adds a "Perfmon4j" tab to [VisualVM](https://visualvm.github.io/)
for any attached Java application running the perfmon4j agent. It browses live interval and
snapshot monitors, charts any numeric field in real time, and can schedule on-demand thread
traces — all without needing a database or log appender configured. It talks to the agent over
RMI, so the target JVM must have perfmon4j's remote-management port enabled.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Step 1 — Install VisualVM](#step-1--install-visualvm)
- [Step 2 — Build the perfmon4j plugin](#step-2--build-the-perfmon4j-plugin)
- [Step 3 — Install the plugin into VisualVM](#step-3--install-the-plugin-into-visualvm)
- [Step 4 — Verify](#step-4--verify)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Detail |
|--------------|--------|
| Target JVM | Running with the perfmon4j agent attached and remote management enabled via `-p<port>` or `-pAUTO`. See [Configuring the Java Agent](Configuring-the-Java-Agent)'s "Remote Management" section — the plugin can't connect without this. |
| Build JDK | JDK 11 or newer, to build the plugin itself (independent of whatever JDK your monitored application runs on). |
| Maven | Any recent Maven 3.x, to run the plugin's build. |

There's no published, pre-built download of the plugin yet — building it from source (Step 2) is
currently the only way to get it.

---

## Step 1 — Install VisualVM

VisualVM hasn't shipped bundled with the JDK since Java 9, so download and install it separately
from the [official VisualVM site](https://visualvm.github.io/download.html). It's cross-platform
(Windows, macOS, Linux) and needs no special configuration to run.

---

## Step 2 — Build the perfmon4j plugin

Clone the perfmon4j repository if you haven't already, then build the `visualvm-plugin/` module:

```
git clone git@github.com:FollettSchoolSolutions/perfmon4j.git
cd perfmon4j/visualvm-plugin
mvn clean install
```

This produces `target/nbm/visualvm-plugin-<version>.nbm` — the installable plugin file.

---

## Step 3 — Install the plugin into VisualVM

1. Launch VisualVM.
2. Open **Tools → Plugins**.
3. Switch to the **Downloaded** tab.
4. Click **Add Plugins...** and select the `.nbm` file built in Step 2.
5. Click **Install**, accept the license agreement, and let the installer finish.
6. Restart VisualVM if prompted.

---

## Step 4 — Verify

1. Start (or attach VisualVM to) a JVM running perfmon4j with `-p`/`-pAUTO` enabled.
2. In VisualVM's Applications tree, double-click that application to open its view.
3. Confirm a **Perfmon4j** tab appears alongside the built-in Overview/Monitor/Threads tabs.
4. Open it and confirm the interval/snapshot monitor tree populates with live data.

---

## Troubleshooting

**The "Perfmon4j" tab doesn't appear for an application.** This almost always means the target
JVM isn't advertising a reachable remote-management port:

- Confirm the agent was started with `-p<port>` or `-pAUTO` (see
  [Configuring the Java Agent](Configuring-the-Java-Agent)) — without it, perfmon4j runs with RMI
  remote management disabled entirely, silently.
- Confirm the port is actually reachable from the machine running VisualVM. By default this is a
  localhost-only connection; monitoring a JVM on a remote host may require additional network/RMI
  configuration.

**Installer shows warnings like "The plugin `<X> API` is requested in version >= A.B but only C.D
was found," and the plugin doesn't install.** This means the `.nbm` you built was compiled against
a newer NetBeans Platform release than the one bundled in your VisualVM install — the plugin's
build pins an exact NetBeans Platform version to match a specific VisualVM release, and that pin
can fall behind (or ahead of) whatever VisualVM version you actually downloaded. Make sure you're
building from the latest `visualvm-plugin/` on the `develop` branch, and if the warning still
appears, please report it (include the exact VisualVM version and the full warning text) so the
build's version pin can be corrected.

# Legacy VisualVM Plugin — Use Cases

Reviewed 2026-07-10 while scoping the `hawtio-plugin` roadmap, to see whether anything here
was a ripe porting source. Conclusion: not really — everything except discovery/version-check
rides on **RMI** (not JMX/Jolokia, which browsers can speak), and most of the interactive
features are write/exec operations, which is exactly the risk category the Hawtio plugin work
is deliberately deferring behind the Jolokia ACL policy (see `hawtio-plugin/ROADMAP.md`). Kept
here as a reference of what the old tool did, in case any individual feature becomes worth
re-scoping later.

Source: `netbeans/Perfmon4jMonitor/src/org/perfmon4j/visualvm/` (`MainWindow`, `Perfmon4jModel`,
`FieldManager`, `chart/*`, `ThreadTrace*`) and the RMI interface it talks to,
`base/src/main/java/org/perfmon4j/remotemanagement/intf/RemoteManagementWrapper.java`.

## Connection / discovery

1. **Auto-detect a perfmon4j-instrumented JVM** — `Perfmon4jModel` checks a VisualVM-attached
   `Application` for the `P4J_LISTENER_PORT` system property (via JMX, Attach API, or
   Serviceability Agent, falling back to regex-parsing
   `-javaagent:...perfmon4j...=...-p<port>` out of the raw JVM args if none of those work),
   then opens an **RMI** connection to perfmon4j's own remote-management server on that port.
   This gates whether the "Perfmon4j" tab even appears for a given process.
2. **Version negotiation/display** — shows the plugin's own version alongside the *attached
   agent's* reported `getServerManagementVersion()`, and separately feature-gates newer
   capabilities (see #4) behind an `instanceof RemoteInterfaceExt1` capability check —
   defensive versioning since the plugin might outlive or predate the agent it's talking to.

## Monitor exploration

3. **Live monitor tree browsing** — lists every registered Interval Monitor and Snapshot
   Monitor from the running JVM (`getMonitors()`), rendered as two dot-notation hierarchical
   trees (e.g. `com.example.MyClass.myMethod`) — a live view of what's actually instrumented,
   no XML reading required. Manual "Refresh" button re-pulls it, since monitors appear
   dynamically as code paths execute.
4. **Force dynamic monitor creation** — right-click a monitor to force-create its child
   monitors immediately rather than waiting for that code path to run for the first time
   (`forceDynamicChildCreation`/`unForceDynamicChildCreation` over RMI).

## Live charting

5. **Ad-hoc real-time charting** — "Add Field to Chart..." picks any numeric field off a
   monitor (throughput, avg duration, active threads, etc.), assigns a color/scale factor, and
   plots it on a scrolling 5-minute time-series chart, polled on VisualVM's global
   refresh-interval preference.
6. **Non-numeric field table** — string-valued fields get routed to a flat table instead,
   since they can't be charted.
7. **Chart element management** — a table lists everything currently charted with per-row
   visibility/color/remove controls, live, without restarting the chart.
8. **Save/load a dashboard to disk** — persists the set of charted fields (key, color, scale,
   visibility) to a `.p4j` properties file and reloads it later, gracefully skipping (with a
   warning) any field that no longer exists in whatever JVM it's reopened against.

## Thread trace (on-demand stack capture)

9. **Schedule a one-shot thread trace** — right-click an interval monitor, set a min-duration
   threshold and max stack depth, and schedule server-side capture of the next matching
   request's call stack — no XML pre-configuration needed.
10. **Thread trace queue** — a tab listing pending/completed traces with per-row View/Cancel or
    View/Delete; results stream back asynchronously and update the row in place.
11. **Thread trace result viewer** — a detail panel showing submission time, monitor name, and
    the captured stack trace text for a completed trace.

## Not a real feature

12. `Perfmon4jAction.java` is explicitly commented as an unused example of adding a VisualVM
    Explorer context-menu item — dead scaffolding, not a use case.

## Relevance to `hawtio-plugin`

- #1/#2 (discovery, version display) are the only RMI-free-in-spirit items — #2 is already
  shipped in the Hawtio plugin as the read-only self-management MBean + About box.
- #3 (live monitor tree) and #9–11 (thread trace) are the closest conceptual matches to
  `hawtio-plugin/ROADMAP.md`'s already-tracked backlog items, but would need a JMX/Jolokia
  equivalent of `RemoteManagementWrapper` built from scratch — nothing here is directly
  reusable code, only the feature shape.
- #4–8 (dynamic monitor creation, charting, dashboard save/load) are write/interactive and out
  of scope until the Jolokia ACL work lands.

# Hawtio Plugin Roadmap

Backlog and status for the perfmon4j Hawtio plugin initiative, spanning both
[`hawtio-plugin/`](README.md) (the plugin bundle) and
[`hawtio-plugin-webapp/`](../hawtio-plugin-webapp/CLAUDE.md) (the deployable WAR that
hosts it). This file tracks *what's next and why* - for architecture/patterns/gotchas,
see each module's `CLAUDE.md` instead; this file shouldn't duplicate that.

## Status

- **Sprint 1 shipped** on `feature/HawtioPluginSprint1`: a standalone Hawtio nav item
  where a user pastes an MBean ObjectName, selects attributes as gauge/counter, and gets
  a ready-to-paste `<mBeanSnapshotMonitor>` XML snippet. No live JVM changes, no `base`
  module changes.
- **Deployment verified working** against a real WildFly instance:
  built as a self-hosting WAR (`hawtio-plugin-webapp`) that registers itself as a
  discoverable Hawtio remote plugin via a JMX MBean - see that module's `CLAUDE.md` for
  the two real deployment bugs found and fixed along the way (an `org.perfmon4j` package
  namespace collision, and a doubled `remoteEntry.js` URL).
- **perfmon4j self-management MBean (plumbing) shipped**: `base` now registers a
  read-only JMX MBean (`org.perfmon4j:type=SelfManagement`, attribute `Version`) when the
  `PerfMon` class loads, and the plugin has an "About" nav item that reads it via Jolokia
  (`jolokiaService.readAttribute`) - the first small step toward the "Live dynamic push"
  backlog item below. No write/exec JMX operations were added; see "Jolokia access-control
  resolved as out of scope" below.
- **MBean search/select picker shipped**: the "paste an ObjectName" text field is now a
  searchable/filterable MBean tree (`MBeanTreePicker.tsx`), built from `@hawtio/react`'s
  `workspace.getTree()`/`MBeanNode`/`MBeanTree` and PatternFly's `TreeView` +
  `PluginTreeViewToolbar`. Selecting a real MBean (a tree node with an `objectName`)
  populates a now-read-only ObjectName field feeding the unchanged `readMBeanAttributes.ts`;
  clicking a domain/type grouping node shows an inline hint instead. No changes were needed
  to `readMBeanAttributes.ts` or downstream attribute-selection/XML-generation code.
- **Jolokia access-control resolved as out of scope for this plugin.** This plugin never
  opens its own Jolokia connection - per `CLAUDE.md`, it reuses `@hawtio/react`'s
  `jolokiaService`/`workspace`, i.e. whatever Jolokia session the host Hawtio console
  already has. Securing that session (a `jolokia-access.xml` ACL, a container
  `security-constraint`, etc.) is the deploying organization's responsibility, same as it
  is for Hawtio's own built-in JMX plugin - this plugin has no code path that could change
  that even if it wanted to. Verified as done correctly against a real WildFly deployment
  of Hawtio: Jolokia was reachable only through a path gated end-to-end by a container
  `security-constraint`, all HTTP methods, no bypass found. No action item remains here for
  this repo.
- **RemoteManagement JMX MBean shipped (Phases A+B)** on `feature/HawtioPluginSprint1`:
  `base` now also registers `org.perfmon4j:type=RemoteManagement`, re-exposing the legacy
  VisualVM RMI remote-management interface's monitor-browsing and thread-trace
  functionality over JMX/Jolokia - `connect`/`disconnect`, `getMonitors`/
  `getFieldsForMonitor`, `subscribe`/`getData` (Phase A), and `scheduleThreadTrace`/
  `unScheduleThreadTrace` (Phase B). Follows `SelfManagement`'s registration pattern and
  delegates to the exact same `ExternalAppender`/`PerfMon` statics `RemoteImpl` already
  calls, rather than proxying RMI. Resolves the session/subscribe-model question the
  backlog item below used to leave open: kept `RemoteInterface`'s existing
  sessionID-as-parameter model as-is - a session opened via JMX shares state with, and can
  run concurrently alongside, one opened over RMI (both delegate to the same static
  `ExternalAppender` session table), confirmed with a dedicated coexistence test. Also
  manually verified against a real Jolokia agent that the plain `String[]`/
  `Map<String,Object>` return types serialize cleanly to JSON with no `CompositeData`
  translation needed. `RemoteInterfaceExt1`'s dynamic-child-creation operations remain
  unported (see Backlog). This is `base`-module plumbing only - no `hawtio-plugin` UI
  consumes this MBean yet.
- **"perfmon4j Chart" nav item shipped** on `feature/HawtioPluginSprint1`: a third nav
  item (`src/chart/`) where a user browses perfmon4j's interval/snapshot monitors via
  the `RemoteManagement` MBean, picks one or more numeric fields, and watches their
  live values plot over a rolling 5-minute window (`@patternfly/react-charts`, added
  as an explicit dependency - previously only resolved transitively via
  `@hawtio/react`'s peerDependency). Polls on its own independent `setInterval`
  cadence (default 5s) rather than `jolokiaService.register()`'s shared console-wide
  scheduler, so its granularity can't be silently slowed by an unrelated change to
  Hawtio's own Preferences page. Handles `subscribe()`'s full-replacement contract
  (always resends the complete field list, confirmed in `RemoteManagement.java`) and
  transparently reconnects + resubscribes on any unrecognized poll failure (a
  `SessionNotFoundException` from the 5-minute idle timeout can't be reliably
  distinguished by text alone - `jolokiaService.execute()` doesn't forward the
  exception class name - so any unclassified failure is treated as a reconnect
  candidate). This is the plugin's first UI feature built on write/exec JMX
  operations (`connect`/`subscribe`/`getData`/`disconnect`), which is why the
  "Graceful degradation" backlog item below is now concrete rather than
  speculative. Also required a small `dev-target/TargetMain.java` addition (a
  background `PerfMonTimer` loop) so the local dev harness has real `INTERVAL` data
  to chart; deliberately does not register `SNAPSHOT`-type monitors there, since
  doing so needs Javassist on the classpath which the bare dev harness doesn't have
  (a pre-existing dev-harness limitation, not something this feature needed to fix).
  **Caveat**: the backend data path (connect/getMonitors/getFieldsForMonitor/
  subscribe/getData/unsubscribe/disconnect) was verified end-to-end against a real
  Jolokia agent, matching the exact call sequence the UI makes, but the rendered UI
  itself was not exercised in a real browser (no headless browser was available in
  the authoring environment) - a manual `npm start` pass is recommended before
  treating this as fully verified.
  - Deliberately deferred to a follow-up, not built in this first cut: non-numeric
    fields routed to a flat value table, per-series color/visibility customization
    or y-axis normalization (v1 uses a single shared y-axis and PatternFly's default
    auto-assigned theme colors), save/load of the chart layout to a file, and any
    thread-trace integration (`scheduleThreadTrace`/`unScheduleThreadTrace`).

## Backlog

Roughly in the order they'd most improve the plugin - not a committed sequence.

- **Live dynamic push to a running JVM.** Add a way to apply the monitor immediately
  instead of only generating a copy-paste snippet. The prerequisite self-management
  MBean now exists in `base` (`org.perfmon4j.selfmanagement.SelfManagement`, read-only
  today) - this backlog item is now specifically about adding write/exec capability to
  it (e.g. an "apply mBeanSnapshotMonitor config now" operation), which is a materially
  different risk profile than the read-only `Version` attribute shipped so far - it raises
  the stakes of the host's own Jolokia hardening (see "Jolokia access-control resolved as
  out of scope" under Status above), though it opens no new access path of its own. Per
  earlier discussion: this should **add** a
  "push live" option alongside the XML snippet, not replace it - the XML is still needed
  so the monitor survives a JVM restart.
- **`RemoteInterfaceExt1`'s dynamic-child-creation operations for the RemoteManagement
  MBean.** The RemoteManagement MBean shipped (see Status above) covers monitor
  browsing and thread-trace scheduling, but not `forceDynamicChildCreation`/
  `unForceDynamicChildCreation`/`getServerManagementVersion` - deliberately deferred as
  lower priority per `LEGACY_VISUALVM_FEATURES.md`. Same delegate-don't-proxy approach
  applies (`ExternalAppender.forceDynamicChildCreation`/`unForceDynamicChildCreation`).
  Like "Live dynamic push" above, this is write/exec - it raises the stakes of the host's
  own Jolokia hardening (see "Jolokia access-control resolved as out of scope" under
  Status above), though it opens no new access path of its own.
- **Graceful degradation when Jolokia write/exec access is unavailable.** No longer
  speculative: the "perfmon4j Chart" nav item (see Status above) is a real, shipped
  consumer of write/exec JMX operations (`connect`/`subscribe`/`getData`/`disconnect`
  on `RemoteManagement`), alongside the still-unbuilt "Live dynamic push" feature. A
  write/exec Jolokia call can fail specifically because the *deploying organization's
  own* ACL (`jolokia-access.xml`, a role-based restriction, etc.) allows reads but
  denies writes/exec - a legitimate, expected configuration, not a bug. The Chart
  screen's `useRemoteManagementChart` hook already classifies this distinctly as an
  `'exec-denied'` connection-error kind (see `remoteManagementErrors.ts`) and shows a
  dedicated `Alert` explaining the likely cause rather than a generic error - but it
  still just dead-ends there with a Retry button; it doesn't fall back to any
  degraded-but-useful read-only experience. That fallback UX (and doing the same for
  "Live dynamic push" once built) is what remains open here.
- **`ratios` support** (computed numerator/denominator attributes), matching the
  `mBeanSnapshotMonitor` grammar's `ratios='name=numerator/denominator'` syntax.
- **Multi-instance fan-out** - `instanceKey`/`instanceValueFilter`/`attributeValueFilter`,
  for monitoring MBeans like `MemoryPool` where several instances share a domain+type.
- **Composite/tabular attribute support** (dotted paths like `Usage.max`), plus
  formatters/suffixes/`displayAsPercentage`/`displayAsDuration`, and custom `displayName`
  overrides.
- **Smart gauge-vs-counter defaulting.** Today every attribute defaults to unclassified;
  a naming-heuristic default (e.g. `Total*`/`*Count` -> counter) would cut clicks.
- **CI wiring.** Neither `hawtio-plugin/` nor `hawtio-plugin-webapp/` is in the root
  Maven reactor or any GitHub Actions workflow (deliberately, for Sprint 1 - see each
  module's `CLAUDE.md`). Needs its own workflow if this graduates beyond manual builds.
- **Persisting/editing `perfmonconfig.xml` in place**, and multi-MBean batch authoring
  (generate several monitors in one pass) instead of one at a time.

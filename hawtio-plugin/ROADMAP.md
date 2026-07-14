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
- **Expose the legacy VisualVM RMI interface's data via a new JMX MBean.** The old
  NetBeans/VisualVM plugin (`netbeans/Perfmon4jMonitor/`, see
  `LEGACY_VISUALVM_FEATURES.md`) browsed interval/snapshot monitors, read live field data,
  and scheduled thread traces over RMI
  (`org.perfmon4j.remotemanagement.intf.RemoteInterface`/`RemoteInterfaceExt1`, implemented
  by `RemoteImpl`). Confirmed feasible to re-expose that functionality as a new JMX MBean
  instead (Jolokia can invoke JMX operations, not just read attributes - same mechanism
  hawtio's built-in JMX plugin already uses) following the `SelfManagement` MBean's
  registration pattern, calling the same underlying `ExternalAppender`/`PerfMon` methods
  `RemoteImpl` already delegates to rather than proxying RMI itself. Undecided: keep
  `RemoteInterface`'s session/subscribe model as-is, or redesign statelessly for a web
  client (thread-trace scheduling stays inherently stateful either way). Like "Live dynamic
  push" above, most of this surface is write/exec (subscribe, schedule/unschedule thread
  trace, connect/disconnect all mutate server-side state) - it raises the stakes of the
  host's own Jolokia hardening (see "Jolokia access-control resolved as out of scope" under
  Status above), though it opens no new access path of its own.
- **Graceful degradation when Jolokia write/exec access is unavailable.** Once either
  write-capable feature above ships, a write/exec Jolokia call can fail specifically
  because the *deploying organization's own* ACL (`jolokia-access.xml`, a role-based
  restriction, etc.) allows reads but denies writes/exec - a legitimate, expected
  configuration, not a bug. The plugin should detect that failure mode distinctly (rather
  than surfacing it as a generic error) and degrade gracefully: fall back to the read-only/
  XML-snippet experience with a visible notice that live-push/write functionality is
  unavailable because the connected Jolokia endpoint is read-only.
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

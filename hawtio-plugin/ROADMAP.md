# Hawtio Plugin Roadmap

Backlog and status for the perfmon4j Hawtio plugin initiative, spanning both
[`hawtio-plugin/`](README.md) (the plugin bundle) and
[`hawtio-plugin-webapp/`](../hawtio-plugin-webapp/CLAUDE.md) (the deployable WAR that
hosts it). This file tracks *what's next and why* - for architecture/patterns/gotchas,
see each module's `CLAUDE.md` instead; this file shouldn't duplicate that.

The **Monitoring tab** initiative — growing the live Chart nav item into a functional
mirror of the Swing VisualVM plugin — has its own design spike and sprint-ready work
breakdown: [`MONITORING_TAB_SPIKE.md`](MONITORING_TAB_SPIKE.md) (proposed layout, the
11-feature gap analysis, and a four-slice sequence) and
[`MONITORING_TAB_TASKS.md`](MONITORING_TAB_TASKS.md) (tasks T1–T14, milestones, NFRs).

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
  translation needed. `RemoteInterfaceExt1`'s dynamic-child-creation operations
  (`forceDynamicChildCreation`/`unForceDynamicChildCreation`/
  `getServerManagementVersion`) have since been ported too (`MONITORING_TAB_TASKS.md`
  T12), same delegate-don't-proxy approach - one non-obvious finding from that work:
  `getServerManagementVersion()`, being a no-arg `getXxx()` method, is classified by
  JMX Standard MBean introspection as a read-only attribute (`ServerManagementVersion`)
  rather than an operation; still fully Jolokia-reachable, just via `read` instead of
  `exec`. This is still `base`-module plumbing only - no `hawtio-plugin` UI consumes
  the Ext1 ops yet (that's T13, blocked on this).
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
  **Verified working end-to-end in a real browser** (headless Chromium via
  Playwright, added as a devDependency for this - see `run` skill's playwright.md
  pattern), against the `dev-target/` harness: nav item renders, monitor search/
  picker works, adding a field subscribes and the Latest Value column updates live,
  the chart renders a real line with sane axes, and removing a field both clears it
  from the UI and drops the server-side subscription. This actual-browser pass
  caught two real bugs invisible to a backend-only (Jolokia-call-sequence) check:
  1. **Stale-session race under React 18 StrictMode.** The dev harness's
     `bootstrap.tsx` wraps in `<React.StrictMode>`, which double-invokes effects in
     development; `useRemoteManagementChart`'s connect/poll effect used a shared
     `mountedRef` instead of this codebase's established per-invocation `cancelled`
     local-variable convention (see `MBeanTreePicker.tsx`), so the first (stale)
     effect invocation's session leaked and raced the second for which one actually
     ended up polled - `getMonitors()` and `getData()` could land on different
     sessions. Fixed: each effect invocation now tracks its own cancellation and
     disconnects any session it obtains after being cancelled.
  2. **Degenerate y-axis for a flat data series.** Victory computed a near-zero-
     width y-domain for a perfectly constant series (e.g. the dev-target demo
     loop's `AverageDuration`), rendering absurdly over-precise axis labels and an
     "Infinity is an invalid CSS width" console warning. Fixed by computing an
     explicit y-domain with a sensible minimum pad instead of relying on Victory's
     default heuristic.
  - Deliberately deferred to a follow-up, not built in this first cut: per-series
    color/visibility customization or y-axis normalization (v1 uses a single shared
    y-axis and PatternFly's default auto-assigned theme colors), save/load of the
    chart layout to a file, and any thread-trace integration
    (`scheduleThreadTrace`/`unScheduleThreadTrace`). Non-numeric fields routed to a
    flat value table has since shipped - see `hawtio-plugin/MONITORING_TAB_TASKS.md`
    T5 (STRING/TIMESTAMP fields land on a "Text fields" tab instead of the chart).
    Per-series color (T6) and per-series visibility toggle (T7) have since shipped
    too - see `MONITORING_TAB_TASKS.md`. Thread-trace integration has since shipped
    end-to-end as well (T8-T11): scheduling (min-duration/max-depth, with real
    input validation the legacy VisualVM dialog never had), a pending/completed
    queue tab with View/Cancel, and a captured-stack result viewer - closing out
    legacy features #9/#10/#11 (see `MONITORING_TAB_SPIKE.md`'s gap table).
    Save/load of the charted-field set to a file has since shipped too (T14,
    closing out legacy feature #8) - a "Save dashboard.../Load dashboard..."
    toolbar in the Monitoring tab downloads/reloads a JSON file of every
    subscribed field's key/color/visibility, skipping (with a dismissible
    warning) any field no longer present on the currently-connected JVM. A
    "Force dynamic monitor creation" tree-row action has since shipped too
    (T13, closing out legacy feature #4's UI half - see T12 above for the
    MBean plumbing it calls): INTERVAL-only, toggles between "Force…"/"Stop
    forcing…" per row based on what this session has itself forced (there's
    no server-side query op for current state, so this isn't restored across
    a reconnect the way charted fields are - a deliberate simplification for
    what's a one-off debugging action, not a standing subscription). This was
    also the first concrete instance of graceful degradation under a
    *per-operation* Jolokia ACL denial (see the Backlog item below, now
    narrowed accordingly): if `forceDynamicChildCreation`/
    `unForceDynamicChildCreation` specifically come back exec-denied, the row
    action is hidden and a dismissible alert explains why, while the rest of
    the session (browsing/charting/thread traces) is entirely unaffected -
    distinct from, and not a replacement for, the still-open whole-session
    exec-denied case below. Only per-series y-axis normalization (deferred at
    v1 shipping above) remains open - it has no dedicated Backlog entry of its
    own, being a small leftover of that same item rather than a separate
    initiative.

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
- **Graceful degradation when the *whole* RemoteManagement session's Jolokia exec
  access is unavailable.** Narrowed by T13 (see Status above): a *per-operation* ACL
  denial (specific op names allow/deny-listed in `jolokia-access.xml` while the rest of
  the session's ops remain allowed) is now handled gracefully for
  `forceDynamicChildCreation`/`unForceDynamicChildCreation` specifically - the row
  action hides itself with an explanatory alert, and everything else keeps working.
  What remains open is the coarser case: a `jolokia-access.xml` (or role-based
  restriction) that denies exec *entirely*, so even `connect()` - the very first call -
  fails. The Chart screen's `useRemoteManagementChart` hook already classifies this
  distinctly as an `'exec-denied'` connection-error kind (see
  `remoteManagementErrors.ts`) and shows a dedicated `Alert` explaining the likely cause
  rather than a generic error - but it still just dead-ends there with a Retry button;
  there is no read-only fallback to fall back *to*, since monitor browsing
  (`getMonitors`/`getFieldsForMonitor`) is itself an exec op under Jolokia's ACL model,
  not a plain attribute read. Doing the same for the still-unbuilt "Live dynamic push"
  feature, once built, is also part of what remains open here.
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

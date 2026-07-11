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
- **Deployment verified working** against a real WildFly instance (destiny-console):
  built as a self-hosting WAR (`hawtio-plugin-webapp`) that registers itself as a
  discoverable Hawtio remote plugin via a JMX MBean - see that module's `CLAUDE.md` for
  the two real deployment bugs found and fixed along the way (an `org.perfmon4j` package
  namespace collision, and a doubled `remoteEntry.js` URL).
- **perfmon4j self-management MBean (plumbing) shipped**: `base` now registers a
  read-only JMX MBean (`org.perfmon4j:type=SelfManagement`, attribute `Version`) when the
  `PerfMon` class loads, and the plugin has an "About" nav item that reads it via Jolokia
  (`jolokiaService.readAttribute`) - the first small step toward the "Live dynamic push"
  backlog item below. No write/exec JMX operations were added; see the new Jolokia ACL
  backlog bullet.

## Backlog

Roughly in the order they'd most improve the plugin - not a committed sequence.

- **MBean search/select picker.** Replace the current "paste an ObjectName" text field
  with a searchable/filterable MBean tree or list embedded in our own plugin page.
  Confirmed feasible: hawtio-react's built-in JMX tree view has no extension point for
  injecting a tab into it (`JmxContent.tsx` hardcodes its tab list), and its tree UI
  component isn't exported publicly - but `@hawtio/react` *does* publicly export
  `workspace.getTree()` and `MBeanNode` (which already implements PatternFly's
  `TreeViewDataItem` and has a `.flatten()` method giving every ObjectName in the tree),
  so a self-built search/tree picker using PatternFly's `TreeView`/`SearchInput` against
  that same live data is realistic. Would feed straight into the existing
  `readMBeanAttributes.ts` - no change needed there.
- **Live dynamic push to a running JVM.** Add a way to apply the monitor immediately
  instead of only generating a copy-paste snippet. The prerequisite self-management
  MBean now exists in `base` (`org.perfmon4j.selfmanagement.SelfManagement`, read-only
  today) - this backlog item is now specifically about adding write/exec capability to
  it (e.g. an "apply mBeanSnapshotMonitor config now" operation), which is a materially
  different risk profile than the read-only `Version` attribute shipped so far (see the
  Jolokia ACL bullet immediately below - it should land before or alongside any
  write-capable operation on this MBean). Per earlier discussion: this should **add** a
  "push live" option alongside the XML snippet, not replace it - the XML is still needed
  so the monitor survives a JVM restart.
- **Jolokia ACL policy file (`jolokia-access.xml`).** Deferred so far. Confirmed via
  repo-wide search: there is currently no Jolokia policy file anywhere in this
  deployment, which means Jolokia defaults to fully open read/write/exec access to every
  MBean in the target JVM - a pre-existing condition, not something the self-management
  MBean or About box introduced (both only perform a read). Becomes materially more
  important once anything write/exec-capable is exposed (see "Live dynamic push" above)
  - should land before or alongside that. Also needs a real delivery mechanism resolved:
  Jolokia's policy file has to live on the classpath of whatever WAR runs the actual
  Jolokia servlet, which in the current deployment (destiny-console) is the vanilla,
  third-party `io.hawt:hawtio-war`, not a WAR this repo builds - likely via a WildFly
  deployment-overlay rather than repackaging that WAR, but unconfirmed against a real
  instance.
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

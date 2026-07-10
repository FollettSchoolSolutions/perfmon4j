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
  instead of only generating a copy-paste snippet, via a new perfmon4j self-management
  MBean in `base` (perfmon4j registers no MBeans of its own today). Per earlier
  discussion: this should **add** a "push live" option alongside the XML snippet, not
  replace it - the XML is still needed so the monitor survives a JVM restart.
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

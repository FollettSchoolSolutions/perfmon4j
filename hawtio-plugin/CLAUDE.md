# hawtio-plugin

## Overview
- A Hawtio console plugin that lets a user author a perfmon4j `mBeanSnapshotMonitor` config
  snippet (see [wiki/MBeanSnapShotMonitor.md](../wiki/MBeanSnapShotMonitor.md)) from a live
  JMX MBean, by selecting which attributes to monitor as gauges/counters
- Sprint 1 only: generates a read-only XML snippet for the user to paste into
  `perfmonconfig.xml`. It does not modify the running JVM, and requires zero changes to the
  `base` module. See [ROADMAP.md](ROADMAP.md) for what's deferred (live dynamic push,
  ratios, instance fan-out, formatters) and why.
- This is a TypeScript/React/npm project, **not** a Maven module — it is not listed in the
  root `pom.xml`'s `<modules>`, mirroring the existing `stress-test/` precedent of a
  top-level directory with independent tooling that Maven/CI never sees.
- Packaged as a Hawtio Module Federation remote plugin (see `README.md`) — it is loaded
  into a user's own existing Hawtio console, not shipped as a standalone console.
- A prior generation of this idea shipped as a NetBeans-based VisualVM plugin
  (`netbeans/Perfmon4jMonitor/`, RMI-based, not JMX/Jolokia). See
  [../netbeans/Perfmon4jMonitor/LEGACY_VISUALVM_FEATURES.md](../netbeans/Perfmon4jMonitor/LEGACY_VISUALVM_FEATURES.md)
  for the use cases it served — useful background when scoping future features here, even
  though its code isn't directly portable (wrong transport, and most of it is write/exec).

## Architecture & Patterns
- `src/plugin.ts` — the plugin's sole composition root, exposed via Module Federation
  as `./plugin` (see `webpack.config.js`). Every nav item this plugin registers is
  wired up from here (`src/mbean-snapshot`, `src/about`, `src/chart`), plus the
  `configManager.addProductInfo(...)` call. Each nav item's own `index.ts` has no
  Module Federation entry of its own — only `src/plugin.ts` does.
- `src/mbean-snapshot/index.ts` — registers the `mBeanSnapshotMonitor`-authoring nav
  item via `hawtio.addPlugin(...)`.
- `src/mbean-snapshot/generateSnapshotXml.ts` — pure function, zero React/Jolokia
  dependencies, `{monitorName, jmxName, gauges, counters} -> XML string`. Validation mirrors
  `base/src/main/java/org/perfmon4j/XMLConfigurationParser.java`'s `mBeanSnapshotMonitor`
  handling (required `name`/`jmxName`, at least one of gauges/counters/ratios).
- `src/mbean-snapshot/MBeanSnapshotPanel.tsx` / `AttributeSelectionTable.tsx` — the React UI.
- `src/mbean-snapshot/MBeanTreePicker.tsx` — searchable/filterable MBean tree, composed by
  `MBeanSnapshotPanel`, built from `@hawtio/react`'s `workspace.getTree()`/`MBeanNode`/
  `MBeanTree` and PatternFly's `TreeView`/`PluginTreeViewToolbar`. Only tree nodes with an
  `objectName` (real MBeans, gated via `mbeanTreeHelpers.ts`'s `isMBeanLeaf`) are
  selectable; domain/type grouping nodes just expand/collapse.
- `src/jolokia/readMBeanAttributes.ts` — given an ObjectName, returns the MBean's flat scalar
  attributes (name + JMX type) via `@hawtio/react`'s `workspace.findMBeans(...)`. Composite/
  tabular attributes (e.g. dotted paths like `Usage.max`) are filtered out — deferred.
- `src/chart/` — the "perfmon4j Chart" nav item (live field charting via the `base`
  module's `RemoteManagement` MBean; see ROADMAP.md Status for the feature summary).
  `useRemoteManagementChart.ts` owns the session/poll/reconnect lifecycle;
  `monitorKey.ts`/`rollingSeries.ts` are pure, Jest-tested logic (parsing monitor/field
  key strings, rolling-window trimming), following the same "keep pure logic
  dependency-free" convention as `generateSnapshotXml.ts`. `src/jolokia/
  remoteManagementClient.ts` is the Jolokia-calling wrapper around the MBean's
  operations; its error classification is split into a separate dependency-free
  `remoteManagementErrors.ts` specifically so it stays Jest-testable — importing
  `jolokiaService` (transitively, via `@patternfly/react-core`) pulls in CSS imports
  Jest's default transform can't parse, which is also why `readMBeanAttributes.ts`/
  `readPerfmon4jVersion.ts` have never had test files of their own.
- `src/index.ts` / `src/bootstrap.tsx` — a **local dev harness only**. It bootstraps a full
  standalone `<Hawtio>` console with this plugin registered, so the plugin can be exercised
  with `npm start` against a real Jolokia-enabled JVM without needing a separate Hawtio
  checkout. It is not part of what ships to users (see README's "Installing" section).
- `dev-target/` — a standalone Java program (`TargetMain.java` + `run.sh`) that loads `base`'s
  compiled classes and attaches a Jolokia javaagent, so `npm start` has a real perfmon4j JVM
  to talk to without needing WildFly. See "Local dev harness against a real JVM" below.
- `webpack.config.js`'s `devServer` block carries several fixes discovered getting `npm start`
  working end-to-end against a real JVM for the first time since Sprint 1 (nobody had
  exercised it that far before) — a broken transitive-dependency alias
  (`@thumbmarkjs/thumbmarkjs`), auth stub routes (`/auth/login`, `/user`), and a `/jolokia`
  proxy. Each has an inline comment explaining what it works around and why; read those before
  touching this block, and see "Local dev harness against a real JVM" below for the full story.

## Key findings from building this against hawtio-react (v2.3.0-pre.1) source
- **No tab-injection extension point exists** in the built-in JMX plugin. Its MBean detail
  view (`packages/hawtio/src/plugins/jmx/JmxContent.tsx` in hawtio-react) builds its
  Attributes/Operations/Chart tabs from a hardcoded array with no plugin-registry hook. This
  is why this plugin is a standalone nav item (user pastes/types the ObjectName) rather than
  a tab inside the existing MBean view.
- `@hawtio/react` exports `workspace` (tree/MBean lookup) and `jolokiaService` at its top
  level (`plugins/shared`), reusing the host console's already-connected Jolokia session —
  this plugin does not open its own connection.
- Registering a remote plugin against a real Hawtio console is done via the console's own
  bootstrap code calling `hawtio.addUrl(<discovery-url>)`, where `<discovery-url>` serves a
  JSON array of `{url, scope, module}`. There is no `hawtconfig.json` field and no in-console
  "add custom plugin" UI action for this — see `README.md` for what that means for
  installation.
- The bare `npm start` dev harness has no backend, which breaks several things `@hawtio/react`
  assumes exist, all traced by reading its bundled `dist/*.js` source directly (no source maps
  shipped for `node_modules`, so this took actual reverse-engineering — see `webpack.config.js`
  inline comments for the exact code paths):
  - Its auth bootstrap (`ConfigManager.initialize()`) falls back to a built-in "Form
    Authentication" login screen when `GET auth/config/login` 404s. `LoginService.login()`
    treats *any* HTTP 200 from `POST auth/login` as success without checking the body, and a
    successful login triggers a full page reload that re-checks `GET user` (expects a 200
    response whose JSON body is the username string itself, e.g. `"admin"`) to decide
    `isLogin`. Both are stubbed in `devServer.setupMiddlewares`.
  - The "Connect" nav item hides itself unless `GET proxy/enabled` succeeds (a feature this
    harness doesn't have) — **not fixed**, just avoided: `jolokiaService` separately
    auto-discovers a Jolokia agent by probing a fixed list of same-origin paths
    (`JOLOKIA_PATHS`: `jolokia`, `/hawtio/jolokia`, `/jolokia` — none with a trailing slash),
    which `devServer.proxy` satisfies by forwarding those to a real agent. No Connect
    connection is ever actually established in this dev harness.
  - If that auto-discovery fails, `jolokiaService` silently falls back to an inert
    `DummyJolokia` client whose `request()` never calls its success/error callback at all — any
    Jolokia call (ours or Hawtio's own built-in plugins') then hangs forever with **zero**
    network request ever firing, which looks exactly like a frontend bug but is entirely a
    same-origin-discovery problem.
  - The standalone `jolokia-jvm` 1.x agent line (as opposed to `jolokia-agent-jvm` 2.x) has two
    further gotchas for this specific use case: its embedded HTTP server 404s
    ("No context found for request") on any request without a trailing slash, unlike a real
    servlet-mapped deployment (worked around by `pathRewrite` always appending one); and its
    response format doesn't fully match what this project's newer `jolokia.js` client (bundled
    in `@hawtio/react` `2.3.0-pre.1`) expects for read requests specifically — the HTTP
    response is correct on the wire, but `jolokiaService.readAttribute()` still resolves with
    `undefined`. Use `org.jolokia:jolokia-agent-jvm:2.x:jar:javaagent` for local testing, not
    the old `jolokia-jvm` artifact.

## Stack Best Practices
- TypeScript + React + PatternFly v6, built with webpack's `ModuleFederationPlugin`,
  scaffolded directly from Hawtio's own reference template,
  [`hawtio-sample-plugin-ts`](https://github.com/hawtio/hawtio-sample-plugin-ts) (branch
  `hawtio-5.x`) — don't improvise webpack/Module Federation config from scratch.
- `package.json` pins `@module-federation/utilities` to `3.1.83` via `overrides` — newer
  versions pulled in by `@hawtio/react`'s dependency chain reference Node's `url` module,
  which webpack 5 no longer polyfills by default, breaking the production build of the local
  dev harness (`hawtio-sample-plugin-ts` does the same thing via yarn `resolutions`).
- `generateSnapshotXml.ts` has no dependencies specifically so it stays unit-testable with
  Jest without a browser or a live MBean server — keep it that way; put any Hawtio/Jolokia
  code in a caller, not in this function.

## Commands & Scripts
- **Install**: `npm install`
- **Unit tests**: `npm test`
- **Typecheck**: `npm run typecheck`
- **Production build**: `npm run build` (outputs `dist/remoteEntry.js` and chunks)
- **Local dev harness**: `npm start` (serves a full standalone Hawtio console with this
  plugin registered at `http://localhost:3001/`)

### Local dev harness against a real JVM (no WildFly needed)
`npm start` alone serves the console shell, but the plugin needs a real JVM with perfmon4j
attached and a Jolokia agent to actually exercise MBean reads. `dev-target/` provides a
throwaway one:
1. Build `base` first: `mvn clean install` (or `mvn compile`) from `base/`.
2. `dev-target/run.sh [port]` (defaults to port 8778) — compiles and runs a tiny standalone
   Java process (`TargetMain.java`) against `base/target/classes`, with a Jolokia javaagent
   attached. It auto-picks the newest `org.jolokia:jolokia-agent-jvm:*:javaagent` jar in the
   local `~/.m2` repo; fetch one first if none is present (see the script's header comment —
   **must** be the 2.x `jolokia-agent-jvm` line, not the old `jolokia-jvm` 1.x artifact; see
   "Key findings" above for why).
3. `npm start` — its `devServer.proxy` config forwards `/jolokia` and `/hawtio/jolokia`
   straight to that target JVM automatically, so the plugin's Jolokia auto-discovery finds it
   with no manual "Connect" step.
4. Hitting a login screen on first load is expected in this bare harness (see "Key findings"
   above) — any non-empty username/password gets past it.

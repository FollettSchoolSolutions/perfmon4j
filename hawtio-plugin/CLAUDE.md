# hawtio-plugin

## Overview
- A Hawtio console plugin that lets a user author a perfmon4j `mBeanSnapshotMonitor` config
  snippet (see [wiki/MBeanSnapShotMonitor.md](../wiki/MBeanSnapShotMonitor.md)) from a live
  JMX MBean, by selecting which attributes to monitor as gauges/counters
- Sprint 1 only: generates a read-only XML snippet for the user to paste into
  `perfmonconfig.xml`. It does not modify the running JVM, and requires zero changes to the
  `base` module. See [ROADMAP.md](ROADMAP.md) for what's deferred (live dynamic push,
  ratios, instance fan-out, formatters, an MBean search/select picker) and why.
- This is a TypeScript/React/npm project, **not** a Maven module — it is not listed in the
  root `pom.xml`'s `<modules>`, mirroring the existing `stress-test/` precedent of a
  top-level directory with independent tooling that Maven/CI never sees.
- Packaged as a Hawtio Module Federation remote plugin (see `README.md`) — it is loaded
  into a user's own existing Hawtio console, not shipped as a standalone console.

## Architecture & Patterns
- `src/mbean-snapshot/index.ts` — plugin entry point, exposed via Module Federation as
  `./plugin` (see `webpack.config.js`). Registers a standalone top-level nav item via
  `hawtio.addPlugin(...)`.
- `src/mbean-snapshot/generateSnapshotXml.ts` — pure function, zero React/Jolokia
  dependencies, `{monitorName, jmxName, gauges, counters} -> XML string`. Validation mirrors
  `base/src/main/java/org/perfmon4j/XMLConfigurationParser.java`'s `mBeanSnapshotMonitor`
  handling (required `name`/`jmxName`, at least one of gauges/counters/ratios).
- `src/mbean-snapshot/MBeanSnapshotPanel.tsx` / `AttributeSelectionTable.tsx` — the React UI.
- `src/jolokia/readMBeanAttributes.ts` — given an ObjectName, returns the MBean's flat scalar
  attributes (name + JMX type) via `@hawtio/react`'s `workspace.findMBeans(...)`. Composite/
  tabular attributes (e.g. dotted paths like `Usage.max`) are filtered out — deferred.
- `src/index.ts` / `src/bootstrap.tsx` — a **local dev harness only**. It bootstraps a full
  standalone `<Hawtio>` console with this plugin registered, so the plugin can be exercised
  with `npm start` against a real Jolokia-enabled JVM without needing a separate Hawtio
  checkout. It is not part of what ships to users (see README's "Installing" section).

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

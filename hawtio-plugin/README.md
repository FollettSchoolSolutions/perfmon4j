# perfmon4j Hawtio plugin

Sprint 1 of a Hawtio plugin for perfmon4j. Lets you pick attributes off a JMX
MBean you're already looking at and generates the corresponding perfmon4j
`<mBeanSnapshotMonitor>` XML for you to paste into `perfmonconfig.xml`.

**This plugin does not touch your running JVM.** It only generates a config
snippet. See [wiki/MBeanSnapShotMonitor.md](../wiki/MBeanSnapShotMonitor.md)
for the full XML grammar, and the Sprint 1 spec for what's intentionally out
of scope for this first slice (ratios, multi-instance fan-out, live push,
etc).

## Build & test

```
npm install
npm test        # unit tests for the XML generator
npm run typecheck
npm run build    # production build -> dist/remoteEntry.js
```

## Try it locally

```
npm start
```

Opens a full standalone Hawtio console at `http://localhost:3001/` with this
plugin already registered as a top-level nav item ("perfmon4j"), so you can
exercise it end to end against any JVM with a Jolokia agent attached. This
harness is for local development only — it's not what real users load (see
below).

## Installing into your own Hawtio console

This plugin is packaged as a Hawtio **Module Federation remote plugin** — the
same mechanism used by Hawtio's own
[hawtio-sample-plugin-ts](https://github.com/hawtio/hawtio-sample-plugin-ts).
`npm run build` produces a static bundle (`dist/remoteEntry.js` + chunks) —
no backend/Java component of any kind is required to host it; any static
file server works.

**Important caveat, confirmed by reading `hawtio-react`'s own source:**
loading a remote plugin into an *existing* Hawtio console is a **code-level**
mechanism, not a runtime "add custom plugin" button or a `hawtconfig.json`
setting. A Hawtio console registers remote plugins by calling, in its own
bootstrap code:

```ts
hawtio.addUrl('<plugin-discovery-url>')
```

where `<plugin-discovery-url>` is any HTTP endpoint — including a plain
static JSON file — that returns a JSON array shaped like:

```json
[{ "url": "https://your-host/perfmon4j-hawtio-plugin/remoteEntry.js", "scope": "perfmon4jHawtioPlugin", "module": "./plugin" }]
```

(`scope` and `module` come from this plugin's `webpack.config.js` and must
match exactly; `url` is wherever you host the built `dist/` output.)

Whether *your* Hawtio installation already exposes a way to add such a
discovery URL (an environment variable, a properties file, etc.) depends on
how it's packaged (hawtio-springboot-starter, hawtio-online, a custom
hawtio-react build, ...) — check that distribution's own docs. If it doesn't,
you'll need a custom console build that calls `hawtio.addUrl(...)` yourself,
following the pattern in `src/bootstrap.tsx` of this project or
`hawtio-sample-plugin-ts`'s `app/src/bootstrap.tsx`.

## Usage

1. Open the "perfmon4j" nav item in Hawtio.
2. Paste in the ObjectName of the MBean you want to monitor (you can copy it
   from the JMX tree view you're already using — it's shown under each
   MBean's title there).
3. Click "Load attributes".
4. For each attribute, choose Skip / Gauge / Counter.
5. Enter a monitor name.
6. Copy the generated XML into your `perfmonconfig.xml`. perfmon4j's existing
   config file-watcher will pick it up without a restart.

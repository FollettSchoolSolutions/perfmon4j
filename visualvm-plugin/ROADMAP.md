# VisualVM Plugin Roadmap

Backlog and status for `visualvm-plugin/`. This file tracks *what's next and why* — for
architecture/patterns/gotchas, see `CLAUDE.md` instead; this file shouldn't duplicate that.

## Status

- **Maven build modernization shipped**: the plugin's source and functionality are unchanged
  from the original NetBeans-IDE-only Ant project (`netbeans/Perfmon4jMonitor/`, now retired);
  it now builds standalone via `nbm-maven-plugin`, with every VisualVM/NetBeans Platform
  dependency resolved from Maven Central instead of an IDE-registered platform install. See
  `CLAUDE.md` for the full dependency/build-gotcha writeup.

## Backlog

Roughly in the order they'd most improve the plugin's distribution/polish - not a committed
sequence.

- **Sign the `.nbm`.** Unsigned modules currently show a trust warning in VisualVM's plugin
  installer. `nbm-maven-plugin`'s `nbm` goal already supports this out of the box (confirmed via
  its own `-Dgoal=nbm -Ddetail=true` help output): `keystore`/`keystorealias`/
  `keystorepassword` configuration parameters. Nothing to build here — this is really "obtain a
  code-signing keystore/cert and wire it into `pom.xml`," plus decide where the keystore and its
  password live for a sole-maintainer repo (most likely GitHub Actions secrets, which pairs
  naturally with the release-automation backlog item below rather than being solved standalone).
- **Research more modern charting options.** The plugin is deliberately pinned to
  `org.jfree:jfreechart:1.0.19`/`jcommon:1.0.24` (see `CLAUDE.md`) rather than the current
  `1.5.x` line, because `1.5.x` restructured/dropped the `jcommon` split that
  `DynamicTimeSeriesChart` and the rest of `chart/*` depend on. Worth a scoped look at either a
  real `1.5.x` migration (real API changes needed - not a drop-in bump) or a different
  Swing-compatible charting library. Constrained to Swing/AWT options specifically, since this is
  a NetBeans Platform desktop UI, not a web view - nothing web-charting-related applies here.
- **Research options to deploy the `.nbm` on release and snapshot builds, and link the latest
  from the wiki.** Right now `wiki/Installing-the-VisualVM-Plugin.md` tells users to build the
  plugin from source, because no pre-built artifact is published anywhere. Two shapes worth
  comparing: (a) a proper NetBeans update site/cluster (`nbm-maven-plugin` has aggregator goals
  for exactly this - the standard NetBeans mechanism for an installable, auto-updatable module),
  or (b) the simpler route of attaching the built `.nbm` as a GitHub Actions release/workflow
  artifact. Whichever is chosen, `wiki/Installing-the-VisualVM-Plugin.md`'s build-from-source
  step should be replaced with a direct download link that stays current. Prerequisite either
  way: this module isn't in root `pom.xml`'s `<modules>` or the GitHub Actions workflow yet (see
  root `CLAUDE.md`'s Module Layout note) - any release automation needs that addressed first.

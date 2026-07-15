# perfmon4j

## Overview
- Java instrumentation agent for monitoring application performance in production servers, via bytecode injection of timers (no code changes required to instrument existing methods)
- Multi-module Maven project: a core agent (`base`), a zero-dependency stub API app code can compile against (`agent-api`), and a set of application-server / framework integration modules
- Collects interval metrics (throughput, response time, active threads, SQL time) and point-in-time snapshots (JVM/JMX/custom), writing them to pluggable appenders (text log, JDBC database, InfluxDB, Azure Log Analytics)
- Published to Maven Central under the `org.perfmon4j` group id; see [README.md](README.md) for installation and [readme.txt](readme.txt) for the full version changelog
- Primary consumers: Java web applications and application servers (Tomcat, WildFly, Quarkus) that need low-overhead production performance monitoring

## Module Layout
- `agent-api/` — no-op stub classes (`api.org.perfmon4j.agent.*`) application code compiles against directly; rewritten in place by the javaagent when attached, otherwise runs as harmless no-ops. Zero-dependency, test-scope-only dependency of `base`. See `agent-api/CLAUDE.md`.
- `base/` — the real implementation: `PerfMon`/`PerfMonTimer` core, Javassist-based bytecode instrumentation, snapshot generation, appenders, reactive-context support. See `base/CLAUDE.md`.
- `dbupgrader/` — Liquibase-based schema upgrade tool for the SQL appender's database.
- `genericfilter/`, `servlet/`, `tomcat7/`, `wildfly8/`, `quarkus2x/`, `quarkus3x/` — application-server and framework integration modules (servlet filters, valves, Quarkus extensions).
- `reportconsole/` — standalone reporting UI for SQL-appender-collected data.
- `utils/` — standalone CLI utilities for parsing/visualizing TextAppender log output.
- `hawtio-plugin/` — Hawtio console plugin for authoring perfmon4j `mBeanSnapshotMonitor` config from a live JMX MBean. TypeScript/React/npm project, **not** a Maven module — like `stress-test/`, it is not listed in root `pom.xml`'s `<modules>` and has its own independent build tooling. See `hawtio-plugin/ROADMAP.md` for current status and backlog.
- `hawtio-plugin-webapp/` — thin WAR that self-hosts `hawtio-plugin/`'s built bundle and registers it as a discoverable Hawtio remote plugin via a JMX MBean, so it can be dropped into an existing Hawtio deployment with no rebuild. A real Maven module (`packaging=war`) but, like `hawtio-plugin/`, deliberately not listed in root `pom.xml`'s `<modules>` since building it needs Node.js.
- `visualvm-plugin/` — VisualVM console plugin exposing live perfmon4j interval/snapshot
  monitors, ad-hoc charting, and thread-trace scheduling over the agent's RMI remote-management
  interface. A NetBeans Platform module built via `nbm-maven-plugin`, resolving the VisualVM/
  NetBeans Platform APIs from Maven Central — like `stress-test/`, it is not listed in root
  `pom.xml`'s `<modules>` and has its own independent build. See `visualvm-plugin/CLAUDE.md`.
- Each module has its own `CLAUDE.md` with module-specific architecture, patterns, and anti-patterns — consult it before making changes inside that module.
- See [Perfmon4j API and Agent Architecture](wiki/Perfmon4j-API-and-Agent-Architecture.md) for how `agent-api` and `base` connect at runtime.

## Architecture & Patterns
- Root `pom.xml` defines the version directly (not inherited from a parent BOM) and lists all modules; bump it here when cutting a release
- Gitflow branching: `develop` (integration branch, always releasable-ish), `master` (released code only), `feature/<Name>` branches merged into `develop` with `--no-ff` and a descriptive merge commit, `release/<version>` branches cut from `develop` for release stabilization
- This is a sole-maintainer repo — direct pushes to `develop`/`master` bypassing branch-protection PR requirements are expected and normal, not a mistake to flag
- CI builds/tests/publishes via GitHub Actions (`.github/workflows/maven.yml`); releases publish to Maven Central from the `master`-branch run specifically (a tag-only run skips publish)

## Stack Best Practices
- Java 11 baseline; per-module `CLAUDE.md` files note where a module needs a newer JDK (e.g. the `stress-test/` virtual-thread harness requires JDK 21+ and is deliberately excluded from the Java 11 Maven build)
- JUnit 3-style tests (`extends TestCase`, methods named `testXxx`) throughout — do not introduce `@Test`-annotated JUnit 4/5-style tests
- Full test suite (`mvn test` from `base/`) requires system properties for external jars (`JAVASSIST_JAR`, `DERBY_EMBEDDED_DRIVER`, `LOG4J_JAR`) — these are already wired into the surefire config in `base/pom.xml`, so plain `mvn test` works if the local Maven repo has those artifacts
- `readme.txt`'s top `** <version> - TBD` block tracks unreleased functional changes merged to `develop`; add a bullet there for any user-visible change (see the `update-change-log` skill)

## Anti-Patterns
- Don't assume the `agent-api`/`base` split is a single generic mechanism — it's four distinct attach/rewrite paths (see the architecture wiki page); a fix or pattern that applies to one does not automatically apply to the others
- Don't let `agent-api` depend on anything at runtime, or add non-test dependencies to it from `base` — it must work from the root classloader before any application class is visible
- Don't hand-edit only one copy of a `@SnapShot*` annotation — `agent-api` and `base` each keep an independent copy that must be kept attribute-compatible by hand (no build-time sync); see `AnnotationTransformer` in `base`
- Don't edit only one copy of a wiki page — see "Project Wiki Publishing" below
- Never use `-uall` with `git status` (can cause memory issues on this repo's size)

## Commands & Scripts
- **Build everything**: `mvn clean install` from the repo root
- **Build/test a single module**: `mvn clean install` from that module's directory (e.g. `base/`)
- **Cut a release**: see the `cut-release` skill and `.github/maven/MAVEN_CENTRAL_PUBLISHING.md`
- **Update the changelog**: see the `update-change-log` skill (edits `readme.txt`'s top `TBD` block)

Project Wiki Publishing
-----------------------
This project's documentation wiki lives in **two separate git repositories** that are kept in sync **manually**. A wiki change is not complete until it has been applied to both.

1. **Source copy — the `wiki/` folder in this repo.** Wiki pages are authored here as plain Markdown files (e.g. `wiki/Configuring-the-Java-Agent.md`) and committed to the `develop` branch like any other source file. This copy is browsable in the source tree but does **not** render on GitHub's Wiki tab.
2. **Published copy — the separate GitHub Wiki repo.** The Wiki tab is backed by a distinct repository, `git@github.com:FollettSchoolSolutions/perfmon4j.wiki.git` (default branch `master`). Only pages pushed here appear at `https://github.com/FollettSchoolSolutions/perfmon4j/wiki/<Page-Name>`.

To publish or update a wiki page:

```
# 1. Author/commit the page in the main repo's wiki/ folder (on develop)
git add wiki/<Page-Name>.md
git commit -m "..."
git push origin develop

# 2. Mirror it into the GitHub Wiki repo so it renders on the Wiki tab
git clone git@github.com:FollettSchoolSolutions/perfmon4j.wiki.git /tmp/p4j-wiki
cp wiki/<Page-Name>.md /tmp/p4j-wiki/
cd /tmp/p4j-wiki
git add <Page-Name>.md
git commit -m "..."
git push origin master
```

Notes:
- **Wiki URLs omit the `.md` extension** — GitHub renders pages, it does not serve raw `.md` files. The file `Configuring-the-Java-Agent.md` is reached at `.../wiki/Configuring-the-Java-Agent`.
- Because the two copies are synced by hand, they can drift. When editing an existing page, update **both** repos.
- `_Sidebar.md` (in the wiki repo) controls the wiki navigation sidebar but does not list every page; new pages still appear in the wiki's automatic "Pages" list without a sidebar entry.
- `wiki/Home.md` is the landing page for both copies — keep it updated with a link to every page when adding or removing one.

Maintainers: update this file as the module layout, branching convention, or release process changes.

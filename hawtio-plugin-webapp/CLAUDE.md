# hawtio-plugin-webapp

## Overview
- A thin, deployable WAR that self-hosts the `hawtio-plugin/` npm project's built bundle
  AND registers it as a discoverable Hawtio remote plugin - drop this one WAR into any
  WildFly (or other servlet container) `deployments/` folder alongside an existing Hawtio
  console WAR (e.g. the plain `io.hawt:hawtio-war` distribution) and it makes itself known,
  no Hawtio rebuild or file edits required.
- This IS a real Maven module (packaging `war`), but is deliberately **not** listed in the
  root `pom.xml`'s `<modules>` - like `hawtio-plugin/`, keeping it out of the main reactor
  means a plain `mvn clean install` from the repo root never needs Node.js. Build it
  explicitly: `mvn -f hawtio-plugin-webapp/pom.xml clean package`.

## How the plugin discovery actually works (reverse-engineered from a real deployed
## hawtio-war 5.0.0, since this isn't documented anywhere)
- `io.hawt.web.plugin.PluginServlet` (bundled in hawtio-war, mapped at `/plugin/*`) queries
  `ManagementFactory.getPlatformMBeanServer()` for MBeans matching ObjectName pattern
  `hawtio:type=plugin,name=*`, and returns their `Url`/`Scope`/`Module` attributes (plus a
  few optional ones) as JSON. The Hawtio frontend's own bootstrap code calls
  `hawtio.addUrl('plugin')` once at page load to fetch this.
- So "registering a plugin" against an already-deployed Hawtio console is just registering
  one MBean on the platform MBeanServer of the same JVM - no code inside the Hawtio WAR
  itself needs to change.
- `PluginRegistrationListener` (`@WebListener`) does exactly that at deploy time, and
  derives `Url` from `ServletContext.getContextPath() + "/remoteEntry.js"` rather than a
  hardcoded host/port - it's origin-relative, so the browser resolves it against whatever
  host/port this WAR happens to be deployed under, and it keeps working if this WAR is
  renamed or redeployed under a different context path.
- `getScope()`/`getModule()` in `Perfmon4jHawtioPlugin` **must** stay in sync with
  `hawtio-plugin/webpack.config.js`'s `ModuleFederationPlugin` `name`/`exposes` values.

## Architecture & Patterns
- `Perfmon4jHawtioPluginMBean` / `Perfmon4jHawtioPlugin` - the plugin-descriptor MBean.
  Attribute names (`Url`, `Scope`, `Module`) are dictated by `PluginServlet`, not chosen
  here - don't rename these getters.
- `PluginRegistrationListener` - registers the MBean on deploy, unregisters it on undeploy
  (`contextDestroyed`) so a redeploy doesn't leave a stale/duplicate registration.
- No `web.xml` - the listener is registered via the `@WebListener` annotation
  (`failOnMissingWebXml=false` in `pom.xml`'s `maven-war-plugin` config).
- The `exec-maven-plugin` executions in `pom.xml` run `npm install`/`npm run build` in
  `../hawtio-plugin` using whatever `npm` is already on the build machine's PATH (not a
  separately-downloaded Node, unlike `frontend-maven-plugin`'s usual pattern) - simpler,
  but means `npm` must be on PATH wherever this module is built.
- `maven-war-plugin`'s `webResources` pulls `../hawtio-plugin/dist/**` (the webpack output,
  including `remoteEntry.js`) straight into the WAR root as static content.

## Anti-Patterns
- **Never put these classes under `org.perfmon4j.*`.** They live in
  `com.follett.perfmon4j.hawtioplugin` deliberately. On any JVM where the real perfmon4j
  javaagent is attached, `org.perfmon4j` is commonly listed in that WildFly instance's
  `-Djboss.modules.system.pkgs=...` (confirmed against a real deployment - see
  `wrapper.conf`'s `wrapper.java.additional.11`), which makes JBoss Modules treat
  `org.perfmon4j.*` as a reserved system package: it refuses to load ANY class under that
  package from a deployment's own `WEB-INF/classes` and only looks in the system
  classloader. The first version of this module used `org.perfmon4j.hawtioplugin` and
  failed deployment with `ClassNotFoundException: org.perfmon4j.hawtioplugin.<Class>` even
  though the class was verifiably present and valid inside the WAR - the exception is
  misleading; it looks like a packaging bug but is actually this namespace reservation.

## Commands & Scripts
- **Build**: `mvn -f hawtio-plugin-webapp/pom.xml clean package` (from repo root) or
  `mvn clean package` (from this directory) - produces
  `target/perfmon4j-hawtio-plugin.war`.
- **Deploy**: copy `target/perfmon4j-hawtio-plugin.war` into the servlet container's
  deployments folder (e.g. WildFly's `standalone/deployments/`) alongside the existing
  Hawtio console WAR - same JVM, same platform MBeanServer.
- **Verify it registered**: after deploy, hit `<hawtio-context>/plugin` directly - the
  JSON response should include an entry with `"Scope":"perfmon4jHawtioPlugin"`. The
  context path is whatever the Hawtio console WAR's own `jboss-web.xml` `<context-root>`
  says (NOT necessarily derived from its filename - e.g. a WAR renamed to
  `destiny-console.war` may still have a `context-root` of `/destiny-console`, unrelated to
  the rename). Reload the Hawtio page in the browser to pick it up (the plugin list is fetched
  once at page bootstrap, not polled).

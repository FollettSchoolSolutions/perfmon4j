Build hawtio-plugin-webapp and deploy it into a WildFly standalone/deployments folder,
alongside an existing Hawtio console, so the perfmon4j Hawtio plugin becomes discoverable.

## CONTEXT

`hawtio-plugin-webapp/` (see its `CLAUDE.md`) is a self-contained WAR: it builds the
`hawtio-plugin/` npm project and bundles the output (`remoteEntry.js`) as static content,
plus a `PluginRegistrationListener` that registers a JMX MBean
(`hawtio:type=plugin,name=perfmon4j`) on the platform MBeanServer at deploy time.
`io.hawt.web.plugin.PluginServlet`, bundled in any hawtio-war-based Hawtio console
deployed in the SAME JVM, discovers that MBean and serves it to the browser — so dropping
this WAR into the same WildFly instance's `standalone/deployments/` folder as an existing
Hawtio console is enough to make the plugin available. No Hawtio rebuild, no file edits
inside the existing Hawtio deployment.

This module is not wired into the root Maven reactor (see root `CLAUDE.md`), so it must be
built with an explicit `-f` pointer.

Deploying into a live WildFly's `deployments/` folder is a real change to a running server
— WildFly's deployment scanner will pick up a new/changed `.war` there automatically if
scanning is enabled, which triggers an actual (re)deployment. Treat this as a risky,
hard-to-reverse action: confirm the exact target path and what will happen with the user
BEFORE copying anything.

## INPUTS

- The path to the target WildFly `standalone/deployments/` directory (or the directory
  containing the existing Hawtio console WAR, one level up). ALWAYS ask the user for this —
  never guess or reuse a path from memory of a previous conversation, since it's
  environment-specific and this action touches a live server.
- (optional) The deployed WAR's file name. Defaults to `perfmon4j-hawtio-plugin.war` (the
  module's Maven `finalName`). Renaming it changes its context path, and therefore the
  `Url` the plugin registers itself under (see `PluginRegistrationListener`) — only rename
  if the user asks.

## INSTRUCTIONS

1. Ask the user for the target deployments directory path if not already given in this
   conversation. Do not proceed without it.
2. Verify the path: confirm the directory exists (`ls`/`Get-ChildItem`). List its current
   `.war` contents so the user can see what's already deployed there (e.g. the existing
   Hawtio console WAR) — this is a sanity check, not a hard requirement, since the user may
   have renamed things.
3. Build the artifact fresh:
   - `mvn -f hawtio-plugin-webapp/pom.xml clean package`
   - Confirm `hawtio-plugin-webapp/target/perfmon4j-hawtio-plugin.war` (or the custom name
     from INPUTS) was produced. If the build fails, STOP and report the error — do not
     attempt to deploy a stale or missing artifact.
4. Before copying, tell the user exactly what will happen: the file that will be copied,
   the exact destination path, and that if the target WildFly instance has its deployment
   scanner running, this may trigger an immediate (re)deployment. Ask for explicit
   confirmation to proceed.
5. Once confirmed, copy the WAR into the target directory.
6. Check for WildFly deployment-scanner marker files next to the deployed WAR
   (`<name>.war.deployed`, `<name>.war.failed`, `<name>.war.isdeploying`,
   `<name>.war.undeployed`) to report on deployment state:
   - If `.deployed` appears, report success.
   - If `.failed` appears, read it (it usually contains the failure reason) and report it —
     do not guess at a fix without seeing the actual error.
   - If neither appears after a short wait, say so plainly — it likely means this WildFly
     instance's deployment scanner isn't running/auto-deploying, and the user needs to
     redeploy through however they normally manage this server (management console, CLI,
     restart).
7. Tell the user how to verify the plugin is actually live:
   - Hit `<hawtio-context-path>/plugin` on the running Hawtio console (e.g.
     `http://host:port/hawtio/plugin`) directly — the JSON response should contain an
     entry with `"Scope":"perfmon4jHawtioPlugin"`.
   - Reload the Hawtio console page in the browser — the plugin list loads once at page
     bootstrap, not polled, so an already-open tab won't pick it up without a reload.

## OUTPUT FORMAT

### Target
- **Deployments directory:** <path, as confirmed with the user>
- **Existing WARs found there:** <short list>

### Build
- **Command:** `mvn -f hawtio-plugin-webapp/pom.xml clean package`
- **Result:** <success / failure + error>

### Deploy
- **Confirmed with user:** yes/no
- **Copied to:** <full destination path>
- **Marker file observed:** <.deployed / .failed (+ reason) / none>

### Verification steps for the user
- <the /plugin URL to check, and the "reload the page" reminder>

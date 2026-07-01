Cut a release of perfmon4j and publish it to Maven Central by landing a
non-SNAPSHOT version on `master`, then reopen development on `develop`.

## CONTEXT

Publishing to Maven Central is already automated. The GitHub Actions workflow
`.github/workflows/maven.yml` runs `mvn deploy -P release,mvnCentral` on every push
to `develop`, `master`, or `hotfix/*`. The `release` profile attaches Javadoc + sources
jars and GPG-signs the artifacts; the `mvnCentral` profile uses the
`central-publishing-maven-plugin` with `autoPublish=true`, so a validated deploy goes
live on Central automatically (no manual "release from staging" step, and it cannot be
undone once published). The GPG key and Central credentials live only in GitHub secrets,
so the actual publish happens in CI — not from a local machine.

This project follows a git-flow layout: `develop` is the default/integration branch and
must always carry a `-SNAPSHOT` version; `master` carries released versions. A release is
triggered by landing the non-SNAPSHOT commit on `master`.

The Maven version is hard-coded in ALL module poms (the root `pom.xml` `<version>` and
each child pom's `<parent><version>`), so version changes must use `mvn versions:set`,
which updates every module at once.

CRITICAL: never push a non-SNAPSHOT version to `develop`. Because CI also deploys on
`develop` pushes, doing so would attempt to publish the release version a second time
(a duplicate publish fails, since Central versions are immutable). Cut the release on a
short-lived `release/<version>` branch so the non-SNAPSHOT commit only ever reaches
`master`, and `develop` moves straight to the next `-SNAPSHOT`.

## INPUTS

- (optional) The release version to cut. If omitted, derive it from the root `pom.xml`
  `<version>` by stripping `-SNAPSHOT` (e.g. `2.2.1-SNAPSHOT` -> `2.2.1`).
- (optional) The next development version. If omitted, bump the release version's patch
  segment and re-append `-SNAPSHOT` (e.g. `2.2.1` -> `2.2.2-SNAPSHOT`).

## INSTRUCTIONS

1. Branch guard — STOP unless releasing from `develop`:
   - Run `git rev-parse --abbrev-ref HEAD`. If the current branch is NOT exactly
     `develop` (e.g. a `feature/*`, `release/*`, `hotfix/*`, or `master` branch), ABORT
     immediately. Do not change any files, versions, or branches. Tell the user they must
     `git checkout develop` (and merge/finish their feature work first) before a release
     can be cut, and stop here.
2. Pre-flight — confirm it is safe to release:
   - Working tree is clean (`git status`) and `develop` is up to date with
     `origin/develop`.
   - CI is green on `develop` (the latest push's "Branch Push or Pull Request" run
     succeeded). Do NOT release on a red build.
   - The top-most `** <version> - TBD` block in `readme.txt` accurately lists everything
     shipping in this release. If it is stale, run `/update-change-log` first.
   - Confirm the release version and next development version (see INPUTS) with the user
     before making any changes.
3. Cut the release on a dedicated branch (keeps the non-SNAPSHOT commit off `develop`):
   - `git checkout -b release/<version> develop`
   - `mvn versions:set -DnewVersion=<version> -DgenerateBackupPoms=false`
   - In `readme.txt`, date-stamp the change log: change `** <version> - TBD` to
     `** <version> - MM/DD/YY` (today's date). Change ONLY that header line.
   - `git commit -am "Release <version>"`
4. Publish by landing on `master` (this triggers the Central deploy):
   - `git checkout master && git merge --no-ff release/<version>`
   - `git tag v<version>`
   - `git push origin master --tags`
   - Watch the resulting Actions run to completion. The "Publish to Maven Central" step
     must succeed. If it fails, STOP and report — do not proceed to the version bump.
5. Verify the artifact is live on Central before continuing (it may take a few minutes):
   - Check https://central.sonatype.com or
     `https://repo1.maven.org/maven2/org/perfmon4j/perfmon4j/<version>/`.
6. Reopen development on `develop`:
   - `git checkout develop && git merge --no-ff release/<version>` (brings the dated
     change log entry back into `develop`).
   - `mvn versions:set -DnewVersion=<next-snapshot> -DgenerateBackupPoms=false`
   - In `readme.txt`, add a fresh `** <next-version> - TBD` block above the just-dated
     block (leave the dated block untouched).
   - `git commit -am "Begin <next-version> development"`
   - `git push origin develop` (deploys the new SNAPSHOT — not a re-release).
7. Clean up: `git branch -d release/<version>`.
8. Never rewrite or delete a published tag/version. Central releases are immutable; to
   correct a bad release, cut a new (higher) version instead.

## OUTPUT FORMAT

### Pre-flight
- **Current branch:** <branch> (must be `develop`, else aborted)
- **Release version:** <version>  |  **Next dev version:** <next-snapshot>
- **Branch/CI state:** <clean & green / issues found>
- **Change log block:** <ready / updated via /update-change-log>

### Release steps performed
- <ordered list of the git/maven commands actually run and their result>

### Publish verification
- **CI run:** <link/status of the master Actions run>
- **Central:** <confirmed present at <url> / pending>

### Development reopened
- **develop now at:** <next-snapshot>
- **New change log block:** `** <next-version> - TBD`

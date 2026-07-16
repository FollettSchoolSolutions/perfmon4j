# Monitoring Tab — Work Breakdown

Sprint-ready engineering tasks derived from
[`MONITORING_TAB_SPIKE.md`](MONITORING_TAB_SPIKE.md). Slice letters (A–D) match the
spike's Sequence section. Owner column left blank for a sole-maintainer repo.

Grounding: existing frontend lives in `hawtio-plugin/src/chart/`
(`ChartPanel`, `LiveChart`, `MonitorTree`, `AddFieldModal`, `MonitoringDetailTabs`,
`SubscribedFieldsTable`, `useRemoteManagementChart`, pure
`monitorKey`/`monitorTreeLogic`/`rollingSeries`) and the shell is
`src/Perfmon4jPanel.tsx`. Backend ops are on
`org.perfmon4j:type=RemoteManagement` (`base`), wrapped by
`src/jolokia/remoteManagementClient.ts`.

### Work breakdown

| ID  | Title                                       | Slice | Effort | Depends on | Status |
|-----|---------------------------------------------|-------|--------|------------|--------|
| T1  | Split-pane Monitoring layout scaffold       | A     | M      | —          | Done   |
| T2  | Left pane: persistent monitor tree + Refresh| A     | M      | T1         | Done (see note) |
| T3  | Right pane: chart-over-tabbed-detail shell  | A     | M      | T1         | Done   |
| T4  | Tree row actions (kebab): Add field to chart| A     | S      | T2, T3     | Add-field item done; Schedule Thread Trace / Force dynamic creation items remain (T9/T13) |
| T5  | Non-numeric fields → Text fields tab        | B     | M      | T3         | Done   |
| T6  | Per-series color assignment + customization | B     | M      | T3         | Done   |
| T7  | Per-series visibility toggle                | B     | S      | T3, T6     |        |
| T8  | Thread-trace client + queue state hook      | C     | M      | —          |        |
| T9  | Schedule Thread Trace dialog                | C     | M      | T4, T8     |        |
| T10 | Thread-trace queue tab (view / cancel)      | C     | M      | T8, T3     |        |
| T11 | Thread-trace result viewer tab              | C     | S      | T10        |        |
| T12 | base: port RemoteInterfaceExt1 to the MBean | D     | L      | —          |        |
| T13 | Force dynamic creation action + degradation | D     | M      | T4, T12    |        |
| T14 | Save / load chart dashboard to file         | D     | M      | T6, T7     |        |

### Tasks

**T1 — Split-pane Monitoring layout scaffold**
- **Description:** Replace the linear `ChartPanel` stack with a two-column
  split-pane inside the Monitoring tab of `Perfmon4jPanel`: left monitor
  column, right column split vertically into chart (top) and detail (bottom).
  Structure only — existing components move in unchanged in later tasks.
- **Acceptance criteria:** Monitoring tab renders the split-pane; connection
  status/alerts from `useRemoteManagementChart` still surface; no horizontal
  body scroll at ≥1024px; panes collapse to stacked below ~720px.
- **Effort:** M
- **Dependencies:** `Perfmon4jPanel.tsx`, `ChartPanel.tsx`.
- **Risk:** PatternFly split-pane / `PageSection` nesting caused the prior
  tab-overlay bug — verify no PageSection re-nesting regression.
- **Test plan:** Playwright smoke (tab renders, no overlay); manual resize.
- **Observability:** n/a (client UI).
- **Docs:** Note new layout in `hawtio-plugin/CLAUDE.md` architecture bullet.
- **Note (done):** New `MonitoringLayout.tsx` uses PatternFly's responsive
  `Grid`/`GridItem` (`span={12} md={3}` / `span={12} md={9}`) rather than a
  resizable `JSplitPane` equivalent — stacks below the `md` breakpoint
  (768px), comfortably inside the ≥1024px/≤720px acceptance thresholds.
  `LiveChart` renders a fixed-width (800px) SVG, so its slot is wrapped in
  its own `overflow-x: auto` container to keep any overflow inside the pane
  rather than the page body. Verified in a real browser (Playwright against
  `dev-target/`) at 600px and 1024px: `document.body.scrollWidth ===
  window.innerWidth` at both, and the 600px shot confirms a single stacked
  column. No PageSection/tab-overlay regression observed. `CLAUDE.md`
  architecture-bullet update still outstanding.

**T2 — Left pane: persistent monitor tree + Refresh**
- **Description:** Promote `MonitorFieldPicker`'s browsing into a persistent
  left-pane INTERVAL/SNAPSHOT tree with a filter box and a manual Refresh that
  re-pulls `getMonitors`. Selection no longer implies "add to chart" — that
  moves to a row action (T4).
- **Acceptance criteria:** Tree lists both monitor types; filter narrows
  live; Refresh re-pulls without losing chart subscriptions; disabled state
  shown when not `connected`.
- **Effort:** M
- **Dependencies:** T1; `MonitorFieldPicker.tsx`, `useRemoteManagementChart`.
- **Risk:** Decoupling select-from-add without regressing current add flow.
- **Test plan:** Unit test any new pure tree/filter helper; Playwright:
  filter + Refresh keeps existing series.
- **Observability:** n/a.
- **Docs:** Update ROADMAP Status once tree/chart split lands.
- **Note (done):** `MonitorFieldPicker.tsx` retired outright, split into
  `monitorTreeLogic.ts` (pure `buildMonitorTree`/`filterMonitorTree`, dot-notation
  + multi-instance fan-out, 11 Jest tests) and `MonitorTree.tsx` (PatternFly
  `TreeView`, root type groups auto-expanded, manual Refresh, disabled
  empty-state when not connected). Built alongside T4's "Add field to chart"
  kebab item (not the full T4 — Schedule Thread Trace / Force dynamic
  creation items still wait on T9/T13): T2's own acceptance criterion
  ("Refresh … without losing chart subscriptions") has nothing to verify
  without *some* way to create a subscription first, so leaving the tree
  fully decoupled from adding fields until T4 landed separately would have
  left the branch without a working add-chart path in between. The kebab
  action opens a new `AddFieldModal.tsx` (the field-checkbox UI lifted
  unchanged from the old picker). Verified end-to-end in a real browser
  (Playwright against `dev-target/`): tree renders INTERVAL monitors
  (dot-notation nesting incl. a monitor that is simultaneously a leaf and a
  group, e.g. `dev` under `dev.target`), Add field to chart → modal →
  subscribe → chart/table update, and Refresh leaves the existing
  subscription in place (`chartedRowsAfterRefresh === chartedRowsAfterAdd`).
  SNAPSHOT root doesn't appear against `dev-target` since that harness
  deliberately registers no SNAPSHOT monitors (pre-existing, documented
  limitation, not exercised by this task). One real bug caught only by
  browser verification: `TreeView`'s `allExpanded` prop *overrides* every
  item's own `defaultExpanded` rather than falling back to it — passing the
  literal `false` state used elsewhere in this codebase (`MBeanTreePicker`'s
  same pattern) forced the whole tree collapsed regardless of per-item
  settings. Fixed by leaving `allExpanded` as `undefined` until the user
  explicitly toggles the toolbar's Expand/Collapse-all.

**T3 — Right pane: chart-over-tabbed-detail shell**
- **Description:** Move `LiveChart` to the top-right pane; add a bottom-right
  PatternFly `Tabs` detail panel whose first tab hosts the existing
  `SubscribedFieldsTable` ("Charted fields"). Stub tabs for Text fields /
  Thread traces / Trace detail (filled by T5/T10/T11).
- **Acceptance criteria:** Chart renders top-right; Charted-fields tab shows
  subscribed series with working remove; empty stub tabs render without error.
- **Effort:** M
- **Dependencies:** T1; `LiveChart.tsx`, `SubscribedFieldsTable.tsx`.
- **Risk:** Rolling-window chart resize inside a fixed pane height.
- **Test plan:** Playwright: add field → appears in chart + Charted-fields
  tab; remove clears both and drops the subscription.
- **Observability:** n/a.
- **Docs:** —
- **Note (done):** New `MonitoringDetailTabs.tsx` wraps the existing
  `SubscribedFieldsTable` unchanged in a "Charted fields" tab (matching the
  established `Perfmon4jPanel`-level `Tabs`/`activeKey` convention) alongside
  three stub tabs (Text fields / Thread traces / Trace detail), each a plain
  `EmptyState` naming its owning future task. `SubscribedFieldsTable` itself
  still returns `null` when empty (unchanged), so the Charted-fields tab
  wraps it with its own small empty-state fallback rather than rendering
  blank when nothing is charted yet. `LiveChart`'s fixed `height={300}` prop
  meant the "chart resize in a fixed pane" risk never materialized - nothing
  about its sizing depends on the surrounding `Tabs`. Verified end-to-end in
  a real browser (Playwright against `dev-target/`): all four tabs present
  and switchable, stub tabs render their placeholder text with no console
  errors, adding a field populates both the chart legend and the
  Charted-fields table, and removing it clears both, drops the
  subscription, and brings the empty-state fallback back.

**T4 — Tree row actions (kebab): Add field to chart**
- **Description:** Add a per-row kebab (`Dropdown`) on tree leaves with "Add
  field to chart," wired to existing `addFields`. Web-native replacement for
  VisualVM's right-click popup; menu is the extension point for T9/T13.
- **Acceptance criteria:** Kebab appears only on real monitor/field leaves;
  Add subscribes the field; keyboard-operable; disabled when not connected.
- **Effort:** S
- **Dependencies:** T2, T3.
- **Risk:** Keyboard/focus handling on nested tree rows.
- **Test plan:** Playwright: kebab → Add → field charts; axe check on menu.
- **Observability:** n/a.
- **Docs:** —
- **Note (partially done):** Built alongside T2 (see its note) since T2's own
  acceptance criteria needed a working add-field path to verify against.
  Only the "Add field to chart…" item exists so far — it opens
  `AddFieldModal.tsx` rather than subscribing directly, since a monitor can
  have several numeric fields and the old picker's checkbox-selection UX was
  worth keeping. Kebab currently appears on every monitor node (leaf or
  hybrid group+monitor), not on individual fields — matches this task's own
  wording ("Add field to chart" is a per-*monitor* action that then opens a
  field picker, consistent with the legacy VisualVM dialog). No axe-specific
  check run yet (deferred, not blocking); manual keyboard tab-through of the
  `MenuToggle`/`DropdownItem` wasn't separately verified. Schedule Thread
  Trace (T9) and Force dynamic creation (T13) still need their own menu
  items added to `MonitorRowAction` in `MonitorTree.tsx`.

**T5 — Non-numeric fields → Text fields tab**
- **Description:** Route string-valued subscribed fields to the "Text fields"
  detail tab as a flat latest-value table instead of the chart (legacy #6).
  Classify numeric vs. string from field metadata / `getData` values.
- **Acceptance criteria:** String fields never plot; appear in Text fields tab
  with live latest value; numeric routing unchanged; mixed selection works.
- **Effort:** M
- **Dependencies:** T3.
- **Risk:** Reliable numeric/string classification across INTERVAL & SNAPSHOT.
- **Test plan:** Unit-test the classifier (pure); Playwright with a
  string-valued snapshot field in `dev-target`.
- **Observability:** n/a.
- **Docs:** ROADMAP: mark #6 done.
- **Note (done):** New pure `fieldRouting.ts` (`partitionByChartability`, 4
  Jest tests) routes on each field's *declared* type
  (`isNumericFieldType(field.fieldType)`), not the runtime `getData()` value
  shape - matching legacy VisualVM's own `FieldElement.isNumeric()`
  (DOUBLE/LONG/INTEGER only; TIMESTAMP and STRING both route to text; see
  `FieldElement.java`/`SelectFieldDlg.java`). This distinction mattered in
  practice, not just in theory: `dev-target`'s real INTERVAL monitor exposes
  `TimeStart`/`TimeStop` TIMESTAMP fields that come back from Jolokia as a
  JS *number* (an epoch-ms long), so a naive runtime `typeof rawValue ===
  'number'` classifier would have wrongly plotted a raw epoch value on the
  chart - declared-metadata routing avoids that by construction. Widened
  `FieldSeries.latestValue` to `number | string | null` and fixed
  `useRemoteManagementChart`'s `poll()`, which previously discarded any
  non-number value outright (`if (typeof rawValue !== 'number') return
  entry`), so a subscribed string/timestamp field's `latestValue` would have
  stayed `null` forever. `AddFieldModal.tsx` now lists every field type
  instead of pre-filtering to numeric only (mirroring legacy
  `SelectFieldDlg.java`, which also let both be selected from one dialog).
  New `TextFieldsTable.tsx` mirrors `SubscribedFieldsTable.tsx` exactly
  (Monitor/Field/Latest Value/Remove); `LiveChart`/`SubscribedFieldsTable`
  needed zero changes - `ChartPanel.tsx` now partitions `series` once and
  feeds each component only its half. Verified end-to-end in a real browser
  (Playwright against `dev-target/`, no SNAPSHOT/STRING field available
  there but `TimeStart` (TIMESTAMP) exercises the identical non-numeric
  path): the Add-field modal lists all 13 fields incl. both TIMESTAMP ones;
  a mixed numeric+TIMESTAMP selection added in one action lands one row in
  Charted fields (charted, live) and one in Text fields (live epoch value,
  confirmed absent from the chart legend); removing both clears each
  independently.

**T6 — Per-series color assignment + customization**
- **Description:** Assign each series a stable color and let the user change it
  from the Charted-fields row (legacy #7, color half). Replace auto-theme-only
  coloring with an explicit per-series color in chart state.
- **Acceptance criteria:** Each series has a legend swatch matching its line;
  changing color updates chart + legend live; colors survive add/remove of
  other series.
- **Effort:** M
- **Dependencies:** T3.
- **Risk:** Color-state ownership vs. rolling-window series identity.
- **Test plan:** Unit-test color-assignment helper; Playwright color change.
- **Observability:** n/a.
- **Docs:** —
- **Note (done):** New pure `seriesColor.ts` (`colorForIndex`, 3 Jest tests)
  cycles through a 5-hue palette pulled from PatternFly's own
  `chart_color_<hue>_400` design tokens (blue/green/orange/purple/
  red-orange - PatternFly ships no cyan/gold/teal "chart_color" family), so
  the new explicit coloring stays visually consistent with the rest of the
  app rather than inventing colors from scratch. Color is assigned once in
  `addFields` (`useRemoteManagementChart.ts`) from the *current* subscribed
  count, stored directly on each `FieldSeries` entry, and left untouched by
  every other operation (`removeField`'s filter, `poll()`'s `...entry`
  spreads) - this resolved the stated risk by construction: since color
  lives on the entry itself rather than being derived from array position,
  it can't shift when unrelated series are added/removed. `setFieldColor`
  is a new client-only hook callback (no server call - color isn't part of
  the RemoteManagement protocol). `LiveChart.tsx` dropped
  `themeColor={ChartThemeColor.multiUnordered}` in favor of an explicit
  `style={{ data: { stroke: s.color } }}` per `ChartLine` and a
  `symbol: { fill: s.color }` per `legendData` entry - PatternFly's Chart
  legend accepts a per-entry symbol fill for exactly this. No PatternFly
  `ColorPicker` component exists in this version, so
  `SubscribedFieldsTable.tsx` gained a leading "Color" column using a
  native `<input type="color">` swatch (keyboard-operable, zero new
  dependency, full custom-color freedom rather than palette-only).
  `TextFieldsTable.tsx` intentionally has no color column - text fields
  are never charted, so there's no legend entry for a color to match.
  Verified end-to-end in a real browser (Playwright against `dev-target/`,
  confirmed by screenshot rather than DOM/CSS scraping since Victory's
  legend swatch markup doesn't expose a plain `fill` attribute): two fields
  added in one action got distinct blue/green swatches matching the chart
  legend exactly; changing one field's color via the native color input
  updated both the table swatch and the chart legend swatch live; removing
  the *other* field afterward left the customized field's color untouched.

**T7 — Per-series visibility toggle**
- **Description:** Add a show/hide toggle per Charted-fields row that keeps the
  subscription but hides the line (legacy #7, visibility half).
- **Acceptance criteria:** Toggling hides/shows the line without dropping the
  server subscription; y-domain recomputes over visible series only.
- **Effort:** S
- **Dependencies:** T3, T6.
- **Risk:** Degenerate y-domain when all visible series are flat (known issue
  already handled once in `LiveChart` — preserve that fix).
- **Test plan:** Playwright: hide → line gone, latest value still updates.
- **Observability:** n/a.
- **Docs:** ROADMAP: mark #7 done.

**T8 — Thread-trace client + queue state hook**
- **Description:** Extend `remoteManagementClient.ts` with
  `scheduleThreadTrace`/`unScheduleThreadTrace` wrappers and a
  `useThreadTraces` hook owning queue state + result polling, mirroring
  `useRemoteManagementChart`'s session/poll/reconnect conventions and the
  dependency-free error-classification split (`remoteManagementErrors.ts`).
- **Acceptance criteria:** Hook schedules a trace, polls for completion,
  exposes pending/completed rows, and reclassifies `exec-denied` distinctly.
- **Effort:** M
- **Dependencies:** — (backend ops shipped in Phase B).
- **Risk:** Async result-streaming model over poll-based Jolokia; session
  idle-timeout reconnect (reuse chart hook's `cancelled` convention).
- **Test plan:** Jest on pure queue-state reducer + error classifier;
  Playwright end-to-end against `dev-target`.
- **Observability:** n/a.
- **Docs:** `CLAUDE.md`: new hook alongside `useRemoteManagementChart`.

**T9 — Schedule Thread Trace dialog**
- **Description:** Add a "Schedule thread trace…" tree-row action (T4 menu)
  opening a `Modal` for min-duration threshold and max stack depth, submitting
  via T8 (legacy #9).
- **Acceptance criteria:** Dialog validates inputs; submit schedules a trace
  and adds a pending row to the queue tab; cancel closes cleanly.
- **Effort:** M
- **Dependencies:** T4, T8.
- **Risk:** Input validation parity with the RMI dialog's constraints.
- **Test plan:** Unit-test validation; Playwright: schedule → pending row.
- **Observability:** n/a.
- **Docs:** —

**T10 — Thread-trace queue tab (view / cancel)**
- **Description:** Fill the "Thread traces" detail tab with a table of
  pending/completed traces, per-row View (→ T11) and Cancel/Delete, updating
  in place as results stream back (legacy #10).
- **Acceptance criteria:** Rows show submit time + monitor + status; Cancel
  removes a pending trace; completed rows enable View; live in-place updates.
- **Effort:** M
- **Dependencies:** T8, T3.
- **Risk:** In-place row updates without full-table reflow flicker.
- **Test plan:** Playwright: schedule → row transitions pending→completed;
  cancel path.
- **Observability:** n/a.
- **Docs:** —

**T11 — Thread-trace result viewer tab**
- **Description:** "Trace detail" tab showing a selected completed trace's
  submission time, monitor name, and captured stack text (legacy #11).
- **Acceptance criteria:** Selecting View shows the stack in a monospace,
  horizontally-scrollable block; empty state before selection.
- **Effort:** S
- **Dependencies:** T10.
- **Risk:** Long stack text overflow — must scroll in-container, not body.
- **Test plan:** Playwright: View → stack text rendered.
- **Observability:** n/a.
- **Docs:** ROADMAP: mark #9–#11 done.

**T12 — base: port RemoteInterfaceExt1 to the RemoteManagement MBean**
- **Description:** Add `forceDynamicChildCreation`/`unForceDynamicChildCreation`
  (+ `getServerManagementVersion`) to `org.perfmon4j:type=RemoteManagement`,
  delegating to the same `ExternalAppender` statics `RemoteImpl` uses — the
  delegate-don't-proxy pattern the existing MBean already follows (legacy #4).
- **Acceptance criteria:** Ops exposed over JMX/Jolokia; RMI+JMX coexistence
  test passes (shared static session state); JSON-serializes cleanly.
- **Effort:** L
- **Dependencies:** — (`base` module).
- **Risk:** Write/exec op raises stakes of host Jolokia hardening (per ROADMAP
  "Jolokia access-control out of scope") — opens no new access path, but must
  be documented as such.
- **Test plan:** JUnit 3-style coexistence + delegation test in `base`
  (`extends TestCase`); manual Jolokia serialization check.
- **Observability:** MBean op reachable via existing JMX tooling.
- **Docs:** ROADMAP: move the `RemoteInterfaceExt1` backlog item to Status.

**T13 — Force dynamic creation action + graceful degradation**
- **Description:** Add "Force dynamic monitor creation" tree-row action calling
  T12's op, and implement the read-only fallback UX when Jolokia denies
  write/exec (ROADMAP "Graceful degradation" item) — the first concrete
  consumer to actually degrade rather than dead-end.
- **Acceptance criteria:** Action force-creates children live; when exec is
  denied, action is hidden/disabled with an explanatory inline alert and
  read-only browsing still works.
- **Effort:** M
- **Dependencies:** T4, T12.
- **Risk:** Distinguishing exec-denied from other failures (already partially
  solved in `remoteManagementErrors.ts`).
- **Test plan:** Playwright with an exec-denied Jolokia ACL → degraded UX.
- **Observability:** n/a.
- **Docs:** ROADMAP: mark graceful-degradation item done.

**T14 — Save / load chart dashboard to file**
- **Description:** Persist the charted-field set (key, color, scale,
  visibility) to a downloadable file and reload it, skipping fields absent in
  the current JVM with a warning (legacy #8).
- **Acceptance criteria:** Save downloads a portable file; Load restores
  present fields, warns on missing ones, doesn't error on a foreign JVM.
- **Effort:** M
- **Dependencies:** T6, T7.
- **Risk:** Browser file I/O (download/upload) vs. the desktop `.p4j` file;
  forward-compat of the serialized format.
- **Test plan:** Unit-test serialize/deserialize (pure); Playwright
  round-trip incl. a missing-field case.
- **Observability:** n/a.
- **Docs:** ROADMAP: mark #8 done.

### Milestones

- **M1 (Prototype):** Slice A (T1–T4) — the Monitoring layout shell with live
  tree + chart + charted-fields tab, reflowed from existing components.
- **M2 (Feature-complete read-only):** Slices B + C (T5–T11) — chart polish and
  full thread-trace UX; everything reachable with read + exec Jolokia access.
- **M3 (GA):** Slice D (T12–T14) — dynamic creation, save/load, and graceful
  degradation under a write/exec-restricted Jolokia ACL.

### NFRs

- **Performance:** Live polling stays on the plugin's own `setInterval`
  cadence (not the shared console scheduler); rolling-window trim keeps series
  bounded; no per-poll full-table reflow in the queue/charted tabs.
- **Security/compliance:** No new Jolokia access path; write/exec (T12–T13)
  relies on the host console's ACL — documented as the deployer's
  responsibility, matching Hawtio's built-in JMX plugin.
- **Accessibility:** Kebab menus, dialogs, and tabs keyboard-operable with
  visible focus; axe check per interactive task; tree rows ARIA-labeled.
- **Internationalization:** Not in scope — plugin is English-only today; keep
  copy centralized to ease later extraction.
- **Reliability/SLOs:** Session idle-timeout reconnect + resubscribe preserved
  across all live features; a denied exec never breaks read-only browsing.

### Analytics & success metrics

- **KPIs/dashboards:** No product analytics in this plugin. Success is
  functional-parity coverage of the legacy 11-feature list (see spike gap
  table) and a clean end-to-end Playwright pass per slice against
  `dev-target/`.

### Definition of done (global)

- Code reviewed; `npm run typecheck` + `npm test` clean; formatted.
- Jest tests for all new pure logic; Playwright pass for each UI slice.
- Live features degrade (not crash) when Jolokia denies write/exec.
- ROADMAP Status + relevant `CLAUDE.md` updated; `readme.txt` TBD block gets a
  bullet for any user-visible `base` change (T12).
- Spike gap table updated as each legacy feature moves to Shipped.

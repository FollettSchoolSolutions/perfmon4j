# Monitoring Tab — Work Breakdown

Sprint-ready engineering tasks derived from
[`MONITORING_TAB_SPIKE.md`](MONITORING_TAB_SPIKE.md). Slice letters (A–D) match the
spike's Sequence section; a Slice "—" task (T15) is a cross-cutting fix found after
M2 shipped, not part of the spike's original four-slice sequence. Owner column left
blank for a sole-maintainer repo.

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
| T4  | Tree row actions (kebab): Add field to chart| A     | S      | T2, T3     | Done (see note) |
| T5  | Non-numeric fields → Text fields tab        | B     | M      | T3         | Done   |
| T6  | Per-series color assignment + customization | B     | M      | T3         | Done   |
| T7  | Per-series visibility toggle                | B     | S      | T3, T6     | Done   |
| T8  | Thread-trace client + queue state hook      | C     | M      | —          | Done   |
| T9  | Schedule Thread Trace dialog                | C     | M      | T4, T8     | Done   |
| T10 | Thread-trace queue tab (view / cancel)      | C     | M      | T8, T3     | Done   |
| T11 | Thread-trace result viewer tab              | C     | S      | T10        | Done   |
| T15 | Persist chart/thread-trace session across nav | —   | M      | —          | Done   |
| T12 | base: port RemoteInterfaceExt1 to the MBean | D     | L      | —          | Done   |
| T13 | Force dynamic creation action + degradation | D     | M      | T4, T12    | Done   |
| T14 | Save / load chart dashboard to file         | D     | M      | T6, T7, T15 | Done   |
| T16 | Per-series y-axis normalization (scale)     | —     | M      | T6, T7, T14 | Done   |

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
- **Note (done):** Built alongside T2 (see its note) since T2's own
  acceptance criteria needed a working add-field path to verify against.
  The "Add field to chart…" item opens `AddFieldModal.tsx` rather than
  subscribing directly, since a monitor can have several numeric fields and
  the old picker's checkbox-selection UX was worth keeping. Kebab currently
  appears on every monitor node (leaf or hybrid group+monitor), not on
  individual fields — matches this task's own wording ("Add field to chart"
  is a per-*monitor* action that then opens a field picker, consistent with
  the legacy VisualVM dialog). No axe-specific check run yet (deferred, not
  blocking); manual keyboard tab-through of the `MenuToggle`/`DropdownItem`
  wasn't separately verified. Schedule Thread Trace (T9) and Force dynamic
  creation (T13) have since added their own menu items to `MonitorRowAction`
  in `MonitorTree.tsx`, completing this task.

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
- **Note (done):** Added a `visible: boolean` flag to `FieldSeries` (`types.ts`,
  defaulted `true` in `addFields`, preserved untouched by `poll()`'s `...entry`
  spreads and by `removeField`/`setFieldColor` - same "lives on the entry
  itself" pattern T6 established for `color`) and a client-only
  `setFieldVisibility` callback on `useRemoteManagementChart` (no server
  call - visibility isn't part of the RemoteManagement subscription
  protocol, same rationale as `setFieldColor`). `LiveChart.tsx` derives a
  `visibleSeries = series.filter(s => s.visible)` and feeds *that* to both
  the y-domain computation and `ChartGroup`/`legendData`, so the stated risk
  (degenerate y-domain when all visible series are flat) resolves via the
  same existing min-pad fallback T3's fix already established - hiding down
  to a single flat series still gets the `Math.max(Math.abs(maxValue) * 0.1,
  1)` floor, confirmed in a real browser rather than assumed. Hidden series
  keep polling/accumulating points (only the derived `visibleSeries` array is
  filtered, not the underlying subscription or `points` array), so
  re-showing needs no re-fetch. `SubscribedFieldsTable.tsx` gained a leading
  eye/eye-slash icon-button column (native PatternFly `EyeIcon`/`EyeSlashIcon`,
  same plain-`Button` pattern as the existing Remove column) toggling
  `onVisibilityChange(fieldKey, !visible)`. Verified end-to-end in a real
  browser (Playwright against `dev-target/`): two fields added in one
  action both plot and appear in the legend; hiding one removes its line
  *and* its legend entry while the other keeps plotting; the hidden row's
  Latest Value cell kept updating across an additional poll cycle (confirmed
  by re-checking the table after an extra 6s wait), proving the
  subscription survived; re-showing restored the line and legend entry with
  no data gap. Also confirmed, by diffing against the unmodified T6 commit
  with the same add-two-fields flow, that a transient "Infinity is an
  invalid CSS width" Victory console warning (visible briefly right after
  adding fields, before the first poll populates any points) is a
  pre-existing condition unrelated to this task, not a regression it
  introduced - out of scope here.

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
- **Note (done):** New `threadTraceKey.ts` (`buildThreadTraceFieldKey`, 5 Jest
  tests) mirrors `FieldKey.buildThreadTraceKeyFromInterval` server-side: a
  thread-trace field key is built from an INTERVAL monitor's *name only*
  (any instance is dropped, matching the Java source), with `MonitorKey`'s
  normally-unused `instance` slot repurposed to carry
  `MinDurationToCapture=<n>,MaxDepth=<n>` args that the server re-tokenizes
  and applies via bean-property reflection - confirmed this string format
  round-trips correctly against a real JVM (see below), not just inferred
  from reading the Java source. New `threadTraceQueue.ts` (pure
  `addPendingTrace`/`removeTrace`/`applyPollResult`, 6 Jest tests) is the
  queue-state reducer - `applyPollResult`'s key behavior is that a
  completed trace's server-side record is a **one-shot read**
  (`ExternalAppender.MonitorMap.getThreadTraceData()` removes each entry
  from its own map the instant it's returned), unlike ordinary
  chart-subscribed fields, so this is the only place a completed trace's
  stack text is ever captured, and an already-`completed` queue entry is
  never re-touched even if its field key reappears in a later poll.
  `remoteManagementClient.ts` gained thin `scheduleThreadTrace`/
  `unScheduleThreadTrace` wrappers (same `execute()`-and-classify pattern
  as every other operation there). New `useThreadTraces.ts` owns its
  **own independent RemoteManagement session** - deliberately not the
  chart hook's session, so this hook works standalone from any tree-row
  action with no live chart mounted, and so the two features' polling
  cadences can't couple or block each other. It mirrors
  `useRemoteManagementChart.ts`'s connect/poll/reconnect skeleton exactly
  (same StrictMode-safe per-effect `cancelled` convention, same
  exec-denied/incompatible-version-terminal vs. any-other-failure-is-a-
  reconnect-candidate classification) and, on reconnect, re-issues
  `scheduleThreadTrace` for any trace still `pending` in a
  `pendingFieldKeysRef` (mirroring the chart hook's resubscribe-on-
  reconnect) - completed traces need no resend since there's nothing left
  server-side to re-request. To keep both hooks' error classification
  identical without duplicating it, factored `ConnectionStatus`/
  `ConnectionError`/`classifyConnectionError` out of
  `useRemoteManagementChart.ts` into a new dependency-free
  `connectionStatus.ts` (imports only `remoteManagementErrors.ts`, not
  `remoteManagementClient.ts`, to avoid pulling in `jolokiaService`'s CSS
  imports and staying Jest-testable) - `useRemoteManagementChart.ts` now
  re-exports the same names from there so `ChartPanel.tsx`'s existing
  import is unaffected. No UI consumes this hook yet (T9's dialog is the
  first real consumer) - end-to-end verification used a temporary debug
  harness (a throwaway button + JSON dump wired into `ChartPanel.tsx`, not
  committed) driven by Playwright against `dev-target/`: scheduled a real
  trace on the `dev.target.demo` monitor with
  `MinDurationToCapture=0,MaxDepth=20`, confirmed the queue entry started
  `pending`, and within one poll cycle transitioned to `completed` with a
  real captured stack (`dev.target.demo` frame at the expected depth) -
  proving the full field-key round-trip (client-built string -> server
  `FieldKey.parse()` -> server-rebuilt `toString()` used as the
  `getData()` result key) actually matches character-for-character against
  live Jolokia, not just in Jest.

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
- **Note (done):** New pure `threadTraceOptionsValidation.ts`
  (`validateThreadTraceForm`/`isThreadTraceFormValid`/`toThreadTraceOptions`,
  10 Jest tests) validates for real what the legacy VisualVM dialog
  (`ThreadTraceOptionsDlg.okButtonActionPerformed`) never did - that method
  has two `// TODO should check for non-numeric and or negative values...`
  comments left unresolved for over a decade. Both fields (min duration
  to capture, max stack depth) stay optional - a blank field is "not
  passed" (server defaults it to 0), matching the legacy dialog's own
  semantics - but a non-blank value must now be a non-negative integer or
  the field shows an inline error and Schedule is disabled. New
  `ScheduleThreadTraceModal.tsx` (PatternFly `NumberInput` x2 with
  `FormHelperText` errors, pre-filled `0`/`20` defaults for a sensible
  one-click trace) opens from a new "Schedule thread trace…" kebab item -
  added to the same `MonitorRowAction` menu `AddFieldModal` already uses
  in `MonitorTree.tsx`, gated to `monitor.type === 'INTERVAL'` only (a
  thread trace can only ever be built from an INTERVAL monitor's name -
  see `threadTraceKey.ts`) and disabled (not hidden) when the thread-trace
  session isn't `connected`, mirroring the existing not-connected-disables
  pattern elsewhere in this tree. `ChartPanel.tsx` now also mounts
  `useThreadTraces()` alongside the chart's own hook - its own independent
  connection status/error surfaces through a second `Alert` (reusing the
  chart's exec-denied/incompatible-version copy pattern, reworded for
  thread-trace scheduling specifically) so a write/exec-denied Jolokia ACL
  degrades this feature visibly rather than silently no-opping the kebab
  item. To satisfy this task's own "adds a pending row to the queue tab"
  acceptance criterion without building T10's full interactive table
  early, added a deliberately minimal `ThreadTraceQueueTable.tsx`
  (Monitor/Submitted/Status columns only, no View/Cancel yet) filling the
  previously-stub "Thread traces" detail tab - a natural base for T10 to
  add action columns to, not a throwaway. Verified end-to-end in a real
  browser (Playwright against `dev-target/`): kebab on the `demo` leaf
  under `dev > target > demo` (perfmon4j auto-registers every ancestor
  segment as its own real INTERVAL monitor, not just the leaf - all three
  levels got their own kebab) → "Schedule thread trace…" → modal → Schedule
  → modal closes → Thread traces tab shows one `pending` row → transitions
  to `completed` within one poll cycle; separately confirmed a negative
  min-duration value disables Schedule with a visible inline error, and
  Cancel closes the modal with no row ever appearing in the queue tab.

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
- **Note (done):** Extended T9's minimal `ThreadTraceQueueTable.tsx` with
  View and Cancel/Delete columns rather than replacing it, as planned when
  it was first built. Cancel is a single button/handler for both statuses
  (labeled "Cancel" while pending, "Delete" once completed) - safe to
  reuse because `unScheduleThreadTrace` is a harmless no-op server-side
  once a trace has already completed (`ExternalAppender.MonitorMap
  .unScheduleThreadTrace`'s `scheduledThreadTraces.remove(...)` returns
  null and is a guarded no-op when the one-shot record is already gone -
  see `threadTraceQueue.ts`'s note on completed traces), so there's no
  need for a second code path just to distinguish the two labels. The
  stated "in-place update without full-table reflow" risk turned out to
  already be resolved by construction from T8/T9: each `<Tr>` is keyed by
  `fieldKey` and `traces` is plain React state, so a poll-driven update
  only touches the cells that actually changed (typically just the Status
  label) rather than remounting rows - nothing new needed here to satisfy
  it, confirmed by there being no visible flicker in manual browser
  verification. View is disabled until a row is `completed` (a pending
  trace has no stack yet) and, once enabled, records the selected
  `fieldKey` and switches the detail tabs to "Trace detail" - both pieces
  of state now live in `MonitoringDetailTabs.tsx` since the tab switch and
  the eventual T11 viewer both need to agree on the same selection. The
  "Trace detail" tab itself still shows a stub (now reflecting the raw
  selected field key rather than a static message) rather than the actual
  formatted stack - T11's explicit job, not duplicated here. Verified
  end-to-end in a real browser (Playwright against `dev-target/`):
  scheduled a trace with an unreachably high min-duration threshold so it
  stays `pending` indefinitely, confirmed View is disabled and Cancel
  removes it (table reverts to its empty state, since it was the only
  row); scheduled a second trace with defaults, waited for it to reach
  `completed`, confirmed View is now enabled, clicking it switches to the
  Trace detail tab with a visible selection, then went back and confirmed
  Delete removes the completed row.

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
- **Note (done):** New `TraceDetailView.tsx` renders monitor label, formatted
  submitted time, and the raw captured stack in a plain `<pre>` wrapped in
  its own `overflow-x: auto` container - built directly, not via
  PatternFly's `CodeBlock`, since that component's default overflow
  behavior for an arbitrarily-long unwrapped line wasn't verified and this
  task's own stated risk is exactly that overflow escaping into the page
  body. `MonitoringDetailTabs.tsx` looks the selected trace up by
  `fieldKey` from the live `threadTraces` list on every render rather than
  holding a separate copy - an intentional consequence of this is that
  Cancel/Delete-ing the currently-selected trace (T10) makes the lookup
  return `undefined` and the view falls back to its own empty state
  automatically, with no extra code needed to handle that case. Verified
  end-to-end in a real browser (Playwright against `dev-target/`): the
  empty state shows before any selection; after scheduling, completing,
  and clicking View on a trace, the detail view shows the monitor name,
  submitted time, and non-empty stack text with `document.body.scrollWidth
  <= window.innerWidth` (no page-level horizontal overflow); deleting the
  selected trace from the Thread traces tab and switching back to Trace
  detail reverts to the empty state as expected from the lookup-by-key
  design, not a special case. This closes out Slice C (T8–T11) -
  legacy features #9/#10/#11 (schedule/queue/view thread traces) are now
  fully shipped end-to-end; see `MONITORING_TAB_SPIKE.md`'s gap table.

**T15 — Persist chart/thread-trace session across Hawtio nav-item switches**
- **Description:** Found after M2 shipped, not part of the original spike sequence:
  `useRemoteManagementChart`'s and `useThreadTraces`' session, subscriptions,
  colors/visibility, and thread-trace queue all live in `ChartPanel`-scoped React
  state. The Monitoring/Config/About tabs *within* the perfmon4j panel don't
  unmount each other (PatternFly `Tabs` defaults to `mountOnEnter`/`unmountOnExit`
  both `false`, so inactive tab content stays mounted, just hidden) - but Hawtio's
  *other* nav items (JMX, Runtime, JVM Diagnostics, …) are separate react-router
  routes, and navigating to one of those fully unmounts `ChartPanel`, tearing down
  the RemoteManagement session (`disconnect()` in the effect cleanup) and losing
  every charted field, its color/visibility, and the whole thread-trace queue.
  Coming back to perfmon4j builds an entirely fresh session from nothing. Move
  the session/poll lifecycle and its data out of per-mount `useState`/`useRef`
  into a module-level singleton store that both hooks attach to (created once,
  independent of any component's mount/unmount), so the connection keeps polling
  live in the background the whole time the plugin's JS stays loaded, not just
  while its panel happens to be visible - confirmed with the user as the wanted
  behavior over a cheaper "remember selections, reconnect+resubscribe on return"
  alternative, specifically so charted values have no visible data gap after
  navigating back.
- **Acceptance criteria:** Navigate to a different Hawtio nav item and back -
  charted fields (with colors/visibility unchanged) and the thread-trace queue
  are exactly as left, and the chart shows no gap in its rolling window (points
  kept accumulating while away). A hard page reload still starts fresh - this is
  in-session persistence only, not cross-reload persistence (that's T14's job for
  the charted-field *set*, and remains out of scope entirely for live chart data
  and the thread-trace queue).
- **Effort:** M
- **Dependencies:** — (touches the same code T6/T7/T8's hooks own, but has no
  hard prerequisite).
- **Risk:** The per-effect `cancelled`-flag idiom both hooks use today for React
  18 StrictMode safety is inherently tied to a *component's* mount/unmount cycle
  - a module singleton created once needs a different lazy-init-exactly-once
  guard instead. A long-lived background session also means a closed/refreshed
  browser tab relies entirely on the server's existing 5-minute idle-session
  reaper to clean up (same as today's documented "tab closed mid-flight" case -
  no new failure mode, just a longer-lived normal one). `base`'s root CLAUDE.md
  anti-patterns section warns against assuming one attach/session pattern
  generalizes elsewhere in this codebase - this singleton is scoped tightly to
  this plugin's own two RemoteManagement-backed hooks, not a general pattern to
  reuse without re-justifying it.
- **Test plan:** Playwright against `dev-target/`: chart fields with custom
  colors/visibility, schedule a thread trace, navigate to a different Hawtio nav
  item, wait through at least one poll interval, navigate back, assert charted
  fields/colors/visibility/queue are unchanged and chart points show no gap.
- **Observability:** n/a.
- **Docs:** `CLAUDE.md`: document the new singleton-store pattern as a deliberate
  deviation from "hook owns its own component-scoped session," and why.
- **Note (done):** New `remoteManagementChartStore.ts`/`threadTraceStore.ts` are
  plain (non-React) classes, each exported as a single `export const ... = new
  ...Store()` instance - ES modules are already evaluated exactly once and
  cached by the module system, so this needed no manual "create once" guard, and
  turned out to sidestep the stated StrictMode risk entirely rather than needing
  a new guard idiom for it: construction happens at module-evaluation time (on
  first `import`, i.e. this plugin's first-ever mount), not inside a component
  effect, so React's double-invoke-on-mount behavior in development never
  touches it. `useRemoteManagementChart.ts`/`useThreadTraces.ts` shrank to thin
  `useSyncExternalStore(store.subscribe, store.getSnapshot)` wrappers - no
  `useState`/`useRef`/`useEffect` left in either. The old per-effect
  disconnect-on-cleanup is gone entirely (there is no more "unmount" for a
  module singleton to react to); an abandoned session now relies solely on the
  server's existing 5-minute idle-session reaper, exactly the accepted tradeoff
  from this task's own Risk note. The `pollMs`/`windowMs`/`maxPoints` options
  both hooks used to accept were dropped (confirmed via grep that no caller
  anywhere ever passed non-default values) - a per-call override doesn't mean
  anything once the session is a shared singleton, only one set of constants can
  apply. Verified end-to-end in a real browser (Playwright against
  `dev-target/`): charted two fields, set a custom color, hid one, scheduled a
  thread trace, noted the exact charted-row text/color/visibility-button-count,
  clicked away to the JMX nav item (confirmed the perfmon4j panel itself
  actually unmounted - `text=perfmon4j: Live Chart` disappears), waited 7s
  (more than one 5s poll cycle), navigated back: row count/color/visibility all
  unchanged, and critically the *values themselves had moved* (StdDeviation
  went from `1` to `0.522…`, TotalHits from `5` to `25`) and the previously-
  pending thread trace was now `completed` - direct proof the connection kept
  polling live in the background the whole time the panel was gone, not just
  that stale state survived. The tree's own expand/search state (owned locally
  by `MonitorTree`, unrelated to the RemoteManagement session) resets on
  remount as before - out of scope here, since the user's original report was
  specifically about charted elements disappearing, not tree UI state.

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
- **Note (done):** Added `getServerManagementVersion`/`forceDynamicChildCreation`/
  `unForceDynamicChildCreation` to `RemoteManagementMBean`/`RemoteManagement.java`
  (`base/src/main/java/org/perfmon4j/remotemanagement/jmx/`), following the exact
  delegate-don't-reimplement pattern the file's Javadoc already documents:
  `forceDynamicChildCreation`/`unForceDynamicChildCreation` parse the `String
  monitorKey` param into a `MonitorKey` (wrapping a caught `UnableToParseKeyException`
  as `IllegalArgumentException`, matching `scheduleThreadTrace`'s existing convention)
  then call the identically-named `ExternalAppender` statics `RemoteImplExt1` already
  calls - no new logic, same session-scoped `SessionNotFoundException` behavior.
  `getServerManagementVersion()` has no `ExternalAppender` counterpart at all (mirrors
  `RemoteImplExt1`) - it just returns the `ManagementVersion.VERSION` constant
  directly. One non-obvious JMX finding worth flagging for anyone adding a similar
  op later: because `getServerManagementVersion()` takes no arguments and is named
  `getXxx`, JMX Standard MBean introspection classifies it as a **read-only attribute**
  (`ServerManagementVersion`), not an operation - confirmed via a live Jolokia
  `list`/`read` against `dev-target/` (`forceDynamicChildCreation`/
  `unForceDynamicChildCreation` remained ops, since their parameters exclude them from
  that convention). Still fully Jolokia-reachable, just via `read` rather than `exec` -
  locked in with a dedicated `MBeanServer.getAttribute(...)` test
  (`testGetServerManagementVersionReachableAsAttributeViaMBeanServer`) rather than
  documented as a surprise for the frontend to rediscover later. Added 7 new JUnit
  3-style tests to `RemoteManagementTest.java`: direct-call and unparsable-key/
  unknown-session error-path coverage for the two new ops, an
  `ExternalAppenderTest.testForceDynamicChildCreation`-mirrored end-to-end dynamic-
  child-creation assertion, an RMI/MBean session-coexistence variant of that same
  assertion (mirrors `testCoexistsWithRemoteImplSessions`, confirming the effect of an
  MBean-driven force-dynamic-creation call is visible process-wide, not scoped to the
  MBean's own session), and a real-`MBeanServer.invoke(...)`-based reachability test
  for `forceDynamicChildCreation`/`unForceDynamicChildCreation` (mirroring
  `testMBeanIsRegisteredAndReachableViaMBeanServer`) - 23/23 tests pass, plus the full
  `org.perfmon4j.remotemanagement.**` package (55/55) with no regressions. Manual
  Jolokia serialization check performed against a live `dev-target/` JVM (JSON POST
  `EXEC`/`read` requests via curl): `getServerManagementVersion` reads `"1.001"`;
  `forceDynamicChildCreation`/`unForceDynamicChildCreation` both exec cleanly
  (`value:null`, JMX void-return convention) against a real INTERVAL monitor key; a
  bogus session ID correctly serializes as a JSON `SessionNotFoundException`
  (`status:500`); an unparsable monitor key correctly serializes as a JSON
  `IllegalArgumentException` (`status:400`). No `base/CLAUDE.md` update needed - the
  delegate-don't-proxy pattern and MBean/RMI coexistence story were already documented
  there and in `RemoteManagement.java`'s own class Javadoc before this task; only the
  interface's stale "deliberately not included yet" doc comment needed updating (done).

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
- **Note (done):** `remoteManagementClient.ts` gained thin `forceDynamicChildCreation`/
  `unForceDynamicChildCreation` wrappers (same `execute()`-and-classify pattern as every
  other op there). `remoteManagementChartStore.ts` (T15's singleton, reused rather than
  spun up as a third independent session - this action is scoped to the chart's own
  monitor tree, not a standalone feature like thread traces) gained a
  `forcedDynamicMonitors: ReadonlySet<string>` snapshot field (which monitorKeys *this
  session* has itself force-called - there's no server-side query op, so a fresh
  session can't know what an earlier session left forced; deliberately not
  resubscribed/reapplied on reconnect like T15 does for charted fields, since this is a
  one-off debugging action, not a standing subscription worth restoring) and a
  `setForceDynamicChildCreation(monitorKey, forced)` method that catches any failure
  into a new `forceDynamicCreationError: ConnectionError | null` field rather than
  throwing, reusing `connectionStatus.ts`'s existing `classifyConnectionError`/
  `ExecAccessDeniedError` classification (T8/T15's Risk note already flagged this as
  "already partially solved"). `MonitorTree.tsx`'s per-row kebab (`MonitorRowAction`)
  gained a third INTERVAL-only item ("Force dynamic monitor creation" / "Stop forcing
  dynamic monitor creation", label driven by `forcedDynamicMonitors.has(monitor.monitorKey)`)
  that is **hidden entirely** (not just disabled) once `forceDynamicCreationError?.kind
  === 'exec-denied'` - the acceptance criteria's "hidden/disabled" is satisfied as
  "hidden" specifically, since a denied op has no reason to re-enable mid-session and a
  visible-but-dead button would just invite repeat clicks. `ChartPanel.tsx` renders a
  dismissible (locally-dismissed, independent of the underlying denial staying in
  effect - dismissing the alert doesn't un-hide the action) warning `Alert` explaining
  which two operations were denied and that everything else (browsing/charting/thread
  traces) is unaffected - this is a **per-operation** ACL scenario, distinct from the
  existing terminal exec-denied handling for the whole session (a real Jolokia
  capability: `jolokia-access.xml` can allow/deny individual operation names within an
  MBean, not just gate `exec` as a single all-or-nothing category). No new pure/Jest-
  testable module was warranted here - the new logic is thin Jolokia-calling
  side-effect code with no independently interesting branching, matching how
  `addFields`/`removeField` in the same store have never had dedicated unit tests
  either (covered by browser verification instead). Verified end-to-end in a real
  browser (Playwright against `dev-target/`): golden path - "Force…" click flips the
  row's own kebab item to "Stop forcing…" with no alert, a second click flips it back,
  confirmed by re-opening the menu after each click (no direct way to observe the
  server-side dynamic-child-creation effect itself from this harness without a
  `TargetMain.java` change to actually create a dynamically-named child, which was
  judged out of scope given that exact effect is already covered by T12's `base`-level
  JUnit tests - this pass targets the UI wiring, not re-proving the server behavior).
  Degraded path - used Playwright's request interception to return a real Jolokia 403
  body (`error_type: java.lang.SecurityException`, matching what `jolokia-access.xml`
  actually returns) specifically for `forceDynamicChildCreation`/
  `unForceDynamicChildCreation` POSTs while every other op continued through
  untouched (a more reliable, portable way to exercise this exact scenario than
  standing up a real `jolokia-access.xml` for one test run, and it still exercises
  real app code end-to-end - only the network response is substituted): the warning
  alert appeared with the exact expected copy and a working Dismiss button, the kebab
  item disappeared from the menu on the very next open, and "Add field to chart…" plus
  the rest of the session (subscribing/charting a field afterward) continued to work
  normally, confirming read-only-and-then-some browsing survives the denial untouched.

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
- **Note (done):** New pure `chartDashboard.ts` (`serializeDashboard`/
  `parseDashboardFile`/`partitionAvailableFields`, 7 Jest tests) plus
  `ChartDashboardControls.tsx` (Save/Load buttons wired into `ChartPanel.tsx`
  above `MonitoringLayout`, so they apply to the whole charted-field set
  regardless of which detail tab is active). Saves every subscribed field
  (chartable + Text-fields-tab, i.e. `series` from `useRemoteManagementChart`,
  not just `chartable`) - fieldKey/monitorKey/fieldName/fieldType/label/color/
  visible per field, as downloadable JSON (no `.p4j` properties-file format -
  this is a browser download, not desktop file I/O). No "scale" field is saved
  - per-series y-axis normalization doesn't exist yet (still an open ROADMAP
  backlog item), so there's nothing of that shape to persist until it does.
  Load re-subscribes via the existing `addFields`, then reapplies saved color/
  visibility via `setFieldColor`/`setFieldVisibility` - all three already
  existed on `useRemoteManagementChart`, so no `remoteManagementChartStore.ts`
  changes were needed. "Skip a missing field with a warning" is resolved by
  checking each saved fieldKey against a live `listMonitors`/
  `listFieldsForMonitor` query (not by relying on `subscribe()` throwing - it
  doesn't; a stale fieldKey subscribes without error and just never appears in
  `getData()`, so availability has to be checked up front instead). Verified
  end-to-end against the real `dev-target/` JVM (headless Chromium via
  Playwright): add a field, recolor it, hide it, Save downloads a JSON file
  with the exact color/visibility; Remove clears it; Load re-adds it with that
  same color/visibility restored. A second load of a two-field file where one
  fieldKey belongs to a monitor that doesn't exist on this JVM charted the
  real field and showed a dismissible warning alert naming the skipped one by
  label, confirming the partial-success (some available, some missing) path.

**T16 — Per-series y-axis normalization (scale)**
- **Description:** Port legacy VisualVM's per-counter "Scale" factor (`FieldElement.factor`
  / `DynamicTimeSeriesChart`, itself modeled on Windows Perfmon's classic Scale column):
  every series shares one fixed [0, 100] y-axis, and the user picks a power-of-ten
  multiplier per series (from a dropdown on its Charted-fields row) that's applied to the
  raw value and clamped into [0, 100] before plotting - the last piece of the original v1
  chart deferral (see ROADMAP.md).
- **Acceptance criteria:** Chart y-axis is a fixed [0, 100] range; each charted series has
  a scale-factor dropdown (powers of ten) that repositions its line without altering its
  raw Latest Value; the scale factor round-trips through Save/Load (T14).
- **Effort:** M
- **Dependencies:** T6, T7 (established per-series client-state pattern), T14 (dashboard
  format needs the new field).
- **Risk:** Clamping can visually pin a line at 0 or 100 if the chosen factor is too
  extreme for the data - an accepted tradeoff inherited directly from the legacy design,
  not a bug to design around.
- **Test plan:** Unit-test the scale/clamp math (pure); Playwright: change scale → line
  position/round-trip through save/load.
- **Observability:** n/a.
- **Docs:** ROADMAP: mark per-series y-axis normalization done.
- **Note (done):** New pure `seriesScale.ts` (`SCALE_FACTORS` - the same 12 powers of ten,
  100000 down to 0.000001, as legacy `ChartElementsTable`'s factor combo box -
  `applyScale`, `formatScaleLabel`, 4 Jest tests) mirrors legacy
  `TimeSeriesWithFactor.adjustNumberBasedOnFactor` exactly: multiply the raw value by the
  factor, then `Math.min(100, Math.max(0, ...))`. Added `scale: number` to `FieldSeries`
  (`types.ts`), defaulted to `DEFAULT_SCALE` (1) in `addFields` and preserved untouched by
  `poll()`'s `...entry` spreads - same "lives on the entry itself" pattern T6/T7 already
  established for `color`/`visible`. New client-only `setFieldScale` on
  `remoteManagementChartStore.ts`/`useRemoteManagementChart.ts` (no server call - scale
  isn't part of the RemoteManagement protocol, same rationale as color/visibility).
  `LiveChart.tsx` was rewritten to a **fixed** `domain.y = [0, 100]` instead of the
  auto-computed-with-minimum-pad domain T3 introduced to avoid a degenerate near-zero-width
  domain for a flat series - a fixed domain has no such degeneracy, so that whole
  workaround (and its accompanying risk note) is obsolete and was removed rather than kept
  dead. Each `ChartLine`'s plotted points now run through `applyScale(rawValue, s.scale)`;
  the Voronoi tooltip was changed to read a new `rawValue` field carried alongside the
  plotted `y` so hovering still shows the true value, not the scaled/clamped one.
  `SubscribedFieldsTable.tsx` gained a "Scale" column with a plain native `<select>`
  (matching the Color column's native-input convention - keyboard-operable, no new
  dependency) listing all 12 factors via `formatScaleLabel`. `chartDashboard.ts` bumped
  `DASHBOARD_FILE_VERSION` 1 → 2 to add `scale` to `DashboardFieldEntry`, but stayed
  backward-compatible rather than rejecting older saves: the field-shape validator
  (renamed `isDashboardFieldEntryBaseShape`) still only requires the pre-existing 7
  fields, and `parseDashboardFile` defaults any missing/non-numeric `scale` to
  `DEFAULT_SCALE` after that check - a real v1 file (from before this task) loads
  cleanly with every field normalized back to ×1, confirmed by a dedicated test using a
  literal v1-shaped JSON fixture with no `scale` key at all.
  `ChartDashboardControls.tsx`'s load path now also calls `setFieldScale` alongside the
  existing `setFieldColor`/`setFieldVisibility` calls for each restored field. Verified
  end-to-end in a real browser (Playwright against `dev-target/`): confirmed the y-axis
  ticks render fixed at 20/40/60/80/100 regardless of data; added a field (default scale
  ×1, dropdown present); changed its scale to ×10 via the dropdown and confirmed the
  value stuck; waited a poll cycle and confirmed the line visibly pinned at the top of
  the chart (TotalHits=25 × 10 = 250, clamped to 100 - the expected clamping behavior,
  not a bug); Saved the dashboard and confirmed the downloaded JSON's `version` is `2`
  and the field's `scale` is `10`; removed the field, loaded the file back, and confirmed
  the Scale dropdown was restored to ×10 with no manual re-selection needed.

### Milestones

- **M1 (Prototype):** Slice A (T1–T4) — the Monitoring layout shell with live
  tree + chart + charted-fields tab, reflowed from existing components.
- **M2 (Feature-complete read-only):** Slices B + C (T5–T11) — chart polish and
  full thread-trace UX; everything reachable with read + exec Jolokia access.
  **Reached** — T5–T11 all Done.
- **M3 (GA):** Slice D (T12–T14) — dynamic creation, save/load, and graceful
  degradation under a write/exec-restricted Jolokia ACL. **Reached** — T12–T14 all Done.

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

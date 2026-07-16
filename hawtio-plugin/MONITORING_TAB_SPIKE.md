# Monitoring Tab — Design Spike

Design spike for growing the Hawtio plugin's **Monitoring** tab into a functional mirror of
the Swing VisualVM plugin (`visualvm-plugin/`), reflowed for the browser. This is the durable,
checked-in copy of the wireframe spike reviewed with the maintainer; see
[`ROADMAP.md`](ROADMAP.md) for live status/backlog and each module's `CLAUDE.md` for
architecture. The engineering tasks derived from this spike (T1–T14, milestones, NFRs) live in
[`MONITORING_TAB_TASKS.md`](MONITORING_TAB_TASKS.md).

## Premise: feature-completion, not a port

The reason `../visualvm-plugin/LEGACY_VISUALVM_FEATURES.md` concluded the old plugin wasn't a
ripe porting source was the **transport** — it rides on RMI, which a browser can't speak. That
blocker is gone: the `RemoteManagement` JMX MBean (Phases A+B, shipped in `base`) re-exposes the
same remote-management interface over JMX/Jolokia, and the "perfmon4j Chart" nav item already
consumes it. So this is not "port a Swing app to React" — it's **growing an existing, working
nav item to close a known gap**, where the hardest risk (transport, session model, live polling,
reconnect) is already behind us.

- **6 / 11** legacy features already have their `base` plumbing in place over JMX/Jolokia.
- **3 / 11** already ship end-to-end in the browser (version display, monitor-tree browse, live chart).
- **1 / 11** still needs new `base` work (`RemoteInterfaceExt1` dynamic child creation).

## Target layout

The Swing `MainWindow.java` is a dense four-quadrant split-pane. The spike reflows that same
information architecture into the existing PatternFly `Perfmon4jPanel` Monitoring tab:

```
┌────────────────┬─────────────────────────────────────────────┐
│  Monitors      │   Live chart  (rolling 5-min window)         │
│  [filter…]     │   ── scrolling time-series, per-field legend │
│                ├─────────────────────────────────────────────┤
│  ▾ INTERVAL    │  [Charted fields] [Text fields]              │
│    processOrder│  [Thread traces ] [Trace detail]             │
│  ▾ SNAPSHOT    │   field | latest | color | show | remove     │
│  [Refresh]     │                                              │
└────────────────┴─────────────────────────────────────────────┘
  Row actions (kebab / right-click):
    Add field to chart · Schedule thread trace · Force dynamic creation
```

- **Left — monitor tree.** Searchable/filterable INTERVAL + SNAPSHOT trees (`getMonitors`), with
  a manual Refresh. Row-level actions replace VisualVM's right-click popup with a web-native
  kebab menu (directional — not yet locked).
- **Top-right — live chart.** Rolling-window time-series of the subscribed numeric fields, with a
  per-field legend. This is today's `LiveChart`, moved into the split-pane.
- **Bottom-right — tabbed detail panel.** Charted-fields table (color / visibility / remove),
  a non-numeric "Text fields" table, the thread-trace queue, and the trace-result viewer.

## Gap analysis — the 11 legacy features vs. today

Status legend: **Shipped** end-to-end · **Polish** (deferred work on a shipped feature) ·
**New UI** (base plumbing already exists) · **Needs base** (new `base` plumbing required first).

| #  | Legacy feature                        | Transport ready?            | Status today                         |
|----|---------------------------------------|-----------------------------|--------------------------------------|
| 1  | Auto-detect instrumented JVM          | n/a                         | Not needed — Hawtio is already connected |
| 2  | Version negotiation / display         | ✓ SelfManagement            | **Shipped** · About tab              |
| 3  | Live monitor tree browsing            | ✓ `getMonitors`             | **Shipped** · MonitorFieldPicker     |
| 5  | Ad-hoc real-time charting             | ✓ `subscribe`/`getData`     | **Shipped** · LiveChart              |
| 6  | Non-numeric field table               | ✓ `getData`                 | **Polish** — deferred                |
| 7  | Per-field color / visibility / remove | ✓                           | **Polish** — remove only today       |
| 8  | Save / load dashboard                 | file-based                  | **Polish** — deferred                |
| 9  | Schedule thread trace                 | ✓ `scheduleThreadTrace` (Phase B) | **New UI** — plumbing ready    |
| 10 | Thread-trace queue                    | ✓                           | **New UI** — plumbing ready          |
| 11 | Thread-trace result viewer            | ✓                           | **New UI** — plumbing ready          |
| 4  | Force dynamic monitor creation        | ✗ `RemoteInterfaceExt1` unported | **Needs base** plumbing first   |

## Sequence

Four independently shippable vertical slices, ordered to de-risk the layout first and defer the
write/exec-gated work to last.

### Slice A — Monitoring layout shell
*Frontend only · reuses `src/chart/*` · features #3, #5*

Land the tree · chart · tabbed-detail arrangement inside the existing `Perfmon4jPanel` Monitoring
tab. Nothing new is computed — this reflows components that already render into the split-pane
information architecture above.

### Slice B — Chart polish
*Frontend only · no `base` change · features #6, #7*

Route non-numeric fields to the "Text fields" tab, and add per-series color and visibility toggles
alongside the existing remove control — closing the deferred gaps on a feature that already ships.

### Slice C — Thread traces
*Frontend only · plumbing shipped in Phase B · features #9, #10, #11*

Build the schedule dialog (min-duration / max-depth), the pending/completed queue table with
per-row view & cancel, and the captured-stack result viewer. Entirely a UI build — the MBean
operations already exist and are tested.

### Slice D — Write/exec-gated features
*`base` + frontend · must respect the Jolokia-denied fallback · features #4, #8*

Port `RemoteInterfaceExt1`'s dynamic-child-creation ops onto the RemoteManagement MBean (#4), and
add dashboard save/load (#8). Both interact with the "graceful degradation when Jolokia write/exec
is denied" backlog item (see `ROADMAP.md`), so they land last.

---

*Spike, not a spec: layout and proportions are directional, meant to align on information
architecture and sequence before Slice-A code is written. Grounded in
`../visualvm-plugin/LEGACY_VISUALVM_FEATURES.md`, `../visualvm-plugin/.../MainWindow.java`, and
the current `hawtio-plugin/` Chart nav item.*

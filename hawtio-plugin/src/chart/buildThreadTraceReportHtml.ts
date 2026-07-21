// Builds the standalone thread-trace report: a single self-contained HTML document
// (inline CSS + JS, no external resource) so the user can open it in a new browser tab
// AND save it to reopen offline later. Pure and dependency-free, same convention as
// generateSnapshotXml.ts / parseThreadTrace.ts, so it stays Jest-testable without a DOM.
//
// The trace is rendered as an actionable, collapsible tree: every frame - which in the
// source text (see parseThreadTrace.ts) has an explicit open line and close line -
// becomes a nested, accordion-style <details> the user can expand/collapse, with the
// frame's exit shown as a subtle footer so the open/close bracket stays visible. Frame
// duration is encoded as a proportional bar in a sequential blue ramp (light->dark by
// magnitude on light, dark->light on dark - prominence tracks magnitude in both), with
// the millisecond value in plain ink beside it (dataviz skill: text wears ink tokens,
// never the mark color).

import { countFrames, TraceNode } from './parseThreadTrace'

export interface ThreadTraceReportInput {
  /** Monitor/category label the trace was captured for. */
  category: string
  /** Preformatted submitted date/time (caller formats, so this stays locale-agnostic). */
  submittedText: string
  minDurationToCaptureMillis?: number
  maxDepth?: number
  roots: TraceNode[]
  truncated: boolean
  /** The original server text, shown verbatim in a collapsed "Raw trace text" section. */
  rawText: string
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// Sequential-ramp bucket (1 = smallest .. 5 = largest) by a frame's share of the
// reference (root) duration - the CSS maps each bucket to a mode-appropriate step.
function seqBucket(ratio: number): number {
  if (ratio >= 0.5) return 5
  if (ratio >= 0.25) return 4
  if (ratio >= 0.1) return 3
  if (ratio >= 0.02) return 2
  return 1
}

function meterHtml(node: TraceNode, referenceMs: number): string {
  const ratio = referenceMs > 0 ? node.durationMs / referenceMs : 0
  const width = node.durationMs > 0 ? Math.min(100, Math.max(2, ratio * 100)) : 0
  const bucket = seqBucket(ratio)
  const sql = node.sqlMs !== undefined ? `<span class="sql">SQL ${node.sqlMs} ms</span>` : ''
  return (
    `<span class="meter" title="${node.durationMs} ms">` +
    `<span class="bar b${bucket}" style="width:${width.toFixed(1)}%"></span></span>` +
    `<span class="dur">${node.durationMs} ms</span>${sql}`
  )
}

function nodeHtml(node: TraceNode, referenceMs: number): string {
  const name = escapeHtml(node.name)
  const meter = meterHtml(node, referenceMs)
  const end = node.endTime ?? '—'

  if (node.children.length === 0) {
    // Leaf: nothing to collapse, so a plain row showing start -> end inline.
    return (
      `<div class="row leaf">` +
      `<span class="tw"></span>` +
      `<span class="t">${node.startTime} → ${end}</span>` +
      `${meter}<span class="name" title="${name}">${name}</span>` +
      `</div>`
    )
  }

  const children = node.children.map(child => nodeHtml(child, referenceMs)).join('')
  return (
    `<details open>` +
    `<summary class="row">` +
    `<span class="tw tri"></span>` +
    `<span class="t">${node.startTime}</span>` +
    `${meter}<span class="name" title="${name}">${name}</span>` +
    `</summary>` +
    `<div class="children">${children}</div>` +
    `<div class="close">↳ ended ${end} · ${name}</div>` +
    `</details>`
  )
}

function metaRow(label: string, value: string): string {
  return `<div class="mk">${escapeHtml(label)}</div><div class="mv">${escapeHtml(value)}</div>`
}

/**
 * Renders the full HTML document for one completed trace. Everything is inlined so the
 * saved file works with no network access; every interpolated value is HTML-escaped.
 */
export function buildThreadTraceReportHtml(input: ThreadTraceReportInput): string {
  const referenceMs = input.roots.reduce((max, r) => Math.max(max, r.durationMs), 0)
  const frames = countFrames(input.roots)
  const totalText = input.roots.length > 0 ? `${referenceMs} ms` : '—'
  const spanText =
    input.roots.length > 0 ? `${input.roots[0].startTime} – ${input.roots[input.roots.length - 1].endTime ?? '—'}` : '—'

  const meta = [
    metaRow('Category', input.category),
    metaRow('Submitted', input.submittedText),
    metaRow('Min duration to capture', input.minDurationToCaptureMillis !== undefined ? `${input.minDurationToCaptureMillis} ms` : '—'),
    metaRow('Max stack depth', input.maxDepth !== undefined ? String(input.maxDepth) : '—'),
    metaRow('Total duration', totalText),
    metaRow('Frames', String(frames)),
    metaRow('Time span', spanText),
  ].join('')

  const truncatedBanner = input.truncated
    ? `<div class="banner">⚠ Thread trace limit exceeded — the capture was truncated; some frames are missing.</div>`
    : ''

  const tree =
    input.roots.length > 0
      ? input.roots.map(root => nodeHtml(root, referenceMs)).join('')
      : `<div class="empty">This trace captured no frames.</div>`

  const title = `perfmon4j thread trace: ${escapeHtml(input.category)}`

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title}</title>
<style>
  :root {
    color-scheme: light;
    --page: #f9f9f7;
    --surface: #fcfcfb;
    --ink: #0b0b0b;
    --ink-2: #52514e;
    --muted: #898781;
    --border: rgba(11,11,11,0.10);
    --guide: #e1e0d9;
    --track: #eef0f2;
    /* sequential blue ramp, small -> large = light -> dark on the light surface */
    --b1: #86b6ef; --b2: #5598e7; --b3: #2a78d6; --b4: #1c5cab; --b5: #104281;
    --warn-bg: #fff4d6; --warn-ink: #6b4e00; --warn-bd: #fab219;
  }
  @media (prefers-color-scheme: dark) {
    :root:where(:not([data-theme="light"])) {
      color-scheme: dark;
      --page: #0d0d0d; --surface: #1a1a19; --ink: #ffffff; --ink-2: #c3c2b7;
      --muted: #898781; --border: rgba(255,255,255,0.10); --guide: #2c2c2a; --track: #24242220;
      /* dark surface: small -> large = dark -> light, so prominence still tracks magnitude */
      --b1: #256abf; --b2: #3987e5; --b3: #5598e7; --b4: #86b6ef; --b5: #b7d3f6;
      --warn-bg: #2a2410; --warn-ink: #f0d488; --warn-bd: #fab219;
    }
  }
  :root[data-theme="dark"] {
    color-scheme: dark;
    --page: #0d0d0d; --surface: #1a1a19; --ink: #ffffff; --ink-2: #c3c2b7;
    --muted: #898781; --border: rgba(255,255,255,0.10); --guide: #2c2c2a; --track: #24242220;
    --b1: #256abf; --b2: #3987e5; --b3: #5598e7; --b4: #86b6ef; --b5: #b7d3f6;
    --warn-bg: #2a2410; --warn-ink: #f0d488; --warn-bd: #fab219;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; background: var(--page); color: var(--ink);
    font-family: system-ui, -apple-system, "Segoe UI", sans-serif; font-size: 14px; line-height: 1.5;
  }
  .wrap { max-width: 1100px; margin: 0 auto; padding: 1.5rem 1.25rem 4rem; }
  h1 { font-size: 1.35rem; margin: 0 0 0.25rem; }
  .sub { color: var(--ink-2); margin: 0 0 1.25rem; }
  .card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 1rem 1.25rem; }
  .meta { display: grid; grid-template-columns: max-content 1fr; gap: 0.35rem 1.25rem; margin: 0 0 1.25rem; }
  .mk { color: var(--ink-2); }
  .mv { font-variant-numeric: tabular-nums; }
  .banner { background: var(--warn-bg); color: var(--warn-ink); border: 1px solid var(--warn-bd);
            border-radius: 8px; padding: 0.6rem 0.9rem; margin: 0 0 1rem; }
  .toolbar { display: flex; gap: 0.5rem; margin: 0 0 0.75rem; }
  .tb { font: inherit; color: var(--ink); background: var(--surface); border: 1px solid var(--border);
        border-radius: 6px; padding: 0.3rem 0.75rem; cursor: pointer; user-select: none; }
  .tb:hover { border-color: var(--muted); }
  /* Bulk expand/collapse via pure-CSS radios - deliberately NO JavaScript, so it works
     both in the saved offline file AND inside a host Hawtio console whose CSP is
     "script-src 'self'" (no 'unsafe-inline'), which blocks inline scripts in the
     blob: document this report is opened as. "Collapse all" force-hides every frame's
     children/close (and resets its disclosure triangle); "Expand all" returns to the
     native per-node state, which defaults to fully expanded. */
  .bulk { position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; }
  #bulk-collapse:checked ~ .card .children,
  #bulk-collapse:checked ~ .card .close { display: none !important; }
  #bulk-collapse:checked ~ .card details[open] > summary .tri::before { transform: none; }
  #bulk-expand:checked ~ .toolbar label[for="bulk-expand"],
  #bulk-collapse:checked ~ .toolbar label[for="bulk-collapse"] {
    border-color: var(--muted); background: color-mix(in srgb, var(--muted) 16%, transparent); font-weight: 600; }
  .tree { font-variant-numeric: tabular-nums; }
  details { border: 0; }
  summary { list-style: none; cursor: pointer; }
  summary::-webkit-details-marker { display: none; }
  .row { display: flex; align-items: center; gap: 0.6rem; padding: 0.12rem 0.25rem; border-radius: 5px; white-space: nowrap; }
  .row:hover { background: color-mix(in srgb, var(--muted) 14%, transparent); }
  .children { margin-left: 0.7rem; padding-left: 0.7rem; border-left: 1px solid var(--guide); }
  .close { color: var(--muted); padding: 0.05rem 0.25rem 0.2rem 1.9rem; font-size: 0.85em; white-space: nowrap; }
  .tw { flex: 0 0 0.8rem; width: 0.8rem; height: 0.8rem; position: relative; }
  .tri::before { content: ""; position: absolute; top: 0.15rem; left: 0.15rem;
                 border-left: 5px solid var(--muted); border-top: 4px solid transparent; border-bottom: 4px solid transparent;
                 transition: transform 0.1s ease; }
  details[open] > summary .tri::before { transform: rotate(90deg); transform-origin: 2px 4px; }
  .t { color: var(--ink-2); flex: 0 0 auto; }
  .meter { flex: 0 0 120px; height: 0.7rem; background: var(--track); border-radius: 4px; overflow: hidden; }
  .bar { display: block; height: 100%; border-radius: 4px; min-width: 2px; }
  .b1 { background: var(--b1); } .b2 { background: var(--b2); } .b3 { background: var(--b3); }
  .b4 { background: var(--b4); } .b5 { background: var(--b5); }
  .dur { flex: 0 0 auto; min-width: 4.5rem; color: var(--ink); }
  .sql { flex: 0 0 auto; color: var(--ink-2); font-size: 0.85em; }
  .name { flex: 1 1 auto; overflow: hidden; text-overflow: ellipsis; }
  .leaf .name { color: var(--ink-2); }
  .empty { color: var(--ink-2); padding: 1rem 0; }
  .scroller { overflow-x: auto; }
  .raw { margin-top: 1.5rem; }
  .raw pre { margin: 0.5rem 0 0; padding: 0.75rem; background: var(--surface); border: 1px solid var(--border);
             border-radius: 8px; overflow-x: auto; white-space: pre; }
</style>
</head>
<body>
  <div class="wrap">
    <h1>perfmon4j thread trace</h1>
    <p class="sub">${escapeHtml(input.category)}</p>
    ${truncatedBanner}
    <div class="meta">${meta}</div>
    <input class="bulk" type="radio" name="bulk" id="bulk-expand" checked>
    <input class="bulk" type="radio" name="bulk" id="bulk-collapse">
    <div class="toolbar" role="group" aria-label="Expand or collapse all frames">
      <label class="tb" for="bulk-expand">Expand all</label>
      <label class="tb" for="bulk-collapse">Collapse all</label>
    </div>
    <div class="card scroller">
      <div class="tree">${tree}</div>
    </div>
    <details class="raw">
      <summary><strong>Raw trace text</strong></summary>
      <pre>${escapeHtml(input.rawText)}</pre>
    </details>
  </div>
</body>
</html>`
}

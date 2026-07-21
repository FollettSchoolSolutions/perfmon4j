// Parses the perfmon4j thread-trace text (the server's one-shot getData() string for
// a completed THREADTRACE field) into a tree, for the standalone HTML report viewer
// (buildThreadTraceReportHtml.ts). Pure and dependency-free so it stays Jest-testable,
// same convention as generateSnapshotXml.ts / threadTraceQueue.ts.
//
// Source format is org.perfmon4j.ThreadTraceData.buildAppenderStringBody(indent)
// (base/src/main/java/org/perfmon4j/ThreadTraceData.java). Every frame emits exactly
// two lines with its children nested between them:
//
//   +-21:12:30:398 (7) WebRequest          <- ENTRY: has "(durationMs)" after the time
//   |\t+-21:12:30:399 (1) ...child...       <- child ENTRY (indent = parent + "|\t")
//   |\t+-21:12:30:400 ...child...           <- child EXIT
//   +-21:12:30:405 WebRequest              <- EXIT: no "(durationMs)"
//
// - The indent prefix repeats "|" + TAB per depth level; some renderings expand the
//   tabs to spaces, so we key off the "|" count rather than tab positions.
// - The "(N)" on an entry line is the frame's DURATION IN MS (getDuration()), not a
//   call count; an optional "(SQL:n)" may immediately follow it.
// - An exit line has the timestamp and name but no "(N)" - that is what distinguishes
//   the two, so the entry pattern must be tried before the exit pattern.
// - toAppenderString() wraps the body in 80-asterisk rule lines and, when the trace
//   overflowed, prepends a "Thread Trace Limit Exceeded -- Data truncated" banner;
//   both are non-frame lines we skip (the banner also flips `truncated`).

export interface TraceNode {
  /** Monitor/category name for the outermost frame, else the instrumented method's
   * fully-qualified name. */
  name: string
  /** Entry timestamp, "HH:MM:SS:mmm" as produced by MiscHelper.formatTimeAsString. */
  startTime: string
  /** Exit timestamp, or null if the trace was truncated before this frame closed. */
  endTime: string | null
  /** Frame duration in milliseconds (endTime - startTime, floored at 0 server-side). */
  durationMs: number
  /** SQL time in milliseconds, only present when the server emitted a "(SQL:n)" segment. */
  sqlMs?: number
  children: TraceNode[]
}

export interface ParsedTrace {
  roots: TraceNode[]
  /** True when the source carried the "Data truncated" overflow banner. */
  truncated: boolean
}

// Anchored right after the timestamp so arbitrary method-name text in `name` can never
// be misread as the duration/SQL segments. Entry: has " (dur)" (+ optional "(SQL:n)").
const ENTRY_RE = /^(?<indent>[|\s]*)\+-(?<time>\d\d:\d\d:\d\d:\d\d\d) \((?<dur>\d+)\)(?:\(SQL:(?<sql>\d+)\))? (?<name>.*)$/
// Exit: timestamp then name, no duration segment.
const EXIT_RE = /^(?<indent>[|\s]*)\+-(?<time>\d\d:\d\d:\d\d:\d\d\d) (?<name>.*)$/

/**
 * Turns a completed trace's stack text into a tree. Relies on the format's balanced
 * open/close pairing: every entry line pushes a frame (as a child of the frame
 * currently on top of the stack), every exit line closes the top frame. Lines that
 * match neither pattern (asterisk rules, the overflow banner, blanks) are skipped.
 * Tolerates more than one root even though a normal trace has exactly one.
 */
export function parseThreadTrace(stack: string): ParsedTrace {
  const roots: TraceNode[] = []
  const open: TraceNode[] = []
  let truncated = false

  for (const line of stack.split(/\r?\n/)) {
    if (line.includes('Data truncated')) {
      truncated = true
      continue
    }

    const entry = ENTRY_RE.exec(line)
    if (entry?.groups) {
      const node: TraceNode = {
        name: entry.groups.name,
        startTime: entry.groups.time,
        endTime: null,
        durationMs: Number(entry.groups.dur),
        children: [],
      }
      if (entry.groups.sql !== undefined) node.sqlMs = Number(entry.groups.sql)
      const parent = open[open.length - 1]
      if (parent) parent.children.push(node)
      else roots.push(node)
      open.push(node)
      continue
    }

    const exit = EXIT_RE.exec(line)
    if (exit?.groups) {
      const node = open.pop()
      if (node) node.endTime = exit.groups.time
    }
  }

  return { roots, truncated }
}

/** Total number of frames in the tree - a header stat in the report. */
export function countFrames(nodes: TraceNode[]): number {
  return nodes.reduce((sum, node) => sum + 1 + countFrames(node.children), 0)
}

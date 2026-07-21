import { THREAD_TRACE_PENDING, ThreadTraceOptions } from './threadTraceKey'

export type ThreadTraceStatus = 'pending' | 'completed'

export interface ThreadTraceEntry {
  fieldKey: string
  monitorLabel: string
  submittedAt: number
  /** The min-duration/max-depth args this trace was scheduled with, surfaced in the
   * report header (buildThreadTraceReportHtml.ts). Kept here rather than re-decoded
   * from fieldKey at view time. */
  options: ThreadTraceOptions
  status: ThreadTraceStatus
  /** The captured stack text once completed; null while pending. */
  stack: string | null
}

export function addPendingTrace(
  traces: ThreadTraceEntry[],
  fieldKey: string,
  monitorLabel: string,
  submittedAt: number,
  options: ThreadTraceOptions,
): ThreadTraceEntry[] {
  return [...traces, { fieldKey, monitorLabel, submittedAt, options, status: 'pending', stack: null }]
}

export function removeTrace(traces: ThreadTraceEntry[], fieldKey: string): ThreadTraceEntry[] {
  return traces.filter(t => t.fieldKey !== fieldKey)
}

/**
 * Applies one getData() poll result to the queue. A completed trace's server-side
 * record is a one-shot read - ExternalAppender removes it from its own map the
 * instant it's returned (see ExternalAppender.MonitorMap.getThreadTraceData()) - so
 * this is the only place a completed trace's stack text is ever captured; a poll
 * that misses it loses it for good. Already-completed entries are left untouched
 * since the server has nothing left to report for them.
 */
export function applyPollResult(traces: ThreadTraceEntry[], data: Record<string, unknown>): ThreadTraceEntry[] {
  return traces.map(entry => {
    if (entry.status === 'completed') return entry
    const value = data[entry.fieldKey]
    if (typeof value !== 'string' || value === THREAD_TRACE_PENDING) return entry
    return { ...entry, status: 'completed', stack: value }
  })
}

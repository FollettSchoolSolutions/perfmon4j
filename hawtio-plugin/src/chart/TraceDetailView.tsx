import { Content, EmptyState, EmptyStateBody } from '@patternfly/react-core'
import React from 'react'
import { ThreadTraceEntry } from './threadTraceQueue'

export interface TraceDetailViewProps {
  /** The trace selected via a completed row's View action (T10), or null before
   * any selection - also null if the selected trace was since Cancel/Delete'd out
   * of the queue, since it's looked up by fieldKey from the live `traces` list
   * rather than held as a standalone copy. */
  trace: ThreadTraceEntry | null
}

/**
 * "Trace detail" tab content (T11, legacy #11) - submission time, monitor name,
 * and the captured stack in a monospace block. The stack can be arbitrarily long
 * on one "line" per frame with no natural wrap point, so the block gets its own
 * `overflow-x: auto` container rather than relying on PatternFly's CodeBlock
 * defaults (unverified for this exact case) - same "contain overflow in the pane,
 * not the page body" convention LiveChart's fixed-width SVG already established
 * (see MONITORING_TAB_TASKS.md T1).
 */
export const TraceDetailView: React.FunctionComponent<TraceDetailViewProps> = ({ trace }) => {
  if (!trace || trace.stack === null) {
    return (
      <EmptyState titleText='No trace selected' headingLevel='h4'>
        <EmptyStateBody>
          Select a completed trace&apos;s View action in the Thread traces tab to see its captured stack here.
        </EmptyStateBody>
      </EmptyState>
    )
  }

  return (
    <div>
      <Content>
        <Content component='p'>
          <strong>Monitor:</strong> {trace.monitorLabel}
          <br />
          <strong>Submitted:</strong> {new Date(trace.submittedAt).toLocaleString()}
        </Content>
      </Content>
      <div style={{ overflowX: 'auto', border: '1px solid var(--pf-t--global--border--color--default)' }}>
        <pre style={{ margin: 0, padding: '0.75rem', fontFamily: 'monospace', whiteSpace: 'pre' }}>{trace.stack}</pre>
      </div>
    </div>
  )
}

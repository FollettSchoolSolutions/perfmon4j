import { Button, Label } from '@patternfly/react-core'
import { TrashIcon } from '@patternfly/react-icons'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React from 'react'
import { ThreadTraceEntry } from './threadTraceQueue'

export interface ThreadTraceQueueTableProps {
  traces: ThreadTraceEntry[]
  /** Enabled only for a `completed` row - a pending trace has no stack yet. Opens the
   * captured stack as a standalone HTML report in a new browser tab (see
   * MonitoringDetailTabs' onViewTrace / buildThreadTraceReportHtml). */
  onView: (fieldKey: string) => void
  /** Safe to call on either status - unScheduleThreadTrace() is a harmless no-op
   * server-side once a trace has already completed (see remoteManagementClient.ts /
   * threadTraceQueue.ts's one-shot-read note), so this always both tells the server
   * and clears the local row, whether it reads as "Cancel" (pending) or "Delete"
   * (completed) to the user. */
  onCancel: (fieldKey: string) => void
}

/**
 * Thread-trace queue listing (T9 built the read-only Monitor/Submitted/Status
 * columns; T10 adds View/Cancel). Rows are keyed by `fieldKey`, so a poll-driven
 * `traces` update (see useThreadTraces.ts) reconciles in place - only the cells
 * whose values actually changed (typically just Status, Pending -> Completed) touch
 * the DOM, not a full table remount, avoiding the reflow-flicker risk this task
 * called out.
 */
export const ThreadTraceQueueTable: React.FunctionComponent<ThreadTraceQueueTableProps> = ({ traces, onView, onCancel }) => {
  if (traces.length === 0) {
    return null
  }

  return (
    <Table aria-label='Thread traces' variant='compact'>
      <Thead>
        <Tr>
          <Th>Monitor</Th>
          <Th>Submitted</Th>
          <Th>Status</Th>
          <Th screenReaderText='View' />
          <Th screenReaderText='Cancel' />
        </Tr>
      </Thead>
      <Tbody>
        {traces.map(({ fieldKey, monitorLabel, submittedAt, status }) => (
          <Tr key={fieldKey}>
            <Td dataLabel='Monitor'>{monitorLabel}</Td>
            <Td dataLabel='Submitted'>{new Date(submittedAt).toLocaleTimeString()}</Td>
            <Td dataLabel='Status'>
              <Label color={status === 'completed' ? 'green' : 'yellow'}>{status}</Label>
            </Td>
            <Td dataLabel='View'>
              <Button variant='link' isInline isDisabled={status !== 'completed'} onClick={() => onView(fieldKey)}>
                View
              </Button>
            </Td>
            <Td dataLabel='Cancel'>
              <Button
                variant='plain'
                aria-label={status === 'completed' ? `Delete trace for ${monitorLabel}` : `Cancel trace for ${monitorLabel}`}
                icon={<TrashIcon />}
                onClick={() => onCancel(fieldKey)}
              />
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

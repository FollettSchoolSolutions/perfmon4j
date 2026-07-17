import { Label } from '@patternfly/react-core'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React from 'react'
import { ThreadTraceEntry } from './threadTraceQueue'

export interface ThreadTraceQueueTableProps {
  traces: ThreadTraceEntry[]
}

/**
 * Minimal read-only queue listing (T9) - proves a scheduled trace shows up here as
 * "pending" and transitions to "completed", per T9's own acceptance criteria. The
 * full interactive table (per-row View/Cancel, live in-place updates as an explicit
 * concern) is T10 - this is deliberately the smaller slice this task's scope needs,
 * built as a natural base for T10 to add columns to rather than replace.
 */
export const ThreadTraceQueueTable: React.FunctionComponent<ThreadTraceQueueTableProps> = ({ traces }) => {
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
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

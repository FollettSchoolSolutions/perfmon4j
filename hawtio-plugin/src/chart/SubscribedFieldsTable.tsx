import { Button } from '@patternfly/react-core'
import { TrashIcon } from '@patternfly/react-icons'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React from 'react'
import { FieldSeries } from './types'

export interface SubscribedFieldsTableProps {
  series: FieldSeries[]
  onRemove: (fieldKey: string) => void
}

export const SubscribedFieldsTable: React.FunctionComponent<SubscribedFieldsTableProps> = ({ series, onRemove }) => {
  if (series.length === 0) {
    return null
  }

  return (
    <Table aria-label='Charted fields' variant='compact'>
      <Thead>
        <Tr>
          <Th>Monitor</Th>
          <Th>Field</Th>
          <Th>Latest Value</Th>
          <Th screenReaderText='Remove' />
        </Tr>
      </Thead>
      <Tbody>
        {series.map(({ field, latestValue }) => (
          <Tr key={field.fieldKey}>
            <Td dataLabel='Monitor'>{field.monitorKey}</Td>
            <Td dataLabel='Field'>{field.fieldName}</Td>
            <Td dataLabel='Latest Value'>{latestValue ?? '—'}</Td>
            <Td dataLabel='Remove'>
              <Button
                variant='plain'
                aria-label={`Remove ${field.label} from chart`}
                icon={<TrashIcon />}
                onClick={() => onRemove(field.fieldKey)}
              />
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
}

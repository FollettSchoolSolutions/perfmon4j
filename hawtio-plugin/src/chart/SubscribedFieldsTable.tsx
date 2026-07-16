import { Button } from '@patternfly/react-core'
import { TrashIcon } from '@patternfly/react-icons'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React from 'react'
import { FieldSeries } from './types'

export interface SubscribedFieldsTableProps {
  series: FieldSeries[]
  onRemove: (fieldKey: string) => void
  onColorChange: (fieldKey: string, color: string) => void
}

export const SubscribedFieldsTable: React.FunctionComponent<SubscribedFieldsTableProps> = ({
  series,
  onRemove,
  onColorChange,
}) => {
  if (series.length === 0) {
    return null
  }

  return (
    <Table aria-label='Charted fields' variant='compact'>
      <Thead>
        <Tr>
          <Th screenReaderText='Color' />
          <Th>Monitor</Th>
          <Th>Field</Th>
          <Th>Latest Value</Th>
          <Th screenReaderText='Remove' />
        </Tr>
      </Thead>
      <Tbody>
        {series.map(({ field, latestValue, color }) => (
          <Tr key={field.fieldKey}>
            <Td dataLabel='Color'>
              {/* No PatternFly ColorPicker exists in this version - a native
                  color input is keyboard-operable, needs no extra dependency,
                  and lets the user pick any color, not just the palette. */}
              <input
                type='color'
                value={color}
                aria-label={`Line color for ${field.label}`}
                onChange={e => onColorChange(field.fieldKey, e.target.value)}
                style={{ width: '1.5rem', height: '1.5rem', padding: 0, border: 'none', background: 'none', cursor: 'pointer' }}
              />
            </Td>
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

import { Button } from '@patternfly/react-core'
import { EyeIcon, EyeSlashIcon, TrashIcon } from '@patternfly/react-icons'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React from 'react'
import { formatScaleLabel, SCALE_FACTORS } from './seriesScale'
import { FieldSeries } from './types'

export interface SubscribedFieldsTableProps {
  series: FieldSeries[]
  onRemove: (fieldKey: string) => void
  onColorChange: (fieldKey: string, color: string) => void
  onVisibilityChange: (fieldKey: string, visible: boolean) => void
  onScaleChange: (fieldKey: string, scale: number) => void
}

export const SubscribedFieldsTable: React.FunctionComponent<SubscribedFieldsTableProps> = ({
  series,
  onRemove,
  onColorChange,
  onVisibilityChange,
  onScaleChange,
}) => {
  if (series.length === 0) {
    return null
  }

  return (
    <Table aria-label='Charted fields' variant='compact'>
      <Thead>
        <Tr>
          <Th screenReaderText='Visible' />
          <Th screenReaderText='Color' />
          <Th>Monitor</Th>
          <Th>Field</Th>
          <Th>Latest Value</Th>
          <Th>Scale</Th>
          <Th screenReaderText='Remove' />
        </Tr>
      </Thead>
      <Tbody>
        {series.map(({ field, latestValue, color, visible, scale }) => (
          <Tr key={field.fieldKey}>
            <Td dataLabel='Visible'>
              <Button
                variant='plain'
                aria-label={visible ? `Hide ${field.label} on chart` : `Show ${field.label} on chart`}
                icon={visible ? <EyeIcon /> : <EyeSlashIcon />}
                onClick={() => onVisibilityChange(field.fieldKey, !visible)}
              />
            </Td>
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
            <Td dataLabel='Scale'>
              {/* Plots this series on LiveChart's shared [0, 100] y-axis at
                  rawValue * scale, clamped - see seriesScale.ts. A plain native
                  <select>, matching the Color column's native-input convention
                  (keyboard-operable, no new dependency). */}
              <select
                aria-label={`Y-axis scale factor for ${field.label}`}
                value={scale}
                onChange={e => onScaleChange(field.fieldKey, Number(e.target.value))}
              >
                {SCALE_FACTORS.map(factor => (
                  <option key={factor} value={factor}>
                    {formatScaleLabel(factor)}
                  </option>
                ))}
              </select>
            </Td>
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

import { Radio } from '@patternfly/react-core'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React from 'react'
import { AttributeRow, Classification } from './types'

export interface AttributeSelectionTableProps {
  attributes: AttributeRow[]
  onChange: (name: string, classification: Classification) => void
}

export const AttributeSelectionTable: React.FunctionComponent<AttributeSelectionTableProps> = ({
  attributes,
  onChange,
}) => (
  <Table aria-label='MBean attribute selection' variant='compact'>
    <Thead>
      <Tr>
        <Th>Attribute</Th>
        <Th>Type</Th>
        <Th>Skip</Th>
        <Th>Gauge</Th>
        <Th>Counter</Th>
      </Tr>
    </Thead>
    <Tbody>
      {attributes.map(attr => (
        <Tr key={attr.name}>
          <Td dataLabel='Attribute'>{attr.name}</Td>
          <Td dataLabel='Type'>{attr.type}</Td>
          {(['skip', 'gauge', 'counter'] as Classification[]).map(option => (
            <Td key={option} dataLabel={option}>
              <Radio
                id={`${attr.name}-${option}`}
                name={`classification-${attr.name}`}
                isChecked={attr.classification === option}
                onChange={() => onChange(attr.name, option)}
                aria-label={`${attr.name} as ${option}`}
              />
            </Td>
          ))}
        </Tr>
      ))}
    </Tbody>
  </Table>
)

import { Alert, Bullseye, Button, Checkbox, SearchInput, Spinner } from '@patternfly/react-core'
import { Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table'
import React, { useEffect, useState } from 'react'
import { isNumericFieldType } from './monitorKey'
import { FieldDescriptor, MonitorDescriptor } from './types'

export interface MonitorFieldPickerProps {
  enabled: boolean
  listMonitors: () => Promise<MonitorDescriptor[]>
  listFieldsForMonitor: (monitorKey: string) => Promise<FieldDescriptor[]>
  addFields: (fields: FieldDescriptor[]) => Promise<void>
}

export const MonitorFieldPicker: React.FunctionComponent<MonitorFieldPickerProps> = ({
  enabled,
  listMonitors,
  listFieldsForMonitor,
  addFields,
}) => {
  const [monitors, setMonitors] = useState<MonitorDescriptor[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [searchText, setSearchText] = useState('')

  const [selectedMonitor, setSelectedMonitor] = useState<MonitorDescriptor | null>(null)
  const [fields, setFields] = useState<FieldDescriptor[] | null>(null)
  const [fieldsError, setFieldsError] = useState<string | null>(null)
  const [selectedFieldKeys, setSelectedFieldKeys] = useState<Set<string>>(new Set())

  const [addError, setAddError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    listMonitors()
      .then(loaded => {
        if (!cancelled) setMonitors(loaded)
      })
      .catch(e => {
        if (!cancelled) setLoadError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [enabled, listMonitors])

  useEffect(() => {
    if (!selectedMonitor) {
      setFields(null)
      return
    }
    let cancelled = false
    setFields(null)
    setFieldsError(null)
    listFieldsForMonitor(selectedMonitor.monitorKey)
      .then(loaded => {
        if (!cancelled) setFields(loaded)
      })
      .catch(e => {
        if (!cancelled) setFieldsError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [selectedMonitor, listFieldsForMonitor])

  if (!enabled) {
    return null
  }

  if (loadError) {
    return <Alert variant='warning' isInline title={`Unable to load monitors: ${loadError}`} />
  }

  if (!monitors) {
    return (
      <Bullseye>
        <Spinner size='lg' aria-label='Loading monitors' />
      </Bullseye>
    )
  }

  const filteredMonitors = searchText
    ? monitors.filter(m => m.label.toLowerCase().includes(searchText.toLowerCase()))
    : monitors

  const numericFields = fields?.filter(f => isNumericFieldType(f.fieldType)) ?? []

  const toggleField = (fieldKey: string) => {
    setSelectedFieldKeys(prev => {
      const next = new Set(prev)
      if (next.has(fieldKey)) {
        next.delete(fieldKey)
      } else {
        next.add(fieldKey)
      }
      return next
    })
  }

  const onAddSelected = async () => {
    setAddError(null)
    const toAdd = numericFields.filter(f => selectedFieldKeys.has(f.fieldKey))
    if (toAdd.length === 0) return
    try {
      await addFields(toAdd)
      setSelectedFieldKeys(new Set())
    } catch (e) {
      setAddError(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <>
      <SearchInput
        aria-label='Search monitors'
        placeholder='Search monitors'
        value={searchText}
        onChange={(_, value) => setSearchText(value)}
        onClear={() => setSearchText('')}
      />

      <Table aria-label='Monitors' variant='compact'>
        <Thead>
          <Tr>
            <Th>Monitor</Th>
            <Th>Type</Th>
          </Tr>
        </Thead>
        <Tbody>
          {filteredMonitors.map(monitor => (
            <Tr
              key={monitor.monitorKey}
              isClickable
              isRowSelected={selectedMonitor?.monitorKey === monitor.monitorKey}
              onRowClick={() => setSelectedMonitor(monitor)}
            >
              <Td dataLabel='Monitor'>{monitor.label}</Td>
              <Td dataLabel='Type'>{monitor.type}</Td>
            </Tr>
          ))}
        </Tbody>
      </Table>

      {selectedMonitor && fieldsError && (
        <Alert variant='warning' isInline title={`Unable to load fields for ${selectedMonitor.label}: ${fieldsError}`} />
      )}

      {selectedMonitor && !fieldsError && !fields && (
        <Bullseye>
          <Spinner size='md' aria-label='Loading fields' />
        </Bullseye>
      )}

      {selectedMonitor && fields && (
        <>
          {numericFields.length === 0 ? (
            <Alert variant='info' isInline title={`${selectedMonitor.label} has no chartable (numeric) fields.`} />
          ) : (
            <>
              {numericFields.map(field => (
                <Checkbox
                  key={field.fieldKey}
                  id={`field-${field.fieldKey}`}
                  label={`${field.fieldName} (${field.fieldType})`}
                  isChecked={selectedFieldKeys.has(field.fieldKey)}
                  onChange={() => toggleField(field.fieldKey)}
                />
              ))}
              <Button variant='secondary' onClick={onAddSelected} isDisabled={selectedFieldKeys.size === 0}>
                Add selected to chart
              </Button>
            </>
          )}
        </>
      )}

      {addError && <Alert variant='danger' isInline title={`Unable to subscribe: ${addError}`} />}
    </>
  )
}

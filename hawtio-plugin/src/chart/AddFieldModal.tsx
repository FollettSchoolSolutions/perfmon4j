import {
  Alert,
  Bullseye,
  Button,
  Checkbox,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Spinner,
} from '@patternfly/react-core'
import React, { useEffect, useState } from 'react'
import { isNumericFieldType } from './monitorKey'
import { FieldDescriptor, MonitorDescriptor } from './types'

export interface AddFieldModalProps {
  /** The monitor being added to, or null when the modal is closed. */
  monitor: MonitorDescriptor | null
  onClose: () => void
  listFieldsForMonitor: (monitorKey: string) => Promise<FieldDescriptor[]>
  addFields: (fields: FieldDescriptor[]) => Promise<void>
}

/**
 * Field-selection modal opened from a monitor tree row's "Add field to
 * chart" action (see MonitorTree.tsx). Lifted out of the pre-T2
 * MonitorFieldPicker, which combined monitor browsing and field selection
 * in one always-visible panel - selecting a monitor no longer implies
 * charting it (see MONITORING_TAB_TASKS.md T2/T4), so this is now an
 * on-demand dialog instead.
 */
export const AddFieldModal: React.FunctionComponent<AddFieldModalProps> = ({
  monitor,
  onClose,
  listFieldsForMonitor,
  addFields,
}) => {
  const [fields, setFields] = useState<FieldDescriptor[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [selectedFieldKeys, setSelectedFieldKeys] = useState<Set<string>>(new Set())
  const [addError, setAddError] = useState<string | null>(null)

  useEffect(() => {
    if (!monitor) return
    let cancelled = false
    setFields(null)
    setLoadError(null)
    setSelectedFieldKeys(new Set())
    setAddError(null)
    listFieldsForMonitor(monitor.monitorKey)
      .then(loaded => {
        if (!cancelled) setFields(loaded)
      })
      .catch(e => {
        if (!cancelled) setLoadError(e instanceof Error ? e.message : String(e))
      })
    return () => {
      cancelled = true
    }
  }, [monitor, listFieldsForMonitor])

  if (!monitor) return null

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
      onClose()
    } catch (e) {
      setAddError(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <Modal isOpen variant='small' onClose={onClose} aria-label={`Add field to chart from ${monitor.label}`}>
      <ModalHeader title={`Add field to chart: ${monitor.label}`} />
      <ModalBody>
        {loadError && <Alert variant='warning' isInline title={`Unable to load fields: ${loadError}`} />}

        {!loadError && !fields && (
          <Bullseye>
            <Spinner size='md' aria-label='Loading fields' />
          </Bullseye>
        )}

        {fields && numericFields.length === 0 && (
          <Alert variant='info' isInline title={`${monitor.label} has no chartable (numeric) fields.`} />
        )}

        {fields &&
          numericFields.map(field => (
            <Checkbox
              key={field.fieldKey}
              id={`add-field-${field.fieldKey}`}
              label={`${field.fieldName} (${field.fieldType})`}
              isChecked={selectedFieldKeys.has(field.fieldKey)}
              onChange={() => toggleField(field.fieldKey)}
            />
          ))}

        {addError && <Alert variant='danger' isInline title={`Unable to subscribe: ${addError}`} />}
      </ModalBody>
      <ModalFooter>
        <Button variant='primary' onClick={onAddSelected} isDisabled={selectedFieldKeys.size === 0}>
          Add selected to chart
        </Button>
        <Button variant='link' onClick={onClose}>
          Cancel
        </Button>
      </ModalFooter>
    </Modal>
  )
}

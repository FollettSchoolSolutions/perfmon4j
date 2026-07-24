import {
  Alert,
  Button,
  Form,
  FormGroup,
  FormHelperText,
  FormSelect,
  FormSelectOption,
  HelperText,
  HelperTextItem,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  NumberInput,
  TextInput,
} from '@patternfly/react-core'
import React, { useEffect, useState } from 'react'
import { ThreadTraceOptions } from './threadTraceKey'
import {
  isThreadTraceFormValid,
  ThreadTraceFormState,
  ThreadTraceTriggerFormType,
  toThreadTraceOptions,
  validateThreadTraceForm,
} from './threadTraceOptionsValidation'
import { MonitorDescriptor } from './types'

// Only the three HTTP-request-associated trigger types are exposed for on-demand
// scheduling - ThreadNameTrigger/ThreadPropertyTrigger stay XML-config-only (see the
// plan doc for why). 'NONE' means "don't attach a trigger."
const TRIGGER_TYPE_OPTIONS: { value: ThreadTraceTriggerFormType; label: string }[] = [
  { value: 'NONE', label: '(none)' },
  { value: 'HTTP', label: 'Request parameter' },
  { value: 'HTTP_SESSION', label: 'Session attribute' },
  { value: 'HTTP_COOKIE', label: 'Cookie' },
]

export interface ScheduleThreadTraceModalProps {
  /** The INTERVAL monitor being targeted, or null when the modal is closed. */
  monitor: MonitorDescriptor | null
  onClose: () => void
  onSchedule: (monitor: MonitorDescriptor, options: ThreadTraceOptions) => Promise<void>
}

// Matches the field defaults ThreadTraceConfig itself falls back to when an arg is
// omitted (0/0) - starting the form there instead of blank lets a user schedule a
// sensible trace (capture-everything, unlimited-ish depth is *not* the default; 0
// duration means "capture every invocation," which is the useful common case for a
// quick ad-hoc trace) with zero typing, while still allowing blank for "don't pass
// this arg at all" (see toThreadTraceOptions).
const DEFAULT_FORM_STATE: ThreadTraceFormState = {
  minDurationToCaptureMillis: 0,
  maxDepth: 20,
  triggerType: 'NONE',
  triggerName: '',
  triggerValue: '',
}

/**
 * Field-selection-style modal opened from a monitor tree row's "Schedule thread
 * trace…" action (see MonitorTree.tsx, T4/T9) - submits via useThreadTraces'
 * scheduleTrace. Legacy VisualVM equivalent: ThreadTraceOptionsDlg.java, whose own
 * okButtonActionPerformed left two "TODO should check for non-numeric and or
 * negative values..." comments unresolved for over a decade - see
 * threadTraceOptionsValidation.ts for the real validation this dialog adds instead.
 */
export const ScheduleThreadTraceModal: React.FunctionComponent<ScheduleThreadTraceModalProps> = ({
  monitor,
  onClose,
  onSchedule,
}) => {
  const [form, setForm] = useState<ThreadTraceFormState>(DEFAULT_FORM_STATE)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // Reset to defaults each time a *different* monitor is targeted (modal re-opens) -
  // not on every render, and not when the modal closes (monitor -> null), so a
  // just-submitted or just-cancelled form doesn't visibly flash back to defaults
  // before the modal finishes closing.
  useEffect(() => {
    if (monitor) {
      setForm(DEFAULT_FORM_STATE)
      setSubmitError(null)
    }
  }, [monitor])

  if (!monitor) return null

  const validation = validateThreadTraceForm(form)
  const valid = isThreadTraceFormValid(validation)

  const onSubmit = async () => {
    if (!valid) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await onSchedule(monitor, toThreadTraceOptions(form))
      onClose()
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : String(e))
    } finally {
      setSubmitting(false)
    }
  }

  const parseNumberInput = (event: React.FormEvent<HTMLInputElement>, fallback: number | ''): number | '' => {
    const raw = (event.target as HTMLInputElement).value
    if (raw === '') return ''
    const parsed = Number(raw)
    return Number.isNaN(parsed) ? fallback : parsed
  }

  return (
    <Modal isOpen variant='small' onClose={onClose} aria-label={`Schedule a thread trace on ${monitor.label}`}>
      <ModalHeader title={`Schedule thread trace: ${monitor.label}`} />
      <ModalBody>
        <Form>
          <FormGroup label='Minimum duration to capture (ms)' fieldId='thread-trace-min-duration'>
            <NumberInput
              id='thread-trace-min-duration'
              value={form.minDurationToCaptureMillis}
              min={0}
              validated={validation.minDurationError ? 'error' : 'default'}
              inputAriaLabel='Minimum duration to capture in milliseconds'
              minusBtnAriaLabel='Decrement minimum duration'
              plusBtnAriaLabel='Increment minimum duration'
              onMinus={() =>
                setForm(f => ({ ...f, minDurationToCaptureMillis: Math.max(0, (f.minDurationToCaptureMillis || 0) - 1) }))
              }
              onPlus={() => setForm(f => ({ ...f, minDurationToCaptureMillis: (f.minDurationToCaptureMillis || 0) + 1 }))}
              onChange={event =>
                setForm(f => ({ ...f, minDurationToCaptureMillis: parseNumberInput(event, f.minDurationToCaptureMillis) }))
              }
            />
            {validation.minDurationError && (
              <FormHelperText>
                <HelperText>
                  <HelperTextItem variant='error'>{validation.minDurationError}</HelperTextItem>
                </HelperText>
              </FormHelperText>
            )}
          </FormGroup>
          <FormGroup label='Maximum stack depth' fieldId='thread-trace-max-depth'>
            <NumberInput
              id='thread-trace-max-depth'
              value={form.maxDepth}
              min={0}
              validated={validation.maxDepthError ? 'error' : 'default'}
              inputAriaLabel='Maximum stack depth'
              minusBtnAriaLabel='Decrement maximum stack depth'
              plusBtnAriaLabel='Increment maximum stack depth'
              onMinus={() => setForm(f => ({ ...f, maxDepth: Math.max(0, (f.maxDepth || 0) - 1) }))}
              onPlus={() => setForm(f => ({ ...f, maxDepth: (f.maxDepth || 0) + 1 }))}
              onChange={event => setForm(f => ({ ...f, maxDepth: parseNumberInput(event, f.maxDepth) }))}
            />
            {validation.maxDepthError && (
              <FormHelperText>
                <HelperText>
                  <HelperTextItem variant='error'>{validation.maxDepthError}</HelperTextItem>
                </HelperText>
              </FormHelperText>
            )}
          </FormGroup>
          <FormGroup label='Trigger (optional)' fieldId='thread-trace-trigger-type'>
            <FormSelect
              id='thread-trace-trigger-type'
              value={form.triggerType}
              aria-label='Trigger type'
              onChange={(_event, value) =>
                setForm(f => ({ ...f, triggerType: value as ThreadTraceTriggerFormType }))
              }
            >
              {TRIGGER_TYPE_OPTIONS.map(opt => (
                <FormSelectOption key={opt.value} value={opt.value} label={opt.label} />
              ))}
            </FormSelect>
          </FormGroup>
          {form.triggerType !== 'NONE' && (
            <>
              <FormGroup label='Trigger name' fieldId='thread-trace-trigger-name'>
                <TextInput
                  id='thread-trace-trigger-name'
                  value={form.triggerName}
                  validated={validation.triggerNameError ? 'error' : 'default'}
                  onChange={(_event, value) => setForm(f => ({ ...f, triggerName: value }))}
                />
                {validation.triggerNameError && (
                  <FormHelperText>
                    <HelperText>
                      <HelperTextItem variant='error'>{validation.triggerNameError}</HelperTextItem>
                    </HelperText>
                  </FormHelperText>
                )}
              </FormGroup>
              <FormGroup label='Trigger value' fieldId='thread-trace-trigger-value'>
                <TextInput
                  id='thread-trace-trigger-value'
                  value={form.triggerValue}
                  validated={validation.triggerValueError ? 'error' : 'default'}
                  onChange={(_event, value) => setForm(f => ({ ...f, triggerValue: value }))}
                />
                {validation.triggerValueError && (
                  <FormHelperText>
                    <HelperText>
                      <HelperTextItem variant='error'>{validation.triggerValueError}</HelperTextItem>
                    </HelperText>
                  </FormHelperText>
                )}
              </FormGroup>
            </>
          )}
        </Form>
        {submitError && <Alert variant='danger' isInline title={`Unable to schedule trace: ${submitError}`} />}
      </ModalBody>
      <ModalFooter>
        <Button variant='primary' onClick={() => void onSubmit()} isDisabled={!valid || submitting} isLoading={submitting}>
          Schedule
        </Button>
        <Button variant='link' onClick={onClose}>
          Cancel
        </Button>
      </ModalFooter>
    </Modal>
  )
}

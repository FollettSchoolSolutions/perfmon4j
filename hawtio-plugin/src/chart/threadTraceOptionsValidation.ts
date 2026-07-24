import { ThreadTraceOptions, ThreadTraceTriggerType } from './threadTraceKey'

// 'NONE' is the form-only "no trigger selected" state; ThreadTraceOptions.trigger is
// simply omitted in that case (see toThreadTraceOptions below).
export type ThreadTraceTriggerFormType = ThreadTraceTriggerType | 'NONE'

// Both min-duration/max-depth are optional here, mirroring the legacy VisualVM dialog
// (ThreadTraceOptionsDlg.okButtonActionPerformed) treating a blank field as "not
// passed" rather than defaulting it - the server itself defaults an omitted arg to 0
// (ThreadTraceConfig's own field defaults). Unlike that legacy dialog, whose
// okButtonActionPerformed has two `// TODO should check for non-numeric and or
// negative values...` comments it never resolved, this validates for real.
export interface ThreadTraceFormState {
  minDurationToCaptureMillis: number | ''
  maxDepth: number | ''
  triggerType: ThreadTraceTriggerFormType
  triggerName: string
  triggerValue: string
}

export interface ThreadTraceFieldValidation {
  minDurationError: string | null
  maxDepthError: string | null
  triggerNameError: string | null
  triggerValueError: string | null
}

function validateNonNegativeInteger(value: number | ''): string | null {
  if (value === '') return null
  if (!Number.isInteger(value)) return 'Must be a whole number'
  if (value < 0) return 'Must be zero or greater'
  return null
}

export function validateThreadTraceForm(values: ThreadTraceFormState): ThreadTraceFieldValidation {
  const triggerSelected = values.triggerType !== 'NONE'
  return {
    minDurationError: validateNonNegativeInteger(values.minDurationToCaptureMillis),
    maxDepthError: validateNonNegativeInteger(values.maxDepth),
    triggerNameError: triggerSelected && values.triggerName.trim() === '' ? 'Required' : null,
    triggerValueError: triggerSelected && values.triggerValue.trim() === '' ? 'Required' : null,
  }
}

export function isThreadTraceFormValid(validation: ThreadTraceFieldValidation): boolean {
  return (
    validation.minDurationError === null &&
    validation.maxDepthError === null &&
    validation.triggerNameError === null &&
    validation.triggerValueError === null
  )
}

/** Only call once isThreadTraceFormValid(validateThreadTraceForm(values)) is true. */
export function toThreadTraceOptions(values: ThreadTraceFormState): ThreadTraceOptions {
  const options: ThreadTraceOptions = {}
  if (values.minDurationToCaptureMillis !== '') options.minDurationToCaptureMillis = values.minDurationToCaptureMillis
  if (values.maxDepth !== '') options.maxDepth = values.maxDepth
  if (values.triggerType !== 'NONE') {
    options.trigger = { type: values.triggerType, name: values.triggerName, value: values.triggerValue }
  }
  return options
}

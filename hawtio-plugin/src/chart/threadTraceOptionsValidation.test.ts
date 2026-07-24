import {
  isThreadTraceFormValid,
  toThreadTraceOptions,
  validateThreadTraceForm,
} from './threadTraceOptionsValidation'

const NO_TRIGGER = { triggerType: 'NONE' as const, triggerName: '', triggerValue: '' }

describe('validateThreadTraceForm', () => {
  it('allows both fields blank', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: '', maxDepth: '', ...NO_TRIGGER })
    expect(result).toEqual({ minDurationError: null, maxDepthError: null, triggerNameError: null, triggerValueError: null })
  })

  it('allows zero for both fields', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: 0, maxDepth: 0, ...NO_TRIGGER })
    expect(result).toEqual({ minDurationError: null, maxDepthError: null, triggerNameError: null, triggerValueError: null })
  })

  it('rejects a negative minDurationToCaptureMillis', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: -1, maxDepth: '', ...NO_TRIGGER })
    expect(result.minDurationError).toMatch(/zero or greater/)
  })

  it('rejects a negative maxDepth', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: '', maxDepth: -5, ...NO_TRIGGER })
    expect(result.maxDepthError).toMatch(/zero or greater/)
  })

  it('rejects a non-integer value', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: 1.5, maxDepth: '', ...NO_TRIGGER })
    expect(result.minDurationError).toMatch(/whole number/)
  })

  it('allows no trigger type selected regardless of name/value', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: '', maxDepth: '', ...NO_TRIGGER })
    expect(result.triggerNameError).toBeNull()
    expect(result.triggerValueError).toBeNull()
  })

  it('requires name and value once a trigger type is selected', () => {
    const result = validateThreadTraceForm({
      minDurationToCaptureMillis: '',
      maxDepth: '',
      triggerType: 'HTTP_COOKIE',
      triggerName: '',
      triggerValue: '',
    })
    expect(result.triggerNameError).toMatch(/Required/)
    expect(result.triggerValueError).toMatch(/Required/)
  })

  it('rejects whitespace-only name/value once a trigger type is selected', () => {
    const result = validateThreadTraceForm({
      minDurationToCaptureMillis: '',
      maxDepth: '',
      triggerType: 'HTTP',
      triggerName: '  ',
      triggerValue: '  ',
    })
    expect(result.triggerNameError).toMatch(/Required/)
    expect(result.triggerValueError).toMatch(/Required/)
  })

  it('is valid once a trigger type, name, and value are all set', () => {
    const result = validateThreadTraceForm({
      minDurationToCaptureMillis: '',
      maxDepth: '',
      triggerType: 'HTTP_SESSION',
      triggerName: 'sessAttr',
      triggerValue: 'v',
    })
    expect(result.triggerNameError).toBeNull()
    expect(result.triggerValueError).toBeNull()
  })
})

describe('isThreadTraceFormValid', () => {
  const NO_TRIGGER_ERRORS = { triggerNameError: null, triggerValueError: null }

  it('is true when no field has an error', () => {
    expect(isThreadTraceFormValid({ minDurationError: null, maxDepthError: null, ...NO_TRIGGER_ERRORS })).toBe(true)
  })

  it('is false when either numeric field has an error', () => {
    expect(isThreadTraceFormValid({ minDurationError: 'bad', maxDepthError: null, ...NO_TRIGGER_ERRORS })).toBe(false)
    expect(isThreadTraceFormValid({ minDurationError: null, maxDepthError: 'bad', ...NO_TRIGGER_ERRORS })).toBe(false)
  })

  it('is false when either trigger field has an error', () => {
    expect(
      isThreadTraceFormValid({ minDurationError: null, maxDepthError: null, triggerNameError: 'bad', triggerValueError: null }),
    ).toBe(false)
    expect(
      isThreadTraceFormValid({ minDurationError: null, maxDepthError: null, triggerNameError: null, triggerValueError: 'bad' }),
    ).toBe(false)
  })
})

describe('toThreadTraceOptions', () => {
  it('omits blank fields entirely', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: '', maxDepth: '', ...NO_TRIGGER })).toEqual({})
  })

  it('includes only the fields that were set, including zero', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: 0, maxDepth: '', ...NO_TRIGGER })).toEqual({
      minDurationToCaptureMillis: 0,
    })
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: '', maxDepth: 20, ...NO_TRIGGER })).toEqual({ maxDepth: 20 })
  })

  it('includes both fields when both are set', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: 100, maxDepth: 20, ...NO_TRIGGER })).toEqual({
      minDurationToCaptureMillis: 100,
      maxDepth: 20,
    })
  })

  it('omits trigger when triggerType is NONE', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: '', maxDepth: '', ...NO_TRIGGER })).toEqual({})
  })

  it('includes trigger when a type is selected', () => {
    expect(
      toThreadTraceOptions({
        minDurationToCaptureMillis: '',
        maxDepth: '',
        triggerType: 'HTTP_COOKIE',
        triggerName: 'JSESSIONID',
        triggerValue: 'abc123',
      }),
    ).toEqual({
      trigger: { type: 'HTTP_COOKIE', name: 'JSESSIONID', value: 'abc123' },
    })
  })
})

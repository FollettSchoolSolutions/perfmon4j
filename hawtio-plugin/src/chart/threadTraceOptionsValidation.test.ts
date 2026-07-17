import {
  isThreadTraceFormValid,
  toThreadTraceOptions,
  validateThreadTraceForm,
} from './threadTraceOptionsValidation'

describe('validateThreadTraceForm', () => {
  it('allows both fields blank', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: '', maxDepth: '' })
    expect(result).toEqual({ minDurationError: null, maxDepthError: null })
  })

  it('allows zero for both fields', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: 0, maxDepth: 0 })
    expect(result).toEqual({ minDurationError: null, maxDepthError: null })
  })

  it('rejects a negative minDurationToCaptureMillis', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: -1, maxDepth: '' })
    expect(result.minDurationError).toMatch(/zero or greater/)
  })

  it('rejects a negative maxDepth', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: '', maxDepth: -5 })
    expect(result.maxDepthError).toMatch(/zero or greater/)
  })

  it('rejects a non-integer value', () => {
    const result = validateThreadTraceForm({ minDurationToCaptureMillis: 1.5, maxDepth: '' })
    expect(result.minDurationError).toMatch(/whole number/)
  })
})

describe('isThreadTraceFormValid', () => {
  it('is true when neither field has an error', () => {
    expect(isThreadTraceFormValid({ minDurationError: null, maxDepthError: null })).toBe(true)
  })

  it('is false when either field has an error', () => {
    expect(isThreadTraceFormValid({ minDurationError: 'bad', maxDepthError: null })).toBe(false)
    expect(isThreadTraceFormValid({ minDurationError: null, maxDepthError: 'bad' })).toBe(false)
  })
})

describe('toThreadTraceOptions', () => {
  it('omits blank fields entirely', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: '', maxDepth: '' })).toEqual({})
  })

  it('includes only the fields that were set, including zero', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: 0, maxDepth: '' })).toEqual({ minDurationToCaptureMillis: 0 })
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: '', maxDepth: 20 })).toEqual({ maxDepth: 20 })
  })

  it('includes both fields when both are set', () => {
    expect(toThreadTraceOptions({ minDurationToCaptureMillis: 100, maxDepth: 20 })).toEqual({
      minDurationToCaptureMillis: 100,
      maxDepth: 20,
    })
  })
})

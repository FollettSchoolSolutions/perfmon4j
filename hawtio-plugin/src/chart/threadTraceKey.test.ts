import { buildThreadTraceFieldKey, THREAD_TRACE_PENDING } from './threadTraceKey'

describe('buildThreadTraceFieldKey', () => {
  it('builds a bare key with no instance segment when no options are given', () => {
    expect(buildThreadTraceFieldKey('com.example.Foo')).toBe('THREADTRACE(name=com.example.Foo):FIELD(name=stack;type=STRING)')
  })

  it('encodes minDurationToCaptureMillis alone into the instance slot', () => {
    expect(buildThreadTraceFieldKey('com.example.Foo', { minDurationToCaptureMillis: 100 })).toBe(
      'THREADTRACE(name=com.example.Foo;instance=MinDurationToCapture=100):FIELD(name=stack;type=STRING)',
    )
  })

  it('encodes maxDepth alone into the instance slot', () => {
    expect(buildThreadTraceFieldKey('com.example.Foo', { maxDepth: 20 })).toBe(
      'THREADTRACE(name=com.example.Foo;instance=MaxDepth=20):FIELD(name=stack;type=STRING)',
    )
  })

  it('encodes both args comma-separated, min-duration first', () => {
    expect(buildThreadTraceFieldKey('com.example.Foo', { minDurationToCaptureMillis: 100, maxDepth: 20 })).toBe(
      'THREADTRACE(name=com.example.Foo;instance=MinDurationToCapture=100,MaxDepth=20):FIELD(name=stack;type=STRING)',
    )
  })

})

describe('THREAD_TRACE_PENDING', () => {
  it('matches the server-side sentinel literal', () => {
    expect(THREAD_TRACE_PENDING).toBe('ThreadTracePending')
  })
})

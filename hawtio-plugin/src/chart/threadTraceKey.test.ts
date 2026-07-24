import { buildThreadTraceFieldKey, encodeTriggerArg, THREAD_TRACE_PENDING } from './threadTraceKey'

// Node's test environment (jsdom) provides atob/btoa; decode manually to assert against
// the plaintext "PREFIX:name=value" payload without depending on the Java side.
function base64UrlDecode(encoded: string): string {
  const base64 = encoded.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
  return decodeURIComponent(escape(atob(padded)))
}

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

  it('encodes a trigger into the instance slot as an opaque Base64 URL-safe token', () => {
    const key = buildThreadTraceFieldKey('com.example.Foo', {
      trigger: { type: 'HTTP_COOKIE', name: 'JSESSIONID', value: 'abc123' },
    })
    const match = key.match(/^THREADTRACE\(name=com\.example\.Foo;instance=Trigger=([^)]+)\):FIELD\(name=stack;type=STRING\)$/)
    expect(match).not.toBeNull()
    expect(base64UrlDecode(match![1])).toBe('HTTP_COOKIE:JSESSIONID=abc123')
  })

  it('orders min-duration, max-depth, then trigger when all three are given', () => {
    const key = buildThreadTraceFieldKey('com.example.Foo', {
      minDurationToCaptureMillis: 100,
      maxDepth: 20,
      trigger: { type: 'HTTP', name: 'reqParam', value: 'v' },
    })
    expect(key.startsWith('THREADTRACE(name=com.example.Foo;instance=MinDurationToCapture=100,MaxDepth=20,Trigger=')).toBe(true)
  })

})

describe('encodeTriggerArg', () => {
  it('round-trips a simple name/value', () => {
    const encoded = encodeTriggerArg('HTTP_COOKIE', 'JSESSIONID', 'abc123')
    expect(base64UrlDecode(encoded)).toBe('HTTP_COOKIE:JSESSIONID=abc123')
  })

  it('produces a URL-safe token with no padding, comma, or equals', () => {
    const encoded = encodeTriggerArg('HTTP_SESSION', 'sessAttr', 'value')
    expect(encoded).toMatch(/^[A-Za-z0-9_-]+$/)
  })

  it('survives a value containing commas and equals signs untouched', () => {
    const encoded = encodeTriggerArg('HTTP', 'token', 'a,b=c=d')
    expect(encoded).toMatch(/^[A-Za-z0-9_-]+$/)
    expect(base64UrlDecode(encoded)).toBe('HTTP:token=a,b=c=d')
  })
})

describe('THREAD_TRACE_PENDING', () => {
  it('matches the server-side sentinel literal', () => {
    expect(THREAD_TRACE_PENDING).toBe('ThreadTracePending')
  })
})

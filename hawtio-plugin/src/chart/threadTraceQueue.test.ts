import { addPendingTrace, applyPollResult, removeTrace, ThreadTraceEntry } from './threadTraceQueue'
import { THREAD_TRACE_PENDING } from './threadTraceKey'

const FIELD_KEY_A = 'THREADTRACE(name=com.example.Foo):FIELD(name=stack;type=STRING)'
const FIELD_KEY_B = 'THREADTRACE(name=com.example.Bar):FIELD(name=stack;type=STRING)'

describe('addPendingTrace', () => {
  it('appends a new pending entry', () => {
    const result = addPendingTrace([], FIELD_KEY_A, 'com.example.Foo', 1000)
    expect(result).toEqual<ThreadTraceEntry[]>([
      { fieldKey: FIELD_KEY_A, monitorLabel: 'com.example.Foo', submittedAt: 1000, status: 'pending', stack: null },
    ])
  })

  it('does not mutate the input array', () => {
    const input: ThreadTraceEntry[] = []
    addPendingTrace(input, FIELD_KEY_A, 'com.example.Foo', 1000)
    expect(input).toEqual([])
  })
})

describe('removeTrace', () => {
  it('removes only the matching entry', () => {
    const traces = [
      { fieldKey: FIELD_KEY_A, monitorLabel: 'A', submittedAt: 1, status: 'pending' as const, stack: null },
      { fieldKey: FIELD_KEY_B, monitorLabel: 'B', submittedAt: 2, status: 'pending' as const, stack: null },
    ]
    expect(removeTrace(traces, FIELD_KEY_A)).toEqual([traces[1]])
  })
})

describe('applyPollResult', () => {
  const pending: ThreadTraceEntry = { fieldKey: FIELD_KEY_A, monitorLabel: 'A', submittedAt: 1, status: 'pending', stack: null }

  it('leaves a pending entry untouched when the field key is absent from the poll data', () => {
    expect(applyPollResult([pending], {})).toEqual([pending])
  })

  it('leaves a pending entry untouched while the server still reports the pending sentinel', () => {
    expect(applyPollResult([pending], { [FIELD_KEY_A]: THREAD_TRACE_PENDING })).toEqual([pending])
  })

  it('marks an entry completed and captures the stack text on a real string value', () => {
    const result = applyPollResult([pending], { [FIELD_KEY_A]: 'at com.example.Foo.bar(Foo.java:42)' })
    expect(result).toEqual([{ ...pending, status: 'completed', stack: 'at com.example.Foo.bar(Foo.java:42)' }])
  })

  it('never re-touches an already-completed entry, even if the field key reappears in poll data', () => {
    const completed: ThreadTraceEntry = { ...pending, status: 'completed', stack: 'original stack' }
    const result = applyPollResult([completed], { [FIELD_KEY_A]: 'different stack' })
    expect(result).toEqual([completed])
  })

  it('ignores a non-string value for the field key', () => {
    const result = applyPollResult([pending], { [FIELD_KEY_A]: 42 })
    expect(result).toEqual([pending])
  })
})

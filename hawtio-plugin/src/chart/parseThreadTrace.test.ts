import { countFrames, parseThreadTrace, TraceNode } from './parseThreadTrace'

const RULE = '*'.repeat(80)

// A small but structurally complete trace, byte-for-byte in the server's format:
// "\r\n" terminators, "|\t"-per-level indentation, an entry line's "(dur)" and an
// optional "(SQL:n)", and asterisk rule lines bracketing the body.
const SAMPLE = [
  RULE,
  '+-21:12:30:398 (7) WebRequest',
  '|\t+-21:12:30:398 (1) Child.a',
  '|\t+-21:12:30:399 Child.a',
  '|\t+-21:12:30:399 (6)(SQL:2) Child.b',
  '|\t|\t+-21:12:30:399 (1) Grandchild',
  '|\t|\t+-21:12:30:400 Grandchild',
  '|\t+-21:12:30:405 Child.b',
  '+-21:12:30:405 WebRequest',
  RULE,
].join('\r\n')

describe('parseThreadTrace', () => {
  it('reconstructs the frame tree with durations, timestamps, and nesting', () => {
    const { roots, truncated } = parseThreadTrace(SAMPLE)
    expect(truncated).toBe(false)
    expect(roots).toEqual<TraceNode[]>([
      {
        name: 'WebRequest',
        startTime: '21:12:30:398',
        endTime: '21:12:30:405',
        durationMs: 7,
        children: [
          { name: 'Child.a', startTime: '21:12:30:398', endTime: '21:12:30:399', durationMs: 1, children: [] },
          {
            name: 'Child.b',
            startTime: '21:12:30:399',
            endTime: '21:12:30:405',
            durationMs: 6,
            sqlMs: 2,
            children: [
              { name: 'Grandchild', startTime: '21:12:30:399', endTime: '21:12:30:400', durationMs: 1, children: [] },
            ],
          },
        ],
      },
    ])
  })

  it('distinguishes an exit line from an entry line by the absence of a duration', () => {
    // A method name that itself contains parentheses-like text must not be misread as
    // a duration; only the "(N)" immediately after the timestamp counts.
    const { roots } = parseThreadTrace(['+-21:12:30:398 (3) a(b) c', '+-21:12:30:401 a(b) c'].join('\r\n'))
    expect(roots).toHaveLength(1)
    expect(roots[0]).toMatchObject({ name: 'a(b) c', durationMs: 3, endTime: '21:12:30:401' })
    expect(roots[0].children).toEqual([])
  })

  it('flags a truncated trace and still parses the frames it did contain', () => {
    const truncatedText = [
      'Thread Trace Limit Exceeded -- Data truncated',
      RULE,
      '+-21:12:30:398 (7) WebRequest',
      '|\t+-21:12:30:398 (1) Child.a',
      '|\t+-21:12:30:399 Child.a',
      '+-21:12:30:405 WebRequest',
      RULE,
    ].join('\r\n')
    const { roots, truncated } = parseThreadTrace(truncatedText)
    expect(truncated).toBe(true)
    expect(roots).toHaveLength(1)
    expect(roots[0].children).toHaveLength(1)
  })

  it('tolerates tab-expanded (space) indentation', () => {
    const { roots } = parseThreadTrace(
      ['+-21:12:30:398 (7) Root', '|    +-21:12:30:399 (1) Kid', '|    +-21:12:30:400 Kid', '+-21:12:30:405 Root'].join(
        '\r\n',
      ),
    )
    expect(roots).toHaveLength(1)
    expect(roots[0].children).toHaveLength(1)
    expect(roots[0].children[0].name).toBe('Kid')
  })

  it('returns an empty result for blank or non-frame input', () => {
    expect(parseThreadTrace('')).toEqual({ roots: [], truncated: false })
    expect(parseThreadTrace([RULE, RULE].join('\r\n'))).toEqual({ roots: [], truncated: false })
  })
})

describe('countFrames', () => {
  it('counts every frame in the tree', () => {
    const { roots } = parseThreadTrace(SAMPLE)
    expect(countFrames(roots)).toBe(4)
  })
})

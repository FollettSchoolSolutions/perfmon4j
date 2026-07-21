import { buildThreadTraceReportHtml, ThreadTraceReportInput } from './buildThreadTraceReportHtml'
import { parseThreadTrace } from './parseThreadTrace'

const RULE = '*'.repeat(80)
const RAW = [
  RULE,
  '+-21:12:30:398 (7) WebRequest',
  '|\t+-21:12:30:398 (1) Child.a',
  '|\t+-21:12:30:399 Child.a',
  '+-21:12:30:405 WebRequest',
  RULE,
].join('\r\n')

function buildFrom(overrides: Partial<ThreadTraceReportInput> = {}): string {
  const { roots, truncated } = parseThreadTrace(RAW)
  return buildThreadTraceReportHtml({
    category: 'WebRequest',
    submittedText: 'Jul 20, 2026, 9:12:30 PM',
    minDurationToCaptureMillis: 0,
    maxDepth: 20,
    roots,
    truncated,
    rawText: RAW,
    ...overrides,
  })
}

describe('buildThreadTraceReportHtml', () => {
  it('produces a complete, self-contained HTML document', () => {
    const html = buildFrom()
    expect(html.startsWith('<!doctype html>')).toBe(true)
    expect(html).toContain('<title>perfmon4j thread trace: WebRequest</title>')
    // Everything inlined - no external stylesheet/script/font/image reference.
    expect(html).not.toMatch(/src\s*=\s*["']https?:/i)
    expect(html).not.toMatch(/href\s*=\s*["']https?:/i)
    expect(html).not.toMatch(/<link\b/i)
  })

  it('uses no JavaScript, so it works under a script-src CSP (blob: inherits the host CSP)', () => {
    const html = buildFrom()
    // A <script> or inline on* handler would be blocked by Hawtio's "script-src 'self'"
    // CSP once opened as a blob: document - bulk expand/collapse must be pure CSS.
    expect(html).not.toContain('<script')
    expect(html).not.toMatch(/\son[a-z]+\s*=/i)
    // The pure-CSS radio bulk controls and their labels are present.
    expect(html).toContain('id="bulk-expand"')
    expect(html).toContain('id="bulk-collapse"')
    expect(html).toContain('>Expand all</label>')
    expect(html).toContain('>Collapse all</label>')
  })

  it('renders the metadata header including the capture parameters', () => {
    const html = buildFrom()
    expect(html).toContain('Category')
    expect(html).toContain('Jul 20, 2026, 9:12:30 PM')
    expect(html).toContain('Min duration to capture')
    expect(html).toContain('Max stack depth')
    expect(html).toContain('Frames')
  })

  it('renders frames-with-children as nested collapsible <details> and leaves as plain rows', () => {
    const html = buildFrom()
    expect(html).toContain('<details open>')
    expect(html).toContain('class="children"')
    // The leaf child (Child.a) has no children, so it must NOT introduce its own details.
    const detailsCount = (html.match(/<details open>/g) ?? []).length
    expect(detailsCount).toBe(1) // only WebRequest wraps a details; Child.a is a leaf row
    expect(html).toContain('class="row leaf"')
  })

  it('shows a frame exit as a close footer so the open/close bracket stays visible', () => {
    expect(buildFrom()).toContain('↳ ended 21:12:30:405 · WebRequest')
  })

  it('escapes interpolated values to prevent HTML injection', () => {
    const raw = ['+-21:12:30:398 (2) <script>alert(1)</script>', '+-21:12:30:400 <script>alert(1)</script>'].join('\r\n')
    const { roots, truncated } = parseThreadTrace(raw)
    const html = buildThreadTraceReportHtml({
      category: 'a & b <x>',
      submittedText: 't',
      roots,
      truncated,
      rawText: raw,
    })
    expect(html).toContain('a &amp; b &lt;x&gt;')
    expect(html).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
    expect(html).not.toContain('<script>alert(1)</script>')
  })

  it('shows a truncation banner only when the trace overflowed', () => {
    expect(buildFrom({ truncated: false })).not.toContain('limit exceeded')
    expect(buildFrom({ truncated: true })).toContain('limit exceeded')
  })

  it('falls back to a dash for parameters that were not set', () => {
    const html = buildFrom({ minDurationToCaptureMillis: undefined, maxDepth: undefined })
    expect(html).toContain('<div class="mv">—</div>')
  })
})

import { appendPoint, trimToWindow } from './rollingSeries'

describe('appendPoint', () => {
  it('appends without mutating the original array', () => {
    const original = [{ timestamp: 1, value: 1 }]
    const result = appendPoint(original, { timestamp: 2, value: 2 })
    expect(original).toHaveLength(1)
    expect(result).toEqual([
      { timestamp: 1, value: 1 },
      { timestamp: 2, value: 2 },
    ])
  })
})

describe('trimToWindow', () => {
  const now = 100_000

  it('keeps points within the window', () => {
    const points = [
      { timestamp: now - 1000, value: 1 },
      { timestamp: now - 500, value: 2 },
      { timestamp: now, value: 3 },
    ]
    expect(trimToWindow(points, now, 2000)).toEqual(points)
  })

  it('drops points older than the window', () => {
    const points = [
      { timestamp: now - 5000, value: 1 },
      { timestamp: now - 500, value: 2 },
    ]
    expect(trimToWindow(points, now, 2000)).toEqual([{ timestamp: now - 500, value: 2 }])
  })

  it('keeps a point exactly at the cutoff boundary', () => {
    const points = [{ timestamp: now - 2000, value: 1 }]
    expect(trimToWindow(points, now, 2000)).toEqual(points)
  })

  it('enforces maxPoints as a defensive cap, keeping the most recent', () => {
    const points = Array.from({ length: 5 }, (_, i) => ({ timestamp: now - (4 - i), value: i }))
    const result = trimToWindow(points, now, 10_000, 3)
    expect(result).toEqual([
      { timestamp: now - 2, value: 2 },
      { timestamp: now - 1, value: 3 },
      { timestamp: now, value: 4 },
    ])
  })

  it('preserves oldest-first ordering', () => {
    const points = [
      { timestamp: now - 300, value: 1 },
      { timestamp: now - 200, value: 2 },
      { timestamp: now - 100, value: 3 },
    ]
    const result = trimToWindow(points, now, 10_000)
    expect(result.map(p => p.value)).toEqual([1, 2, 3])
  })

  it('uses the default window/cap when not specified', () => {
    const points = [{ timestamp: now, value: 1 }]
    expect(trimToWindow(points, now)).toEqual(points)
  })
})

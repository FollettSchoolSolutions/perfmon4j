import { applyScale, DEFAULT_SCALE, formatScaleLabel, SCALE_FACTORS } from './seriesScale'

describe('SCALE_FACTORS', () => {
  it('includes the default scale', () => {
    expect(SCALE_FACTORS).toContain(DEFAULT_SCALE)
  })

  it('is ordered largest to smallest, spanning six powers of ten in each direction', () => {
    expect(SCALE_FACTORS).toEqual([100000, 10000, 1000, 100, 10, 1, 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001])
  })
})

describe('applyScale', () => {
  it('multiplies the raw value by the scale factor', () => {
    expect(applyScale(50, 1)).toBe(50)
    expect(applyScale(5, 10)).toBe(50)
    expect(applyScale(5000, 0.01)).toBe(50)
  })

  it('clamps above 100 to 100', () => {
    expect(applyScale(150, 1)).toBe(100)
    expect(applyScale(500000, 0.0001)).toBeCloseTo(50)
    expect(applyScale(1000000, 1)).toBe(100)
  })

  it('clamps below 0 to 0', () => {
    expect(applyScale(-10, 1)).toBe(0)
    expect(applyScale(10, -1)).toBe(0)
  })
})

describe('formatScaleLabel', () => {
  it('prefixes the factor with a multiplication sign', () => {
    expect(formatScaleLabel(1)).toBe('× 1')
    expect(formatScaleLabel(0.01)).toBe('× 0.01')
    expect(formatScaleLabel(100000)).toBe('× 100000')
  })
})

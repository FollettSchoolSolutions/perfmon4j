import { colorForIndex, SERIES_COLOR_PALETTE } from './seriesColor'

describe('colorForIndex', () => {
  it('returns the palette entry at the given index', () => {
    expect(colorForIndex(0)).toBe(SERIES_COLOR_PALETTE[0])
    expect(colorForIndex(1)).toBe(SERIES_COLOR_PALETTE[1])
    expect(colorForIndex(SERIES_COLOR_PALETTE.length - 1)).toBe(SERIES_COLOR_PALETTE[SERIES_COLOR_PALETTE.length - 1])
  })

  it('wraps around once the palette is exhausted', () => {
    expect(colorForIndex(SERIES_COLOR_PALETTE.length)).toBe(SERIES_COLOR_PALETTE[0])
    expect(colorForIndex(SERIES_COLOR_PALETTE.length + 2)).toBe(SERIES_COLOR_PALETTE[2])
  })

  it('gives consecutive indices distinct colors within one palette cycle', () => {
    const colors = Array.from({ length: SERIES_COLOR_PALETTE.length }, (_, i) => colorForIndex(i))
    expect(new Set(colors).size).toBe(SERIES_COLOR_PALETTE.length)
  })
})

// A small categorical palette pulled from PatternFly's own chart color
// tokens (chart-color-<hue>-400 - a mid-brightness tier with good contrast
// on a light background), rather than inventing colors from scratch. Only
// hues PatternFly actually ships a "chart_color_*" family for are usable
// here (blue/green/orange/purple/red-orange - no cyan/gold/teal family).
export const SERIES_COLOR_PALETTE: string[] = [
  '#004d99', // chart-color-blue-400
  '#3d7317', // chart-color-green-400
  '#9e4a06', // chart-color-orange-400
  '#3d2785', // chart-color-purple-400
  '#b1380b', // chart-color-red-orange-400
]

/**
 * Deterministic color assignment, cycling through SERIES_COLOR_PALETTE by
 * index. Called once per field at add-time (see useRemoteManagementChart.ts's
 * addFields) and stored on the resulting FieldSeries - never recomputed from
 * an entry's current array position, so a field's color stays stable
 * regardless of what's added/removed around it afterward.
 */
export function colorForIndex(index: number): string {
  return SERIES_COLOR_PALETTE[index % SERIES_COLOR_PALETTE.length]
}

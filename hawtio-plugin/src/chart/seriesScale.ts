// Pure, dependency-free (see monitorKey.ts/rollingSeries.ts convention) - porting
// legacy VisualVM's FieldElement.factor / DynamicTimeSeriesChart.TimeSeriesWithFactor
// (visualvm-plugin/src/main/java/org/perfmon4j/visualvm/chart/), itself modeled on
// Windows Perfmon's classic per-counter "Scale" column: every series shares one fixed
// [0, 100] y-axis, and each series' raw value is multiplied by its own power-of-ten
// scale factor before being clamped into that range for display - so wildly
// different-magnitude fields (a 0-1 ratio next to a multi-thousand-ms duration) can be
// usefully overlaid on one chart by picking an appropriate factor per series. The
// scale only ever affects where a line is plotted - it never changes the raw value
// shown elsewhere (Latest Value column, saved dashboard fields, tooltips).

/** Matches legacy ChartElementsTable's factor combo box exactly (same 12 values,
 * same descending order). */
export const SCALE_FACTORS: readonly number[] = [100000, 10000, 1000, 100, 10, 1, 0.1, 0.01, 0.001, 0.0001, 0.00001, 0.000001]

export const DEFAULT_SCALE = 1

/** Mirrors legacy TimeSeriesWithFactor.adjustNumberBasedOnFactor: multiply by the
 * scale factor, then clamp into the shared [0, 100] chart range. */
export function applyScale(value: number, scale: number): number {
  return Math.min(100, Math.max(0, value * scale))
}

export function formatScaleLabel(scale: number): string {
  return `× ${scale}`
}

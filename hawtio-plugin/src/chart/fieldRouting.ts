import { isNumericFieldType } from './monitorKey'
import { FieldSeries } from './types'

export interface FieldSeriesPartition {
  /** Numeric fields - go to LiveChart + the "Charted fields" tab. */
  chartable: FieldSeries[]
  /** STRING/TIMESTAMP fields - go to the "Text fields" tab, never plotted. */
  textOnly: FieldSeries[]
}

/**
 * Routes subscribed field series to the chart or the flat "Text fields"
 * table, based on each field's declared type - the same distinction legacy
 * VisualVM's "Add Field to Chart..." dialog made via `FieldElement.isNumeric()`
 * (DOUBLE/LONG/INTEGER only; TIMESTAMP and STRING both route to text). See
 * LEGACY_VISUALVM_FEATURES.md #6. Pure and dependency-free, mirroring the
 * monitorKey.ts/monitorTreeLogic.ts convention.
 */
export function partitionByChartability(series: FieldSeries[]): FieldSeriesPartition {
  const chartable: FieldSeries[] = []
  const textOnly: FieldSeries[] = []
  for (const entry of series) {
    if (isNumericFieldType(entry.field.fieldType)) {
      chartable.push(entry)
    } else {
      textOnly.push(entry)
    }
  }
  return { chartable, textOnly }
}

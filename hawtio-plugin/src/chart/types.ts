export type NumericFieldType = 'INTEGER' | 'LONG' | 'DOUBLE'
export type FieldType = NumericFieldType | 'TIMESTAMP' | 'STRING'
export type MonitorType = 'INTERVAL' | 'SNAPSHOT' | 'THREADTRACE'

export interface MonitorDescriptor {
  /** Raw key as returned by getMonitors(), e.g. "INTERVAL(name=com.example.Foo)". */
  monitorKey: string
  type: MonitorType
  name: string
  instance?: string
  label: string
}

export interface FieldDescriptor {
  /** Raw key as returned by getFieldsForMonitor() - the full "<monitorKey>:FIELD(...)" string. */
  fieldKey: string
  monitorKey: string
  fieldName: string
  fieldType: FieldType
  label: string
}

export interface ChartDataPoint {
  /** Epoch ms. */
  timestamp: number
  value: number
}

export interface FieldSeries {
  field: FieldDescriptor
  /** Oldest-first, rolling-window-trimmed. Only ever populated for numeric fields. */
  points: ChartDataPoint[]
  /** A string for STRING/TIMESTAMP fields, a number for numeric ones. */
  latestValue: number | string | null
  /** Assigned once at add-time (see seriesColor.ts) and stable thereafter -
   * only meaningful for chartable (numeric) series, but kept on every entry
   * for a uniform FieldSeries shape. User-changeable from the Charted-fields
   * row (see MONITORING_TAB_TASKS.md T6). */
  color: string
  /** Toggled from the Charted-fields row (T7). Hides the line from LiveChart
   * without dropping the server-side subscription - polling/points keep
   * accumulating while hidden. Only meaningful for chartable (numeric)
   * series, but kept on every entry for a uniform FieldSeries shape. */
  visible: boolean
}

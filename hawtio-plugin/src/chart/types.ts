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
  /** Oldest-first, rolling-window-trimmed. */
  points: ChartDataPoint[]
  latestValue: number | null
}

export type Classification = 'gauge' | 'counter' | 'skip'

export interface AttributeRow {
  name: string
  type: string
  classification: Classification
}

export interface GenerateSnapshotXmlInput {
  monitorName: string
  jmxName: string
  gauges: string[]
  counters: string[]
}

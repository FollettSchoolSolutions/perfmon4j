import { DASHBOARD_FILE_VERSION, parseDashboardFile, partitionAvailableFields, serializeDashboard } from './chartDashboard'
import { FieldSeries } from './types'

const series: FieldSeries[] = [
  {
    field: {
      fieldKey: 'INTERVAL(name=com.example.Foo):FIELD(name=AverageDuration;type=DOUBLE)',
      monitorKey: 'INTERVAL(name=com.example.Foo)',
      fieldName: 'AverageDuration',
      fieldType: 'DOUBLE',
      label: 'com.example.Foo — AverageDuration',
    },
    points: [{ timestamp: 1, value: 2 }],
    latestValue: 2,
    color: '#123456',
    visible: false,
  },
]

describe('serializeDashboard / parseDashboardFile', () => {
  it('round-trips a charted series (key, color, visibility) without points/latestValue', () => {
    const file = serializeDashboard(series)
    expect(file.version).toBe(DASHBOARD_FILE_VERSION)
    expect(file.fields).toEqual([
      {
        fieldKey: 'INTERVAL(name=com.example.Foo):FIELD(name=AverageDuration;type=DOUBLE)',
        monitorKey: 'INTERVAL(name=com.example.Foo)',
        fieldName: 'AverageDuration',
        fieldType: 'DOUBLE',
        label: 'com.example.Foo — AverageDuration',
        color: '#123456',
        visible: false,
      },
    ])

    const reparsed = parseDashboardFile(JSON.stringify(file))
    expect(reparsed).toEqual(file)
  })

  it('rejects invalid JSON', () => {
    expect(() => parseDashboardFile('not json')).toThrow('not valid JSON')
  })

  it('rejects JSON that is not a dashboard file', () => {
    expect(() => parseDashboardFile('{}')).toThrow('not a perfmon4j chart dashboard file')
    expect(() => parseDashboardFile('[]')).toThrow('not a perfmon4j chart dashboard file')
    expect(() => parseDashboardFile(JSON.stringify({ fields: [{ fieldKey: 'x' }] }))).toThrow(
      'not a perfmon4j chart dashboard file',
    )
  })

  it('defaults a missing/non-numeric version rather than rejecting the file', () => {
    const withoutVersion = parseDashboardFile(JSON.stringify({ fields: [] }))
    expect(withoutVersion.version).toBe(DASHBOARD_FILE_VERSION)
  })
})

describe('partitionAvailableFields', () => {
  const fileFields = serializeDashboard(series).fields

  it('treats a field as available when its fieldKey is in the current JVM', () => {
    const { available, missing } = partitionAvailableFields(fileFields, new Set([fileFields[0].fieldKey]))
    expect(available).toEqual(fileFields)
    expect(missing).toEqual([])
  })

  it('treats a field as missing when absent from the current JVM (foreign JVM / removed monitor)', () => {
    const { available, missing } = partitionAvailableFields(fileFields, new Set())
    expect(available).toEqual([])
    expect(missing).toEqual(fileFields)
  })
})

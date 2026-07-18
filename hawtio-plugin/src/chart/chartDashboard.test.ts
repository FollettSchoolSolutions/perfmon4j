import { DASHBOARD_FILE_VERSION, parseDashboardFile, partitionAvailableFields, serializeDashboard } from './chartDashboard'
import { DEFAULT_SCALE } from './seriesScale'
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
    scale: 0.01,
  },
]

describe('serializeDashboard / parseDashboardFile', () => {
  it('round-trips a charted series (key, color, visibility, scale) without points/latestValue', () => {
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
        scale: 0.01,
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

  it('defaults scale to DEFAULT_SCALE when loading a version-1 file saved before scale existed', () => {
    const v1File = {
      version: 1,
      savedAt: '2026-01-01T00:00:00.000Z',
      fields: [
        {
          fieldKey: 'INTERVAL(name=com.example.Foo):FIELD(name=AverageDuration;type=DOUBLE)',
          monitorKey: 'INTERVAL(name=com.example.Foo)',
          fieldName: 'AverageDuration',
          fieldType: 'DOUBLE',
          label: 'com.example.Foo — AverageDuration',
          color: '#123456',
          visible: false,
          // no `scale` field - this is what a real v1 save produced.
        },
      ],
    }
    const parsed = parseDashboardFile(JSON.stringify(v1File))
    expect(parsed.version).toBe(1)
    expect(parsed.fields[0].scale).toBe(DEFAULT_SCALE)
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

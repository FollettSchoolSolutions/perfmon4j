import { toFieldDescriptor, toMonitorDescriptor } from './monitorKey'
import { partitionByChartability } from './fieldRouting'
import { FieldSeries } from './types'

const monitor = toMonitorDescriptor('INTERVAL(name=com.example.Foo)')

function seriesFor(fieldName: string, fieldType: string, latestValue: number | string | null = null): FieldSeries {
  const fieldKey = `${monitor.monitorKey}:FIELD(name=${fieldName};type=${fieldType})`
  return { field: toFieldDescriptor(monitor, fieldKey), points: [], latestValue, color: '#000000', visible: true, scale: 1 }
}

describe('partitionByChartability', () => {
  it('routes INTEGER, LONG, and DOUBLE fields to chartable', () => {
    const series = [seriesFor('a', 'INTEGER'), seriesFor('b', 'LONG'), seriesFor('c', 'DOUBLE')]
    const { chartable, textOnly } = partitionByChartability(series)
    expect(chartable).toEqual(series)
    expect(textOnly).toEqual([])
  })

  it('routes STRING and TIMESTAMP fields to textOnly', () => {
    const series = [seriesFor('stack', 'STRING'), seriesFor('TimeStop', 'TIMESTAMP')]
    const { chartable, textOnly } = partitionByChartability(series)
    expect(chartable).toEqual([])
    expect(textOnly).toEqual(series)
  })

  it('splits a mixed selection, preserving relative order within each group', () => {
    const num1 = seriesFor('AverageDuration', 'LONG')
    const str1 = seriesFor('stack', 'STRING')
    const num2 = seriesFor('ThroughputPerSecond', 'DOUBLE')
    const ts1 = seriesFor('TimeStop', 'TIMESTAMP')

    const { chartable, textOnly } = partitionByChartability([num1, str1, num2, ts1])
    expect(chartable).toEqual([num1, num2])
    expect(textOnly).toEqual([str1, ts1])
  })

  it('returns empty arrays for an empty input', () => {
    expect(partitionByChartability([])).toEqual({ chartable: [], textOnly: [] })
  })
})

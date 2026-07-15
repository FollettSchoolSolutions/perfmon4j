import { Chart, ChartAxis, ChartGroup, ChartLine, ChartThemeColor, ChartVoronoiContainer } from '@patternfly/react-charts/victory'
import { EmptyState, EmptyStateBody } from '@patternfly/react-core'
import React from 'react'
import { FieldSeries } from './types'

export interface LiveChartProps {
  series: FieldSeries[]
  windowMs: number
}

interface ChartPoint {
  x: number
  y: number
  name: string
}

/**
 * A single shared y-axis for v1 - fields with very different magnitudes/units (e.g.
 * a millisecond duration next to a 0-1 ratio) can look flat next to each other. A
 * known, accepted v1 rough edge; per-series normalization is a possible follow-up.
 */
export const LiveChart: React.FunctionComponent<LiveChartProps> = ({ series, windowMs }) => {
  if (series.length === 0) {
    return (
      <EmptyState titleText='No fields charted yet' headingLevel='h4'>
        <EmptyStateBody>Add a monitor field above to start charting its live values.</EmptyStateBody>
      </EmptyState>
    )
  }

  const now = Date.now()

  // Victory computes its own y-domain from the data range when none is given, but
  // for a perfectly flat series (min === max - e.g. a monitor under constant load,
  // exactly the case a bare dev-target demo loop produces) that degenerates to a
  // near-zero-width domain, which renders absurdly over-precise axis labels (and is
  // also what triggered a Victory "Infinity is an invalid value for width" console
  // warning, observed with a single-point series before this fix). Computing an
  // explicit domain with a sensible minimum pad avoids depending on Victory's own
  // heuristic for this edge case.
  const allValues = series.flatMap(s => s.points.map(p => p.value))
  const minValue = allValues.length > 0 ? Math.min(...allValues) : 0
  const maxValue = allValues.length > 0 ? Math.max(...allValues) : 1
  const valueRange = maxValue - minValue
  const yPadding = valueRange > 0 ? valueRange * 0.1 : Math.max(Math.abs(maxValue) * 0.1, 1)
  const yDomain: [number, number] = [minValue - yPadding, maxValue + yPadding]

  return (
    <Chart
      ariaTitle='perfmon4j live chart'
      containerComponent={<ChartVoronoiContainer labels={({ datum }: { datum: ChartPoint }) => `${datum.name}: ${datum.y}`} />}
      legendData={series.map(s => ({ name: s.field.label }))}
      legendPosition='bottom'
      themeColor={ChartThemeColor.multiUnordered}
      scale={{ x: 'time', y: 'linear' }}
      domain={{ x: [now - windowMs, now], y: yDomain }}
      height={300}
      width={800}
      padding={{ top: 20, bottom: 75, left: 60, right: 30 }}
    >
      <ChartAxis />
      <ChartAxis dependentAxis showGrid />
      <ChartGroup>
        {series.map(s => (
          <ChartLine
            key={s.field.fieldKey}
            name={s.field.label}
            data={s.points.map((p): ChartPoint => ({ x: p.timestamp, y: p.value, name: s.field.label }))}
          />
        ))}
      </ChartGroup>
    </Chart>
  )
}

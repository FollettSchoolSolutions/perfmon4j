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

  return (
    <Chart
      ariaTitle='perfmon4j live chart'
      containerComponent={<ChartVoronoiContainer labels={({ datum }: { datum: ChartPoint }) => `${datum.name}: ${datum.y}`} />}
      legendData={series.map(s => ({ name: s.field.label }))}
      legendPosition='bottom'
      themeColor={ChartThemeColor.multiUnordered}
      scale={{ x: 'time', y: 'linear' }}
      domain={{ x: [now - windowMs, now] }}
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

import { Chart, ChartAxis, ChartGroup, ChartLine, ChartVoronoiContainer } from '@patternfly/react-charts/victory'
import React from 'react'
import { applyScale } from './seriesScale'
import { FieldSeries } from './types'

export interface LiveChartProps {
  series: FieldSeries[]
  windowMs: number
}

interface ChartPoint {
  x: number
  y: number
  name: string
  /** Unscaled value, for the tooltip - `y` above is what's actually plotted
   * (post-`applyScale`), which is not what a user hovering wants to read. */
  rawValue: number
}

const Y_DOMAIN: [number, number] = [0, 100]

/**
 * Every series shares this fixed [0, 100] y-axis (T16, porting legacy VisualVM's
 * Windows-Perfmon-style per-counter "Scale" - see seriesScale.ts) rather than an
 * auto-computed one: each series' own `scale` factor (set from the Charted-fields
 * row, SubscribedFieldsTable.tsx) is applied to its raw value and clamped into
 * [0, 100] before plotting, so wildly different-magnitude fields (a 0-1 ratio next
 * to a multi-thousand-ms duration) can be usefully overlaid on one chart. This also
 * obsoletes the old per-render auto-domain-with-minimum-pad computation this
 * component used to need to avoid a degenerate near-zero-width domain for a
 * perfectly flat series - a fixed domain has no such degeneracy to guard against.
 * The `series.length === 0` case (no fields charted at all) is guarded by the
 * caller, ChartPanel.tsx, which renders a single shared empty state for the
 * whole content area instead of invoking this component - see MONITORING_TAB_TASKS.md.
 */
export const LiveChart: React.FunctionComponent<LiveChartProps> = ({ series, windowMs }) => {
  // Hidden series (T7) keep their subscription and keep accumulating points -
  // only what's drawn is filtered here, so re-showing a series doesn't need to
  // re-fetch anything.
  const visibleSeries = series.filter(s => s.visible)

  const now = Date.now()

  return (
    <Chart
      ariaTitle='perfmon4j live chart'
      containerComponent={
        <ChartVoronoiContainer labels={({ datum }: { datum: ChartPoint }) => `${datum.name}: ${datum.rawValue}`} />
      }
      legendData={visibleSeries.map(s => ({ name: s.field.label, symbol: { fill: s.color } }))}
      legendPosition='bottom'
      scale={{ x: 'time', y: 'linear' }}
      domain={{ x: [now - windowMs, now], y: Y_DOMAIN }}
      height={300}
      width={800}
      padding={{ top: 20, bottom: 75, left: 60, right: 30 }}
    >
      <ChartAxis />
      <ChartAxis dependentAxis showGrid />
      <ChartGroup>
        {visibleSeries.map(s => (
          <ChartLine
            key={s.field.fieldKey}
            name={s.field.label}
            style={{ data: { stroke: s.color } }}
            data={s.points.map(
              (p): ChartPoint => ({ x: p.timestamp, y: applyScale(p.value, s.scale), name: s.field.label, rawValue: p.value }),
            )}
          />
        ))}
      </ChartGroup>
    </Chart>
  )
}

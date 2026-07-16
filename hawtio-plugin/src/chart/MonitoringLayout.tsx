import { Grid, GridItem, Stack, StackItem } from '@patternfly/react-core'
import React from 'react'

export interface MonitoringLayoutProps {
  /** Monitor browsing/selection - left column. */
  left: React.ReactNode
  /** Live chart - top of the right column. */
  chart: React.ReactNode
  /** Tabbed/table detail below the chart - bottom of the right column. */
  detail: React.ReactNode
}

/**
 * Structural scaffold for the Monitoring tab (T1) - reflows VisualVM
 * MainWindow's monitor-tree / chart / detail split-pane into a responsive
 * PatternFly Grid rather than a resizable JSplitPane: a fixed column
 * proportion at >=md (768px, comfortably below the 1024px acceptance
 * threshold) and a single stacked column below that, so the page body never
 * scrolls horizontally regardless of viewport width. What's slotted into
 * each region is unchanged by this task - see MONITORING_TAB_TASKS.md T2
 * (left pane) and T3 (right pane detail tabs).
 */
export const MonitoringLayout: React.FunctionComponent<MonitoringLayoutProps> = ({ left, chart, detail }) => (
  <Grid hasGutter>
    <GridItem span={12} md={3}>
      {left}
    </GridItem>
    <GridItem span={12} md={9}>
      <Stack hasGutter>
        <StackItem>
          {/* LiveChart renders a fixed-width SVG (see LiveChart.tsx) - contained
              here so an overflow scrolls this pane, not the page body. */}
          <div style={{ overflowX: 'auto' }}>{chart}</div>
        </StackItem>
        <StackItem>{detail}</StackItem>
      </Stack>
    </GridItem>
  </Grid>
)

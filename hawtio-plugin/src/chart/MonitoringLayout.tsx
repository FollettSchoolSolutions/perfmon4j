import { Drawer, DrawerContent, DrawerContentBody, DrawerPanelContent, Stack, StackItem } from '@patternfly/react-core'
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
 * Structural scaffold for the Monitoring tab (T1, reworked post-launch for the
 * manual-testing UI pass) - reflows VisualVM MainWindow's monitor-tree / chart /
 * detail split-pane into a PatternFly `Drawer` used inline (`isInline`) and
 * always-open (`isStatic`) as a plain two-pane layout rather than its usual
 * slide-out-overlay role. The left pane is user-resizable
 * (`DrawerPanelContent isResizable`) with min/default/max bounds, replacing an
 * earlier fixed 12-column-Grid proportion that had no overflow guard and could
 * be too narrow or too wide depending on how deep/long a JVM's monitor names
 * happened to be. This intentionally does not stack the panes at narrow
 * viewports the way the old Grid did below ~768px - this plugin is desktop/
 * admin-tool usage in practice, and Drawer has no built-in equivalent to
 * Grid's responsive column collapse. What's slotted into each region is
 * unchanged by this - see MONITORING_TAB_TASKS.md T2 (left pane) and T3
 * (right pane detail tabs).
 */
export const MonitoringLayout: React.FunctionComponent<MonitoringLayoutProps> = ({ left, chart, detail }) => (
  // isExpanded is required alongside isStatic - PatternFly's panel-visibility CSS keys off
  // the drawer's `pf-m-expanded` modifier class regardless of isStatic, so without it the
  // panel renders in the DOM but stays `visibility: hidden`.
  <Drawer isStatic isExpanded isInline position='start'>
    <DrawerContent
      panelContent={
        <DrawerPanelContent isResizable minSize='180px' defaultSize='260px' maxSize='50%'>
          {left}
        </DrawerPanelContent>
      }
    >
      <DrawerContentBody>
        <Stack hasGutter>
          <StackItem>
            {/* LiveChart renders a fixed-width SVG (see LiveChart.tsx) - contained
                here so an overflow scrolls this pane, not the page body. */}
            <div style={{ overflowX: 'auto' }}>{chart}</div>
          </StackItem>
          <StackItem>{detail}</StackItem>
        </Stack>
      </DrawerContentBody>
    </DrawerContent>
  </Drawer>
)

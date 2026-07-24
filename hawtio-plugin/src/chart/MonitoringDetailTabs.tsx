import { EmptyState, EmptyStateBody, Tab, TabTitleText, Tabs } from '@patternfly/react-core'
import React, { useState } from 'react'
import { buildThreadTraceReportHtml } from './buildThreadTraceReportHtml'
import { openThreadTraceReport } from './openThreadTraceReport'
import { parseThreadTrace } from './parseThreadTrace'
import { SubscribedFieldsTable } from './SubscribedFieldsTable'
import { TextFieldsTable } from './TextFieldsTable'
import { ThreadTraceEntry } from './threadTraceQueue'
import { ThreadTraceQueueTable } from './ThreadTraceQueueTable'
import { FieldSeries } from './types'

export interface MonitoringDetailTabsProps {
  /** Numeric fields - see fieldRouting.ts's partitionByChartability. */
  chartableSeries: FieldSeries[]
  /** STRING/TIMESTAMP fields - never plotted, latest value only. */
  textSeries: FieldSeries[]
  onRemoveField: (fieldKey: string) => void
  onColorChange: (fieldKey: string, color: string) => void
  onVisibilityChange: (fieldKey: string, visible: boolean) => void
  onScaleChange: (fieldKey: string, scale: number) => void
  threadTraces: ThreadTraceEntry[]
  onCancelThreadTrace: (fieldKey: string) => void
}

type DetailTabKey = 'charted' | 'text' | 'threadTraces'

const StubTabBody: React.FunctionComponent<{ title: string; body: string }> = ({ title, body }) => (
  <EmptyState titleText={title} headingLevel='h4'>
    <EmptyStateBody>{body}</EmptyStateBody>
  </EmptyState>
)

/**
 * Bottom-right tabbed detail panel (T3) - Charted fields (SubscribedFieldsTable),
 * Text fields (TextFieldsTable, T5), and Thread traces (ThreadTraceQueueTable, T9/T10).
 * A completed trace's View opens a standalone HTML report in a new browser tab (built
 * by buildThreadTraceReportHtml) rather than an in-panel viewer. See MONITORING_TAB_TASKS.md.
 */
export const MonitoringDetailTabs: React.FunctionComponent<MonitoringDetailTabsProps> = ({
  chartableSeries,
  textSeries,
  onRemoveField,
  onColorChange,
  onVisibilityChange,
  onScaleChange,
  threadTraces,
  onCancelThreadTrace,
}) => {
  const [activeTabKey, setActiveTabKey] = useState<DetailTabKey>('charted')

  // View parses the completed trace's stack into a tree and opens a self-contained,
  // savable HTML report in a new tab. The trace is looked up by fieldKey from the live
  // list at click time, so a since-cancelled row simply no-ops.
  const onViewTrace = (fieldKey: string) => {
    const trace = threadTraces.find(t => t.fieldKey === fieldKey)
    if (!trace || trace.stack === null) return
    const { roots, truncated } = parseThreadTrace(trace.stack)
    const html = buildThreadTraceReportHtml({
      category: trace.monitorLabel,
      submittedText: new Date(trace.submittedAt).toLocaleString(),
      minDurationToCaptureMillis: trace.options.minDurationToCaptureMillis,
      maxDepth: trace.options.maxDepth,
      trigger: trace.options.trigger,
      roots,
      truncated,
      rawText: trace.stack,
    })
    openThreadTraceReport(html)
  }

  return (
    <Tabs activeKey={activeTabKey} onSelect={(_event, tabKey) => setActiveTabKey(tabKey as DetailTabKey)}>
      <Tab eventKey='charted' title={<TabTitleText>Charted fields</TabTitleText>}>
        {/* chartableSeries is never empty here - ChartPanel.tsx only renders this
            component once at least one field is charted, showing its own single
            shared empty state for the whole content area otherwise. */}
        <SubscribedFieldsTable
          series={chartableSeries}
          onRemove={onRemoveField}
          onColorChange={onColorChange}
          onVisibilityChange={onVisibilityChange}
          onScaleChange={onScaleChange}
        />
      </Tab>
      <Tab eventKey='text' title={<TabTitleText>Text fields</TabTitleText>}>
        {textSeries.length === 0 ? (
          <StubTabBody
            title='No text fields yet'
            body='Add a non-numeric (string or timestamp) field from the monitor tree to see its live value here.'
          />
        ) : (
          <TextFieldsTable series={textSeries} onRemove={onRemoveField} />
        )}
      </Tab>
      <Tab eventKey='threadTraces' title={<TabTitleText>Thread traces</TabTitleText>}>
        {threadTraces.length === 0 ? (
          <StubTabBody
            title='No thread traces yet'
            body='Schedule a thread trace from the monitor tree to see its status here.'
          />
        ) : (
          <ThreadTraceQueueTable traces={threadTraces} onView={onViewTrace} onCancel={onCancelThreadTrace} />
        )}
      </Tab>
    </Tabs>
  )
}

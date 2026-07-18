import { EmptyState, EmptyStateBody, Tab, TabTitleText, Tabs } from '@patternfly/react-core'
import React, { useState } from 'react'
import { SubscribedFieldsTable } from './SubscribedFieldsTable'
import { TextFieldsTable } from './TextFieldsTable'
import { ThreadTraceEntry } from './threadTraceQueue'
import { ThreadTraceQueueTable } from './ThreadTraceQueueTable'
import { TraceDetailView } from './TraceDetailView'
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

type DetailTabKey = 'charted' | 'text' | 'threadTraces' | 'traceDetail'

const StubTabBody: React.FunctionComponent<{ title: string; body: string }> = ({ title, body }) => (
  <EmptyState titleText={title} headingLevel='h4'>
    <EmptyStateBody>{body}</EmptyStateBody>
  </EmptyState>
)

/**
 * Bottom-right tabbed detail panel (T3) - Charted fields (SubscribedFieldsTable),
 * Text fields (TextFieldsTable, T5), Thread traces (ThreadTraceQueueTable, T9/T10),
 * and Trace detail (TraceDetailView, T11). See MONITORING_TAB_TASKS.md.
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
  // Set by a completed row's View action (T10), consumed by TraceDetailView (T11) -
  // kept here rather than in either child, since View's tab switch and the viewer
  // both need to agree on the same selection.
  const [selectedTraceFieldKey, setSelectedTraceFieldKey] = useState<string | null>(null)

  const onViewTrace = (fieldKey: string) => {
    setSelectedTraceFieldKey(fieldKey)
    setActiveTabKey('traceDetail')
  }

  // Looked up by fieldKey each render, not held as its own copy - if the selected
  // trace is since Cancel/Delete'd from the queue (T10), this naturally reverts to
  // null and TraceDetailView falls back to its empty state.
  const selectedTrace = threadTraces.find(t => t.fieldKey === selectedTraceFieldKey) ?? null

  return (
    <Tabs activeKey={activeTabKey} onSelect={(_event, tabKey) => setActiveTabKey(tabKey as DetailTabKey)}>
      <Tab eventKey='charted' title={<TabTitleText>Charted fields</TabTitleText>}>
        {chartableSeries.length === 0 ? (
          <StubTabBody title='No fields charted yet' body='Add a numeric field from the monitor tree to see it here.' />
        ) : (
          <SubscribedFieldsTable
            series={chartableSeries}
            onRemove={onRemoveField}
            onColorChange={onColorChange}
            onVisibilityChange={onVisibilityChange}
            onScaleChange={onScaleChange}
          />
        )}
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
      <Tab eventKey='traceDetail' title={<TabTitleText>Trace detail</TabTitleText>}>
        <TraceDetailView trace={selectedTrace} />
      </Tab>
    </Tabs>
  )
}

import { EmptyState, EmptyStateBody, Tab, TabTitleText, Tabs } from '@patternfly/react-core'
import React, { useState } from 'react'
import { SubscribedFieldsTable } from './SubscribedFieldsTable'
import { TextFieldsTable } from './TextFieldsTable'
import { FieldSeries } from './types'

export interface MonitoringDetailTabsProps {
  /** Numeric fields - see fieldRouting.ts's partitionByChartability. */
  chartableSeries: FieldSeries[]
  /** STRING/TIMESTAMP fields - never plotted, latest value only. */
  textSeries: FieldSeries[]
  onRemoveField: (fieldKey: string) => void
  onColorChange: (fieldKey: string, color: string) => void
  onVisibilityChange: (fieldKey: string, visible: boolean) => void
}

type DetailTabKey = 'charted' | 'text' | 'threadTraces' | 'traceDetail'

const StubTabBody: React.FunctionComponent<{ title: string; body: string }> = ({ title, body }) => (
  <EmptyState titleText={title} headingLevel='h4'>
    <EmptyStateBody>{body}</EmptyStateBody>
  </EmptyState>
)

/**
 * Bottom-right tabbed detail panel (T3) - the Charted-fields tab hosts the
 * existing SubscribedFieldsTable, and the Text fields tab (T5) hosts
 * TextFieldsTable for STRING/TIMESTAMP fields, which never plot. The
 * remaining two are stubs until their own tasks land (T10/T11 thread
 * traces). See MONITORING_TAB_TASKS.md.
 */
export const MonitoringDetailTabs: React.FunctionComponent<MonitoringDetailTabsProps> = ({
  chartableSeries,
  textSeries,
  onRemoveField,
  onColorChange,
  onVisibilityChange,
}) => {
  const [activeTabKey, setActiveTabKey] = useState<DetailTabKey>('charted')

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
        <StubTabBody
          title='Thread traces'
          body='Scheduled thread-trace captures will be queued here (coming soon - see MONITORING_TAB_TASKS.md T10).'
        />
      </Tab>
      <Tab eventKey='traceDetail' title={<TabTitleText>Trace detail</TabTitleText>}>
        <StubTabBody
          title='Trace detail'
          body="A selected thread trace's captured stack will render here (coming soon - see MONITORING_TAB_TASKS.md T11)."
        />
      </Tab>
    </Tabs>
  )
}

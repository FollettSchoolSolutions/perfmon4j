import { EmptyState, EmptyStateBody, Tab, TabTitleText, Tabs } from '@patternfly/react-core'
import React, { useState } from 'react'
import { SubscribedFieldsTable } from './SubscribedFieldsTable'
import { FieldSeries } from './types'

export interface MonitoringDetailTabsProps {
  series: FieldSeries[]
  onRemoveField: (fieldKey: string) => void
}

type DetailTabKey = 'charted' | 'text' | 'threadTraces' | 'traceDetail'

const StubTabBody: React.FunctionComponent<{ title: string; body: string }> = ({ title, body }) => (
  <EmptyState titleText={title} headingLevel='h4'>
    <EmptyStateBody>{body}</EmptyStateBody>
  </EmptyState>
)

/**
 * Bottom-right tabbed detail panel (T3) - the Charted-fields tab hosts the
 * existing SubscribedFieldsTable unchanged; the other three are stubs until
 * their own tasks land (T5 non-numeric fields, T10/T11 thread traces). See
 * MONITORING_TAB_TASKS.md.
 */
export const MonitoringDetailTabs: React.FunctionComponent<MonitoringDetailTabsProps> = ({ series, onRemoveField }) => {
  const [activeTabKey, setActiveTabKey] = useState<DetailTabKey>('charted')

  return (
    <Tabs activeKey={activeTabKey} onSelect={(_event, tabKey) => setActiveTabKey(tabKey as DetailTabKey)}>
      <Tab eventKey='charted' title={<TabTitleText>Charted fields</TabTitleText>}>
        {series.length === 0 ? (
          <StubTabBody title='No fields charted yet' body='Add a field to chart from the monitor tree to see it here.' />
        ) : (
          <SubscribedFieldsTable series={series} onRemove={onRemoveField} />
        )}
      </Tab>
      <Tab eventKey='text' title={<TabTitleText>Text fields</TabTitleText>}>
        <StubTabBody
          title='Text fields'
          body='Non-numeric field values will be listed here (coming soon - see MONITORING_TAB_TASKS.md T5).'
        />
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

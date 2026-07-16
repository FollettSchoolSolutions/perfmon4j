import { PageSection, Tab, TabTitleText, Tabs } from '@patternfly/react-core'
import React, { useState } from 'react'
import { AboutPanel } from './about'
import { ChartPanel } from './chart'
import { MBeanSnapshotPanel } from './mbean-snapshot'

type TabKey = 'monitoring' | 'config' | 'about'

/**
 * The single "Perfmon4j" nav item's component (see plugin.ts). Wraps the three
 * previously-standalone nav items (chart/mbean-snapshot/about) as tabs instead.
 * In-memory tab state only - no per-tab routing, so a refresh or shared link
 * always lands back on the Monitoring tab.
 *
 * Owns the single PageSection wrapper for all three tabs - each panel used to be
 * its own top-level page and wrapped itself in one, but nesting three PageSections
 * inside this one caused a real layout bug (confirmed via Playwright): the active
 * tab's content rendered a few pixels *above* the tab bar itself, permanently
 * covering the other tab buttons so they could never be clicked. Each panel's own
 * PageSection was removed for the same reason.
 */
export const Perfmon4jPanel: React.FunctionComponent = () => {
  const [activeTabKey, setActiveTabKey] = useState<TabKey>('monitoring')

  return (
    <PageSection hasBodyWrapper={false}>
      <Tabs activeKey={activeTabKey} onSelect={(_event, tabKey) => setActiveTabKey(tabKey as TabKey)}>
        <Tab eventKey='monitoring' title={<TabTitleText>Monitoring</TabTitleText>}>
          <ChartPanel />
        </Tab>
        <Tab eventKey='config' title={<TabTitleText>Config</TabTitleText>}>
          <MBeanSnapshotPanel />
        </Tab>
        <Tab eventKey='about' title={<TabTitleText>About</TabTitleText>}>
          <AboutPanel />
        </Tab>
      </Tabs>
    </PageSection>
  )
}

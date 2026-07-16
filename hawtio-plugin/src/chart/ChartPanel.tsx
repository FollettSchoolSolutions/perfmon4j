import { Alert, Button, Content, Spinner } from '@patternfly/react-core'
import React from 'react'
import { LiveChart } from './LiveChart'
import { MonitoringLayout } from './MonitoringLayout'
import { MonitorTree } from './MonitorTree'
import { SubscribedFieldsTable } from './SubscribedFieldsTable'
import { ConnectionErrorKind, useRemoteManagementChart } from './useRemoteManagementChart'
import { DEFAULT_WINDOW_MS } from './rollingSeries'

const CONNECTION_ERROR_COPY: Record<ConnectionErrorKind, string> = {
  'exec-denied':
    "This Hawtio console's Jolokia connection doesn't have permission to execute MBean operations, which the " +
    "perfmon4j chart requires. Ask your administrator to allow exec access for org.perfmon4j:type=RemoteManagement " +
    '(see the "Graceful degradation when Jolokia write/exec access is unavailable" item in the plugin\'s ROADMAP.md).',
  'incompatible-version':
    "The connected JVM's perfmon4j RemoteManagement version doesn't match this plugin - likely a version skew " +
    'between the plugin and the attached base build.',
  other: '',
}

export const ChartPanel: React.FunctionComponent = () => {
  const { status, connectionError, series, listMonitors, listFieldsForMonitor, addFields, removeField, retryConnect } =
    useRemoteManagementChart()

  return (
    <>
      <Content>
        <Content component='h1'>perfmon4j: Live Chart</Content>
        <Content component='p'>
          Browse perfmon4j&apos;s interval and snapshot monitors, pick one or more numeric fields, and watch their
          live values plot over a rolling {DEFAULT_WINDOW_MS / 60_000}-minute window.
        </Content>
      </Content>

      {status === 'connecting' && <Spinner size='md' aria-label='Connecting to perfmon4j remote management' />}

      {status === 'reconnecting' && (
        <Alert variant='info' isInline title='Session lost contact - reconnecting and restoring your chart selection…' />
      )}

      {status === 'disconnected' && connectionError && (
        <Alert
          variant='danger'
          isInline
          title={connectionError.kind === 'other' ? connectionError.message : CONNECTION_ERROR_COPY[connectionError.kind]}
          actionLinks={
            <Button variant='link' isInline onClick={retryConnect}>
              Retry
            </Button>
          }
        />
      )}

      <MonitoringLayout
        left={
          <MonitorTree
            enabled={status === 'connected'}
            listMonitors={listMonitors}
            listFieldsForMonitor={listFieldsForMonitor}
            addFields={addFields}
          />
        }
        chart={<LiveChart series={series} windowMs={DEFAULT_WINDOW_MS} />}
        detail={<SubscribedFieldsTable series={series} onRemove={removeField} />}
      />
    </>
  )
}

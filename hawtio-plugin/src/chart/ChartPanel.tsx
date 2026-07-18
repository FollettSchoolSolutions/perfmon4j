import { Alert, Button, Content, Spinner } from '@patternfly/react-core'
import React, { useEffect, useState } from 'react'
import { ChartDashboardControls } from './ChartDashboardControls'
import { LiveChart } from './LiveChart'
import { partitionByChartability } from './fieldRouting'
import { MonitoringDetailTabs } from './MonitoringDetailTabs'
import { MonitoringLayout } from './MonitoringLayout'
import { MonitorTree } from './MonitorTree'
import { MonitorDescriptor } from './types'
import { ConnectionErrorKind, useRemoteManagementChart } from './useRemoteManagementChart'
import { useThreadTraces } from './useThreadTraces'
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

const THREAD_TRACE_ERROR_COPY: Record<ConnectionErrorKind, string> = {
  'exec-denied':
    "This Hawtio console's Jolokia connection doesn't have permission to execute MBean operations, which scheduling " +
    'a thread trace requires. Ask your administrator to allow exec access for org.perfmon4j:type=RemoteManagement.',
  'incompatible-version':
    "The connected JVM's perfmon4j RemoteManagement version doesn't match this plugin - likely a version skew " +
    'between the plugin and the attached base build.',
  other: '',
}

// Unlike the two error copies above (terminal - the whole session/feature is down),
// this one specifically covers T13's graceful-degradation case: the rest of the
// session keeps working (monitor browsing, charting, thread traces) but this one
// operation is excluded from the host's Jolokia ACL - a real, supported Jolokia
// capability (per-operation allow/deny, not just an all-or-nothing exec toggle).
const FORCE_DYNAMIC_CREATION_DENIED_COPY =
  "This Hawtio console's Jolokia connection doesn't have permission to execute " +
  'forceDynamicChildCreation/unForceDynamicChildCreation specifically, so the "Force dynamic monitor creation" ' +
  'row action has been hidden. Everything else (browsing, charting, thread traces) is unaffected. Ask your ' +
  'administrator to allow exec access for these two operations on org.perfmon4j:type=RemoteManagement if you need it.'

export const ChartPanel: React.FunctionComponent = () => {
  const {
    status,
    connectionError,
    series,
    listMonitors,
    listFieldsForMonitor,
    addFields,
    removeField,
    setFieldColor,
    setFieldVisibility,
    retryConnect,
    forcedDynamicMonitors,
    forceDynamicCreationError,
    setForceDynamicChildCreation,
  } = useRemoteManagementChart()
  const { chartable, textOnly } = partitionByChartability(series)

  const {
    status: threadTraceStatus,
    connectionError: threadTraceConnectionError,
    traces: threadTraces,
    scheduleTrace,
    cancelTrace,
    retryConnect: retryThreadTraceConnect,
  } = useThreadTraces()

  const forceDynamicCreationDenied = forceDynamicCreationError?.kind === 'exec-denied'
  const [forceDynamicCreationAlertDismissed, setForceDynamicCreationAlertDismissed] = useState(false)
  useEffect(() => {
    if (!forceDynamicCreationDenied) setForceDynamicCreationAlertDismissed(false)
  }, [forceDynamicCreationDenied])

  const onToggleForceDynamicCreation = (monitor: MonitorDescriptor) => {
    void setForceDynamicChildCreation(monitor.monitorKey, !forcedDynamicMonitors.has(monitor.monitorKey))
  }

  return (
    <>
      <Content>
        <Content component='h1'>perfmon4j: Live Chart</Content>
        <Content component='p'>
          Browse perfmon4j&apos;s interval and snapshot monitors and pick one or more fields to monitor live -
          numeric fields plot on a rolling {DEFAULT_WINDOW_MS / 60_000}-minute chart, others show their latest
          value in the Text fields tab.
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

      {threadTraceStatus === 'disconnected' && threadTraceConnectionError && (
        <Alert
          variant='danger'
          isInline
          title={
            threadTraceConnectionError.kind === 'other'
              ? threadTraceConnectionError.message
              : THREAD_TRACE_ERROR_COPY[threadTraceConnectionError.kind]
          }
          actionLinks={
            <Button variant='link' isInline onClick={retryThreadTraceConnect}>
              Retry
            </Button>
          }
        />
      )}

      {forceDynamicCreationDenied && !forceDynamicCreationAlertDismissed && (
        <Alert
          variant='warning'
          isInline
          title={FORCE_DYNAMIC_CREATION_DENIED_COPY}
          actionLinks={
            <Button variant='link' isInline onClick={() => setForceDynamicCreationAlertDismissed(true)}>
              Dismiss
            </Button>
          }
        />
      )}

      <ChartDashboardControls
        series={series}
        enabled={status === 'connected'}
        listMonitors={listMonitors}
        listFieldsForMonitor={listFieldsForMonitor}
        addFields={addFields}
        setFieldColor={setFieldColor}
        setFieldVisibility={setFieldVisibility}
      />

      <MonitoringLayout
        left={
          <MonitorTree
            enabled={status === 'connected'}
            listMonitors={listMonitors}
            listFieldsForMonitor={listFieldsForMonitor}
            addFields={addFields}
            canScheduleThreadTrace={threadTraceStatus === 'connected'}
            onScheduleThreadTrace={scheduleTrace}
            forcedDynamicMonitors={forcedDynamicMonitors}
            canForceDynamicCreation={!forceDynamicCreationDenied}
            onToggleForceDynamicCreation={onToggleForceDynamicCreation}
          />
        }
        chart={<LiveChart series={chartable} windowMs={DEFAULT_WINDOW_MS} />}
        detail={
          <MonitoringDetailTabs
            chartableSeries={chartable}
            textSeries={textOnly}
            onRemoveField={removeField}
            onColorChange={setFieldColor}
            onVisibilityChange={setFieldVisibility}
            threadTraces={threadTraces}
            onCancelThreadTrace={fieldKey => void cancelTrace(fieldKey)}
          />
        }
      />
    </>
  )
}

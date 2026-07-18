import { useSyncExternalStore } from 'react'
import { ConnectionError, ConnectionStatus } from './connectionStatus'
import { chartStore } from './remoteManagementChartStore'
import { FieldDescriptor, FieldSeries, MonitorDescriptor } from './types'

export type { ConnectionStatus, ConnectionErrorKind, ConnectionError } from './connectionStatus'

export interface UseRemoteManagementChartResult {
  status: ConnectionStatus
  connectionError: ConnectionError | null
  series: FieldSeries[]
  listMonitors: () => Promise<MonitorDescriptor[]>
  listFieldsForMonitor: (monitorKey: string) => Promise<FieldDescriptor[]>
  /** Batched - pass every field to add in one call, not one call per field. */
  addFields: (fields: FieldDescriptor[]) => Promise<void>
  removeField: (fieldKey: string) => Promise<void>
  /** Client-side only - no server call, since color isn't part of the
   * RemoteManagement subscription protocol. */
  setFieldColor: (fieldKey: string, color: string) => void
  /** Client-side only - no server call; the subscription (and point
   * accumulation) stays live while hidden, only the chart line disappears. */
  setFieldVisibility: (fieldKey: string, visible: boolean) => void
  /** Client-side only - no server call; see seriesScale.ts for how this affects
   * LiveChart's fixed [0, 100] y-axis. */
  setFieldScale: (fieldKey: string, scale: number) => void
  retryConnect: () => void
  /** monitorKeys this session has forced dynamic child creation on (T13). */
  forcedDynamicMonitors: ReadonlySet<string>
  /** Set (until the next successful call) when force/un-force fails - see
   * remoteManagementChartStore.ts. */
  forceDynamicCreationError: ConnectionError | null
  setForceDynamicChildCreation: (monitorKey: string, forced: boolean) => Promise<void>
}

/**
 * A thin `useSyncExternalStore` view onto `chartStore` (T15) - the session,
 * subscriptions, and series data all live in that module-level singleton, not in
 * this hook, so they survive this component unmounting (e.g. Hawtio navigating to
 * a different nav item and back) instead of resetting to a fresh session every
 * time. See hawtio-plugin/CLAUDE.md and remoteManagementChartStore.ts for the full
 * reconnect/full-replacement-subscribe rationale.
 */
export function useRemoteManagementChart(): UseRemoteManagementChartResult {
  const snapshot = useSyncExternalStore(chartStore.subscribe, chartStore.getSnapshot)

  return {
    status: snapshot.status,
    connectionError: snapshot.connectionError,
    series: snapshot.series,
    listMonitors: chartStore.listMonitors,
    listFieldsForMonitor: chartStore.listFieldsForMonitor,
    addFields: chartStore.addFields,
    removeField: chartStore.removeField,
    setFieldColor: chartStore.setFieldColor,
    setFieldVisibility: chartStore.setFieldVisibility,
    setFieldScale: chartStore.setFieldScale,
    retryConnect: chartStore.retryConnect,
    forcedDynamicMonitors: snapshot.forcedDynamicMonitors,
    forceDynamicCreationError: snapshot.forceDynamicCreationError,
    setForceDynamicChildCreation: chartStore.setForceDynamicChildCreation,
  }
}

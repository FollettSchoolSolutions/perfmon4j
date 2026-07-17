import * as remoteManagementClient from '../jolokia/remoteManagementClient'
import { classifyConnectionError, ConnectionError, ConnectionStatus } from './connectionStatus'
import { isNumericFieldType, toFieldDescriptor, toMonitorDescriptor } from './monitorKey'
import { appendPoint, DEFAULT_MAX_POINTS, DEFAULT_WINDOW_MS, trimToWindow } from './rollingSeries'
import { colorForIndex } from './seriesColor'
import { FieldDescriptor, FieldSeries, MonitorDescriptor } from './types'

export interface RemoteManagementChartSnapshot {
  status: ConnectionStatus
  connectionError: ConnectionError | null
  series: FieldSeries[]
}

// Deliberately independent of jolokiaService's own console-wide update-rate
// preference (connect.jolokia.updateRate) - this chart gets a guaranteed cadence
// rather than one that can silently drift if that shared, global setting changes
// for reasons unrelated to this screen.
const POLL_MS = 5000

/**
 * Owns the RemoteManagement session lifecycle (connect/subscribe/poll/disconnect)
 * as a plain, non-React object - not React state, and not tied to any component's
 * mount/unmount (T15). Hawtio's other nav items are separate react-router routes;
 * navigating to one of those used to fully unmount `ChartPanel`, tearing this
 * session down and losing every charted field. A single instance of this class is
 * constructed once, at module-evaluation time (see the `chartStore` export below) -
 * ES modules are already singletons cached by the module system, so this needs no
 * extra "create once" guard of its own, and (unlike the old per-effect version) is
 * entirely unaffected by React 18 StrictMode's double-invoke-on-mount behavior,
 * since it isn't triggered by a render or an effect at all.
 * `useRemoteManagementChart.ts` is now a thin `useSyncExternalStore` wrapper around
 * this instance.
 */
class RemoteManagementChartStore {
  private snapshot: RemoteManagementChartSnapshot = { status: 'connecting', connectionError: null, series: [] }
  private readonly listeners = new Set<() => void>()

  private sessionId: string | null = null
  private subscribedFields: FieldDescriptor[] = []
  private intervalHandle: ReturnType<typeof setInterval> | null = null

  constructor() {
    void this.establishSession()
  }

  getSnapshot = (): RemoteManagementChartSnapshot => this.snapshot

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  private publish(partial: Partial<RemoteManagementChartSnapshot>): void {
    this.snapshot = { ...this.snapshot, ...partial }
    this.listeners.forEach(listener => listener())
  }

  private stopPolling(): void {
    if (this.intervalHandle !== null) {
      clearInterval(this.intervalHandle)
      this.intervalHandle = null
    }
  }

  private async poll(): Promise<void> {
    const sessionId = this.sessionId
    if (!sessionId) return
    try {
      const data = await remoteManagementClient.getData(sessionId)
      const now = Date.now()
      const series = this.snapshot.series.map(entry => {
        const rawValue = data[entry.field.fieldKey]
        // Routing (chart vs. Text fields tab) is decided by the field's declared
        // type (see fieldRouting.ts), not the runtime value - TIMESTAMP fields can
        // come back as a numeric epoch, and that must never get appended as a
        // chart point. Only points for a declared-numeric field get accumulated;
        // latestValue is kept for any recognized value so the Text fields tab
        // always has something to show.
        if (isNumericFieldType(entry.field.fieldType) && typeof rawValue === 'number') {
          const points = trimToWindow(appendPoint(entry.points, { timestamp: now, value: rawValue }), now, DEFAULT_WINDOW_MS, DEFAULT_MAX_POINTS)
          return { ...entry, points, latestValue: rawValue }
        }
        if (typeof rawValue === 'string' || typeof rawValue === 'number') {
          return { ...entry, latestValue: rawValue }
        }
        return entry
      })
      this.publish({ series })
    } catch (e) {
      const classified = classifyConnectionError(e)
      if (classified.kind === 'exec-denied' || classified.kind === 'incompatible-version') {
        // Retrying can't succeed for either of these - surface as a terminal error.
        this.stopPolling()
        this.publish({ status: 'disconnected', connectionError: classified })
        return
      }
      // Any other failure (most likely a SessionNotFoundException from the 5-minute
      // idle timeout, but jolokiaService.execute() doesn't reliably forward the
      // exception class name - see remoteManagementClient.ts) is treated as a
      // reconnect candidate: attempt one blind reconnect + resubscribe rather than
      // failing hard. Cheap and harmless if the session was actually fine.
      this.stopPolling()
      this.publish({ status: 'reconnecting' })
      await this.establishSession()
    }
  }

  private async establishSession(): Promise<void> {
    this.publish({ connectionError: null })
    try {
      const sessionId = await remoteManagementClient.connect()
      this.sessionId = sessionId
      if (this.subscribedFields.length > 0) {
        await remoteManagementClient.subscribeFields(
          sessionId,
          this.subscribedFields.map(f => f.fieldKey),
        )
      }
      this.publish({ status: 'connected' })
      this.stopPolling()
      this.intervalHandle = setInterval(() => {
        void this.poll()
      }, POLL_MS)
    } catch (e) {
      this.publish({ status: 'disconnected', connectionError: classifyConnectionError(e) })
    }
  }

  listMonitors = async (): Promise<MonitorDescriptor[]> => {
    const sessionId = this.sessionId
    if (!sessionId) return []
    const monitorKeys = await remoteManagementClient.getMonitors(sessionId)
    return monitorKeys
      .map(toMonitorDescriptor)
      .filter(m => m.type !== 'THREADTRACE') // thread-trace integration is out of scope for this chart
      .sort((a, b) => a.label.localeCompare(b.label))
  }

  listFieldsForMonitor = async (monitorKey: string): Promise<FieldDescriptor[]> => {
    const sessionId = this.sessionId
    if (!sessionId) return []
    const monitor = toMonitorDescriptor(monitorKey)
    const fieldKeys = await remoteManagementClient.getFieldsForMonitor(sessionId, monitorKey)
    return fieldKeys.map(fieldKey => toFieldDescriptor(monitor, fieldKey))
  }

  addFields = async (fields: FieldDescriptor[]): Promise<void> => {
    const sessionId = this.sessionId
    if (!sessionId) return
    const existingKeys = new Set(this.subscribedFields.map(f => f.fieldKey))
    const newFields = fields.filter(f => !existingKeys.has(f.fieldKey))
    if (newFields.length === 0) return
    // Colors are assigned once here, from the count of already-subscribed fields
    // at this moment - never recomputed from an entry's later array position, so
    // an existing field's color can't shift when others are added/removed around
    // it (see seriesColor.ts).
    const startColorIndex = this.subscribedFields.length
    const updated = [...this.subscribedFields, ...newFields]
    // subscribe() is full-replacement server-side - always send the complete list.
    await remoteManagementClient.subscribeFields(
      sessionId,
      updated.map(f => f.fieldKey),
    )
    this.subscribedFields = updated
    this.publish({
      series: [
        ...this.snapshot.series,
        ...newFields.map((field, i) => ({
          field,
          points: [],
          latestValue: null,
          color: colorForIndex(startColorIndex + i),
          visible: true,
        })),
      ],
    })
  }

  removeField = async (fieldKey: string): Promise<void> => {
    const sessionId = this.sessionId
    if (!sessionId) return
    const updated = this.subscribedFields.filter(f => f.fieldKey !== fieldKey)
    await remoteManagementClient.subscribeFields(
      sessionId,
      updated.map(f => f.fieldKey),
    )
    this.subscribedFields = updated
    this.publish({ series: this.snapshot.series.filter(entry => entry.field.fieldKey !== fieldKey) })
  }

  setFieldColor = (fieldKey: string, color: string): void => {
    this.publish({ series: this.snapshot.series.map(entry => (entry.field.fieldKey === fieldKey ? { ...entry, color } : entry)) })
  }

  setFieldVisibility = (fieldKey: string, visible: boolean): void => {
    this.publish({ series: this.snapshot.series.map(entry => (entry.field.fieldKey === fieldKey ? { ...entry, visible } : entry)) })
  }

  retryConnect = (): void => {
    this.publish({ status: 'connecting' })
    void this.establishSession()
  }
}

export const chartStore = new RemoteManagementChartStore()

import { useCallback, useEffect, useRef, useState } from 'react'
import * as remoteManagementClient from '../jolokia/remoteManagementClient'
import { classifyConnectionError, ConnectionError, ConnectionStatus } from './connectionStatus'
import { isNumericFieldType, toFieldDescriptor, toMonitorDescriptor } from './monitorKey'
import { appendPoint, DEFAULT_MAX_POINTS, DEFAULT_WINDOW_MS, trimToWindow } from './rollingSeries'
import { colorForIndex } from './seriesColor'
import { FieldDescriptor, FieldSeries, MonitorDescriptor } from './types'

export type { ConnectionStatus, ConnectionErrorKind, ConnectionError } from './connectionStatus'

export interface UseRemoteManagementChartOptions {
  pollMs?: number
  windowMs?: number
  maxPoints?: number
}

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
  retryConnect: () => void
}

// Deliberately independent of jolokiaService's own console-wide update-rate
// preference (connect.jolokia.updateRate) - this chart gets a guaranteed cadence
// rather than one that can silently drift if that shared, global setting changes
// for reasons unrelated to this screen.
const DEFAULT_POLL_MS = 5000

/**
 * Owns the RemoteManagement session lifecycle (connect/subscribe/poll/disconnect)
 * behind a small React-friendly API. See hawtio-plugin/CLAUDE.md and the chart
 * feature's design notes for the full reconnect/full-replacement-subscribe rationale.
 */
export function useRemoteManagementChart(options?: UseRemoteManagementChartOptions): UseRemoteManagementChartResult {
  const pollMs = options?.pollMs ?? DEFAULT_POLL_MS
  const windowMs = options?.windowMs ?? DEFAULT_WINDOW_MS
  const maxPoints = options?.maxPoints ?? DEFAULT_MAX_POINTS

  const [status, setStatus] = useState<ConnectionStatus>('connecting')
  const [connectionError, setConnectionError] = useState<ConnectionError | null>(null)
  const [series, setSeries] = useState<FieldSeries[]>([])

  const sessionIdRef = useRef<string | null>(null)
  const subscribedFieldsRef = useRef<FieldDescriptor[]>([])
  const intervalHandleRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const establishSessionRef = useRef<() => void>(() => {})

  useEffect(() => {
    // A per-invocation local flag, not a ref - React 18 StrictMode double-invokes
    // this effect in development (mount -> cleanup -> mount again), and a single
    // ref shared across both invocations would let the FIRST invocation's cleanup
    // get silently un-done by the SECOND invocation resetting it back to "alive",
    // causing the first invocation's in-flight connect()/subscribeFields() calls to
    // race the second's and corrupt sessionIdRef/intervalHandleRef with whichever
    // one happens to resolve last (this was an actual observed bug: two connect()
    // calls firing, with getData() polling landing on a different session than
    // getMonitors() used). Mirrors the per-effect `cancelled` flag convention already
    // used elsewhere in this plugin (see MBeanTreePicker.tsx/AboutPanel.tsx).
    let cancelled = false

    function stopPolling() {
      if (intervalHandleRef.current !== null) {
        clearInterval(intervalHandleRef.current)
        intervalHandleRef.current = null
      }
    }

    async function poll() {
      const sessionID = sessionIdRef.current
      if (!sessionID || cancelled) return
      try {
        const data = await remoteManagementClient.getData(sessionID)
        if (cancelled) return
        const now = Date.now()
        setSeries(prev =>
          prev.map(entry => {
            const rawValue = data[entry.field.fieldKey]
            // Routing (chart vs. Text fields tab) is decided by the field's
            // declared type (see fieldRouting.ts), not the runtime value -
            // TIMESTAMP fields can come back as a numeric epoch, and that
            // must never get appended as a chart point. Only points for a
            // declared-numeric field get accumulated; latestValue is kept
            // for any recognized value so the Text fields tab always has
            // something to show.
            if (isNumericFieldType(entry.field.fieldType) && typeof rawValue === 'number') {
              const points = trimToWindow(appendPoint(entry.points, { timestamp: now, value: rawValue }), now, windowMs, maxPoints)
              return { ...entry, points, latestValue: rawValue }
            }
            if (typeof rawValue === 'string' || typeof rawValue === 'number') {
              return { ...entry, latestValue: rawValue }
            }
            return entry
          }),
        )
      } catch (e) {
        if (cancelled) return
        const classified = classifyConnectionError(e)
        if (classified.kind === 'exec-denied' || classified.kind === 'incompatible-version') {
          // Retrying can't succeed for either of these - surface as a terminal error.
          stopPolling()
          setStatus('disconnected')
          setConnectionError(classified)
          return
        }
        // Any other failure (most likely a SessionNotFoundException from the 5-minute
        // idle timeout, but jolokiaService.execute() doesn't reliably forward the
        // exception class name - see remoteManagementClient.ts) is treated as a
        // reconnect candidate: attempt one blind reconnect + resubscribe rather than
        // failing hard. Cheap and harmless if the session was actually fine.
        stopPolling()
        setStatus('reconnecting')
        await establishSession()
      }
    }

    async function establishSession() {
      if (cancelled) return
      setConnectionError(null)
      try {
        const sessionID = await remoteManagementClient.connect()
        if (cancelled) {
          // This effect invocation was cleaned up while connect() was in flight -
          // nobody else knows about this session, so disconnect it ourselves rather
          // than leaking it (it would otherwise sit until its 5-minute idle timeout).
          remoteManagementClient.disconnect(sessionID).catch(() => undefined)
          return
        }
        sessionIdRef.current = sessionID
        if (subscribedFieldsRef.current.length > 0) {
          await remoteManagementClient.subscribeFields(
            sessionID,
            subscribedFieldsRef.current.map(f => f.fieldKey),
          )
        }
        if (cancelled) {
          remoteManagementClient.disconnect(sessionID).catch(() => undefined)
          return
        }
        setStatus('connected')
        stopPolling()
        intervalHandleRef.current = setInterval(() => {
          void poll()
        }, pollMs)
      } catch (e) {
        if (cancelled) return
        setStatus('disconnected')
        setConnectionError(classifyConnectionError(e))
      }
    }

    establishSessionRef.current = () => {
      setStatus('connecting')
      void establishSession()
    }

    setStatus('connecting')
    void establishSession()

    return () => {
      cancelled = true
      stopPolling()
      const sessionID = sessionIdRef.current
      if (sessionID) {
        // Best-effort, fire-and-forget - the component is already gone, and the
        // 5-minute server-side idle timeout reclaims the session regardless if this
        // call never lands (e.g. tab closed mid-flight).
        remoteManagementClient.disconnect(sessionID).catch(() => undefined)
      }
    }
    // establishSessionRef/stopPolling/poll are all local to this effect and rebuilt
    // together whenever the effect re-runs, so only the numeric options need listing.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pollMs, windowMs, maxPoints])

  const listMonitors = useCallback(async (): Promise<MonitorDescriptor[]> => {
    const sessionID = sessionIdRef.current
    if (!sessionID) return []
    const monitorKeys = await remoteManagementClient.getMonitors(sessionID)
    return monitorKeys
      .map(toMonitorDescriptor)
      .filter(m => m.type !== 'THREADTRACE') // thread-trace integration is out of scope for this chart
      .sort((a, b) => a.label.localeCompare(b.label))
  }, [])

  const listFieldsForMonitor = useCallback(async (monitorKey: string): Promise<FieldDescriptor[]> => {
    const sessionID = sessionIdRef.current
    if (!sessionID) return []
    const monitor = toMonitorDescriptor(monitorKey)
    const fieldKeys = await remoteManagementClient.getFieldsForMonitor(sessionID, monitorKey)
    return fieldKeys.map(fieldKey => toFieldDescriptor(monitor, fieldKey))
  }, [])

  const addFields = useCallback(async (fields: FieldDescriptor[]): Promise<void> => {
    const sessionID = sessionIdRef.current
    if (!sessionID) return
    const existingKeys = new Set(subscribedFieldsRef.current.map(f => f.fieldKey))
    const newFields = fields.filter(f => !existingKeys.has(f.fieldKey))
    if (newFields.length === 0) return
    // Colors are assigned once here, from the count of already-subscribed
    // fields at this moment - never recomputed from an entry's later array
    // position, so an existing field's color can't shift when others are
    // added/removed around it (see seriesColor.ts).
    const startColorIndex = subscribedFieldsRef.current.length
    const updated = [...subscribedFieldsRef.current, ...newFields]
    // subscribe() is full-replacement server-side - always send the complete list.
    await remoteManagementClient.subscribeFields(sessionID, updated.map(f => f.fieldKey))
    subscribedFieldsRef.current = updated
    setSeries(prev => [
      ...prev,
      ...newFields.map((field, i) => ({
        field,
        points: [],
        latestValue: null,
        color: colorForIndex(startColorIndex + i),
        visible: true,
      })),
    ])
  }, [])

  const removeField = useCallback(async (fieldKey: string): Promise<void> => {
    const sessionID = sessionIdRef.current
    if (!sessionID) return
    const updated = subscribedFieldsRef.current.filter(f => f.fieldKey !== fieldKey)
    await remoteManagementClient.subscribeFields(sessionID, updated.map(f => f.fieldKey))
    subscribedFieldsRef.current = updated
    setSeries(prev => prev.filter(entry => entry.field.fieldKey !== fieldKey))
  }, [])

  const setFieldColor = useCallback((fieldKey: string, color: string): void => {
    setSeries(prev => prev.map(entry => (entry.field.fieldKey === fieldKey ? { ...entry, color } : entry)))
  }, [])

  const setFieldVisibility = useCallback((fieldKey: string, visible: boolean): void => {
    setSeries(prev => prev.map(entry => (entry.field.fieldKey === fieldKey ? { ...entry, visible } : entry)))
  }, [])

  const retryConnect = useCallback(() => {
    establishSessionRef.current()
  }, [])

  return {
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
  }
}

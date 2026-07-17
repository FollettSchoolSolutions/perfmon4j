import { useCallback, useEffect, useRef, useState } from 'react'
import * as remoteManagementClient from '../jolokia/remoteManagementClient'
import { classifyConnectionError, ConnectionError, ConnectionStatus } from './connectionStatus'
import { buildThreadTraceFieldKey, ThreadTraceOptions } from './threadTraceKey'
import { addPendingTrace, applyPollResult, removeTrace, ThreadTraceEntry } from './threadTraceQueue'
import { MonitorDescriptor } from './types'

export type { ConnectionStatus, ConnectionErrorKind, ConnectionError } from './connectionStatus'
export type { ThreadTraceEntry, ThreadTraceStatus } from './threadTraceQueue'

export interface UseThreadTracesOptions {
  pollMs?: number
}

export interface UseThreadTracesResult {
  status: ConnectionStatus
  connectionError: ConnectionError | null
  traces: ThreadTraceEntry[]
  scheduleTrace: (monitor: MonitorDescriptor, options?: ThreadTraceOptions) => Promise<void>
  cancelTrace: (fieldKey: string) => Promise<void>
  retryConnect: () => void
}

// Independent of useRemoteManagementChart's own poll cadence by design (see that
// hook's identical constant) - this owns a wholly separate RemoteManagement session,
// so the two features' polling can't drift into or block one another.
const DEFAULT_POLL_MS = 5000

/**
 * Owns its own RemoteManagement session (connect/poll/reconnect) dedicated to
 * thread-trace scheduling and result collection - deliberately not the chart
 * hook's session, so this hook can be used standalone (e.g. from a tree-row
 * action with no chart mounted) without coupling the two features' lifecycles.
 * Mirrors useRemoteManagementChart.ts's session-lifecycle conventions (the
 * per-effect `cancelled` flag for React 18 StrictMode safety, exec-denied/
 * incompatible-version as terminal errors, any other getData() failure as a
 * blind reconnect candidate). See threadTraceQueue.ts for why a completed
 * trace must be captured off the very poll that reports it.
 */
export function useThreadTraces(options?: UseThreadTracesOptions): UseThreadTracesResult {
  const pollMs = options?.pollMs ?? DEFAULT_POLL_MS

  const [status, setStatus] = useState<ConnectionStatus>('connecting')
  const [connectionError, setConnectionError] = useState<ConnectionError | null>(null)
  const [traces, setTraces] = useState<ThreadTraceEntry[]>([])

  const sessionIdRef = useRef<string | null>(null)
  // Tracks fieldKeys still awaiting a result, independent of React state, so a
  // reconnect (which runs outside any render) can re-issue scheduleThreadTrace for
  // them without a stale closure over `traces`. A completed trace's server-side
  // record is a one-shot read (see threadTraceQueue.ts), so there is nothing to
  // resend for those - only entries still pending here matter.
  const pendingFieldKeysRef = useRef<Set<string>>(new Set())
  const intervalHandleRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const establishSessionRef = useRef<() => void>(() => {})

  useEffect(() => {
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
        setTraces(prev => {
          const next = applyPollResult(prev, data)
          for (const entry of next) {
            if (entry.status === 'completed') pendingFieldKeysRef.current.delete(entry.fieldKey)
          }
          return next
        })
      } catch (e) {
        if (cancelled) return
        const classified = classifyConnectionError(e)
        if (classified.kind === 'exec-denied' || classified.kind === 'incompatible-version') {
          stopPolling()
          setStatus('disconnected')
          setConnectionError(classified)
          return
        }
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
          remoteManagementClient.disconnect(sessionID).catch(() => undefined)
          return
        }
        sessionIdRef.current = sessionID
        const stillPending = Array.from(pendingFieldKeysRef.current)
        if (stillPending.length > 0) {
          await Promise.all(stillPending.map(fieldKey => remoteManagementClient.scheduleThreadTrace(sessionID, fieldKey)))
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
        remoteManagementClient.disconnect(sessionID).catch(() => undefined)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pollMs])

  const scheduleTrace = useCallback(async (monitor: MonitorDescriptor, traceOptions?: ThreadTraceOptions): Promise<void> => {
    const sessionID = sessionIdRef.current
    if (!sessionID) return
    const fieldKey = buildThreadTraceFieldKey(monitor.name, traceOptions)
    if (pendingFieldKeysRef.current.has(fieldKey)) return
    await remoteManagementClient.scheduleThreadTrace(sessionID, fieldKey)
    pendingFieldKeysRef.current.add(fieldKey)
    setTraces(prev => addPendingTrace(prev, fieldKey, monitor.label, Date.now()))
  }, [])

  const cancelTrace = useCallback(async (fieldKey: string): Promise<void> => {
    const sessionID = sessionIdRef.current
    if (!sessionID) return
    await remoteManagementClient.unScheduleThreadTrace(sessionID, fieldKey)
    pendingFieldKeysRef.current.delete(fieldKey)
    setTraces(prev => removeTrace(prev, fieldKey))
  }, [])

  const retryConnect = useCallback(() => {
    establishSessionRef.current()
  }, [])

  return {
    status,
    connectionError,
    traces,
    scheduleTrace,
    cancelTrace,
    retryConnect,
  }
}

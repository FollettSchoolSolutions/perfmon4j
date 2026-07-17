import * as remoteManagementClient from '../jolokia/remoteManagementClient'
import { classifyConnectionError, ConnectionError, ConnectionStatus } from './connectionStatus'
import { buildThreadTraceFieldKey, ThreadTraceOptions } from './threadTraceKey'
import { addPendingTrace, applyPollResult, removeTrace, ThreadTraceEntry } from './threadTraceQueue'
import { MonitorDescriptor } from './types'

export interface ThreadTraceSnapshot {
  status: ConnectionStatus
  connectionError: ConnectionError | null
  traces: ThreadTraceEntry[]
}

// Independent of the chart store's own poll cadence by design (see that store's
// identical constant) - this owns a wholly separate RemoteManagement session, so
// the two features' polling can't drift into or block one another.
const POLL_MS = 5000

/**
 * Owns its own RemoteManagement session (connect/poll/reconnect), as a plain
 * non-React object constructed once at module-evaluation time (see the
 * `threadTraceStore` export below) - the same singleton pattern
 * `remoteManagementChartStore.ts` uses, and for the same reason (T15): session
 * state used to live in `ChartPanel`-scoped React state, which gets torn down
 * every time Hawtio navigates to a different nav item. Deliberately still not the
 * chart store's session - this store can be used standalone (e.g. from a tree-row
 * action with no chart mounted) without coupling the two features' lifecycles.
 * `useThreadTraces.ts` is now a thin `useSyncExternalStore` wrapper around this
 * instance. See threadTraceQueue.ts for why a completed trace must be captured
 * off the very poll that reports it.
 */
class ThreadTraceStore {
  private snapshot: ThreadTraceSnapshot = { status: 'connecting', connectionError: null, traces: [] }
  private readonly listeners = new Set<() => void>()

  private sessionId: string | null = null
  // Tracks fieldKeys still awaiting a result, so a reconnect can re-issue
  // scheduleThreadTrace for them. A completed trace's server-side record is a
  // one-shot read (see threadTraceQueue.ts), so there is nothing to resend for
  // those - only entries still pending here matter.
  private readonly pendingFieldKeys = new Set<string>()
  private intervalHandle: ReturnType<typeof setInterval> | null = null

  constructor() {
    void this.establishSession()
  }

  getSnapshot = (): ThreadTraceSnapshot => this.snapshot

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  private publish(partial: Partial<ThreadTraceSnapshot>): void {
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
      const next = applyPollResult(this.snapshot.traces, data)
      for (const entry of next) {
        if (entry.status === 'completed') this.pendingFieldKeys.delete(entry.fieldKey)
      }
      this.publish({ traces: next })
    } catch (e) {
      const classified = classifyConnectionError(e)
      if (classified.kind === 'exec-denied' || classified.kind === 'incompatible-version') {
        this.stopPolling()
        this.publish({ status: 'disconnected', connectionError: classified })
        return
      }
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
      const stillPending = Array.from(this.pendingFieldKeys)
      if (stillPending.length > 0) {
        await Promise.all(stillPending.map(fieldKey => remoteManagementClient.scheduleThreadTrace(sessionId, fieldKey)))
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

  scheduleTrace = async (monitor: MonitorDescriptor, options?: ThreadTraceOptions): Promise<void> => {
    const sessionId = this.sessionId
    if (!sessionId) return
    const fieldKey = buildThreadTraceFieldKey(monitor.name, options)
    if (this.pendingFieldKeys.has(fieldKey)) return
    await remoteManagementClient.scheduleThreadTrace(sessionId, fieldKey)
    this.pendingFieldKeys.add(fieldKey)
    this.publish({ traces: addPendingTrace(this.snapshot.traces, fieldKey, monitor.label, Date.now()) })
  }

  cancelTrace = async (fieldKey: string): Promise<void> => {
    const sessionId = this.sessionId
    if (!sessionId) return
    await remoteManagementClient.unScheduleThreadTrace(sessionId, fieldKey)
    this.pendingFieldKeys.delete(fieldKey)
    this.publish({ traces: removeTrace(this.snapshot.traces, fieldKey) })
  }

  retryConnect = (): void => {
    this.publish({ status: 'connecting' })
    void this.establishSession()
  }
}

export const threadTraceStore = new ThreadTraceStore()

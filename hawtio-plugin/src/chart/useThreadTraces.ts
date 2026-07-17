import { useSyncExternalStore } from 'react'
import { ConnectionError, ConnectionStatus } from './connectionStatus'
import { ThreadTraceOptions } from './threadTraceKey'
import { ThreadTraceEntry } from './threadTraceQueue'
import { threadTraceStore } from './threadTraceStore'
import { MonitorDescriptor } from './types'

export type { ConnectionStatus, ConnectionErrorKind, ConnectionError } from './connectionStatus'
export type { ThreadTraceEntry, ThreadTraceStatus } from './threadTraceQueue'

export interface UseThreadTracesResult {
  status: ConnectionStatus
  connectionError: ConnectionError | null
  traces: ThreadTraceEntry[]
  scheduleTrace: (monitor: MonitorDescriptor, options?: ThreadTraceOptions) => Promise<void>
  cancelTrace: (fieldKey: string) => Promise<void>
  retryConnect: () => void
}

/**
 * A thin `useSyncExternalStore` view onto `threadTraceStore` (T15) - the session
 * and queue live in that module-level singleton, not in this hook, so they
 * survive this component unmounting instead of resetting on every remount. See
 * threadTraceStore.ts for the session-lifecycle and reconnect-resend rationale.
 */
export function useThreadTraces(): UseThreadTracesResult {
  const snapshot = useSyncExternalStore(threadTraceStore.subscribe, threadTraceStore.getSnapshot)

  return {
    status: snapshot.status,
    connectionError: snapshot.connectionError,
    traces: snapshot.traces,
    scheduleTrace: threadTraceStore.scheduleTrace,
    cancelTrace: threadTraceStore.cancelTrace,
    retryConnect: threadTraceStore.retryConnect,
  }
}

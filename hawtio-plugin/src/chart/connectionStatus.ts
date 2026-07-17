// Deliberately imports from remoteManagementErrors.ts directly, not
// jolokia/remoteManagementClient.ts - the latter pulls in jolokiaService
// (transitively, @patternfly/react-core's CSS imports Jest's default transform
// can't parse), which would break this module's own Jest-testability. Shared
// between useRemoteManagementChart and useThreadTraces, which both own an
// independent RemoteManagement session with identical connect/poll/reconnect
// error handling.
import { ExecAccessDeniedError, IncompatibleClientVersionError } from '../jolokia/remoteManagementErrors'

export type ConnectionStatus = 'connecting' | 'connected' | 'reconnecting' | 'disconnected'
export type ConnectionErrorKind = 'exec-denied' | 'incompatible-version' | 'other'

export interface ConnectionError {
  kind: ConnectionErrorKind
  message: string
}

export function classifyConnectionError(e: unknown): ConnectionError {
  if (e instanceof ExecAccessDeniedError) return { kind: 'exec-denied', message: e.message }
  if (e instanceof IncompatibleClientVersionError) return { kind: 'incompatible-version', message: e.message }
  return { kind: 'other', message: e instanceof Error ? e.message : String(e) }
}

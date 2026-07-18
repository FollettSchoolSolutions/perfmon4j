import { jolokiaService } from '@hawtio/react'
import { classifyRemoteManagementError } from './remoteManagementErrors'

export { ExecAccessDeniedError, IncompatibleClientVersionError, RemoteManagementError } from './remoteManagementErrors'

// Must match RemoteManagement.OBJECT_NAME in base's
// org.perfmon4j.remotemanagement.jmx.RemoteManagement (Java) - do not rename
// casually, this string is a cross-language contract between the two.
export const REMOTE_MANAGEMENT_OBJECT_NAME = 'org.perfmon4j:type=RemoteManagement'

// Must major-version-match org.perfmon4j.remotemanagement.intf.ManagementVersion.VERSION
// server-side (format "%d.%03d" of MAJOR_VERSION/MINOR_VERSION), or connect() throws
// IncompatibleClientVersionException.
export const CLIENT_VERSION = '1.001'

async function execute(operation: string, signature: string, args: unknown[]): Promise<unknown> {
  try {
    return await jolokiaService.execute(REMOTE_MANAGEMENT_OBJECT_NAME, `${operation}(${signature})`, args)
  } catch (e) {
    throw classifyRemoteManagementError(e, operation)
  }
}

export async function connect(): Promise<string> {
  return (await execute('connect', 'java.lang.String', [CLIENT_VERSION])) as string
}

export async function disconnect(sessionID: string): Promise<void> {
  await execute('disconnect', 'java.lang.String', [sessionID])
}

export async function getMonitors(sessionID: string): Promise<string[]> {
  return (await execute('getMonitors', 'java.lang.String', [sessionID])) as string[]
}

export async function getFieldsForMonitor(sessionID: string, monitorKey: string): Promise<string[]> {
  return (await execute('getFieldsForMonitor', 'java.lang.String,java.lang.String', [sessionID, monitorKey])) as string[]
}

/**
 * subscribe() is FULL-REPLACEMENT server-side, not additive - RemoteManagement.subscribe()
 * diffs the passed array against the currently-subscribed set and unsubscribes anything
 * not present in it. Callers must always pass the complete current field-key list, never
 * just the delta, or earlier subscriptions will be silently dropped. Passing [] unsubscribes
 * everything.
 */
export async function subscribeFields(sessionID: string, fieldKeys: string[]): Promise<void> {
  await execute('subscribe', 'java.lang.String,[Ljava.lang.String;', [sessionID, fieldKeys])
}

export async function getData(sessionID: string): Promise<Record<string, unknown>> {
  return (await execute('getData', 'java.lang.String', [sessionID])) as Record<string, unknown>
}

/**
 * fieldKey must be a THREADTRACE-type field key built by threadTraceKey.ts's
 * buildThreadTraceFieldKey() - the server validates the type and throws
 * InvalidMonitorTypeException otherwise. The result of a scheduled trace is not
 * returned here - it arrives later through getData(), merged into the same result
 * map as regular subscribed fields (see RemoteManagement.getData()).
 */
export async function scheduleThreadTrace(sessionID: string, fieldKey: string): Promise<void> {
  await execute('scheduleThreadTrace', 'java.lang.String,java.lang.String', [sessionID, fieldKey])
}

export async function unScheduleThreadTrace(sessionID: string, fieldKey: string): Promise<void> {
  await execute('unScheduleThreadTrace', 'java.lang.String,java.lang.String', [sessionID, fieldKey])
}

/**
 * Only has an observable effect on an INTERVAL-type monitorKey - a no-op server-side
 * for SNAPSHOT/THREADTRACE keys (see ExternalAppender.forceDynamicChildCreation).
 * There is no corresponding "is this forced?" query op, so callers must track the
 * forced/not-forced state themselves (see remoteManagementChartStore.ts).
 */
export async function forceDynamicChildCreation(sessionID: string, monitorKey: string): Promise<void> {
  await execute('forceDynamicChildCreation', 'java.lang.String,java.lang.String', [sessionID, monitorKey])
}

export async function unForceDynamicChildCreation(sessionID: string, monitorKey: string): Promise<void> {
  await execute('unForceDynamicChildCreation', 'java.lang.String,java.lang.String', [sessionID, monitorKey])
}

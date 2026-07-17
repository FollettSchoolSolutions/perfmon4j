// Mirrors org.perfmon4j.remotemanagement.intf.FieldKey.buildThreadTraceKeyFromInterval
// (base/src/main/java/org/perfmon4j/remotemanagement/intf/FieldKey.java) - a thread
// trace is scheduled by building a THREADTRACE-type field key from an INTERVAL
// monitor's *name only* (any instance is dropped), with the MonitorKey's normally-
// unused "instance" slot repurposed to carry min-duration/max-depth args as a
// "Key=Value,Key=Value" string that the server re-tokenizes with
// MiscHelper.tokenizeCSVString and applies via bean-property reflection onto
// ExternalThreadTraceConfig (setMinDurationToCapture/setMaxDepth).

// Must match FieldKey.THREAD_TRACE_MIN_DURATION_ARG / THREAD_TRACE_MAX_DEPTH_ARG.
export const THREAD_TRACE_MIN_DURATION_ARG = 'MinDurationToCapture'
export const THREAD_TRACE_MAX_DEPTH_ARG = 'MaxDepth'

// Must match FieldKey.THREAD_TRACE_PENDING - the sentinel value getData() returns
// for a scheduled-but-not-yet-captured trace, in the same result map as regular
// subscribed fields (see RemoteManagement.getData()'s copyToResultMap of
// ExternalAppender.getThreadTraceData()).
export const THREAD_TRACE_PENDING = 'ThreadTracePending'

export interface ThreadTraceOptions {
  minDurationToCaptureMillis?: number
  maxDepth?: number
}

/**
 * Builds a THREADTRACE field key string from an INTERVAL monitor's name, for use
 * with remoteManagementClient's scheduleThreadTrace/unScheduleThreadTrace. The
 * returned string is also the key getData() uses in its result map once the trace
 * completes (or the "ThreadTracePending" sentinel while it doesn't) - the server
 * reconstructs the identical string from the same parsed components on its side
 * (FieldKey.toString()), so this must byte-for-byte match MonitorKey/FieldKey's own
 * toString() format.
 */
export function buildThreadTraceFieldKey(monitorName: string, options?: ThreadTraceOptions): string {
  const params: string[] = []
  if (options?.minDurationToCaptureMillis !== undefined) {
    params.push(`${THREAD_TRACE_MIN_DURATION_ARG}=${options.minDurationToCaptureMillis}`)
  }
  if (options?.maxDepth !== undefined) {
    params.push(`${THREAD_TRACE_MAX_DEPTH_ARG}=${options.maxDepth}`)
  }
  const instance = params.length > 0 ? `;instance=${params.join(',')}` : ''
  return `THREADTRACE(name=${monitorName}${instance}):FIELD(name=stack;type=STRING)`
}

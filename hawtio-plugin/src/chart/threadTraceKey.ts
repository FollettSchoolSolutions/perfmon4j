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
// Must match FieldKey.THREAD_TRACE_TRIGGER_ARG.
export const THREAD_TRACE_TRIGGER_ARG = 'Trigger'

// Must match FieldKey.THREAD_TRACE_PENDING - the sentinel value getData() returns
// for a scheduled-but-not-yet-captured trace, in the same result map as regular
// subscribed fields (see RemoteManagement.getData()'s copyToResultMap of
// ExternalAppender.getThreadTraceData()).
export const THREAD_TRACE_PENDING = 'ThreadTracePending'

// Must match the three ThreadTraceConfig.TriggerType prefixes exposed for on-demand
// scheduling (HTTPCookieTrigger/HTTPRequestTrigger/HTTPSessionTrigger) -
// ThreadNameTrigger/ThreadPropertyTrigger stay XML-config-only, see the plan doc.
export type ThreadTraceTriggerType = 'HTTP' | 'HTTP_SESSION' | 'HTTP_COOKIE'

export interface ThreadTraceTrigger {
  type: ThreadTraceTriggerType
  name: string
  value: string
}

export interface ThreadTraceOptions {
  minDurationToCaptureMillis?: number
  maxDepth?: number
  trigger?: ThreadTraceTrigger
}

/**
 * Base64 URL-safe, no padding - matches FieldKey.encodeTriggerArg
 * (java.util.Base64.getUrlEncoder().withoutPadding()) byte-for-byte, so the server's
 * java.util.Base64.getUrlDecoder() round-trips it. Necessary (not just cosmetic)
 * because the raw "PREFIX:name=value" payload is carried through a naive comma-
 * separated/'='-split tokenizer (MiscHelper.tokenizeCSVString + String.split("=")) that
 * would corrupt or silently drop a name/value containing ',' or '=' - browsers have no
 * built-in URL-safe base64 encoder (btoa produces '+'/'/'/'=' padding), hence the manual
 * translation below. Uses the encodeURIComponent/unescape trick (rather than
 * TextEncoder, which jsdom's test environment doesn't provide) to get a UTF-8 byte
 * string btoa can safely consume - a standard cross-environment technique, works
 * identically in real browsers.
 */
function base64UrlEncode(input: string): string {
  const utf8Binary = unescape(encodeURIComponent(input))
  return btoa(utf8Binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

/** Mirrors FieldKey.encodeTriggerArg(String, String, String). */
export function encodeTriggerArg(triggerTypePrefix: ThreadTraceTriggerType, name: string, value: string): string {
  return base64UrlEncode(`${triggerTypePrefix}:${name}=${value}`)
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
  if (options?.trigger !== undefined) {
    params.push(`${THREAD_TRACE_TRIGGER_ARG}=${encodeTriggerArg(options.trigger.type, options.trigger.name, options.trigger.value)}`)
  }
  const instance = params.length > 0 ? `;instance=${params.join(',')}` : ''
  return `THREADTRACE(name=${monitorName}${instance}):FIELD(name=stack;type=STRING)`
}

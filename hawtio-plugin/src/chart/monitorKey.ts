import { FieldDescriptor, FieldType, MonitorDescriptor, MonitorType, NumericFieldType } from './types'

// Mirrors org.perfmon4j.remotemanagement.intf.MonitorKey's own toString()/parse
// regexes (base/src/main/java/org/perfmon4j/remotemanagement/intf/MonitorKey.java) -
// try the with-instance form first, then fall back to the no-instance form.
const MONITOR_KEY_WITH_INSTANCE = /^([^(]+)\(name=([^,]+);instance=([^)]+)\)$/
const MONITOR_KEY_NO_INSTANCE = /^([^(]+)\(name=([^)]+)\)$/

export function parseMonitorKey(monitorKey: string): { type: MonitorType; name: string; instance?: string } {
  const withInstance = monitorKey.match(MONITOR_KEY_WITH_INSTANCE)
  if (withInstance) {
    return { type: withInstance[1] as MonitorType, name: withInstance[2], instance: withInstance[3] }
  }
  const noInstance = monitorKey.match(MONITOR_KEY_NO_INSTANCE)
  if (noInstance) {
    return { type: noInstance[1] as MonitorType, name: noInstance[2] }
  }
  throw new Error(`Unable to parse monitor key: ${monitorKey}`)
}

// getFieldsForMonitor(sessionID, monitorKey) always returns field keys of the exact
// form `${monitorKey}:FIELD(name=X;type=Y)` (org.perfmon4j.remotemanagement.intf.FieldKey's
// toString()) - since the caller already knows monitorKey verbatim (it's what was
// passed in), strip that known prefix rather than independently regexing the whole
// field key. This sidesteps any ambiguity from monitor/instance names that might
// themselves contain ':' or ';'.
const FIELD_KEY_SUFFIX = /^:FIELD\(name=([^;]+);type=([^)]+)\)$/

export function parseFieldSuffix(monitorKey: string, fieldKey: string): { fieldName: string; fieldType: FieldType } {
  if (!fieldKey.startsWith(monitorKey)) {
    throw new Error(`Field key "${fieldKey}" does not start with expected monitor key "${monitorKey}"`)
  }
  const suffix = fieldKey.substring(monitorKey.length)
  const match = suffix.match(FIELD_KEY_SUFFIX)
  if (!match) {
    throw new Error(`Unable to parse field key: ${fieldKey}`)
  }
  return { fieldName: match[1], fieldType: match[2] as FieldType }
}

export function isNumericFieldType(fieldType: FieldType): fieldType is NumericFieldType {
  return fieldType === 'INTEGER' || fieldType === 'LONG' || fieldType === 'DOUBLE'
}

export function formatMonitorLabel(parsed: { name: string; instance?: string }): string {
  return parsed.instance ? `${parsed.name} (${parsed.instance})` : parsed.name
}

export function formatFieldLabel(monitorLabel: string, fieldName: string): string {
  return `${monitorLabel} — ${fieldName}`
}

/**
 * Builds display metadata for a raw monitorKey string. The raw string itself remains
 * the sole identity used for every RemoteManagement call - parsed parts here are for
 * display only, never reconstructed back into a key.
 */
export function toMonitorDescriptor(monitorKey: string): MonitorDescriptor {
  const parsed = parseMonitorKey(monitorKey)
  return {
    monitorKey,
    type: parsed.type,
    name: parsed.name,
    instance: parsed.instance,
    label: formatMonitorLabel(parsed),
  }
}

export function toFieldDescriptor(monitor: MonitorDescriptor, fieldKey: string): FieldDescriptor {
  const { fieldName, fieldType } = parseFieldSuffix(monitor.monitorKey, fieldKey)
  return {
    fieldKey,
    monitorKey: monitor.monitorKey,
    fieldName,
    fieldType,
    label: formatFieldLabel(monitor.label, fieldName),
  }
}

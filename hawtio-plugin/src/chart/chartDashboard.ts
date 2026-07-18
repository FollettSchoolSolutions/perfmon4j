import { FieldSeries } from './types'

/** Bumped only on a breaking change to the saved shape - see parseDashboardFile. */
export const DASHBOARD_FILE_VERSION = 1

export interface DashboardFieldEntry {
  fieldKey: string
  monitorKey: string
  fieldName: string
  fieldType: string
  label: string
  color: string
  visible: boolean
}

export interface DashboardFile {
  version: number
  savedAt: string
  fields: DashboardFieldEntry[]
}

/**
 * key/color/visible per T14 - "scale" (per-series y-axis normalization) isn't
 * tracked anywhere yet (see ROADMAP.md's "Y-axis normalization" backlog item,
 * still open), so there's nothing of that shape to persist until it exists.
 */
export function serializeDashboard(series: FieldSeries[]): DashboardFile {
  return {
    version: DASHBOARD_FILE_VERSION,
    savedAt: new Date().toISOString(),
    fields: series.map(({ field, color, visible }) => ({
      fieldKey: field.fieldKey,
      monitorKey: field.monitorKey,
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      label: field.label,
      color,
      visible,
    })),
  }
}

function isDashboardFieldEntry(value: unknown): value is DashboardFieldEntry {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return (
    typeof v.fieldKey === 'string' &&
    typeof v.monitorKey === 'string' &&
    typeof v.fieldName === 'string' &&
    typeof v.fieldType === 'string' &&
    typeof v.label === 'string' &&
    typeof v.color === 'string' &&
    typeof v.visible === 'boolean'
  )
}

/**
 * Throws with a user-presentable message on anything not shaped like a file this
 * plugin saved - deliberately permissive on `version` (no fields have ever changed
 * shape yet) rather than rejecting a file just because a future version bumped it.
 */
export function parseDashboardFile(json: string): DashboardFile {
  let parsed: unknown
  try {
    parsed = JSON.parse(json)
  } catch {
    throw new Error('That file is not valid JSON.')
  }
  if (typeof parsed !== 'object' || parsed === null || !Array.isArray((parsed as { fields?: unknown }).fields)) {
    throw new Error('That file is not a perfmon4j chart dashboard file.')
  }
  const fields = (parsed as { fields: unknown[] }).fields
  if (!fields.every(isDashboardFieldEntry)) {
    throw new Error('That file is not a perfmon4j chart dashboard file.')
  }
  const version = (parsed as { version?: unknown }).version
  return { version: typeof version === 'number' ? version : DASHBOARD_FILE_VERSION, savedAt: String((parsed as { savedAt?: unknown }).savedAt ?? ''), fields }
}

/**
 * Splits a loaded file's fields against the field keys actually available on the
 * currently-connected JVM (the caller builds `availableFieldKeys` by listing
 * monitors/fields live - see ChartDashboardControls) - a field can be absent
 * because its JVM instance restarted, its monitor was removed from
 * perfmonconfig.xml, or the file was saved against a different application
 * entirely (legacy #8's "don't error on a foreign JVM").
 */
export function partitionAvailableFields(
  fields: DashboardFieldEntry[],
  availableFieldKeys: ReadonlySet<string>,
): { available: DashboardFieldEntry[]; missing: DashboardFieldEntry[] } {
  const available: DashboardFieldEntry[] = []
  const missing: DashboardFieldEntry[] = []
  for (const field of fields) {
    ;(availableFieldKeys.has(field.fieldKey) ? available : missing).push(field)
  }
  return { available, missing }
}

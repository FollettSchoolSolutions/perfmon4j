import { DEFAULT_SCALE } from './seriesScale'
import { FieldSeries } from './types'

/** Bumped only on a breaking change to the saved shape - see parseDashboardFile.
 * Bumped 1 -> 2 when `scale` (T16) was added to DashboardFieldEntry; a v1 file
 * (saved before `scale` existed) is still accepted, just defaulted to
 * DEFAULT_SCALE per field rather than rejected - see isDashboardFieldEntryBaseShape. */
export const DASHBOARD_FILE_VERSION = 2

export interface DashboardFieldEntry {
  fieldKey: string
  monitorKey: string
  fieldName: string
  fieldType: string
  label: string
  color: string
  visible: boolean
  /** Y-axis scale factor (T16, see seriesScale.ts) - defaults to DEFAULT_SCALE
   * when loading a file saved before this field existed (version 1). */
  scale: number
}

export interface DashboardFile {
  version: number
  savedAt: string
  fields: DashboardFieldEntry[]
}

export function serializeDashboard(series: FieldSeries[]): DashboardFile {
  return {
    version: DASHBOARD_FILE_VERSION,
    savedAt: new Date().toISOString(),
    fields: series.map(({ field, color, visible, scale }) => ({
      fieldKey: field.fieldKey,
      monitorKey: field.monitorKey,
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      label: field.label,
      color,
      visible,
      scale,
    })),
  }
}

/** Validates every field except `scale`, which a version-1 file won't have -
 * parseDashboardFile defaults it separately rather than requiring it here. */
function isDashboardFieldEntryBaseShape(value: unknown): value is Omit<DashboardFieldEntry, 'scale'> {
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
 * plugin saved - deliberately permissive on `version` (only one field has ever been
 * added, and it's defaulted rather than required) rather than rejecting a file just
 * because a future version bumped it.
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
  const rawFields = (parsed as { fields: unknown[] }).fields
  if (!rawFields.every(isDashboardFieldEntryBaseShape)) {
    throw new Error('That file is not a perfmon4j chart dashboard file.')
  }
  const fields: DashboardFieldEntry[] = rawFields.map(f => {
    const rawScale = (f as unknown as Record<string, unknown>).scale
    return { ...f, scale: typeof rawScale === 'number' ? rawScale : DEFAULT_SCALE }
  })
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

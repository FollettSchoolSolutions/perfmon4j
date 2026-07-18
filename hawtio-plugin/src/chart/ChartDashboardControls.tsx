import { Alert, Button, Flex, FlexItem } from '@patternfly/react-core'
import React, { useRef, useState } from 'react'
import { DashboardFieldEntry, partitionAvailableFields, parseDashboardFile, serializeDashboard } from './chartDashboard'
import { FieldDescriptor, FieldSeries, FieldType, MonitorDescriptor } from './types'

export interface ChartDashboardControlsProps {
  /** Every subscribed field (chartable + text-only) - the full charted-field set to save. */
  series: FieldSeries[]
  /** Gates Load only (needs a live session to look up monitors/fields) - Save works
   * from in-memory series regardless of connection state. */
  enabled: boolean
  listMonitors: () => Promise<MonitorDescriptor[]>
  listFieldsForMonitor: (monitorKey: string) => Promise<FieldDescriptor[]>
  addFields: (fields: FieldDescriptor[]) => Promise<void>
  setFieldColor: (fieldKey: string, color: string) => void
  setFieldVisibility: (fieldKey: string, visible: boolean) => void
  setFieldScale: (fieldKey: string, scale: number) => void
}

function toFieldDescriptor(entry: DashboardFieldEntry): FieldDescriptor {
  return {
    fieldKey: entry.fieldKey,
    monitorKey: entry.monitorKey,
    fieldName: entry.fieldName,
    fieldType: entry.fieldType as FieldType,
    label: entry.label,
  }
}

/**
 * Save/Load toolbar for the Monitoring tab's charted-field set (T14, legacy #8) -
 * downloads/reloads a JSON file of {fieldKey, monitorKey, ..., color, visible} per
 * subscribed field. Load re-subscribes each field still present on the currently
 * connected JVM via `addFields`, then reapplies its saved color/visibility; a field
 * whose fieldKey isn't among what `listMonitors`/`listFieldsForMonitor` currently
 * report (a different JVM, a removed monitor, a restarted instance) is skipped with
 * a warning rather than erroring the whole load - see chartDashboard.ts.
 */
export const ChartDashboardControls: React.FunctionComponent<ChartDashboardControlsProps> = ({
  series,
  enabled,
  listMonitors,
  listFieldsForMonitor,
  addFields,
  setFieldColor,
  setFieldVisibility,
  setFieldScale,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [missingFields, setMissingFields] = useState<DashboardFieldEntry[] | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const handleSave = () => {
    const file = serializeDashboard(series)
    const blob = new Blob([JSON.stringify(file, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `perfmon4j-chart-dashboard-${file.savedAt.replace(/[:.]/g, '-')}.json`
    link.click()
    URL.revokeObjectURL(url)
  }

  const handleFileChosen = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const chosenFile = event.target.files?.[0] ?? null
    // Reset so choosing the same filename again still fires onChange.
    event.target.value = ''
    if (!chosenFile) return

    setLoadError(null)
    setMissingFields(null)
    setIsLoading(true)
    try {
      const text = await chosenFile.text()
      const dashboard = parseDashboardFile(text)

      const monitors = await listMonitors()
      const availableMonitorKeys = new Set(monitors.map(m => m.monitorKey))
      const referencedMonitorKeys = Array.from(new Set(dashboard.fields.map(f => f.monitorKey))).filter(k =>
        availableMonitorKeys.has(k),
      )
      const availableFieldKeys = new Set<string>()
      for (const monitorKey of referencedMonitorKeys) {
        const fields = await listFieldsForMonitor(monitorKey)
        fields.forEach(f => availableFieldKeys.add(f.fieldKey))
      }

      const { available, missing } = partitionAvailableFields(dashboard.fields, availableFieldKeys)
      if (available.length > 0) {
        await addFields(available.map(toFieldDescriptor))
        available.forEach(entry => {
          setFieldColor(entry.fieldKey, entry.color)
          setFieldVisibility(entry.fieldKey, entry.visible)
          setFieldScale(entry.fieldKey, entry.scale)
        })
      }
      if (missing.length > 0) {
        setMissingFields(missing)
      }
    } catch (e) {
      setLoadError(e instanceof Error ? e.message : String(e))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <>
      <Flex justifyContent={{ default: 'justifyContentFlexEnd' }}>
        <FlexItem>
          <Button variant='secondary' onClick={handleSave} isDisabled={series.length === 0}>
            Save dashboard…
          </Button>
        </FlexItem>
        <FlexItem>
          <Button
            variant='secondary'
            onClick={() => fileInputRef.current?.click()}
            isLoading={isLoading}
            isDisabled={isLoading || !enabled}
          >
            Load dashboard…
          </Button>
          <input
            ref={fileInputRef}
            type='file'
            accept='application/json'
            hidden
            aria-label='Load chart dashboard file'
            onChange={event => void handleFileChosen(event)}
          />
        </FlexItem>
      </Flex>

      {loadError && (
        <Alert
          variant='danger'
          isInline
          title={`Unable to load dashboard: ${loadError}`}
          actionLinks={
            <Button variant='link' isInline onClick={() => setLoadError(null)}>
              Dismiss
            </Button>
          }
        />
      )}

      {missingFields && missingFields.length > 0 && (
        <Alert
          variant='warning'
          isInline
          title={`${missingFields.length} field(s) from the loaded dashboard weren't found on this JVM and were skipped.`}
          actionLinks={
            <Button variant='link' isInline onClick={() => setMissingFields(null)}>
              Dismiss
            </Button>
          }
        >
          <ul>
            {missingFields.map(f => (
              <li key={f.fieldKey}>{f.label}</li>
            ))}
          </ul>
        </Alert>
      )}
    </>
  )
}

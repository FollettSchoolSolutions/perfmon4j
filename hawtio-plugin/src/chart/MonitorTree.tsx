import { PluginTreeViewToolbar } from '@hawtio/react'
import {
  Alert,
  Bullseye,
  Button,
  Dropdown,
  DropdownItem,
  DropdownList,
  EmptyState,
  EmptyStateBody,
  MenuToggle,
  Spinner,
  TreeView,
  TreeViewDataItem,
} from '@patternfly/react-core'
import { EllipsisVIcon, SyncAltIcon } from '@patternfly/react-icons'
import React, { ChangeEvent, useEffect, useState } from 'react'
import { AddFieldModal } from './AddFieldModal'
import { buildMonitorTree, filterMonitorTree, MonitorTreeNode } from './monitorTreeLogic'
import { FieldDescriptor, MonitorDescriptor } from './types'

export interface MonitorTreeProps {
  enabled: boolean
  listMonitors: () => Promise<MonitorDescriptor[]>
  listFieldsForMonitor: (monitorKey: string) => Promise<FieldDescriptor[]>
  addFields: (fields: FieldDescriptor[]) => Promise<void>
}

interface MonitorRowActionProps {
  monitor: MonitorDescriptor
  onAddField: (monitor: MonitorDescriptor) => void
}

/** Per-row kebab menu. Only "Add field to chart" today - the Schedule Thread
 * Trace (T9) and Force dynamic monitor creation (T13) items join this same
 * menu once their own MBean plumbing lands. */
const MonitorRowAction: React.FunctionComponent<MonitorRowActionProps> = ({ monitor, onAddField }) => {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <Dropdown
      isOpen={isOpen}
      onOpenChange={setIsOpen}
      popperProps={{ position: 'right' }}
      toggle={toggleRef => (
        <MenuToggle
          ref={toggleRef}
          variant='plain'
          aria-label={`Actions for ${monitor.label}`}
          onClick={e => {
            e.stopPropagation() // don't also trigger the tree row's own expand/select
            setIsOpen(open => !open)
          }}
        >
          <EllipsisVIcon />
        </MenuToggle>
      )}
    >
      <DropdownList>
        <DropdownItem
          onClick={e => {
            e.stopPropagation()
            setIsOpen(false)
            onAddField(monitor)
          }}
        >
          Add field to chart…
        </DropdownItem>
      </DropdownList>
    </Dropdown>
  )
}

function toTreeViewItem(node: MonitorTreeNode, depth: number, onAddField: (monitor: MonitorDescriptor) => void): TreeViewDataItem {
  return {
    id: node.id,
    name: node.label,
    defaultExpanded: depth === 0,
    children: node.children.length > 0 ? node.children.map(c => toTreeViewItem(c, depth + 1, onAddField)) : undefined,
    action: node.monitor ? <MonitorRowAction monitor={node.monitor} onAddField={onAddField} /> : undefined,
  }
}

/**
 * Persistent left-pane monitor browser (T2) - a searchable INTERVAL/SNAPSHOT
 * dot-notation tree (see monitorTree.ts) with a manual Refresh, replacing
 * the pre-T2 MonitorFieldPicker's combined browse+select panel. Selecting a
 * monitor no longer implies charting it: "Add field to chart" is now a
 * per-row kebab action (T4) opening AddFieldModal.
 */
export const MonitorTree: React.FunctionComponent<MonitorTreeProps> = ({
  enabled,
  listMonitors,
  listFieldsForMonitor,
  addFields,
}) => {
  const [monitors, setMonitors] = useState<MonitorDescriptor[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [searchText, setSearchText] = useState('')
  // undefined (not false) so TreeView honors each item's own `defaultExpanded`
  // (root type groups start expanded) until the user explicitly toggles the
  // toolbar's Expand/Collapse all - passing a literal false there overrides
  // per-item defaultExpanded and forces everything collapsed.
  const [allExpanded, setAllExpanded] = useState<boolean | undefined>(undefined)
  const [addFieldMonitor, setAddFieldMonitor] = useState<MonitorDescriptor | null>(null)
  const [refreshNonce, setRefreshNonce] = useState(0)

  useEffect(() => {
    if (!enabled) return
    let cancelled = false
    setIsRefreshing(true)
    listMonitors()
      .then(loaded => {
        if (cancelled) return
        setMonitors(loaded)
        setLoadError(null)
      })
      .catch(e => {
        if (!cancelled) setLoadError(e instanceof Error ? e.message : String(e))
      })
      .finally(() => {
        if (!cancelled) setIsRefreshing(false)
      })
    return () => {
      cancelled = true
    }
    // refreshNonce is a manual re-fetch trigger, not itself read in the effect.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, listMonitors, refreshNonce])

  if (!enabled) {
    return (
      <EmptyState titleText='Not connected' headingLevel='h4'>
        <EmptyStateBody>Monitor browsing is unavailable until the RemoteManagement session connects.</EmptyStateBody>
      </EmptyState>
    )
  }

  if (loadError) {
    return (
      <Alert
        variant='warning'
        isInline
        title={`Unable to load monitors: ${loadError}`}
        actionLinks={
          <Button variant='link' isInline onClick={() => setRefreshNonce(n => n + 1)}>
            Retry
          </Button>
        }
      />
    )
  }

  if (!monitors) {
    return (
      <Bullseye>
        <Spinner size='lg' aria-label='Loading monitors' />
      </Bullseye>
    )
  }

  const tree = buildMonitorTree(monitors)
  const filteredTree = filterMonitorTree(tree, searchText)
  const treeViewData = filteredTree.map(n => toTreeViewItem(n, 0, setAddFieldMonitor))

  const onSearch = (e: ChangeEvent<HTMLInputElement>) => setSearchText(e.target.value)

  return (
    <>
      <Button
        variant='secondary'
        icon={<SyncAltIcon />}
        isLoading={isRefreshing}
        isDisabled={isRefreshing}
        onClick={() => setRefreshNonce(n => n + 1)}
      >
        Refresh
      </Button>

      {monitors.length === 0 ? (
        <EmptyState titleText='No monitors registered yet' headingLevel='h4'>
          <EmptyStateBody>Monitors appear here as instrumented code paths run. Try Refresh after some activity.</EmptyStateBody>
        </EmptyState>
      ) : (
        <TreeView
          aria-label='Monitors'
          data={treeViewData}
          hasGuides
          allExpanded={allExpanded}
          toolbar={<PluginTreeViewToolbar onSearch={onSearch} onSetExpanded={setAllExpanded} />}
        />
      )}

      <AddFieldModal
        monitor={addFieldMonitor}
        onClose={() => setAddFieldMonitor(null)}
        listFieldsForMonitor={listFieldsForMonitor}
        addFields={addFields}
      />
    </>
  )
}

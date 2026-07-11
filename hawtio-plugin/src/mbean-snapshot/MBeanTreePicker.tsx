import { MBeanNode, MBeanTree, PluginTreeViewToolbar, workspace } from '@hawtio/react'
import { Alert, Bullseye, Spinner, TreeView, TreeViewDataItem } from '@patternfly/react-core'
import React, { ChangeEvent, useEffect, useState } from 'react'
import { isMBeanLeaf } from './mbeanTreeHelpers'

export interface MBeanTreePickerProps {
  selectedObjectName: string
  onSelectMBean: (objectName: string, node: MBeanNode) => void
}

/**
 * Searchable/filterable MBean tree, built from @hawtio/react's already-loaded
 * `workspace` tree data - the built-in JMX plugin's own tree view has no plugin
 * extension point (see CLAUDE.md), so this is a self-built one embedded in our
 * own page rather than a tab injected into that view.
 */
export const MBeanTreePicker: React.FunctionComponent<MBeanTreePickerProps> = ({
  selectedObjectName,
  onSelectMBean,
}) => {
  const [mbeanTree, setMbeanTree] = useState<MBeanTree | null>(null)
  const [filteredTree, setFilteredTree] = useState<MBeanNode[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [allExpanded, setAllExpanded] = useState(false)
  const [nonLeafHint, setNonLeafHint] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const loaded = await workspace.getTree()
        if (cancelled) return
        setMbeanTree(loaded)
        setFilteredTree(loaded.getTree())
      } catch (e) {
        if (!cancelled) {
          setLoadError(e instanceof Error ? e.message : String(e))
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const onSearch = (e: ChangeEvent<HTMLInputElement>) => {
    if (!mbeanTree) return
    const roots = mbeanTree.getTree()
    const input = e.target.value
    const matches = MBeanTree.filter(roots, node => node.name.toLowerCase().includes(input.toLowerCase()))
    setFilteredTree(input && matches.length ? matches : roots)
  }

  const onSelect = (_: React.MouseEvent, item: TreeViewDataItem) => {
    const node = item as MBeanNode
    if (isMBeanLeaf(node) && node.objectName) {
      setNonLeafHint(null)
      onSelectMBean(node.objectName, node)
    } else {
      setNonLeafHint(node.name)
    }
  }

  if (loadError) {
    return <Alert variant='warning' title={`Unable to load the MBean tree: ${loadError}`} />
  }

  if (!filteredTree || !mbeanTree) {
    return (
      <Bullseye>
        <Spinner size='lg' aria-label='Loading MBean tree' />
      </Bullseye>
    )
  }

  const selectedNode = selectedObjectName ? mbeanTree.flatten()[selectedObjectName] : undefined

  return (
    <>
      <TreeView
        data={filteredTree}
        hasGuides
        hasSelectableNodes
        activeItems={selectedNode ? [selectedNode] : undefined}
        onSelect={onSelect}
        toolbar={<PluginTreeViewToolbar onSearch={onSearch} onSetExpanded={setAllExpanded} />}
        allExpanded={allExpanded}
      />
      {nonLeafHint && (
        <Alert variant='info' isInline title={`'${nonLeafHint}' is a folder - expand it and pick an MBean underneath.`} />
      )}
    </>
  )
}

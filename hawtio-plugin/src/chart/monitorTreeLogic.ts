import { MonitorDescriptor, MonitorType } from './types'

export interface MonitorTreeNode {
  id: string
  label: string
  children: MonitorTreeNode[]
  /** Set only on a node that represents one selectable monitor (a true leaf). */
  monitor?: MonitorDescriptor
}

const TYPE_ORDER: MonitorType[] = ['INTERVAL', 'SNAPSHOT']

/**
 * Groups monitors into a two-level tree: monitor type at the root, then a
 * dot-notation hierarchy of their `name` (e.g. "com.example.Foo.bar" nests
 * under "com.example.Foo"), mirroring the legacy VisualVM plugin's monitor
 * tree (see LEGACY_VISUALVM_FEATURES.md #3). Pure and dependency-free so it
 * stays unit-testable without a live session - see monitorKey.ts for the
 * same convention.
 */
export function buildMonitorTree(monitors: MonitorDescriptor[]): MonitorTreeNode[] {
  const roots: MonitorTreeNode[] = []
  for (const type of TYPE_ORDER) {
    const group = monitors.filter(m => m.type === type)
    if (group.length === 0) continue
    roots.push({ id: type, label: type, children: buildNameLevel(group, type) })
  }
  return roots
}

interface TrieNode {
  children: Map<string, TrieNode>
  /** Monitors whose full dotted `name` terminates exactly at this path. */
  descriptors?: MonitorDescriptor[]
}

function buildNameLevel(monitors: MonitorDescriptor[], idPrefix: string): MonitorTreeNode[] {
  // Group by full dotted name first - several monitors can share one name and
  // differ only by `instance` (e.g. several SNAPSHOT instances of one MBean).
  const byName = new Map<string, MonitorDescriptor[]>()
  for (const m of monitors) {
    const list = byName.get(m.name) ?? []
    list.push(m)
    byName.set(m.name, list)
  }

  const root: TrieNode = { children: new Map() }
  for (const [name, descriptors] of byName) {
    let node = root
    for (const segment of name.split('.')) {
      let next = node.children.get(segment)
      if (!next) {
        next = { children: new Map() }
        node.children.set(segment, next)
      }
      node = next
    }
    node.descriptors = descriptors
  }

  return toTreeNodes(root, idPrefix)
}

function toTreeNodes(trie: TrieNode, idPath: string): MonitorTreeNode[] {
  const segments = [...trie.children.entries()].sort(([a], [b]) => a.localeCompare(b))
  const nodes: MonitorTreeNode[] = []

  for (const [segment, child] of segments) {
    const id = `${idPath}/${segment}`
    const deeperChildren = toTreeNodes(child, id)

    if (!child.descriptors) {
      // Pure grouping segment - some monitor's name extends past this point.
      nodes.push({ id, label: segment, children: deeperChildren })
      continue
    }

    if (child.descriptors.length === 1) {
      // The sole monitor at this exact path is directly selectable here, even
      // if other monitors also nest deeper under this same path - both can be
      // true at once (e.g. "com.example.Foo" and "com.example.Foo.Bar").
      nodes.push({ id, label: segment, children: deeperChildren, monitor: child.descriptors[0] })
      continue
    }

    // Multiple instances share this exact name - keep the segment as a group
    // and fan the instances out as their own leaves alongside any deeper children.
    const instanceLeaves: MonitorTreeNode[] = child.descriptors.map(d => ({
      id: `${id}#${d.instance ?? ''}`,
      label: d.instance ?? segment,
      children: [],
      monitor: d,
    }))
    nodes.push({ id, label: segment, children: [...instanceLeaves, ...deeperChildren] })
  }

  return nodes
}

/**
 * Case-insensitive substring filter over a monitor tree. A leaf matches when
 * its monitor's display label matches; a group matches (and keeps its full
 * subtree) when its own segment label matches; otherwise a group survives
 * pruned down to only its matching descendants. Mirrors MBeanTree.filter's
 * ancestor-preserving behavior (see MBeanTreePicker.tsx).
 */
export function filterMonitorTree(nodes: MonitorTreeNode[], query: string): MonitorTreeNode[] {
  const q = query.trim().toLowerCase()
  if (!q) return nodes
  return filterNodes(nodes, q)
}

function filterNodes(nodes: MonitorTreeNode[], q: string): MonitorTreeNode[] {
  const result: MonitorTreeNode[] = []
  for (const node of nodes) {
    const monitorMatches = node.monitor ? node.monitor.label.toLowerCase().includes(q) : false

    if (node.children.length === 0) {
      // Pure leaf - only its own monitor's label can match.
      if (monitorMatches) result.push(node)
      continue
    }

    // A group (or a monitor that is simultaneously a group, e.g.
    // "com.example.Foo" alongside a deeper "com.example.Foo.Bar") keeps its
    // whole subtree when either its own monitor or its segment label matches.
    if (monitorMatches || node.label.toLowerCase().includes(q)) {
      result.push(node)
      continue
    }
    const filteredChildren = filterNodes(node.children, q)
    if (filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren })
    }
  }
  return result
}

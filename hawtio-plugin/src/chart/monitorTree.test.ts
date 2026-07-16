import { toMonitorDescriptor } from './monitorKey'
import { buildMonitorTree, filterMonitorTree, MonitorTreeNode } from './monitorTree'

function monitor(key: string) {
  return toMonitorDescriptor(key)
}

describe('buildMonitorTree', () => {
  it('roots by monitor type, INTERVAL before SNAPSHOT', () => {
    const tree = buildMonitorTree([monitor('SNAPSHOT(name=JVMSnapShot)'), monitor('INTERVAL(name=com.example.Foo)')])
    expect(tree.map(n => n.id)).toEqual(['INTERVAL', 'SNAPSHOT'])
  })

  it('omits a type root with no monitors', () => {
    const tree = buildMonitorTree([monitor('INTERVAL(name=com.example.Foo)')])
    expect(tree.map(n => n.id)).toEqual(['INTERVAL'])
  })

  it('nests dotted names into groups, sorted alphabetically', () => {
    const tree = buildMonitorTree([
      monitor('INTERVAL(name=com.example.Bar)'),
      monitor('INTERVAL(name=com.example.Foo)'),
    ])
    const com = tree[0].children[0]
    expect(com.label).toBe('com')
    expect(com.monitor).toBeUndefined()
    const example = com.children[0]
    expect(example.label).toBe('example')
    expect(example.children.map(n => n.label)).toEqual(['Bar', 'Foo'])
    expect(example.children[0].monitor?.name).toBe('com.example.Bar')
  })

  it('makes the sole monitor at a path a direct leaf', () => {
    const tree = buildMonitorTree([monitor('INTERVAL(name=com.example.Foo)')])
    const leaf = tree[0].children[0].children[0].children[0]
    expect(leaf.label).toBe('Foo')
    expect(leaf.children).toEqual([])
    expect(leaf.monitor?.monitorKey).toBe('INTERVAL(name=com.example.Foo)')
  })

  it('fans multiple instances of the same name out as sibling leaves', () => {
    const tree = buildMonitorTree([
      monitor('SNAPSHOT(name=org.perfmon4j.java.management.JVMSnapShot;instance=G1 Eden Space)'),
      monitor('SNAPSHOT(name=org.perfmon4j.java.management.JVMSnapShot;instance=G1 Old Gen)'),
    ])
    const jvmSnapShotNode = tree[0].children[0].children[0].children[0].children[0].children[0]
    expect(jvmSnapShotNode.label).toBe('JVMSnapShot')
    expect(jvmSnapShotNode.monitor).toBeUndefined() // group, not itself selectable
    expect(jvmSnapShotNode.children.map(n => n.label)).toEqual(['G1 Eden Space', 'G1 Old Gen'])
    expect(jvmSnapShotNode.children[0].monitor?.instance).toBe('G1 Eden Space')
  })

  it('handles a monitor name that is simultaneously a leaf and a prefix of another', () => {
    const tree = buildMonitorTree([
      monitor('INTERVAL(name=com.example.Foo)'),
      monitor('INTERVAL(name=com.example.Foo.Bar)'),
    ])
    const fooNode = tree[0].children[0].children[0].children[0]
    expect(fooNode.label).toBe('Foo')
    expect(fooNode.monitor?.name).toBe('com.example.Foo')
    expect(fooNode.children.map(n => n.label)).toEqual(['Bar'])
  })
})

describe('filterMonitorTree', () => {
  const tree = buildMonitorTree([
    monitor('INTERVAL(name=com.example.OrderService.processOrder)'),
    monitor('INTERVAL(name=com.example.OrderService.validate)'),
    monitor('SNAPSHOT(name=JVMMemory)'),
    monitor('SNAPSHOT(name=GarbageCollector)'),
  ])

  it('returns the tree unchanged for an empty query', () => {
    expect(filterMonitorTree(tree, '')).toBe(tree)
    expect(filterMonitorTree(tree, '   ')).toBe(tree)
  })

  it('keeps only branches with a matching leaf', () => {
    const filtered = filterMonitorTree(tree, 'processOrder')
    expect(filtered.map(n => n.id)).toEqual(['INTERVAL'])
    const leaves = collectLeaves(filtered)
    expect(leaves.map(l => l.monitor?.name)).toEqual(['com.example.OrderService.processOrder'])
  })

  it('is case-insensitive', () => {
    const filtered = filterMonitorTree(tree, 'JVMMEMORY')
    expect(collectLeaves(filtered).map(l => l.label)).toEqual(['JVMMemory'])
  })

  it('keeps a whole subtree when a group label itself matches', () => {
    const filtered = filterMonitorTree(tree, 'ordersERVICE')
    const leaves = collectLeaves(filtered)
    expect(leaves.map(l => l.label).sort()).toEqual(['processOrder', 'validate'])
  })

  it('returns no roots when nothing matches', () => {
    expect(filterMonitorTree(tree, 'nonexistent')).toEqual([])
  })
})

function collectLeaves(nodes: MonitorTreeNode[]): MonitorTreeNode[] {
  return nodes.flatMap(n => (n.monitor && n.children.length === 0 ? [n] : collectLeaves(n.children)))
}

import { workspace } from '@hawtio/react'
import { AttributeRow } from '../mbean-snapshot/types'

// Attribute JMX types that don't map to a flat perfmon4j gauge/counter value
// (composite/tabular attributes, e.g. MemoryPoolMXBean's "Usage" attribute).
// Fanning these out into dotted paths like "Usage.max" is deferred - see
// the Sprint 1 backlog in the plan.
const UNSUPPORTED_ATTRIBUTE_TYPES = new Set([
  'javax.management.openmbean.CompositeData',
  '[Ljavax.management.openmbean.CompositeData;',
])

function parseObjectName(objectName: string): { domain: string; properties: Record<string, string> } {
  const colon = objectName.indexOf(':')
  if (colon === -1) {
    throw new Error(`Invalid JMX ObjectName: ${objectName}`)
  }
  const domain = objectName.substring(0, colon)
  const properties: Record<string, string> = {}
  for (const pair of objectName.substring(colon + 1).split(',')) {
    const eq = pair.indexOf('=')
    if (eq === -1) continue
    properties[pair.substring(0, eq)] = pair.substring(eq + 1)
  }
  return { domain, properties }
}

/**
 * Looks up a single MBean by its full ObjectName in the console's already-loaded
 * JMX workspace tree, and returns its flat scalar attributes (name + JMX type).
 * Composite/tabular attributes are filtered out - see UNSUPPORTED_ATTRIBUTE_TYPES.
 */
export async function readMBeanAttributes(objectName: string): Promise<AttributeRow[]> {
  const { domain, properties } = parseObjectName(objectName)
  const nodes = await workspace.findMBeans(domain, properties)
  const node = nodes.find(n => n.objectName === objectName) ?? nodes[0]

  if (!node?.mbean?.attr) {
    return []
  }

  return Object.entries(node.mbean.attr)
    .filter(([, attr]) => !UNSUPPORTED_ATTRIBUTE_TYPES.has(attr.type) && !attr.type?.startsWith('['))
    .map(([name, attr]) => ({
      name,
      type: attr.type,
      classification: 'skip' as const,
    }))
    .sort((a, b) => a.name.localeCompare(b.name))
}

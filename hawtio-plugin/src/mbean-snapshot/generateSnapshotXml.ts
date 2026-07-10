import { GenerateSnapshotXmlInput } from './types'

function escapeXmlAttribute(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, '&apos;')
}

/**
 * Builds the <mBeanSnapshotMonitor> XML element documented in
 * wiki/MBeanSnapShotMonitor.md, from a user's attribute selections.
 *
 * This is intentionally free of any Hawtio/Jolokia/React dependency so it can
 * be unit tested and validated against the real perfmon4j XML parser
 * (XMLConfigurationParser) independently of the browser UI.
 */
export function generateSnapshotXml(input: GenerateSnapshotXmlInput): string {
  const { monitorName, jmxName, gauges, counters } = input

  if (!monitorName.trim()) {
    throw new Error('monitorName is required')
  }
  if (!jmxName.trim()) {
    throw new Error('jmxName is required')
  }
  if (gauges.length === 0 && counters.length === 0) {
    throw new Error('At least one gauge or counter attribute must be selected')
  }

  const attrs = [`name='${escapeXmlAttribute(monitorName)}'`, `jmxName='${escapeXmlAttribute(jmxName)}'`]
  if (counters.length > 0) {
    attrs.push(`counters='${escapeXmlAttribute(counters.join(','))}'`)
  }
  if (gauges.length > 0) {
    attrs.push(`gauges='${escapeXmlAttribute(gauges.join(','))}'`)
  }

  return [
    `<mBeanSnapshotMonitor ${attrs.join('\n  ')}>`,
    `  <appender name='text-appender'/>`,
    `</mBeanSnapshotMonitor>`,
  ].join('\n')
}

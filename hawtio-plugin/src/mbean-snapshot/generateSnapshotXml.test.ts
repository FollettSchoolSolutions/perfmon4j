import { generateSnapshotXml } from './generateSnapshotXml'

describe('generateSnapshotXml', () => {
  it('renders gauges only', () => {
    const xml = generateSnapshotXml({
      monitorName: 'JVMThreading',
      jmxName: 'java.lang:type=ClassLoading',
      gauges: ['LoadedClassCount'],
      counters: [],
    })
    expect(xml).toContain(`name='JVMThreading'`)
    expect(xml).toContain(`jmxName='java.lang:type=ClassLoading'`)
    expect(xml).toContain(`gauges='LoadedClassCount'`)
    expect(xml).not.toContain('counters=')
  })

  it('renders counters only', () => {
    const xml = generateSnapshotXml({
      monitorName: 'JVMThreading',
      jmxName: 'java.lang:type=ClassLoading',
      gauges: [],
      counters: ['TotalLoadedClassCount', 'UnloadedClassCount'],
    })
    expect(xml).toContain(`counters='TotalLoadedClassCount,UnloadedClassCount'`)
    expect(xml).not.toContain('gauges=')
  })

  it('renders both gauges and counters, matching the documented example', () => {
    const xml = generateSnapshotXml({
      monitorName: 'JVMThreading',
      jmxName: 'java.lang:type=ClassLoading',
      gauges: ['LoadedClassCount'],
      counters: ['TotalLoadedClassCount', 'UnloadedClassCount'],
    })
    expect(xml).toContain(`counters='TotalLoadedClassCount,UnloadedClassCount'`)
    expect(xml).toContain(`gauges='LoadedClassCount'`)
    expect(xml).toContain(`<appender name='text-appender'/>`)
  })

  it('escapes XML-significant characters in attribute values', () => {
    const xml = generateSnapshotXml({
      monitorName: `Weird'<>&Name`,
      jmxName: 'java.lang:type=ClassLoading',
      gauges: ['LoadedClassCount'],
      counters: [],
    })
    expect(xml).toContain(`name='Weird&apos;&lt;&gt;&amp;Name'`)
  })

  it('throws when monitorName is missing', () => {
    expect(() =>
      generateSnapshotXml({ monitorName: '', jmxName: 'java.lang:type=ClassLoading', gauges: ['X'], counters: [] }),
    ).toThrow(/monitorName/)
  })

  it('throws when jmxName is missing', () => {
    expect(() =>
      generateSnapshotXml({ monitorName: 'M', jmxName: '', gauges: ['X'], counters: [] }),
    ).toThrow(/jmxName/)
  })

  it('throws when no gauges or counters are selected, mirroring XMLConfigurationParser validation', () => {
    expect(() =>
      generateSnapshotXml({ monitorName: 'M', jmxName: 'java.lang:type=ClassLoading', gauges: [], counters: [] }),
    ).toThrow(/gauge or counter/)
  })
})

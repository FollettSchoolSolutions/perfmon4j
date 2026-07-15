import {
  formatFieldLabel,
  formatMonitorLabel,
  isNumericFieldType,
  parseFieldSuffix,
  parseMonitorKey,
  toFieldDescriptor,
  toMonitorDescriptor,
} from './monitorKey'

describe('parseMonitorKey', () => {
  it('parses an INTERVAL key with no instance', () => {
    expect(parseMonitorKey('INTERVAL(name=com.example.Foo)')).toEqual({
      type: 'INTERVAL',
      name: 'com.example.Foo',
    })
  })

  it('parses a SNAPSHOT key with an instance', () => {
    expect(parseMonitorKey('SNAPSHOT(name=org.perfmon4j.java.management.JVMSnapShot;instance=G1 Eden Space)')).toEqual({
      type: 'SNAPSHOT',
      name: 'org.perfmon4j.java.management.JVMSnapShot',
      instance: 'G1 Eden Space',
    })
  })

  it('parses a THREADTRACE key', () => {
    expect(parseMonitorKey('THREADTRACE(name=org.apache)')).toEqual({
      type: 'THREADTRACE',
      name: 'org.apache',
    })
  })

  it('throws on an unparseable key', () => {
    expect(() => parseMonitorKey('not a key')).toThrow(/Unable to parse monitor key/)
  })
})

describe('parseFieldSuffix', () => {
  it('parses a LONG field on an INTERVAL monitor', () => {
    const monitorKey = 'INTERVAL(name=jmxX)'
    const fieldKey = 'INTERVAL(name=jmxX):FIELD(name=AverageDuration;type=LONG)'
    expect(parseFieldSuffix(monitorKey, fieldKey)).toEqual({ fieldName: 'AverageDuration', fieldType: 'LONG' })
  })

  it('parses each field type', () => {
    const monitorKey = 'INTERVAL(name=x)'
    const cases: Array<[string, string]> = [
      ['Count', 'INTEGER'],
      ['AverageDuration', 'LONG'],
      ['ThroughputPerSecond', 'DOUBLE'],
      ['TimeStop', 'TIMESTAMP'],
      ['stack', 'STRING'],
    ]
    for (const [fieldName, fieldType] of cases) {
      const fieldKey = `${monitorKey}:FIELD(name=${fieldName};type=${fieldType})`
      expect(parseFieldSuffix(monitorKey, fieldKey)).toEqual({ fieldName, fieldType })
    }
  })

  it('throws when the field key does not start with the given monitor key', () => {
    expect(() => parseFieldSuffix('INTERVAL(name=x)', 'INTERVAL(name=y):FIELD(name=Z;type=LONG)')).toThrow(
      /does not start with expected monitor key/,
    )
  })

  it('throws on an unparseable suffix', () => {
    expect(() => parseFieldSuffix('INTERVAL(name=x)', 'INTERVAL(name=x)garbage')).toThrow(/Unable to parse field key/)
  })
})

describe('isNumericFieldType', () => {
  it('accepts INTEGER, LONG, DOUBLE', () => {
    expect(isNumericFieldType('INTEGER')).toBe(true)
    expect(isNumericFieldType('LONG')).toBe(true)
    expect(isNumericFieldType('DOUBLE')).toBe(true)
  })

  it('rejects TIMESTAMP and STRING', () => {
    expect(isNumericFieldType('TIMESTAMP')).toBe(false)
    expect(isNumericFieldType('STRING')).toBe(false)
  })
})

describe('formatMonitorLabel / formatFieldLabel', () => {
  it('formats a monitor with no instance', () => {
    expect(formatMonitorLabel({ name: 'com.example.Foo' })).toBe('com.example.Foo')
  })

  it('formats a monitor with an instance', () => {
    expect(formatMonitorLabel({ name: 'JVMSnapShot', instance: 'G1 Eden Space' })).toBe('JVMSnapShot (G1 Eden Space)')
  })

  it('formats a field label from a monitor label', () => {
    expect(formatFieldLabel('com.example.Foo', 'AverageDuration')).toBe('com.example.Foo — AverageDuration')
  })
})

describe('toMonitorDescriptor / toFieldDescriptor', () => {
  it('builds a MonitorDescriptor preserving the raw key', () => {
    const descriptor = toMonitorDescriptor('INTERVAL(name=com.example.Foo)')
    expect(descriptor).toEqual({
      monitorKey: 'INTERVAL(name=com.example.Foo)',
      type: 'INTERVAL',
      name: 'com.example.Foo',
      instance: undefined,
      label: 'com.example.Foo',
    })
  })

  it('builds a FieldDescriptor preserving the raw key', () => {
    const monitor = toMonitorDescriptor('INTERVAL(name=com.example.Foo)')
    const fieldKey = 'INTERVAL(name=com.example.Foo):FIELD(name=AverageDuration;type=LONG)'
    const descriptor = toFieldDescriptor(monitor, fieldKey)
    expect(descriptor).toEqual({
      fieldKey,
      monitorKey: 'INTERVAL(name=com.example.Foo)',
      fieldName: 'AverageDuration',
      fieldType: 'LONG',
      label: 'com.example.Foo — AverageDuration',
    })
  })
})

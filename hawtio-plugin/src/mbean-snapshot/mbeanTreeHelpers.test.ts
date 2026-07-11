import { isMBeanLeaf } from './mbeanTreeHelpers'

describe('isMBeanLeaf', () => {
  it('is true for a node with an ObjectName (a real MBean)', () => {
    expect(isMBeanLeaf({ objectName: 'java.lang:type=ClassLoading' })).toBe(true)
  })

  it('is false for a node with no ObjectName (a domain/type grouping node)', () => {
    expect(isMBeanLeaf({})).toBe(false)
  })
})

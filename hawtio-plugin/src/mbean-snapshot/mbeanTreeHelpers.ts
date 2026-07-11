// Structurally typed (not `MBeanNode` from '@hawtio/react') so this stays a plain,
// dependency-free function - Jest-testable without a browser, same as generateSnapshotXml.ts.
export function isMBeanLeaf(node: { objectName?: string }): boolean {
  return !!node.objectName
}

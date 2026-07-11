import { jolokiaService } from '@hawtio/react'

// Must match SelfManagement.OBJECT_NAME in base's
// org.perfmon4j.selfmanagement.SelfManagement (Java) - do not rename casually,
// this string is a cross-language contract between the two.
export const SELF_MANAGEMENT_OBJECT_NAME = 'org.perfmon4j:type=SelfManagement'

/**
 * Reads perfmon4j's version from the target JVM's self-management MBean via
 * Jolokia. Throws if the MBean isn't present (e.g. no perfmon4j attached to the
 * target JVM, or a pre-self-management-MBean version of perfmon4j).
 */
export async function readPerfmon4jVersion(): Promise<string> {
  const value = await jolokiaService.readAttribute(SELF_MANAGEMENT_OBJECT_NAME, 'Version')
  return String(value)
}

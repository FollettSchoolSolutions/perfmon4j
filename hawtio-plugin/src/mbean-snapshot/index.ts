import { HawtioPlugin, hawtio, configManager } from '@hawtio/react'
import { MBeanSnapshotPanel } from './MBeanSnapshotPanel'

const pluginId = 'perfmon4j-mbean-snapshot'
const pluginTitle = 'perfmon4j'
const pluginPath = '/perfmon4j-mbean-snapshot'

/**
 * Entry point exposed via Module Federation as './plugin' (see webpack.config.js).
 *
 * Registered as a standalone top-level nav item rather than a tab inside the
 * built-in JMX plugin's MBean view: hawtio-react's JmxContent component builds
 * its Attributes/Operations/Chart tabs from a hardcoded list with no public
 * extension point for third-party tabs (confirmed by reading
 * packages/hawtio/src/plugins/jmx/JmxContent.tsx in hawtio-react). See Sprint 1
 * spec (Spike A) for the full finding.
 */
export const plugin: HawtioPlugin = () => {
  hawtio.addPlugin({
    id: pluginId,
    title: pluginTitle,
    path: pluginPath,
    component: MBeanSnapshotPanel,
    isActive: async () => true,
  })

  configManager.addProductInfo('perfmon4j Hawtio Plugin', '__PACKAGE_VERSION_PLACEHOLDER__')
}

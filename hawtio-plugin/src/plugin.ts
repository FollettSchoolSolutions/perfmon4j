import { HawtioPlugin, hawtio, configManager } from '@hawtio/react'
import { Perfmon4jPanel } from './Perfmon4jPanel'

const pluginId = 'perfmon4j'
const pluginTitle = 'perfmon4j'
const pluginPath = '/perfmon4j'

/**
 * Entry point exposed via Module Federation as './plugin' (see webpack.config.js).
 * This is the plugin's sole composition root - a real Hawtio console only ever
 * loads this one exposed module. Registers a single "Perfmon4j" nav item whose
 * component renders the Monitoring/Config/About tabs (see Perfmon4jPanel) -
 * previously three separate standalone nav items (chart/mbean-snapshot/about).
 */
export const plugin: HawtioPlugin = () => {
  hawtio.addPlugin({
    id: pluginId,
    title: pluginTitle,
    path: pluginPath,
    component: Perfmon4jPanel,
    isActive: async () => true,
  })

  configManager.addProductInfo('perfmon4j Hawtio Plugin', '__PACKAGE_VERSION_PLACEHOLDER__')
}

import { HawtioPlugin, hawtio, configManager } from '@hawtio/react'
import { plugin as registerMBeanSnapshotPlugin } from './mbean-snapshot'
import { plugin as registerAboutPlugin } from './about'
import { plugin as registerChartPlugin } from './chart'

/**
 * Entry point exposed via Module Federation as './plugin' (see webpack.config.js).
 * This is the plugin's sole composition root - a real Hawtio console only ever
 * loads this one exposed module, so every nav item this plugin registers is wired
 * up from here.
 */
export const plugin: HawtioPlugin = () => {
  registerMBeanSnapshotPlugin()
  registerAboutPlugin()
  registerChartPlugin()

  configManager.addProductInfo('perfmon4j Hawtio Plugin', '__PACKAGE_VERSION_PLACEHOLDER__')
}

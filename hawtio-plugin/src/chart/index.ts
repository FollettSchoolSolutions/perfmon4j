import { HawtioPlugin, hawtio } from '@hawtio/react'
import { ChartPanel } from './ChartPanel'

const pluginId = 'perfmon4j-chart'
const pluginTitle = 'perfmon4j Chart'
const pluginPath = '/perfmon4j-chart'

/**
 * Registered from src/plugin.ts, the plugin's sole composition root (see
 * webpack.config.js) - this file has no Module Federation entry of its own.
 */
export const plugin: HawtioPlugin = () => {
  hawtio.addPlugin({
    id: pluginId,
    title: pluginTitle,
    path: pluginPath,
    component: ChartPanel,
    isActive: async () => true,
  })
}

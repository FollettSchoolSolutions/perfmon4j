import { HawtioPlugin, hawtio } from '@hawtio/react'
import { AboutPanel } from './AboutPanel'

const pluginId = 'perfmon4j-about'
const pluginTitle = 'perfmon4j About'
const pluginPath = '/perfmon4j-about'

/**
 * Registered from src/mbean-snapshot/index.ts, which is the sole entry point
 * exposed via Module Federation (see webpack.config.js) - this file has no
 * Module Federation entry of its own.
 */
export const plugin: HawtioPlugin = () => {
  hawtio.addPlugin({
    id: pluginId,
    title: pluginTitle,
    path: pluginPath,
    component: AboutPanel,
    isActive: async () => true,
  })
}

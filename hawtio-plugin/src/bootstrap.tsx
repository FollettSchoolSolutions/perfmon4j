import React from 'react'
import ReactDOM from 'react-dom/client'
import { plugin } from './plugin'

// Local development harness only - mirrors hawtio-sample-plugin-ts's bootstrap so
// this plugin can be exercised standalone against a real Jolokia-enabled JVM via
// `npm start`, without requiring a full Hawtio console checkout. Not used when this
// plugin is loaded as a remote by a real Hawtio console (see README.md).
const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement)

import('@hawtio/react').then(async ({ hawtio, registerPlugins }) => {
  registerPlugins()
  plugin()

  await hawtio.bootstrap()
  const { Hawtio } = await import('@hawtio/react/ui')
  root.render(
    <React.StrictMode>
      <Hawtio />
    </React.StrictMode>,
  )
})

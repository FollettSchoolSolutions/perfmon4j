import { Alert, Content, PageSection, Spinner } from '@patternfly/react-core'
import React, { useEffect, useState } from 'react'
import { readPerfmon4jVersion } from '../jolokia/readPerfmon4jVersion'

type State = { status: 'loading' } | { status: 'loaded'; version: string } | { status: 'error'; message: string }

export const AboutPanel: React.FunctionComponent = () => {
  const [state, setState] = useState<State>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    readPerfmon4jVersion()
      .then(version => {
        if (!cancelled) setState({ status: 'loaded', version })
      })
      .catch(e => {
        if (!cancelled) setState({ status: 'error', message: e instanceof Error ? e.message : String(e) })
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <PageSection hasBodyWrapper={false}>
      <Content>
        <Content component='h1'>About perfmon4j</Content>
      </Content>

      {state.status === 'loading' && <Spinner size='md' aria-label='Loading perfmon4j version' />}

      {state.status === 'loaded' && <Content component='p'>perfmon4j version: {state.version}</Content>}

      {state.status === 'error' && (
        <Alert variant='warning' title='Unable to read perfmon4j version from this JVM'>
          {state.message}
        </Alert>
      )}
    </PageSection>
  )
}

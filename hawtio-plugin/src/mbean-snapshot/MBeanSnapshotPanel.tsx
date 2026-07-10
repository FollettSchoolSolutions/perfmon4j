import {
  Alert,
  Button,
  ClipboardCopy,
  ClipboardCopyVariant,
  Form,
  FormGroup,
  PageSection,
  TextInput,
  Content,
} from '@patternfly/react-core'
import React, { useState } from 'react'
import { readMBeanAttributes } from '../jolokia/readMBeanAttributes'
import { AttributeSelectionTable } from './AttributeSelectionTable'
import { generateSnapshotXml } from './generateSnapshotXml'
import { AttributeRow, Classification } from './types'

export const MBeanSnapshotPanel: React.FunctionComponent = () => {
  const [objectName, setObjectName] = useState('')
  const [monitorName, setMonitorName] = useState('')
  const [attributes, setAttributes] = useState<AttributeRow[]>([])
  const [error, setError] = useState<string | null>(null)

  const loadAttributes = async () => {
    setError(null)
    try {
      const rows = await readMBeanAttributes(objectName.trim())
      setAttributes(rows)
      if (rows.length === 0) {
        setError('No supported (flat, scalar) attributes were found for this ObjectName.')
      }
    } catch (e) {
      setAttributes([])
      setError(e instanceof Error ? e.message : String(e))
    }
  }

  const setClassification = (name: string, classification: Classification) => {
    setAttributes(prev => prev.map(a => (a.name === name ? { ...a, classification } : a)))
  }

  const gauges = attributes.filter(a => a.classification === 'gauge').map(a => a.name)
  const counters = attributes.filter(a => a.classification === 'counter').map(a => a.name)
  const canGenerate = monitorName.trim().length > 0 && objectName.trim().length > 0 && gauges.length + counters.length > 0

  let xml = ''
  let generateError: string | null = null
  if (canGenerate) {
    try {
      xml = generateSnapshotXml({ monitorName, jmxName: objectName.trim(), gauges, counters })
    } catch (e) {
      generateError = e instanceof Error ? e.message : String(e)
    }
  }

  return (
    <PageSection hasBodyWrapper={false}>
      <Content>
        <Content component='h1'>perfmon4j: Define JMXSnapShot Monitor</Content>
        <Content component='p'>
          Enter the ObjectName of a JMX MBean you are already viewing in this console, choose which attributes
          perfmon4j should snapshot-monitor, and copy the generated XML into your perfmonconfig.xml. This does not
          modify the running JVM.
        </Content>
      </Content>

      <Form>
        <FormGroup label='MBean ObjectName' isRequired fieldId='objectName'>
          <TextInput
            id='objectName'
            value={objectName}
            onChange={(_, value) => setObjectName(value)}
            placeholder='java.lang:type=ClassLoading'
          />
        </FormGroup>
        <Button variant='secondary' onClick={loadAttributes} isDisabled={objectName.trim().length === 0}>
          Load attributes
        </Button>

        {error && <Alert variant='warning' title={error} />}

        {attributes.length > 0 && (
          <>
            <AttributeSelectionTable attributes={attributes} onChange={setClassification} />

            <FormGroup label='Monitor name' isRequired fieldId='monitorName'>
              <TextInput id='monitorName' value={monitorName} onChange={(_, value) => setMonitorName(value)} />
            </FormGroup>
          </>
        )}

        {generateError && <Alert variant='danger' title={generateError} />}

        {xml && (
          <FormGroup label='Generated mBeanSnapshotMonitor XML' fieldId='generatedXml'>
            <ClipboardCopy
              isCode
              isReadOnly
              variant={ClipboardCopyVariant.expansion}
              hoverTip='Copy'
              clickTip='Copied'
            >
              {xml}
            </ClipboardCopy>
          </FormGroup>
        )}
      </Form>
    </PageSection>
  )
}

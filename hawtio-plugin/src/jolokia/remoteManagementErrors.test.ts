import { classifyRemoteManagementError, ExecAccessDeniedError, IncompatibleClientVersionError, RemoteManagementError } from './remoteManagementErrors'

describe('classifyRemoteManagementError', () => {
  it('classifies an HTTP-level 403 fetch failure as ExecAccessDeniedError', () => {
    const err = classifyRemoteManagementError('403: Forbidden', 'connect')
    expect(err).toBeInstanceOf(ExecAccessDeniedError)
    expect(err.operation).toBe('connect')
  })

  it('classifies a Jolokia-ACL security exception as ExecAccessDeniedError', () => {
    const err = classifyRemoteManagementError('java.lang.SecurityException: No access allowed', 'subscribe')
    expect(err).toBeInstanceOf(ExecAccessDeniedError)
  })

  it('classifies an IncompatibleClientVersionException by name as IncompatibleClientVersionError', () => {
    const err = classifyRemoteManagementError(
      'org.perfmon4j.remotemanagement.intf.IncompatibleClientVersionException: Client version: 2.000 is not compatible with 1.001',
      'connect',
    )
    expect(err).toBeInstanceOf(IncompatibleClientVersionError)
  })

  it('falls back to a generic RemoteManagementError for an unrecognized message', () => {
    const err = classifyRemoteManagementError('org.perfmon4j.remotemanagement.intf.SessionNotFoundException: Session not found: abc', 'getData')
    expect(err).toBeInstanceOf(RemoteManagementError)
    expect(err).not.toBeInstanceOf(ExecAccessDeniedError)
    expect(err).not.toBeInstanceOf(IncompatibleClientVersionError)
  })

  it('accepts a non-Error thrown value', () => {
    const err = classifyRemoteManagementError('plain string failure', 'getMonitors')
    expect(err.message).toBe('plain string failure')
  })

  it('accepts an Error instance', () => {
    const err = classifyRemoteManagementError(new Error('boom'), 'getMonitors')
    expect(err.message).toBe('boom')
  })
})

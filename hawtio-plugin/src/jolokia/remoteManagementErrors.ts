// Deliberately dependency-free (no @hawtio/react import) so this stays Jest-testable
// in isolation - importing jolokiaService pulls in @patternfly/react-core's full
// component tree, which includes CSS imports Jest's default transform can't parse.
// Mirrors generateSnapshotXml.ts's "keep pure logic separate" convention.

export class RemoteManagementError extends Error {
  constructor(
    message: string,
    public readonly operation: string,
  ) {
    super(message)
    this.name = 'RemoteManagementError'
  }
}

export class ExecAccessDeniedError extends RemoteManagementError {}
export class IncompatibleClientVersionError extends RemoteManagementError {}

/**
 * jolokiaService.execute() only ever rejects with a plain string (either
 * `error.stacktrace || error.error` from a Jolokia-level exec failure, or
 * `"<status>: <statusText>"` from an HTTP-level fetch failure - see
 * JolokiaService.execute()/configureFetchErrorCallback() in @hawtio/react) - the
 * thrown exception's fully-qualified class name is not reliably forwarded, so this
 * classifies by matching against whatever text comes back rather than an error type.
 */
export function classifyRemoteManagementError(e: unknown, operation: string): RemoteManagementError {
  const message = e instanceof Error ? e.message : String(e)

  if (/\b403\b|forbidden|no access|not allowed|security ?exception/i.test(message)) {
    return new ExecAccessDeniedError(message, operation)
  }
  if (/IncompatibleClientVersionException/.test(message)) {
    return new IncompatibleClientVersionError(message, operation)
  }
  return new RemoteManagementError(message, operation)
}

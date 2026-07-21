// Thin, impure DOM side-effect: opens a fully-formed HTML document (from
// buildThreadTraceReportHtml.ts) in a new browser tab the user can also save and
// reopen offline. Kept out of the pure builder so the builder stays Jest-testable
// without a DOM, mirroring this codebase's "side effects in a caller" convention.
//
// A Blob URL (rather than document.write into an opened window) gives the new tab a
// real, saveable resource whose "Save as…" produces a standalone .html file. Called
// only from a View click handler, so the window.open is a user-gesture and survives
// popup blockers.

/**
 * Opens the given HTML in a new tab. The object URL is revoked shortly after so it
 * isn't leaked; the already-loaded document keeps working (and saves fine) after
 * revocation. Returns false if the browser blocked the popup.
 */
export function openThreadTraceReport(html: string): boolean {
  const blob = new Blob([html], { type: 'text/html' })
  const url = URL.createObjectURL(blob)
  const win = window.open(url, '_blank')
  // Revoke after the new tab has had a chance to load the resource.
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  return win !== null
}

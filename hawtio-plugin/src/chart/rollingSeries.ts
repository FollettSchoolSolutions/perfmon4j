import { ChartDataPoint } from './types'

/** 5 minutes - matches the legacy VisualVM tool's scrolling chart window. This is a
 * coincidence, not a functional link, with ExternalAppender.DEFAULT_TIMEOUT_SECONDS'
 * unrelated 5-minute session idle timeout on the Java side. */
export const DEFAULT_WINDOW_MS = 5 * 60 * 1000

/** Defensive cap only - at the default 5s poll rate, 5 minutes naturally produces
 * ~60 points/series; this only bites if polling is manually sped up well past that. */
export const DEFAULT_MAX_POINTS = 600

export function appendPoint(points: ChartDataPoint[], point: ChartDataPoint): ChartDataPoint[] {
  return [...points, point]
}

/**
 * Drops points older than `windowMs` relative to `now`, then enforces `maxPoints` as
 * a defensive cap. `now` is an explicit parameter (never Date.now() internally)
 * purely to keep this trivially unit-testable.
 */
export function trimToWindow(
  points: ChartDataPoint[],
  now: number,
  windowMs: number = DEFAULT_WINDOW_MS,
  maxPoints: number = DEFAULT_MAX_POINTS,
): ChartDataPoint[] {
  const cutoff = now - windowMs
  const withinWindow = points.filter(p => p.timestamp >= cutoff)
  return withinWindow.length > maxPoints ? withinWindow.slice(withinWindow.length - maxPoints) : withinWindow
}

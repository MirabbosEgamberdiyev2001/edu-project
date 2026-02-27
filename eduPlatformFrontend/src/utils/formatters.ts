/**
 * Shared formatting utilities used across the app.
 */

/** Format a score as "earned / max (XX%)" */
export function formatScore(earned: number | null, max: number | null): string {
  if (earned == null || max == null) return '-';
  const pct = max > 0 ? Math.round((earned / max) * 100) : 0;
  return `${earned}/${max} (${pct}%)`;
}

/** Format a percentage value as "XX%" */
export function formatPercent(value: number | null | undefined): string {
  if (value == null) return '-';
  return `${Math.round(value)}%`;
}

/** Format duration in seconds → "Xm Ys" */
export function formatDuration(seconds: number | null | undefined): string {
  if (seconds == null) return '-';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m === 0) return `${s}s`;
  return `${m}m ${s}s`;
}

/** Format duration in minutes → "Xm" or "Xh Xm" */
export function formatMinutes(minutes: number | null | undefined): string {
  if (minutes == null) return '-';
  if (minutes < 60) return `${minutes}m`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
}

/** Format an ISO date string to a readable local date */
export function formatDate(
  iso: string | null | undefined,
  locale = 'uz-UZ',
): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleDateString(locale, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}

/** Format an ISO date string to date + time */
export function formatDateTime(
  iso: string | null | undefined,
  locale = 'uz-UZ',
): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleString(locale, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Return a color string based on a percentage score */
export function scoreColor(pct: number | null | undefined): 'success' | 'warning' | 'error' | 'default' {
  if (pct == null) return 'default';
  if (pct >= 70) return 'success';
  if (pct >= 40) return 'warning';
  return 'error';
}

/** Return a MUI color hex based on a percentage score */
export function scoreHex(pct: number | null | undefined): string {
  if (pct == null) return '#64748b';
  if (pct >= 70) return '#22c55e';
  if (pct >= 40) return '#f59e0b';
  return '#ef4444';
}

/** Truncate text to given char length with ellipsis */
export function truncate(text: string | null | undefined, maxLen = 60): string {
  if (!text) return '';
  return text.length <= maxLen ? text : `${text.slice(0, maxLen)}…`;
}

/** Format a number with thousand separators */
export function formatNumber(n: number | null | undefined): string {
  if (n == null) return '-';
  return n.toLocaleString();
}

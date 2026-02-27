import { Chip } from '@mui/material';
import type { ChipProps } from '@mui/material';

/** Unified color map for common status strings across the app */
const STATUS_COLOR_MAP: Record<string, ChipProps['color']> = {
  // Attempt statuses
  IN_PROGRESS: 'primary',
  SUBMITTED: 'info',
  AUTO_GRADED: 'success',
  NEEDS_REVIEW: 'warning',
  GRADED: 'success',
  EXPIRED: 'default',

  // Assignment statuses
  ACTIVE: 'success',
  DRAFT: 'default',
  SCHEDULED: 'info',
  COMPLETED: 'success',
  ARCHIVED: 'default',
  CANCELLED: 'error',

  // Moderation statuses
  PENDING_MODERATION: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  NONE: 'default',

  // User statuses
  ENABLED: 'success',
  DISABLED: 'error',
  LOCKED: 'warning',

  // Generic
  true: 'success',
  false: 'error',
};

interface StatusBadgeProps {
  status: string | null | undefined;
  label?: string;
  size?: ChipProps['size'];
  variant?: ChipProps['variant'];
}

export default function StatusBadge({
  status,
  label,
  size = 'small',
  variant = 'filled',
}: StatusBadgeProps) {
  if (!status) return null;
  const color = STATUS_COLOR_MAP[status] ?? 'default';

  return (
    <Chip
      label={label ?? status}
      color={color}
      size={size}
      variant={variant}
    />
  );
}

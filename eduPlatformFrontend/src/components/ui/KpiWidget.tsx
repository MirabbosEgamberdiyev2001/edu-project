import { Box, Paper, Typography, Skeleton } from '@mui/material';
import type { SxProps } from '@mui/material';

interface KpiWidgetProps {
  label: string;
  value: string | number | null | undefined;
  icon?: React.ReactNode;
  iconBg?: string;
  /** Subtext shown below value */
  sub?: string;
  loading?: boolean;
  sx?: SxProps;
}

export default function KpiWidget({
  label,
  value,
  icon,
  iconBg = '#eff6ff',
  sub,
  loading = false,
  sx,
}: KpiWidgetProps) {
  return (
    <Paper
      sx={{
        p: 2.5,
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        ...sx,
      }}
    >
      {icon && (
        <Box
          sx={{
            width: 48,
            height: 48,
            borderRadius: 2,
            bgcolor: iconBg,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
      )}
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="overline" color="text.secondary" sx={{ lineHeight: 1.2, display: 'block' }}>
          {label}
        </Typography>
        {loading ? (
          <Skeleton width={60} height={32} />
        ) : (
          <Typography variant="h5" fontWeight={700} sx={{ lineHeight: 1.2 }}>
            {value ?? '-'}
          </Typography>
        )}
        {sub && (
          <Typography variant="caption" color="text.secondary">
            {sub}
          </Typography>
        )}
      </Box>
    </Paper>
  );
}

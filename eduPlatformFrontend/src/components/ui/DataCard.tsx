import { Paper, Box, Typography, Divider } from '@mui/material';
import type { SxProps } from '@mui/material';

interface DataCardProps {
  title?: string;
  subtitle?: string;
  /** Slot for actions (buttons, etc.) next to the title */
  headerAction?: React.ReactNode;
  children: React.ReactNode;
  sx?: SxProps;
  /** Remove padding from body (e.g. for full-bleed tables) */
  noPadding?: boolean;
}

/**
 * Generic card container used for data sections.
 * Optional title/subtitle header with divider, then children.
 */
export default function DataCard({
  title,
  subtitle,
  headerAction,
  children,
  sx,
  noPadding = false,
}: DataCardProps) {
  const hasHeader = Boolean(title || headerAction);

  return (
    <Paper sx={{ overflow: 'hidden', ...sx }}>
      {hasHeader && (
        <>
          <Box
            sx={{
              px: 2.5,
              py: 1.75,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 2,
            }}
          >
            <Box>
              {title && (
                <Typography variant="h6" fontWeight={600}>
                  {title}
                </Typography>
              )}
              {subtitle && (
                <Typography variant="caption" color="text.secondary">
                  {subtitle}
                </Typography>
              )}
            </Box>
            {headerAction && <Box sx={{ flexShrink: 0 }}>{headerAction}</Box>}
          </Box>
          <Divider />
        </>
      )}
      <Box sx={noPadding ? undefined : { p: 2.5 }}>{children}</Box>
    </Paper>
  );
}

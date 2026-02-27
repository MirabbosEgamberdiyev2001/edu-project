import { Box, Typography, Breadcrumbs, Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

interface Crumb {
  label: string;
  to?: string;
}

interface PageShellProps {
  title: string;
  subtitle?: string;
  /** Breadcrumb items; last item is the current page (no link) */
  breadcrumbs?: Crumb[];
  /** Slot for action buttons in the top-right */
  actions?: React.ReactNode;
  children: React.ReactNode;
}

/**
 * Standard page wrapper: title row + optional breadcrumbs + content.
 * Keeps consistent spacing and heading style across all pages.
 */
export default function PageShell({
  title,
  subtitle,
  breadcrumbs,
  actions,
  children,
}: PageShellProps) {
  return (
    <Box>
      {/* Page header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          mb: 3,
          gap: 2,
        }}
      >
        <Box>
          {breadcrumbs && breadcrumbs.length > 0 && (
            <Breadcrumbs sx={{ mb: 0.5 }} aria-label="breadcrumb">
              {breadcrumbs.map((crumb, i) =>
                crumb.to ? (
                  <Link
                    key={i}
                    component={RouterLink}
                    to={crumb.to}
                    underline="hover"
                    color="text.secondary"
                    variant="body2"
                  >
                    {crumb.label}
                  </Link>
                ) : (
                  <Typography key={i} variant="body2" color="text.primary">
                    {crumb.label}
                  </Typography>
                )
              )}
            </Breadcrumbs>
          )}
          <Typography variant="h4" component="h1">
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions && (
          <Box sx={{ flexShrink: 0, display: 'flex', gap: 1, pt: 0.5 }}>
            {actions}
          </Box>
        )}
      </Box>

      {children}
    </Box>
  );
}

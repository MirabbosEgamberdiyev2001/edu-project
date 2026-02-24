import { Link as RouterLink } from 'react-router-dom';
import { Breadcrumbs, Link, Typography } from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface PageBreadcrumbsProps {
  items: BreadcrumbItem[];
}

export default function PageBreadcrumbs({ items }: PageBreadcrumbsProps) {
  if (items.length === 0) return null;

  return (
    <Breadcrumbs
      separator={<NavigateNextIcon fontSize="small" />}
      aria-label="breadcrumb"
      sx={{ mb: 2 }}
    >
      {items.map((item, index) => {
        const isLast = index === items.length - 1;

        if (isLast || !item.href) {
          return (
            <Typography
              key={index}
              color="text.primary"
              variant="body2"
              fontWeight={isLast ? 600 : 400}
            >
              {item.label}
            </Typography>
          );
        }

        return (
          <Link
            key={index}
            component={RouterLink}
            to={item.href}
            underline="hover"
            color="inherit"
            variant="body2"
          >
            {item.label}
          </Link>
        );
      })}
    </Breadcrumbs>
  );
}

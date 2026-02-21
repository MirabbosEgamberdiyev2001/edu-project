import {
  Box,
  Typography,
  Grid,
  CircularProgress,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import { useTranslation } from 'react-i18next';
import type { ParentChildDto } from '@/types/parent';
import ChildCard from './ChildCard';

interface ChildrenListProps {
  children: ParentChildDto[] | undefined;
  isLoading: boolean;
}

export default function ChildrenList({ children, isLoading }: ChildrenListProps) {
  const { t } = useTranslation('parent');

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!children || children.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <PeopleIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          {t('noChildren')}
        </Typography>
        <Typography variant="body2" color="text.disabled">
          {t('noChildrenDescription')}
        </Typography>
      </Box>
    );
  }

  return (
    <Grid container spacing={2.5}>
      {children.map((pairing) => (
        <Grid item xs={12} sm={6} md={4} lg={3} key={pairing.id}>
          <ChildCard pairing={pairing} />
        </Grid>
      ))}
    </Grid>
  );
}

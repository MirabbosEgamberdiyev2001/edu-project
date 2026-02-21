import { useParams, useNavigate } from 'react-router-dom';
import { Box, Typography, Button, CircularProgress } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useTranslation } from 'react-i18next';
import { useChildDashboard } from '../hooks/useChildDashboard';
import ChildDashboardView from '../components/ChildDashboardView';

export default function ChildDashboardPage() {
  const { childId } = useParams<{ childId: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('parent');
  const { data, isLoading } = useChildDashboard(childId!);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!data) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">{t('childNotFound')}</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/my-children')} sx={{ mb: 2 }}>
        {t('backToChildren')}
      </Button>

      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>
        {data.childName}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {t('childDashboard')}
      </Typography>

      <ChildDashboardView data={data} />
    </Box>
  );
}

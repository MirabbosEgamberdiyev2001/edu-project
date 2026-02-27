import { useParams } from 'react-router-dom';
import { Box, Typography, CircularProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useChildDashboard } from '../hooks/useChildDashboard';
import ChildDashboardView from '../components/ChildDashboardView';

export default function ChildDashboardPage() {
  const { childId } = useParams<{ childId: string }>();
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
    <PageShell
      title={data.childName}
      subtitle={t('childDashboard')}
      breadcrumbs={[
        { label: t('myChildrenTitle'), to: '/my-children' },
        { label: data.childName },
      ]}
    >
      <ChildDashboardView data={data} />
    </PageShell>
  );
}

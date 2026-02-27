import { useParams } from 'react-router-dom';
import {
  Box,
  Typography,
  CircularProgress,
  Chip,
} from '@mui/material';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useAssignment } from '../hooks/useAssignments';
import { useLiveMonitoring } from '../hooks/useLiveMonitoring';
import LiveMonitoringPanel from '../components/LiveMonitoringPanel';

export default function LiveMonitoringPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('assignment');
  const { data: assignment } = useAssignment(id!);
  const { data: liveData, isLoading } = useLiveMonitoring(id!);

  return (
    <PageShell
      title={t('liveMonitoring')}
      subtitle={assignment ? `${assignment.title} — ${assignment.groupName}` : undefined}
      breadcrumbs={[
        { label: t('common:assignments'), to: '/assignments' },
        { label: assignment?.title || '...', to: `/assignments/${id}` },
        { label: t('liveMonitoring') },
      ]}
      actions={
        <Chip
          icon={<FiberManualRecordIcon sx={{ fontSize: 12 }} />}
          label={t('liveLabel')}
          color="error"
          size="small"
        />
      }
    >

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : liveData ? (
        <LiveMonitoringPanel data={liveData} />
      ) : (
        <Typography color="text.secondary">{t('noLiveData')}</Typography>
      )}
    </PageShell>
  );
}

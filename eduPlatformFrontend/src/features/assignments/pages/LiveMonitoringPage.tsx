import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  CircularProgress,
  Chip,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import { useTranslation } from 'react-i18next';
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
    <Box>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(`/assignments/${id}`)} sx={{ mb: 2 }}>
        {t('backToDetail')}
      </Button>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>
          {t('liveMonitoring')}
        </Typography>
        <Chip
          icon={<FiberManualRecordIcon sx={{ fontSize: 12 }} />}
          label={t('liveLabel')}
          color="error"
          size="small"
        />
      </Box>

      {assignment && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {assignment.title} - {assignment.groupName}
        </Typography>
      )}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : liveData ? (
        <LiveMonitoringPanel data={liveData} />
      ) : (
        <Typography color="text.secondary">{t('noLiveData')}</Typography>
      )}
    </Box>
  );
}

import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Chip,
  Button,
  CircularProgress,
  Grid,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import CancelIcon from '@mui/icons-material/Cancel';
import MonitorIcon from '@mui/icons-material/Monitor';
import BarChartIcon from '@mui/icons-material/BarChart';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useAssignment } from '../hooks/useAssignments';
import { useAssignmentMutations } from '../hooks/useAssignmentMutations';
import { AssignmentStatus } from '@/types/assignment';
import AssignmentSettingsForm from '../components/AssignmentSettingsForm';
import PromoCodeSection from '../components/PromoCodeSection';

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
  DRAFT: 'default',
  SCHEDULED: 'warning',
  ACTIVE: 'info',
  COMPLETED: 'success',
  ENDED: 'success',
  CANCELLED: 'error',
};

export default function AssignmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('assignment');
  const { data: assignment, isLoading } = useAssignment(id!);
  const { activate, cancel } = useAssignmentMutations();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!assignment) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">{t('notFound')}</Typography>
      </Box>
    );
  }

  const progress = assignment.totalStudents > 0
    ? Math.round((assignment.completedStudents / assignment.totalStudents) * 100)
    : 0;

  return (
    <PageShell
      title={assignment.title}
      subtitle={assignment.description || undefined}
      breadcrumbs={[
        { label: t('common:assignments'), to: '/assignments' },
        { label: assignment.title },
      ]}
      actions={
        <Chip
          label={t(`status.${assignment.status}`)}
          color={STATUS_COLORS[assignment.status] || 'default'}
        />
      }
    >
      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">{t('group')}</Typography>
            <Typography variant="body1" fontWeight={500}>{assignment.groupName}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">{t('test')}</Typography>
            <Typography variant="body1" fontWeight={500}>{assignment.testTitle}</Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">{t('progress')}</Typography>
            <Typography variant="body1" fontWeight={500}>
              {assignment.completedStudents}/{assignment.totalStudents} ({progress}%)
            </Typography>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Typography variant="caption" color="text.secondary">{t('averageScore')}</Typography>
            <Typography variant="body1" fontWeight={500}>
              {assignment.averageScore != null ? `${Math.round(assignment.averageScore)}%` : '-'}
            </Typography>
          </Grid>
        </Grid>

        {assignment.durationMinutes && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="caption" color="text.secondary">{t('durationMinutes')}</Typography>
            <Typography variant="body2">{assignment.durationMinutes} {t('minutesShort')}</Typography>
          </Box>
        )}

        {(assignment.startDate || assignment.endDate) && (
          <Grid container spacing={2} sx={{ mt: 1 }}>
            {assignment.startDate && (
              <Grid item xs={6}>
                <Typography variant="caption" color="text.secondary">{t('startDate')}</Typography>
                <Typography variant="body2">{new Date(assignment.startDate).toLocaleString()}</Typography>
              </Grid>
            )}
            {assignment.endDate && (
              <Grid item xs={6}>
                <Typography variant="caption" color="text.secondary">{t('endDate')}</Typography>
                <Typography variant="body2">{new Date(assignment.endDate).toLocaleString()}</Typography>
              </Grid>
            )}
          </Grid>
        )}
      </Paper>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>{t('settingsTitle')}</Typography>
        <AssignmentSettingsForm
          settings={{
            durationMinutes: assignment.durationMinutes ?? undefined,
            maxAttempts: assignment.maxAttempts,
            shuffleQuestions: assignment.shuffleQuestions,
            shuffleOptions: assignment.shuffleOptions,
            preventTabSwitch: assignment.preventTabSwitch,
            preventCopyPaste: assignment.preventCopyPaste,
          }}
          onChange={() => {}}
          disabled
        />
      </Paper>

      {(assignment.status === AssignmentStatus.ACTIVE || assignment.status === AssignmentStatus.DRAFT) && (
        <PromoCodeSection assignmentId={id!} />
      )}

      <Box sx={{ display: 'flex', gap: 2 }}>
        {assignment.status === AssignmentStatus.DRAFT && (
          <Button
            variant="contained"
            startIcon={<PlayArrowIcon />}
            onClick={() => activate.mutate(id!)}
            disabled={activate.isPending}
          >
            {t('activate')}
          </Button>
        )}
        {assignment.status === AssignmentStatus.SCHEDULED && (
          <Button
            variant="outlined"
            color="error"
            startIcon={<CancelIcon />}
            onClick={() => cancel.mutate(id!)}
            disabled={cancel.isPending}
          >
            {t('cancelAssignment')}
          </Button>
        )}
        {assignment.status === AssignmentStatus.ACTIVE && (
          <>
            <Button
              variant="contained"
              startIcon={<MonitorIcon />}
              onClick={() => navigate(`/assignments/${id}/live`)}
            >
              {t('monitor')}
            </Button>
            <Button
              variant="outlined"
              color="error"
              startIcon={<CancelIcon />}
              onClick={() => cancel.mutate(id!)}
              disabled={cancel.isPending}
            >
              {t('cancelAssignment')}
            </Button>
          </>
        )}
        {(assignment.status === AssignmentStatus.COMPLETED || assignment.status === AssignmentStatus.CANCELLED) && (
          <Button
            variant="contained"
            startIcon={<BarChartIcon />}
            onClick={() => navigate(`/assignments/${id}/results`)}
          >
            {t('viewResults')}
          </Button>
        )}
      </Box>
    </PageShell>
  );
}

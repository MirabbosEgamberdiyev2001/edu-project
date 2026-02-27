import {
  Box,
  Typography,
  Paper,
  CircularProgress,
  Grid,
  Button,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
import { useTeacherDashboard, useExportTeacherDashboard } from '../hooks/useTeacherDashboard';
import StatSummaryCards from '../components/StatSummaryCards';
import ScoreTrendChart from '../components/ScoreTrendChart';
import TopStudentsTable from '../components/TopStudentsTable';
import AtRiskStudentsTable from '../components/AtRiskStudentsTable';
import { PageShell } from '@/components/ui';

export default function TeacherAnalyticsPage() {
  const { t } = useTranslation('analytics');
  const { data, isLoading } = useTeacherDashboard();
  const { exportDashboard } = useExportTeacherDashboard();

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
        <Typography variant="h6" color="text.secondary">{t('noData')}</Typography>
        <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>{t('noDataDescription')}</Typography>
      </Box>
    );
  }

  const stats = [
    { label: t('totalStudents'), value: data.totalStudents, color: '#1976d2' },
    { label: t('totalGroups'), value: data.totalGroups, color: '#2e7d32' },
    { label: t('totalAssignments'), value: data.totalAssignments, color: '#ed6c02' },
    { label: t('avgScore'), value: Math.round(data.averageScore), color: '#9c27b0', suffix: '%' },
    { label: t('completionRate'), value: Math.round(data.completionRate), color: '#00796b', suffix: '%' },
  ];

  return (
    <PageShell
      title={t('teacherTitle')}
      subtitle={t('teacherSubtitle')}
      actions={
        <Button startIcon={<DownloadIcon />} onClick={exportDashboard} variant="outlined" size="small">
          {t('exportPdf')}
        </Button>
      }
    >

      <Box sx={{ mb: 3 }}>
        <StatSummaryCards stats={stats} />
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <ScoreTrendChart data={data.testCreationTrend} title={t('testCreationTrend')} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('recentActivity')}</Typography>
            {data.recentActivity.map((activity, i) => (
              <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">{activity.description}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {new Date(activity.createdAt).toLocaleDateString()}
                </Typography>
              </Box>
            ))}
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('topStudents')}</Typography>
            <TopStudentsTable students={data.topStudents} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('atRiskStudents')}</Typography>
            <AtRiskStudentsTable students={data.atRiskStudents} />
          </Paper>
        </Grid>
      </Grid>
    </PageShell>
  );
}

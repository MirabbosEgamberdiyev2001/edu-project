import {
  Box,
  Paper,
  CircularProgress,
  Grid,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useMyAnalytics } from '../hooks/useStudentAnalytics';
import StatSummaryCards from '../components/StatSummaryCards';
import ScoreTrendChart from '../components/ScoreTrendChart';
import SubjectBreakdownChart from '../components/SubjectBreakdownChart';
import WeakAreasCard from '../components/WeakAreasCard';
import WeeklyActivityCard from '../components/WeeklyActivityCard';
import { PageShell } from '@/components/ui';

export default function StudentAnalyticsPage() {
  const { t } = useTranslation('analytics');
  const { data, isLoading } = useMyAnalytics();

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
    { label: t('overallAverage'), value: Math.round(data.overallAverage), color: '#1976d2', suffix: '%' },
    { label: t('totalAttempts'), value: data.totalAttempts, color: '#2e7d32' },
    { label: t('totalAssignments'), value: data.totalAssignments, color: '#ed6c02' },
    { label: t('completionRate'), value: Math.round(data.completionRate), color: '#00796b', suffix: '%' },
  ];

  return (
    <PageShell title={t('studentTitle')} subtitle={t('studentSubtitle')}>
      <Box sx={{ mb: 3 }}>
        <StatSummaryCards stats={stats} />
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <ScoreTrendChart data={data.scoreTrend} title={t('scoreTrend')} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <WeeklyActivityCard data={data.weeklyActivity} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <SubjectBreakdownChart data={data.subjectBreakdown} />
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <WeakAreasCard weakAreas={data.weakAreas} strongAreas={data.strongAreas} />
          </Paper>
        </Grid>
      </Grid>
    </PageShell>
  );
}

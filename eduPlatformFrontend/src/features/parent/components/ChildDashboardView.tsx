import { Box, Typography, Paper, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { ChildDashboardDto } from '@/types/parent';
import SubjectScoreBar from './SubjectScoreBar';
import RecentAttemptsTable from './RecentAttemptsTable';

interface ChildDashboardViewProps {
  data: ChildDashboardDto;
}

export default function ChildDashboardView({ data }: ChildDashboardViewProps) {
  const { t } = useTranslation('parent');

  return (
    <Box>
      <Paper sx={{ p: 3, mb: 3 }}>
        <Grid container spacing={3}>
          <Grid item xs={6} sm={3}>
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="h3" fontWeight={700} color="primary.main">
                {Math.round(data.averageScore)}%
              </Typography>
              <Typography variant="caption">{t('averageScore')}</Typography>
            </Box>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Box sx={{ textAlign: 'center' }}>
              <Typography variant="h3" fontWeight={700} color="info.main">
                {data.totalTests}
              </Typography>
              <Typography variant="caption">{t('totalTests')}</Typography>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('subjectScores')}</Typography>
            {data.subjectScores.map((score) => (
              <SubjectScoreBar key={score.subjectId} score={score} />
            ))}
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('recentAttempts')}</Typography>
            <RecentAttemptsTable attempts={data.recentAttempts} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

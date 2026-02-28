import { Box, Typography, Paper, Grid, Divider, Stack } from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import TrendingFlatIcon from '@mui/icons-material/TrendingFlat';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import AssignmentIcon from '@mui/icons-material/Assignment';
import BarChartIcon from '@mui/icons-material/BarChart';
import { useTranslation } from 'react-i18next';
import type { ChildDashboardDto } from '@/types/parent';
import { KpiWidget } from '@/components/ui';
import SubjectScoreBar from './SubjectScoreBar';
import RecentAttemptsTable from './RecentAttemptsTable';

interface ChildDashboardViewProps {
  data: ChildDashboardDto;
}

const TREND_CONFIG = {
  UP: { Icon: TrendingUpIcon, color: '#10b981', bg: '#ecfdf5' },
  DOWN: { Icon: TrendingDownIcon, color: '#ef4444', bg: '#fef2f2' },
  STABLE: { Icon: TrendingFlatIcon, color: '#64748b', bg: '#f1f5f9' },
} as const;

export default function ChildDashboardView({ data }: ChildDashboardViewProps) {
  const { t } = useTranslation('parent');

  const trend = TREND_CONFIG[data.scoreTrend] ?? TREND_CONFIG.STABLE;
  const { Icon: TrendIcon } = trend;
  const avgScore = Math.round(Number(data.averageScore));

  return (
    <Box>
      {/* KPI Row */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}>
          <KpiWidget
            label={t('averageScore')}
            value={`${avgScore}%`}
            icon={<BarChartIcon sx={{ color: '#3b82f6' }} />}
            iconBg="#eff6ff"
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <KpiWidget
            label={t('completedTests')}
            value={data.completedAssignments}
            icon={<CheckCircleOutlineIcon sx={{ color: '#10b981' }} />}
            iconBg="#ecfdf5"
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <KpiWidget
            label={t('pendingTests')}
            value={data.pendingAssignments}
            icon={<HourglassEmptyIcon sx={{ color: '#f59e0b' }} />}
            iconBg="#fffbeb"
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <KpiWidget
            label={t('totalAssignments')}
            value={data.totalAssignments}
            icon={<AssignmentIcon sx={{ color: '#8b5cf6' }} />}
            iconBg="#f5f3ff"
            sub={t(`scoreTrend.${data.scoreTrend}`)}
          />
        </Grid>
      </Grid>

      {/* Trend banner */}
      <Paper
        elevation={0}
        sx={{
          p: 2,
          mb: 3,
          display: 'flex',
          alignItems: 'center',
          gap: 1.5,
          bgcolor: trend.bg,
          border: '1px solid',
          borderColor: `${trend.color}40`,
          borderRadius: 2,
        }}
      >
        <Box
          sx={{
            width: 40,
            height: 40,
            borderRadius: 1.5,
            bgcolor: `${trend.color}20`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <TrendIcon sx={{ color: trend.color }} />
        </Box>
        <Box>
          <Typography variant="body2" fontWeight={700} sx={{ color: trend.color }}>
            {t(`scoreTrend.${data.scoreTrend}`)}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {t('performance')}
          </Typography>
        </Box>
      </Paper>

      <Grid container spacing={3}>
        {/* Weekly Activity */}
        {data.weeklyActivity && (
          <Grid item xs={12} sm={6} md={4}>
            <Paper sx={{ p: 2.5, height: '100%' }}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 2 }}>
                {t('weeklyActivity')}
              </Typography>
              <Stack spacing={1.5}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2" color="text.secondary">{t('testsToday')}</Typography>
                  <Typography variant="body2" fontWeight={700}>{data.weeklyActivity.testsCompletedToday}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2" color="text.secondary">{t('testsThisWeek')}</Typography>
                  <Typography variant="body2" fontWeight={700}>{data.weeklyActivity.testsCompletedThisWeek}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2" color="text.secondary">{t('testsThisMonth')}</Typography>
                  <Typography variant="body2" fontWeight={700}>{data.weeklyActivity.testsCompletedThisMonth}</Typography>
                </Box>
                <Divider />
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2" color="text.secondary">{t('avgThisWeek')}</Typography>
                  <Typography variant="body2" fontWeight={700}>
                    {Math.round(Number(data.weeklyActivity.averageScoreThisWeek))}%
                  </Typography>
                </Box>
                {data.weeklyActivity.totalTimeSpentMinutesToday > 0 && (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2" color="text.secondary">{t('timeSpentToday')}</Typography>
                    <Typography variant="body2" fontWeight={700}>
                      {data.weeklyActivity.totalTimeSpentMinutesToday} {t('minutesShort')}
                    </Typography>
                  </Box>
                )}
              </Stack>
            </Paper>
          </Grid>
        )}

        {/* Subject Breakdown */}
        <Grid item xs={12} sm={6} md={data.weeklyActivity ? 4 : 6}>
          <Paper sx={{ p: 2.5, height: '100%' }}>
            <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 2 }}>
              {t('subjects')}
            </Typography>
            {data.subjectBreakdown.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                {t('noRecentTests')}
              </Typography>
            ) : (
              data.subjectBreakdown.map((score) => (
                <SubjectScoreBar key={score.subjectName} subject={score} />
              ))
            )}
          </Paper>
        </Grid>

        {/* Recent Attempts */}
        <Grid item xs={12} md={data.weeklyActivity ? 4 : 6}>
          <Paper sx={{ p: 2.5, height: '100%' }}>
            <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 2 }}>
              {t('recentTests')}
            </Typography>
            <RecentAttemptsTable attempts={data.recentAttempts} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Grid,
  CircularProgress,
  Chip,
  Alert,
  LinearProgress,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Divider,
} from '@mui/material';
import BarChartIcon from '@mui/icons-material/BarChart';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import TrendingDownIcon from '@mui/icons-material/TrendingDown';
import QuizIcon from '@mui/icons-material/Quiz';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import StarIcon from '@mui/icons-material/Star';
import WarningIcon from '@mui/icons-material/Warning';
import { analyticsApi } from '@/api/analyticsApi';
import PageBreadcrumbs from '@/components/PageBreadcrumbs';

export default function StudentStatisticsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['student-analytics-me'],
    queryFn: () => analyticsApi.getMyAnalytics().then(r => r.data.data),
    staleTime: 60_000,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError || !data) {
    return (
      <Box>
        <PageBreadcrumbs items={[{ label: 'Statistika' }]} />
        <Alert severity="info" sx={{ mt: 2 }}>
          Statistika hali mavjud emas. Test ishlab ko'ring!
        </Alert>
      </Box>
    );
  }

  const scoreColor = data.overallAverage >= 80 ? 'success' : data.overallAverage >= 60 ? 'warning' : 'error';

  return (
    <Box>
      <PageBreadcrumbs items={[{ label: 'Mening Statistikam' }]} />

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <BarChartIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Box>
          <Typography variant="h5" fontWeight={700}>
            Mening Statistikam
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {data.firstName} {data.lastName} — umumiy ko'rsatkichlar
          </Typography>
        </Box>
      </Box>

      {/* Summary Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="O'rtacha ball"
            value={`${data.overallAverage.toFixed(1)}%`}
            color={scoreColor === 'success' ? '#2e7d32' : scoreColor === 'warning' ? '#e65100' : '#c62828'}
            icon={<TrendingUpIcon />}
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="Jami urinishlar"
            value={data.totalAttempts.toString()}
            color="#1565c0"
            icon={<QuizIcon />}
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="Topshirishlar"
            value={data.totalAssignments.toString()}
            color="#4a148c"
            icon={<CheckCircleIcon />}
          />
        </Grid>
        <Grid item xs={6} sm={3}>
          <StatCard
            title="Bajarish darajasi"
            value={`${(data.completionRate * 100).toFixed(0)}%`}
            color="#00695c"
            icon={<StarIcon />}
          />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        {/* Subject Breakdown */}
        {data.subjectBreakdown && data.subjectBreakdown.length > 0 && (
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3, height: '100%' }}>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
                Fanlar bo'yicha natijalar
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {data.subjectBreakdown.map(subj => (
                  <Box key={subj.subjectId}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                      <Typography variant="body2" fontWeight={600}>{subj.subjectName}</Typography>
                      <Typography variant="body2" fontWeight={700} color={
                        subj.averageScore >= 80 ? 'success.main' :
                          subj.averageScore >= 60 ? 'warning.main' : 'error.main'
                      }>
                        {subj.averageScore.toFixed(1)}%
                      </Typography>
                    </Box>
                    <LinearProgress
                      variant="determinate"
                      value={Math.min(subj.averageScore, 100)}
                      color={subj.averageScore >= 80 ? 'success' : subj.averageScore >= 60 ? 'warning' : 'error'}
                      sx={{ borderRadius: 4, height: 8 }}
                    />
                    <Typography variant="caption" color="text.secondary">
                      {subj.totalAttempts} ta urinish
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          </Grid>
        )}

        {/* Weak Areas */}
        {data.weakAreas && data.weakAreas.length > 0 && (
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <WarningIcon color="error" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>
                  Kuchsiz mavzular
                </Typography>
              </Box>
              <List dense>
                {data.weakAreas.slice(0, 6).map((area, idx) => (
                  <Box key={area.topicId}>
                    <ListItem disablePadding sx={{ py: 0.5 }}>
                      <ListItemAvatar sx={{ minWidth: 40 }}>
                        <Avatar sx={{ width: 28, height: 28, bgcolor: 'error.light', fontSize: '0.75rem' }}>
                          {idx + 1}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={area.topicName}
                        secondary={`${area.subjectName} — ${area.averageScore.toFixed(0)}%`}
                        primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }}
                        secondaryTypographyProps={{ variant: 'caption' }}
                      />
                      <Chip
                        label={`${area.averageScore.toFixed(0)}%`}
                        size="small"
                        color="error"
                        sx={{ minWidth: 48 }}
                      />
                    </ListItem>
                    {idx < data.weakAreas.length - 1 && <Divider component="li" />}
                  </Box>
                ))}
              </List>
            </Paper>
          </Grid>
        )}

        {/* Strong Areas */}
        {data.strongAreas && data.strongAreas.length > 0 && (
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
                <StarIcon color="success" fontSize="small" />
                <Typography variant="subtitle1" fontWeight={700}>
                  Kuchli mavzular
                </Typography>
              </Box>
              <List dense>
                {data.strongAreas.slice(0, 6).map((area, idx) => (
                  <Box key={area.topicId}>
                    <ListItem disablePadding sx={{ py: 0.5 }}>
                      <ListItemAvatar sx={{ minWidth: 40 }}>
                        <Avatar sx={{ width: 28, height: 28, bgcolor: 'success.light', fontSize: '0.75rem' }}>
                          {idx + 1}
                        </Avatar>
                      </ListItemAvatar>
                      <ListItemText
                        primary={area.topicName}
                        secondary={`${area.subjectName} — ${area.averageScore.toFixed(0)}%`}
                        primaryTypographyProps={{ variant: 'body2', fontWeight: 600 }}
                        secondaryTypographyProps={{ variant: 'caption' }}
                      />
                      <Chip
                        label={`${area.averageScore.toFixed(0)}%`}
                        size="small"
                        color="success"
                        sx={{ minWidth: 48 }}
                      />
                    </ListItem>
                    {idx < data.strongAreas.length - 1 && <Divider component="li" />}
                  </Box>
                ))}
              </List>
            </Paper>
          </Grid>
        )}

        {/* Score Trend */}
        {data.scoreTrend && data.scoreTrend.length > 0 && (
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
                Ball tendensiyasi
              </Typography>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                {data.scoreTrend.slice(-10).map((point, idx) => (
                  <Box key={idx} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ minWidth: 80 }}>
                      {point.date}
                    </Typography>
                    <Box sx={{ flex: 1 }}>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(point.value, 100)}
                        color={point.value >= 80 ? 'success' : point.value >= 60 ? 'warning' : 'error'}
                        sx={{ borderRadius: 4, height: 10 }}
                      />
                    </Box>
                    <Typography variant="caption" fontWeight={700} sx={{ minWidth: 36 }}>
                      {point.value.toFixed(0)}%
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          </Grid>
        )}
      </Grid>
    </Box>
  );
}

function StatCard({ title, value, color, icon }: {
  title: string;
  value: string;
  color: string;
  icon: React.ReactNode;
}) {
  return (
    <Paper
      sx={{
        p: 2.5,
        textAlign: 'center',
        border: '2px solid',
        borderColor: color,
        borderRadius: 2,
      }}
    >
      <Avatar sx={{ bgcolor: color, mx: 'auto', mb: 1, width: 40, height: 40 }}>
        {icon}
      </Avatar>
      <Typography variant="h5" fontWeight={800} sx={{ color, lineHeight: 1 }}>
        {value}
      </Typography>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }}>
        {title}
      </Typography>
    </Paper>
  );
}

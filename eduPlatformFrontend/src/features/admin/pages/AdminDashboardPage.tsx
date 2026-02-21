import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  Grid,
  Paper,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  LinearProgress,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import TopicIcon from '@mui/icons-material/Topic';
import QuizIcon from '@mui/icons-material/Quiz';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import AssignmentIcon from '@mui/icons-material/Assignment';
import DownloadIcon from '@mui/icons-material/Download';
import {
  useDashboardStats,
  useTrendData,
  useContentStats,
  useSystemInfo,
} from '../hooks/useAdminDashboard';
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';
import StatCard from '../components/StatCard';
import MiniBarChart from '../components/MiniBarChart';
import TrendChart from '../components/TrendChart';

export default function AdminDashboardPage() {
  const { t } = useTranslation('admin');
  const { user } = useAuth();

  const { data: stats, isLoading: statsLoading } = useDashboardStats();
  const { data: trends, isLoading: trendsLoading } = useTrendData();
  const { data: content } = useContentStats();
  const { data: systemInfo } = useSystemInfo();

  const isSuperAdmin = user?.role === Role.SUPER_ADMIN;

  if (statsLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>{t('dashboard.title')}</Typography>
        <Typography variant="body2" color="text.secondary">{t('dashboard.subtitle')}</Typography>
      </Box>

      {/* Stat Cards */}
      {stats && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.totalUsers')} value={stats.totalUsers} icon={<PeopleIcon />} color="#1976d2" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.newUsersThisWeek')} value={stats.newUsersThisWeek} icon={<PersonAddIcon />} color="#2e7d32" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.totalSubjects')} value={stats.totalSubjects} icon={<MenuBookIcon />} color="#ed6c02" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.totalTopics')} value={stats.totalTopics} icon={<TopicIcon />} color="#9c27b0" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.totalQuestions')} value={stats.totalQuestions} icon={<QuizIcon />} color="#0288d1" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.pendingQuestions')} value={stats.pendingQuestions} icon={<PendingActionsIcon />} color="#f44336" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.totalTests')} value={stats.totalTests} icon={<AssignmentIcon />} color="#00796b" />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard title={t('dashboard.totalDownloads')} value={stats.totalDownloads} icon={<DownloadIcon />} color="#5d4037" />
          </Grid>
        </Grid>
      )}

      {/* Trends */}
      {trends && !trendsLoading && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2.5 }}>
              <TrendChart data={trends.weeklyRegistrations} title={t('trends.weeklyRegistrations')} color="#1976d2" />
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2.5 }}>
              <TrendChart data={trends.weeklyTestCreations} title={t('trends.weeklyTestCreations')} color="#2e7d32" />
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2.5 }}>
              <TrendChart data={trends.dailyActiveUsers} title={t('trends.dailyActiveUsers')} color="#ed6c02" />
            </Paper>
          </Grid>
        </Grid>
      )}

      {/* Charts Row */}
      {stats && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {/* Users by Role */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2.5 }}>
              <MiniBarChart
                title={t('usersByRole.title')}
                data={[
                  { label: t('usersByRole.teachers'), value: stats.usersByRole.teachers, color: '#1976d2' },
                  { label: t('usersByRole.students'), value: stats.usersByRole.students, color: '#2e7d32' },
                  { label: t('usersByRole.moderators'), value: stats.usersByRole.moderators, color: '#ed6c02' },
                  { label: t('usersByRole.admins'), value: stats.usersByRole.admins, color: '#9c27b0' },
                  { label: t('usersByRole.parents'), value: stats.usersByRole.parents, color: '#0288d1' },
                ]}
              />
            </Paper>
          </Grid>

          {/* Questions by Difficulty */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2.5 }}>
              <MiniBarChart
                title={t('questionsByDifficulty.title')}
                data={[
                  { label: t('questionsByDifficulty.easy'), value: stats.questionsByDifficulty.easy, color: '#4caf50' },
                  { label: t('questionsByDifficulty.medium'), value: stats.questionsByDifficulty.medium, color: '#ff9800' },
                  { label: t('questionsByDifficulty.hard'), value: stats.questionsByDifficulty.hard, color: '#f44336' },
                ]}
              />
            </Paper>
          </Grid>

          {/* Users by Status */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2.5 }}>
              <MiniBarChart
                title={t('usersByStatus.title')}
                data={[
                  { label: t('usersByStatus.active'), value: stats.usersByStatus.active, color: '#4caf50' },
                  { label: t('usersByStatus.inactive'), value: stats.usersByStatus.inactive, color: '#9e9e9e' },
                  { label: t('usersByStatus.blocked'), value: stats.usersByStatus.blocked, color: '#f44336' },
                  { label: t('usersByStatus.pendingVerification'), value: stats.usersByStatus.pendingVerification, color: '#ff9800' },
                ]}
              />
            </Paper>
          </Grid>
        </Grid>
      )}

      {/* Content Stats Table */}
      {content && content.subjectStats.length > 0 && (
        <Paper sx={{ p: 2.5, mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>{t('contentStats.subjectStats')}</Typography>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('contentStats.subject')}</TableCell>
                  <TableCell align="right">{t('contentStats.topics')}</TableCell>
                  <TableCell align="right">{t('contentStats.questions')}</TableCell>
                  <TableCell align="right">{t('contentStats.tests')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {content.subjectStats.slice(0, 10).map((s) => (
                  <TableRow key={s.subjectId}>
                    <TableCell>{s.subjectName}</TableCell>
                    <TableCell align="right">{s.topicCount}</TableCell>
                    <TableCell align="right">{s.questionCount}</TableCell>
                    <TableCell align="right">{s.testCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}

      {/* Top Teachers */}
      {content && content.topTeachers.length > 0 && (
        <Paper sx={{ p: 2.5, mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>{t('contentStats.topTeachers')}</Typography>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('contentStats.teacher')}</TableCell>
                  <TableCell>{t('contentStats.email')}</TableCell>
                  <TableCell align="right">{t('contentStats.questionsCreated')}</TableCell>
                  <TableCell align="right">{t('contentStats.subjectsCount')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {content.topTeachers.map((teacher) => (
                  <TableRow key={teacher.userId}>
                    <TableCell>{teacher.firstName} {teacher.lastName}</TableCell>
                    <TableCell>{teacher.email}</TableCell>
                    <TableCell align="right">{teacher.questionCount}</TableCell>
                    <TableCell align="right">{teacher.subjectCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}

      {/* Recent Activity */}
      {stats && stats.recentActivity.length > 0 && (
        <Paper sx={{ p: 2.5, mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>{t('recentActivity.title')}</Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {stats.recentActivity.map((activity) => (
              <Box key={activity.id} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Chip size="small" label={activity.actionCategory} color="primary" variant="outlined" />
                <Typography variant="body2">{activity.action}</Typography>
                <Chip size="small" label={activity.entityType} variant="outlined" />
                <Box sx={{ flex: 1 }} />
                <Typography variant="caption" color="text.secondary">
                  {new Date(activity.createdAt).toLocaleString()}
                </Typography>
              </Box>
            ))}
          </Box>
        </Paper>
      )}

      {/* System Info - Super Admin only */}
      {isSuperAdmin && systemInfo && (
        <Paper sx={{ p: 2.5 }}>
          <Typography variant="subtitle2" gutterBottom>{t('systemInfo.title')}</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">{t('systemInfo.jvm')}</Typography>
              <Box sx={{ mt: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2">{t('systemInfo.usedMemory')}</Typography>
                  <Typography variant="body2">{systemInfo.jvm.usedMemoryMb} / {systemInfo.jvm.maxMemoryMb} MB</Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={(systemInfo.jvm.usedMemoryMb / systemInfo.jvm.maxMemoryMb) * 100}
                  sx={{ mb: 1, height: 8, borderRadius: 4 }}
                />
                <Typography variant="caption" color="text.secondary">
                  {t('systemInfo.javaVersion')}: {systemInfo.jvm.javaVersion} | {t('systemInfo.processors')}: {systemInfo.jvm.availableProcessors}
                </Typography>
              </Box>
            </Grid>
            <Grid item xs={12} md={6}>
              <Typography variant="caption" color="text.secondary">{t('systemInfo.database')}</Typography>
              <Box sx={{ mt: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2">{t('systemInfo.activeConnections')}</Typography>
                  <Typography variant="body2">{systemInfo.database.activeConnections} / {systemInfo.database.maxPoolSize}</Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={(systemInfo.database.activeConnections / systemInfo.database.maxPoolSize) * 100}
                  sx={{ mb: 1, height: 8, borderRadius: 4 }}
                />
                <Typography variant="caption" color="text.secondary">
                  {t('systemInfo.uptime')}: {systemInfo.uptime} | {t('systemInfo.serverTime')}: {systemInfo.serverTime}
                </Typography>
              </Box>
            </Grid>
          </Grid>
        </Paper>
      )}
    </Box>
  );
}

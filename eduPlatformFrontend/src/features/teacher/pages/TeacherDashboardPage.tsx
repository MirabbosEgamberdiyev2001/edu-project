import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Grid,
  Button,
  Skeleton,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Chip,
  Avatar,
  Divider,
  Alert,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import GroupsIcon from '@mui/icons-material/Groups';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import AddIcon from '@mui/icons-material/Add';
import QuizIcon from '@mui/icons-material/Quiz';
import AssignmentIcon from '@mui/icons-material/Assignment';
import BarChartIcon from '@mui/icons-material/BarChart';
import SchoolIcon from '@mui/icons-material/School';
import { KpiWidget, PageShell } from '@/components/ui';
import { useTeacherDashboard } from '@/features/analytics/hooks/useTeacherDashboard';
import { useAuth } from '@/hooks/useAuth';
import { formatPercent } from '@/utils/formatters';

export default function TeacherDashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useTranslation(['common', 'analytics']);
  const { data, isLoading } = useTeacherDashboard();

  const kpis = [
    {
      label: t('analytics:totalStudents'),
      value: data?.totalStudents ?? '-',
      icon: <PeopleIcon sx={{ color: '#2563eb' }} />,
      iconBg: '#eff6ff',
    },
    {
      label: t('analytics:totalGroups'),
      value: data?.totalGroups ?? '-',
      icon: <GroupsIcon sx={{ color: '#16a34a' }} />,
      iconBg: '#f0fdf4',
    },
    {
      label: t('analytics:totalAssignments'),
      value: data?.totalAssignments ?? '-',
      icon: <AssignmentTurnedInIcon sx={{ color: '#d97706' }} />,
      iconBg: '#fffbeb',
    },
    {
      label: t('analytics:avgScore'),
      value: data ? formatPercent(data.averageScore) : '-',
      icon: <TrendingUpIcon sx={{ color: '#7c3aed' }} />,
      iconBg: '#f5f3ff',
    },
  ];

  const quickActions = [
    { label: t('common:tests'), icon: <QuizIcon />, path: '/tests', color: '#0288d1' },
    { label: t('common:assignments'), icon: <AssignmentIcon />, path: '/assignments', color: '#7b1fa2' },
    { label: t('common:groups'), icon: <GroupsIcon />, path: '/groups', color: '#2e7d32' },
    { label: t('common:analytics'), icon: <BarChartIcon />, path: '/analytics/teacher', color: '#c62828' },
    { label: t('common:attestation'), icon: <SchoolIcon />, path: '/attestation', color: '#00796b' },
  ];

  return (
    <PageShell
      title={`${t('common:dashboard')}`}
      subtitle={user ? `${user.firstName} ${user.lastName}` : undefined}
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/tests/generate')}
        >
          {t('common:newTest')}
        </Button>
      }
    >
      {/* KPI Row */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {kpis.map((kpi) => (
          <Grid item xs={12} sm={6} md={3} key={kpi.label}>
            <KpiWidget
              label={kpi.label}
              value={kpi.value}
              icon={kpi.icon}
              iconBg={kpi.iconBg}
              loading={isLoading}
            />
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        {/* Quick Actions */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2.5 }}>
            <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
              {t('common:quickActions')}
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {quickActions.map((action) => (
                <Button
                  key={action.path}
                  variant="outlined"
                  fullWidth
                  startIcon={
                    <Box sx={{ color: action.color, display: 'flex' }}>{action.icon}</Box>
                  }
                  onClick={() => navigate(action.path)}
                  sx={{
                    justifyContent: 'flex-start',
                    borderColor: '#e2e8f0',
                    color: 'text.primary',
                    fontWeight: 500,
                    '&:hover': { borderColor: action.color, color: action.color },
                  }}
                >
                  {action.label}
                </Button>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* At-Risk Students */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight={600}>
                {t('analytics:atRiskStudents')}
              </Typography>
              <Button
                size="small"
                onClick={() => navigate('/analytics/teacher')}
                sx={{ fontSize: '0.8125rem' }}
              >
                {t('common:viewAll')}
              </Button>
            </Box>

            {isLoading ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} height={48} variant="rectangular" sx={{ borderRadius: 1 }} />
                ))}
              </Box>
            ) : !data?.atRiskStudents?.length ? (
              <Alert severity="success" sx={{ borderRadius: 1 }}>
                {t('analytics:noAtRiskStudents')}
              </Alert>
            ) : (
              <List disablePadding>
                {data.atRiskStudents.slice(0, 6).map((student, i) => (
                  <Box key={student.studentId}>
                    {i > 0 && <Divider />}
                    <ListItem sx={{ px: 0, py: 1 }}>
                      <Avatar
                        sx={{
                          width: 36,
                          height: 36,
                          mr: 1.5,
                          fontSize: '0.875rem',
                          bgcolor: '#fee2e2',
                          color: '#ef4444',
                        }}
                      >
                        {student.firstName?.[0] ?? '?'}
                      </Avatar>
                      <ListItemText
                        primary={
                          <Typography variant="body2" fontWeight={500}>
                            {student.firstName} {student.lastName}
                          </Typography>
                        }
                        secondary={
                          <Typography variant="caption" color="text.secondary">
                            {t('analytics:missedAssignments')}: {student.missedAssignments}
                          </Typography>
                        }
                      />
                      <ListItemSecondaryAction>
                        <Chip
                          label={formatPercent(student.averageScore)}
                          size="small"
                          color={student.averageScore < 40 ? 'error' : 'warning'}
                        />
                      </ListItemSecondaryAction>
                    </ListItem>
                  </Box>
                ))}
              </List>
            )}
          </Paper>
        </Grid>
      </Grid>
    </PageShell>
  );
}

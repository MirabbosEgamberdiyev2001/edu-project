import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Button,
  Card,
  CardContent,
  CardActions,
  Chip,
  CircularProgress,
  Avatar,
  Stack,
  Divider,
  Alert,
} from '@mui/material';
import QuizIcon from '@mui/icons-material/Quiz';
import AssignmentIcon from '@mui/icons-material/Assignment';
import GroupsIcon from '@mui/icons-material/Groups';
import BarChartIcon from '@mui/icons-material/BarChart';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StarIcon from '@mui/icons-material/Star';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import PublicIcon from '@mui/icons-material/Public';
import { analyticsApi } from '@/api/analyticsApi';
import { globalTestApi } from '@/api/globalTestApi';
import { groupApi } from '@/api/groupApi';
import { useAuth } from '@/hooks/useAuth';
import { TestCategory } from '@/types/test';

const CATEGORY_LABELS: Record<string, string> = {
  DTM: '🎓 DTM',
  SCHOOL: '🏫 Maktab',
  OLYMPIAD: '🏆 Olimpiada',
  CERTIFICATE: '📜 Sertifikat',
  ATTESTATSIYA: '📋 Attestatsiya',
};

export default function StudentDashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const { data: analytics } = useQuery({
    queryKey: ['student-analytics-me'],
    queryFn: () => analyticsApi.getMyAnalytics().then(r => r.data.data),
    staleTime: 60_000,
  });

  const { data: globalTestsData } = useQuery({
    queryKey: ['global-tests-recent'],
    queryFn: () => globalTestApi.getAll({ page: 0, size: 6 }).then(r => r.data.data),
    staleTime: 30_000,
  });

  const { data: groupsData } = useQuery({
    queryKey: ['my-groups-student'],
    queryFn: () => groupApi.getMyGroups({ size: 10 }).then(r => r.data.data),
    staleTime: 60_000,
  });

  const recentTests = globalTestsData?.content?.slice(0, 3) || [];
  const myGroups = groupsData?.content || [];
  const totalGroups = groupsData?.totalElements || 0;
  const scoreColor = analytics?.overallAverage
    ? analytics.overallAverage >= 80 ? '#2e7d32' : analytics.overallAverage >= 60 ? '#e65100' : '#c62828'
    : '#1565c0';

  return (
    <Box>
      {/* Welcome Header */}
      <Paper
        sx={{
          p: 3,
          mb: 3,
          background: 'linear-gradient(135deg, #1565c0 0%, #0288d1 100%)',
          color: 'white',
          borderRadius: 3,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar sx={{ width: 56, height: 56, bgcolor: 'rgba(255,255,255,0.3)', fontSize: '1.4rem', fontWeight: 700 }}>
            {user?.firstName?.[0]?.toUpperCase()}{user?.lastName?.[0]?.toUpperCase()}
          </Avatar>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h5" fontWeight={700}>
              Xush kelibsiz, {user?.firstName}!
            </Typography>
            <Typography variant="body2" sx={{ opacity: 0.85 }}>
              Bugun ham test ishlashni unutmang
            </Typography>
          </Box>
          <Chip label="O'quvchi" sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white' }} />
        </Box>
      </Paper>

      {/* Quick Stats */}
      {analytics && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 2, textAlign: 'center', border: `2px solid ${scoreColor}`, borderRadius: 2 }}>
              <Typography variant="h4" fontWeight={800} sx={{ color: scoreColor }}>
                {analytics.overallAverage.toFixed(0)}%
              </Typography>
              <Typography variant="caption" color="text.secondary">O'rtacha ball</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 2, textAlign: 'center', border: '2px solid #1565c0', borderRadius: 2 }}>
              <Typography variant="h4" fontWeight={800} color="primary">
                {analytics.totalAttempts}
              </Typography>
              <Typography variant="caption" color="text.secondary">Jami urinish</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 2, textAlign: 'center', border: '2px solid #4a148c', borderRadius: 2 }}>
              <Typography variant="h4" fontWeight={800} sx={{ color: '#4a148c' }}>
                {analytics.totalAssignments}
              </Typography>
              <Typography variant="caption" color="text.secondary">Topshiriq</Typography>
            </Paper>
          </Grid>
          <Grid item xs={6} sm={3}>
            <Paper sx={{ p: 2, textAlign: 'center', border: '2px solid #00695c', borderRadius: 2 }}>
              <Typography variant="h4" fontWeight={800} sx={{ color: '#00695c' }}>
                {(analytics.completionRate * 100).toFixed(0)}%
              </Typography>
              <Typography variant="caption" color="text.secondary">Bajarish</Typography>
            </Paper>
          </Grid>
        </Grid>
      )}

      {/* Main Navigation Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <NavCard
            title="Mening Testlarim"
            desc="O'qituvchi bergan testlar"
            icon={<AssignmentIcon sx={{ fontSize: 36, color: '#1565c0' }} />}
            onClick={() => navigate('/my-tests')}
            color="#1565c0"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <NavCard
            title="Global Testlar"
            desc="Barcha tasdiqlangan testlar"
            icon={<PublicIcon sx={{ fontSize: 36, color: '#2e7d32' }} />}
            onClick={() => navigate('/global-tests')}
            color="#2e7d32"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <NavCard
            title="Mening Guruhlarim"
            desc={`${totalGroups} ta guruh`}
            icon={<GroupsIcon sx={{ fontSize: 36, color: '#6a1b9a' }} />}
            onClick={() => navigate('/my-groups')}
            color="#6a1b9a"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <NavCard
            title="Statistika"
            desc="O'z natijalarim"
            icon={<BarChartIcon sx={{ fontSize: 36, color: '#c62828' }} />}
            onClick={() => navigate('/student-statistics')}
            color="#c62828"
          />
        </Grid>
      </Grid>

      <Grid container spacing={2}>
        {/* Recent Global Tests */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle1" fontWeight={700}>
                Yangi Global Testlar
              </Typography>
              <Button
                size="small"
                endIcon={<ArrowForwardIcon />}
                onClick={() => navigate('/global-tests')}
              >
                Barchasi
              </Button>
            </Box>
            {recentTests.length === 0 ? (
              <Alert severity="info" variant="outlined">
                Hozircha global testlar mavjud emas
              </Alert>
            ) : (
              <Stack spacing={1.5}>
                {recentTests.map(test => (
                  <Box key={test.id}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="body2" fontWeight={600}>{test.title}</Typography>
                        <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5 }}>
                          {test.category && (
                            <Chip label={CATEGORY_LABELS[test.category] || test.category} size="small" />
                          )}
                          <Chip label={`${test.questionCount} savol`} size="small" variant="outlined" />
                          <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
                            {test.subjectName}
                          </Typography>
                        </Box>
                      </Box>
                      <Button
                        variant="outlined"
                        size="small"
                        startIcon={<PlayArrowIcon />}
                        onClick={() => navigate('/global-tests')}
                        sx={{ flexShrink: 0 }}
                      >
                        Boshlash
                      </Button>
                    </Box>
                    <Divider sx={{ mt: 1.5 }} />
                  </Box>
                ))}
              </Stack>
            )}
          </Paper>
        </Grid>

        {/* My Groups + Weak Areas */}
        <Grid item xs={12} md={5}>
          <Stack spacing={2}>
            {/* My Groups */}
            <Paper sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="subtitle1" fontWeight={700}>
                  Guruhlarim
                </Typography>
                <Button size="small" endIcon={<ArrowForwardIcon />} onClick={() => navigate('/my-groups')}>
                  Ko'rish
                </Button>
              </Box>
              {myGroups.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  Guruhga qo'shilmagan
                </Typography>
              ) : (
                <Stack spacing={1}>
                  {myGroups.slice(0, 3).map(group => (
                    <Box key={group.id} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                      <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: '0.85rem' }}>
                        {group.name[0]}
                      </Avatar>
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="body2" fontWeight={600}>{group.name}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {group.teacherName} • {group.memberCount} o'quvchi
                        </Typography>
                      </Box>
                      <Chip
                        label={group.status === 'ACTIVE' ? 'Faol' : 'Arxiv'}
                        size="small"
                        color={group.status === 'ACTIVE' ? 'success' : 'default'}
                      />
                    </Box>
                  ))}
                </Stack>
              )}
            </Paper>

            {/* Categories */}
            <Paper sx={{ p: 3 }}>
              <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
                Kategoriyalar bo'yicha testlar
              </Typography>
              <Stack spacing={1}>
                {Object.values(TestCategory).map(cat => (
                  <Button
                    key={cat}
                    variant="outlined"
                    fullWidth
                    size="small"
                    onClick={() => navigate(`/global-tests?category=${cat}`)}
                    sx={{ justifyContent: 'flex-start', fontWeight: 600 }}
                  >
                    {CATEGORY_LABELS[cat] || cat}
                  </Button>
                ))}
              </Stack>
            </Paper>
          </Stack>
        </Grid>
      </Grid>
    </Box>
  );
}

function NavCard({ title, desc, icon, onClick, color }: {
  title: string;
  desc: string;
  icon: React.ReactNode;
  onClick: () => void;
  color: string;
}) {
  return (
    <Paper
      onClick={onClick}
      sx={{
        p: 2.5,
        cursor: 'pointer',
        border: `2px solid transparent`,
        transition: 'all 0.2s',
        '&:hover': {
          boxShadow: 4,
          borderColor: color,
          transform: 'translateY(-2px)',
        },
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        {icon}
        <Box>
          <Typography variant="subtitle2" fontWeight={700}>{title}</Typography>
          <Typography variant="caption" color="text.secondary">{desc}</Typography>
        </Box>
      </Box>
    </Paper>
  );
}

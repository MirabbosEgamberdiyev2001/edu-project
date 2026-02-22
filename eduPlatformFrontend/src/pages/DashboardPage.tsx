import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Button,
  Avatar,
  Chip,
} from '@mui/material';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import QuizIcon from '@mui/icons-material/Quiz';
import AssignmentIcon from '@mui/icons-material/Assignment';
import PersonIcon from '@mui/icons-material/Person';
import RateReviewIcon from '@mui/icons-material/RateReview';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import GroupsIcon from '@mui/icons-material/Groups';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import BarChartIcon from '@mui/icons-material/BarChart';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import LinkIcon from '@mui/icons-material/Link';
import ListAltIcon from '@mui/icons-material/ListAlt';
import GradingIcon from '@mui/icons-material/Grading';
import PeopleIcon from '@mui/icons-material/People';
import HistoryIcon from '@mui/icons-material/History';
import { useAuth } from '@/hooks/useAuth';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { Role } from '@/types/user';

interface CardDef {
  label: string;
  icon: React.ReactNode;
  path: string;
}

export default function DashboardPage() {
  const { t } = useTranslation(['profile', 'common', 'admin']);
  const { user } = useAuth();
  const navigate = useNavigate();
  useCurrentUser();

  const cards = useMemo((): CardDef[] => {
    if (!user) return [];

    switch (user.role) {
      case Role.TEACHER:
        return [
          { label: t('common:subjects'), icon: <MenuBookIcon sx={{ fontSize: 40, color: '#ed6c02' }} />, path: '/subjects' },
          { label: t('common:questions'), icon: <QuizIcon sx={{ fontSize: 40, color: '#0288d1' }} />, path: '/questions' },
          { label: t('common:tests'), icon: <AssignmentIcon sx={{ fontSize: 40, color: '#00796b' }} />, path: '/tests' },
          { label: t('common:groups'), icon: <GroupsIcon sx={{ fontSize: 40, color: '#5d4037' }} />, path: '/groups' },
          { label: t('common:assignments'), icon: <AssignmentTurnedInIcon sx={{ fontSize: 40, color: '#7b1fa2' }} />, path: '/assignments' },
          { label: t('common:analytics'), icon: <BarChartIcon sx={{ fontSize: 40, color: '#1565c0' }} />, path: '/analytics/teacher' },
        ];

      case Role.STUDENT:
        return [
          { label: t('common:myTests'), icon: <ListAltIcon sx={{ fontSize: 40, color: '#0288d1' }} />, path: '/my-tests' },
          { label: t('common:myAttempts'), icon: <GradingIcon sx={{ fontSize: 40, color: '#00796b' }} />, path: '/my-attempts' },
          { label: t('common:analytics'), icon: <BarChartIcon sx={{ fontSize: 40, color: '#1565c0' }} />, path: '/analytics/student' },
          { label: t('common:pairing'), icon: <LinkIcon sx={{ fontSize: 40, color: '#ed6c02' }} />, path: '/pairing' },
        ];

      case Role.PARENT:
        return [
          { label: t('common:myChildren'), icon: <FamilyRestroomIcon sx={{ fontSize: 40, color: '#7b1fa2' }} />, path: '/my-children' },
        ];

      case Role.MODERATOR:
        return [
          { label: t('common:questions'), icon: <QuizIcon sx={{ fontSize: 40, color: '#0288d1' }} />, path: '/questions' },
          { label: t('admin:nav.moderation'), icon: <RateReviewIcon sx={{ fontSize: 40, color: '#9c27b0' }} />, path: '/admin/moderation' },
        ];

      case Role.ADMIN:
      case Role.SUPER_ADMIN:
        return [
          { label: t('admin:nav.dashboard'), icon: <AdminPanelSettingsIcon sx={{ fontSize: 40, color: '#1976d2' }} />, path: '/admin' },
          { label: t('admin:nav.users'), icon: <PeopleIcon sx={{ fontSize: 40, color: '#5d4037' }} />, path: '/admin/users' },
          { label: t('admin:nav.moderation'), icon: <RateReviewIcon sx={{ fontSize: 40, color: '#9c27b0' }} />, path: '/admin/moderation' },
          { label: t('admin:nav.auditLog'), icon: <HistoryIcon sx={{ fontSize: 40, color: '#00796b' }} />, path: '/admin/audit-logs' },
        ];

      default:
        return [
          { label: t('common:profile'), icon: <PersonIcon sx={{ fontSize: 40, color: '#5d4037' }} />, path: '/profile' },
        ];
    }
  }, [user, t]);

  return (
    <Box>
      {/* Welcome Section */}
      <Paper sx={{ p: 3, mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Avatar
          sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontSize: '1.25rem' }}
        >
          {user?.firstName?.[0]?.toUpperCase()}{user?.lastName?.[0]?.toUpperCase()}
        </Avatar>
        <Box sx={{ flex: 1 }}>
          <Typography variant="h5" fontWeight={700}>
            {t('profile:welcomeBack', { name: user?.firstName })}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
            <Chip label={user?.role} size="small" color="primary" variant="outlined" />
            <Typography variant="body2" color="text.secondary">
              {user?.email || user?.phone}
            </Typography>
          </Box>
        </Box>
        <Button variant="outlined" size="small" onClick={() => navigate('/profile')}>
          {t('profile:editProfile')}
        </Button>
      </Paper>

      {/* Quick Navigation */}
      <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('profile:quickStats')}</Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {cards.map((card) => (
          <Grid item xs={12} sm={6} md={4} key={card.path}>
            <NavCard
              title={card.label}
              icon={card.icon}
              onClick={() => navigate(card.path)}
            />
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

function NavCard({ title, icon, onClick }: { title: string; icon: React.ReactNode; onClick: () => void }) {
  return (
    <Paper
      sx={{
        p: 2.5,
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        cursor: 'pointer',
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 4 },
      }}
      onClick={onClick}
    >
      {icon}
      <Typography variant="subtitle1" fontWeight={600}>{title}</Typography>
    </Paper>
  );
}

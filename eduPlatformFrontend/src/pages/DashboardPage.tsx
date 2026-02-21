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
import { useAuth } from '@/hooks/useAuth';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { Role } from '@/types/user';

export default function DashboardPage() {
  const { t } = useTranslation('profile');
  const { t: tc } = useTranslation('common');
  const { user } = useAuth();
  const navigate = useNavigate();
  useCurrentUser();

  const isModerator = user?.role === Role.MODERATOR || user?.role === Role.ADMIN || user?.role === Role.SUPER_ADMIN;
  const isAdmin = user?.role === Role.ADMIN || user?.role === Role.SUPER_ADMIN;

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
            {t('welcomeBack', { name: user?.firstName })}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
            <Chip label={user?.role} size="small" color="primary" variant="outlined" />
            <Typography variant="body2" color="text.secondary">
              {user?.email || user?.phone}
            </Typography>
          </Box>
        </Box>
        <Button variant="outlined" size="small" onClick={() => navigate('/profile')}>
          {t('editProfile')}
        </Button>
      </Paper>

      {/* Quick Navigation */}
      <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('quickStats')}</Typography>
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={4}>
          <NavCard
            title={tc('subjects')}
            icon={<MenuBookIcon sx={{ fontSize: 40, color: '#ed6c02' }} />}
            onClick={() => navigate('/subjects')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <NavCard
            title={tc('questions')}
            icon={<QuizIcon sx={{ fontSize: 40, color: '#0288d1' }} />}
            onClick={() => navigate('/questions')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <NavCard
            title={tc('tests')}
            icon={<AssignmentIcon sx={{ fontSize: 40, color: '#00796b' }} />}
            onClick={() => navigate('/tests')}
          />
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <NavCard
            title={tc('profile')}
            icon={<PersonIcon sx={{ fontSize: 40, color: '#5d4037' }} />}
            onClick={() => navigate('/profile')}
          />
        </Grid>

        {/* Moderator/Admin cards */}
        {isModerator && (
          <Grid item xs={12} sm={6} md={4}>
            <NavCard
              title={t('admin:nav.moderation', 'Moderation')}
              icon={<RateReviewIcon sx={{ fontSize: 40, color: '#9c27b0' }} />}
              onClick={() => navigate('/admin/moderation')}
            />
          </Grid>
        )}
        {isAdmin && (
          <Grid item xs={12} sm={6} md={4}>
            <NavCard
              title={t('admin:nav.dashboard', 'Admin Panel')}
              icon={<AdminPanelSettingsIcon sx={{ fontSize: 40, color: '#1976d2' }} />}
              onClick={() => navigate('/admin')}
            />
          </Grid>
        )}
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

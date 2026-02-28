import { useState, useMemo } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Menu,
  MenuItem,
  Divider,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import TopicIcon from '@mui/icons-material/Topic';
import QuizIcon from '@mui/icons-material/Quiz';
import AssignmentIcon from '@mui/icons-material/Assignment';
import PersonIcon from '@mui/icons-material/Person';
import LockIcon from '@mui/icons-material/Lock';
import LogoutIcon from '@mui/icons-material/Logout';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import PeopleIcon from '@mui/icons-material/People';
import HistoryIcon from '@mui/icons-material/History';
import RateReviewIcon from '@mui/icons-material/RateReview';
import GroupsIcon from '@mui/icons-material/Groups';
import AssignmentTurnedInIcon from '@mui/icons-material/AssignmentTurnedIn';
import BarChartIcon from '@mui/icons-material/BarChart';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import LinkIcon from '@mui/icons-material/Link';
import ListAltIcon from '@mui/icons-material/ListAlt';
import GradingIcon from '@mui/icons-material/Grading';
import SchoolIcon from '@mui/icons-material/School';
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';
import { authApi } from '@/api/authApi';
import { storage } from '@/lib/storage';
import Logo from './Logo';
import LanguageSwitcher from '@/features/auth/components/LanguageSwitcher';

const DRAWER_WIDTH = 260;

interface NavItem {
  label: string;
  icon: React.ReactNode;
  path: string;
}

function getMenuItems(role: Role, t: (key: string) => string): (NavItem | 'divider')[] {
  const items: (NavItem | 'divider')[] = [];

  // Dashboard - all roles
  items.push({ label: t('common:dashboard'), icon: <DashboardIcon />, path: '/dashboard' });

  switch (role) {
    case Role.TEACHER:
      items.push({ label: t('common:subjects'), icon: <MenuBookIcon />, path: '/subjects' });
      items.push({ label: t('common:topics'), icon: <TopicIcon />, path: '/topics' });
      items.push({ label: t('common:questions'), icon: <QuizIcon />, path: '/questions' });
      items.push({ label: t('common:tests'), icon: <AssignmentIcon />, path: '/tests' });
      items.push({ label: t('common:groups'), icon: <GroupsIcon />, path: '/groups' });
      items.push({ label: t('common:assignments'), icon: <AssignmentTurnedInIcon />, path: '/assignments' });
      items.push({ label: t('common:analytics'), icon: <BarChartIcon />, path: '/analytics/teacher' });
      items.push({ label: t('common:attestation'), icon: <SchoolIcon />, path: '/attestation' });
      break;

    case Role.STUDENT:
      items.push({ label: t('common:myTests'), icon: <ListAltIcon />, path: '/my-tests' });
      items.push({ label: t('common:globalTests'), icon: <SchoolIcon />, path: '/global-tests' });
      items.push({ label: t('common:myGroups'), icon: <GroupsIcon />, path: '/my-groups' });
      items.push({ label: t('common:myAttempts'), icon: <GradingIcon />, path: '/my-attempts' });
      items.push({ label: t('common:analytics'), icon: <BarChartIcon />, path: '/analytics/student' });
      items.push({ label: t('common:pairing'), icon: <LinkIcon />, path: '/pairing' });
      break;

    case Role.PARENT:
      items.push({ label: t('common:myChildren'), icon: <FamilyRestroomIcon />, path: '/my-children' });
      break;

    case Role.MODERATOR:
      items.push({ label: t('common:questions'), icon: <QuizIcon />, path: '/questions' });
      items.push('divider');
      items.push({ label: t('admin:nav.moderation'), icon: <RateReviewIcon />, path: '/admin/moderation' });
      break;

    case Role.ADMIN:
    case Role.SUPER_ADMIN:
      items.push({ label: t('common:subjects'), icon: <MenuBookIcon />, path: '/subjects' });
      items.push({ label: t('common:topics'), icon: <TopicIcon />, path: '/topics' });
      items.push({ label: t('common:questions'), icon: <QuizIcon />, path: '/questions' });
      items.push({ label: t('common:tests'), icon: <AssignmentIcon />, path: '/tests' });
      items.push({ label: t('common:groups'), icon: <GroupsIcon />, path: '/groups' });
      items.push({ label: t('common:assignments'), icon: <AssignmentTurnedInIcon />, path: '/assignments' });
      items.push('divider');
      items.push({ label: t('admin:nav.dashboard'), icon: <AdminPanelSettingsIcon />, path: '/admin' });
      items.push({ label: t('admin:nav.users'), icon: <PeopleIcon />, path: '/admin/users' });
      items.push({ label: t('admin:nav.auditLog'), icon: <HistoryIcon />, path: '/admin/audit-logs' });
      items.push({ label: t('admin:nav.moderation'), icon: <RateReviewIcon />, path: '/admin/moderation' });
      break;
  }

  // Profile & Settings - all roles
  items.push('divider');
  items.push({ label: t('common:profile'), icon: <PersonIcon />, path: '/profile' });
  items.push({ label: t('common:settings'), icon: <LockIcon />, path: '/settings/change-password' });

  return items;
}

export default function AppLayout() {
  const { t } = useTranslation(['common', 'admin']);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const menuItems = useMemo(
    () => (user?.role ? getMenuItems(user.role, t) : []),
    [user?.role, t]
  );

  const handleLogout = async () => {
    try {
      const refreshToken = storage.getRefreshToken();
      if (refreshToken) {
        await authApi.logout({ refreshToken });
      }
    } finally {
      logout();
      navigate('/auth/login', { replace: true });
    }
  };

  const drawer = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Toolbar>
        <Logo size="small" />
      </Toolbar>
      <Divider />

      {/* User info */}
      <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main', fontSize: '0.875rem', flexShrink: 0 }}>
          {user?.firstName?.[0]?.toUpperCase()}{user?.lastName?.[0]?.toUpperCase()}
        </Avatar>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" fontWeight={600} noWrap>
            {user?.firstName} {user?.lastName}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {user?.role?.replace(/_/g, ' ')}
          </Typography>
        </Box>
      </Box>
      <Divider />

      {/* Nav items */}
      <List disablePadding sx={{ flexGrow: 1, overflowY: 'auto', py: 1 }}>
        {menuItems.map((item, index) => {
          if (item === 'divider') {
            return <Divider key={`divider-${index}`} sx={{ my: 0.5, mx: 2 }} />;
          }
          const isSelected =
            location.pathname === item.path ||
            (item.path !== '/dashboard' && location.pathname.startsWith(item.path + '/'));
          return (
            <ListItemButton
              key={item.path}
              selected={isSelected}
              onClick={() => {
                navigate(item.path);
                setMobileOpen(false);
              }}
              sx={{
                mx: 1,
                borderRadius: 1,
                mb: 0.25,
                '&.Mui-selected': {
                  bgcolor: '#eff6ff',
                  color: 'primary.main',
                  '& .MuiListItemIcon-root': { color: 'primary.main' },
                  '&:hover': { bgcolor: '#dbeafe' },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 36 }}>{item.icon}</ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{ variant: 'body2', fontWeight: isSelected ? 600 : 400 }}
              />
            </ListItemButton>
          );
        })}
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
          bgcolor: 'white',
          color: 'text.primary',
          boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
        }}
      >
        <Toolbar>
          <IconButton
            edge="start"
            onClick={() => setMobileOpen(!mobileOpen)}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Box sx={{ flexGrow: 1 }} />
          <LanguageSwitcher />
          <IconButton onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ ml: 2 }}>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: '0.875rem' }}>
              {user?.firstName?.[0]?.toUpperCase() || user?.lastName?.[0]?.toUpperCase() || '?'}
            </Avatar>
          </IconButton>
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={() => setAnchorEl(null)}
          >
            <MenuItem disabled>
              <Typography variant="body2">
                {user?.firstName} {user?.lastName}
              </Typography>
            </MenuItem>
            <Divider />
            <MenuItem onClick={handleLogout}>
              <ListItemIcon><LogoutIcon fontSize="small" /></ListItemIcon>
              {t('common:logout')}
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: DRAWER_WIDTH },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: 8,
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}

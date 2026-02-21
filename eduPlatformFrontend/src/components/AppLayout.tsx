import { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
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
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';
import { authApi } from '@/api/authApi';
import { storage } from '@/lib/storage';
import Logo from './Logo';
import LanguageSwitcher from '@/features/auth/components/LanguageSwitcher';

const DRAWER_WIDTH = 260;

export default function AppLayout() {
  const { t } = useTranslation('common');
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

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
    <Box>
      <Toolbar>
        <Logo size="small" />
      </Toolbar>
      <Divider />
      <List>
        <ListItemButton onClick={() => navigate('/dashboard')}>
          <ListItemIcon><DashboardIcon /></ListItemIcon>
          <ListItemText primary={t('dashboard')} />
        </ListItemButton>
        <ListItemButton onClick={() => navigate('/subjects')}>
          <ListItemIcon><MenuBookIcon /></ListItemIcon>
          <ListItemText primary={t('subjects')} />
        </ListItemButton>
        <ListItemButton onClick={() => navigate('/topics')}>
          <ListItemIcon><TopicIcon /></ListItemIcon>
          <ListItemText primary={t('topics')} />
        </ListItemButton>
        <ListItemButton onClick={() => navigate('/questions')}>
          <ListItemIcon><QuizIcon /></ListItemIcon>
          <ListItemText primary={t('questions')} />
        </ListItemButton>
        <ListItemButton onClick={() => navigate('/tests')}>
          <ListItemIcon><AssignmentIcon /></ListItemIcon>
          <ListItemText primary={t('tests')} />
        </ListItemButton>
        <ListItemButton onClick={() => navigate('/profile')}>
          <ListItemIcon><PersonIcon /></ListItemIcon>
          <ListItemText primary={t('profile')} />
        </ListItemButton>
        <ListItemButton onClick={() => navigate('/settings/change-password')}>
          <ListItemIcon><LockIcon /></ListItemIcon>
          <ListItemText primary={t('settings')} />
        </ListItemButton>
      </List>

      {/* Moderation - visible for MODERATOR/ADMIN/SUPER_ADMIN */}
      {(user?.role === Role.MODERATOR || user?.role === Role.ADMIN || user?.role === Role.SUPER_ADMIN) && (
        <>
          <Divider />
          <List>
            <ListItemButton onClick={() => navigate('/admin/moderation')}>
              <ListItemIcon><RateReviewIcon /></ListItemIcon>
              <ListItemText primary={t('admin:nav.moderation')} />
            </ListItemButton>
          </List>
        </>
      )}

      {/* Admin navigation - visible only for ADMIN/SUPER_ADMIN */}
      {(user?.role === Role.ADMIN || user?.role === Role.SUPER_ADMIN) && (
        <>
          <Divider />
          <List>
            <ListItemButton onClick={() => navigate('/admin')}>
              <ListItemIcon><AdminPanelSettingsIcon /></ListItemIcon>
              <ListItemText primary={t('admin:nav.dashboard')} />
            </ListItemButton>
            <ListItemButton onClick={() => navigate('/admin/users')}>
              <ListItemIcon><PeopleIcon /></ListItemIcon>
              <ListItemText primary={t('admin:nav.users')} />
            </ListItemButton>
            <ListItemButton onClick={() => navigate('/admin/audit-logs')}>
              <ListItemIcon><HistoryIcon /></ListItemIcon>
              <ListItemText primary={t('admin:nav.auditLog')} />
            </ListItemButton>
          </List>
        </>
      )}
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
              {t('logout')}
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

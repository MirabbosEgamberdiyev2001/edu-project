import { Box, Paper, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import SchoolIcon from '@mui/icons-material/School';
import AutoStoriesIcon from '@mui/icons-material/AutoStories';
import GroupsIcon from '@mui/icons-material/Groups';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import Logo from '@/components/Logo';
import LanguageSwitcher from './LanguageSwitcher';

interface AuthLayoutProps {
  children: ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  const { t } = useTranslation('auth');

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', bgcolor: 'background.default' }}>
      {/* Left branding panel */}
      <Box
        sx={{
          display: { xs: 'none', md: 'flex' },
          flex: 1,
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          background: (theme) =>
            `linear-gradient(145deg, ${theme.palette.primary.dark} 0%, ${theme.palette.primary.main} 50%, ${theme.palette.primary.light} 100%)`,
          color: 'white',
          p: 6,
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Decorative circles */}
        <Box sx={{
          position: 'absolute', top: -80, right: -80,
          width: 300, height: 300, borderRadius: '50%',
          background: 'rgba(255,255,255,0.06)',
        }} />
        <Box sx={{
          position: 'absolute', bottom: -120, left: -60,
          width: 400, height: 400, borderRadius: '50%',
          background: 'rgba(255,255,255,0.04)',
        }} />
        <Box sx={{
          position: 'absolute', top: '40%', left: -40,
          width: 200, height: 200, borderRadius: '50%',
          background: 'rgba(255,255,255,0.03)',
        }} />

        <Box sx={{ position: 'relative', zIndex: 1, textAlign: 'center', maxWidth: 420 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1.5, mb: 4 }}>
            <SchoolIcon sx={{ fontSize: 42 }} />
            <Typography variant="h4" fontWeight={800} letterSpacing="-0.02em">
              EduPlatform
            </Typography>
          </Box>

          <Typography variant="h5" fontWeight={600} sx={{ mb: 2, lineHeight: 1.4 }}>
            {t('branding.welcome')}
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.85, lineHeight: 1.7, mb: 6 }}>
            {t('branding.tagline')}
          </Typography>

          {/* Feature icons */}
          <Box sx={{ display: 'flex', justifyContent: 'center', gap: 4 }}>
            {[
              { icon: <AutoStoriesIcon />, label: t('branding.lessons'), key: 'lessons' },
              { icon: <GroupsIcon />, label: t('branding.team'), key: 'team' },
              { icon: <TrendingUpIcon />, label: t('branding.results'), key: 'results' },
            ].map((item) => (
              <Box key={item.key} sx={{ textAlign: 'center' }}>
                <Box sx={{
                  width: 52, height: 52, borderRadius: 3,
                  bgcolor: 'rgba(255,255,255,0.15)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  mx: 'auto', mb: 1,
                  backdropFilter: 'blur(10px)',
                  transition: 'transform 0.2s ease',
                  '&:hover': { transform: 'translateY(-2px)' },
                }}>
                  {item.icon}
                </Box>
                <Typography variant="caption" sx={{ opacity: 0.8, fontWeight: 500 }}>
                  {item.label}
                </Typography>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>

      {/* Right form panel */}
      <Box
        sx={{
          flex: { xs: 1, md: '0 0 540px' },
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 2, sm: 4 },
          position: 'relative',
          bgcolor: 'background.paper',
        }}
      >
        <Box sx={{ position: 'absolute', top: 20, right: 20 }}>
          <LanguageSwitcher />
        </Box>

        <Box sx={{ display: { xs: 'block', md: 'none' }, mb: 4 }}>
          <Logo size="medium" />
        </Box>

        <Paper
          elevation={0}
          sx={{
            width: '100%',
            maxWidth: 440,
            p: { xs: 3, sm: 4 },
          }}
        >
          {children}
        </Paper>
      </Box>
    </Box>
  );
}

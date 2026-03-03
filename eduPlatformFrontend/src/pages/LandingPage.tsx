import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Container,
  Divider,
  Grid,
  Stack,
  Tab,
  Tabs,
  Typography,
  Avatar,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import SecurityIcon from '@mui/icons-material/Security';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import LanguageIcon from '@mui/icons-material/Language';
import PhoneAndroidIcon from '@mui/icons-material/PhoneAndroid';
import AssignmentIcon from '@mui/icons-material/Assignment';
import GroupsIcon from '@mui/icons-material/Groups';
import BarChartIcon from '@mui/icons-material/BarChart';
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart';
import PublicIcon from '@mui/icons-material/Public';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import PeopleIcon from '@mui/icons-material/People';
import FormatQuoteIcon from '@mui/icons-material/FormatQuote';
import Logo from '@/components/Logo';
import RoleSelectModal from '@/components/RoleSelectModal';
import LanguageSwitcher from '@/features/auth/components/LanguageSwitcher';

// ─────────────────────────────────────────────────────────────────────────────
// Feature tab panel helper
// ─────────────────────────────────────────────────────────────────────────────
interface FeatureCard { icon: React.ReactNode; titleKey: string; descKey: string; }

function FeatureTabPanel({
  features,
  titleKey,
  subtitleKey,
  onCta,
  ns,
}: {
  features: FeatureCard[];
  titleKey: string;
  subtitleKey: string;
  onCta: () => void;
  ns: string;
}) {
  const { t } = useTranslation(ns);
  return (
    <Box sx={{ pt: 4 }}>
      <Typography variant="h5" fontWeight={700} gutterBottom>
        {t(titleKey)}
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        {t(subtitleKey)}
      </Typography>
      <Grid container spacing={3}>
        {features.map((f, i) => (
          <Grid item xs={12} sm={6} key={i}>
            <Card
              elevation={0}
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 3,
                height: '100%',
                transition: 'box-shadow .2s',
                '&:hover': { boxShadow: 3 },
              }}
            >
              <CardContent sx={{ p: 3 }}>
                <Box sx={{ color: 'primary.main', mb: 1.5 }}>{f.icon}</Box>
                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                  {t(f.titleKey)}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {t(f.descKey)}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      <Box sx={{ mt: 4 }}>
        <Button variant="contained" size="large" onClick={onCta} sx={{ borderRadius: 2, px: 4 }}>
          {t('nav.start')}
        </Button>
      </Box>
    </Box>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Main component
// ─────────────────────────────────────────────────────────────────────────────
export default function LandingPage() {
  const { t } = useTranslation('landing');
  const navigate = useNavigate();
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [featuresTab, setFeaturesTab] = useState(0);

  const openModal = () => setRoleModalOpen(true);
  const closeModal = () => setRoleModalOpen(false);

  const teacherFeatures: FeatureCard[] = [
    { icon: <AssignmentIcon />, titleKey: 'features.teacher.tests.title', descKey: 'features.teacher.tests.description' },
    { icon: <GroupsIcon />, titleKey: 'features.teacher.groups.title', descKey: 'features.teacher.groups.description' },
    { icon: <MonitorHeartIcon />, titleKey: 'features.teacher.monitoring.title', descKey: 'features.teacher.monitoring.description' },
    { icon: <BarChartIcon />, titleKey: 'features.teacher.stats.title', descKey: 'features.teacher.stats.description' },
  ];
  const studentFeatures: FeatureCard[] = [
    { icon: <AssignmentIcon />, titleKey: 'features.student.tests.title', descKey: 'features.student.tests.description' },
    { icon: <PublicIcon />, titleKey: 'features.student.global.title', descKey: 'features.student.global.description' },
    { icon: <CheckCircleIcon />, titleKey: 'features.student.results.title', descKey: 'features.student.results.description' },
  ];
  const parentFeatures: FeatureCard[] = [
    { icon: <NotificationsActiveIcon />, titleKey: 'features.parent.monitor.title', descKey: 'features.parent.monitor.description' },
    { icon: <BarChartIcon />, titleKey: 'features.parent.reports.title', descKey: 'features.parent.reports.description' },
    { icon: <PeopleIcon />, titleKey: 'features.parent.multi.title', descKey: 'features.parent.multi.description' },
  ];

  const tabData = [
    { labelKey: 'features.tabs.teacher', features: teacherFeatures, titleKey: 'features.teacher.title', subtitleKey: 'features.teacher.subtitle' },
    { labelKey: 'features.tabs.student', features: studentFeatures, titleKey: 'features.student.title', subtitleKey: 'features.student.subtitle' },
    { labelKey: 'features.tabs.parent', features: parentFeatures, titleKey: 'features.parent.title', subtitleKey: 'features.parent.subtitle' },
  ];

  const howItWorksSteps = [
    { num: '01', titleKey: 'howItWorks.steps.register.title', descKey: 'howItWorks.steps.register.description' },
    { num: '02', titleKey: 'howItWorks.steps.create.title', descKey: 'howItWorks.steps.create.description' },
    { num: '03', titleKey: 'howItWorks.steps.analyze.title', descKey: 'howItWorks.steps.analyze.description' },
  ];

  const trustSignals = [
    { key: 'hero.trustSignals.jwt' },
    { key: 'hero.trustSignals.otp' },
    { key: 'hero.trustSignals.multilang' },
    { key: 'hero.trustSignals.free' },
  ];

  const securityItems = [
    { icon: <VerifiedUserIcon sx={{ fontSize: 36 }} />, titleKey: 'security.jwt', descKey: 'security.jwtDesc' },
    { icon: <SecurityIcon sx={{ fontSize: 36 }} />, titleKey: 'security.otp', descKey: 'security.otpDesc' },
    { icon: <MonitorHeartIcon sx={{ fontSize: 36 }} />, titleKey: 'security.monitoring', descKey: 'security.monitoringDesc' },
    { icon: <LanguageIcon sx={{ fontSize: 36 }} />, titleKey: 'security.multilang', descKey: 'security.multilangDesc' },
  ];

  const testimonials = [0, 1, 2];

  return (
    <Box>
      <RoleSelectModal open={roleModalOpen} onClose={closeModal} />

      {/* ── NAVBAR ─────────────────────────────────────────────────────────── */}
      <Box
        component="nav"
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 100,
          bgcolor: 'background.paper',
          borderBottom: '1px solid',
          borderColor: 'divider',
          py: 1.5,
        }}
      >
        <Container maxWidth="lg">
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Box sx={{ cursor: 'pointer' }} onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
              <Logo size="small" />
            </Box>
            <Stack direction="row" alignItems="center" spacing={1}>
              <LanguageSwitcher />
              <Button variant="text" size="small" onClick={() => navigate('/auth/login')}>
                {t('nav.login')}
              </Button>
              <Button variant="contained" size="small" onClick={openModal} sx={{ borderRadius: 2 }}>
                {t('nav.start')}
              </Button>
            </Stack>
          </Stack>
        </Container>
      </Box>

      {/* ── HERO ───────────────────────────────────────────────────────────── */}
      <Box
        sx={{
          background: 'linear-gradient(135deg, #1565c0 0%, #0288d1 100%)',
          color: 'white',
          py: { xs: 8, md: 14 },
          textAlign: 'center',
        }}
      >
        <Container maxWidth="md">
          <Chip
            label={t('hero.badge')}
            size="small"
            sx={{ mb: 3, bgcolor: 'rgba(255,255,255,.18)', color: 'white', fontWeight: 600, border: '1px solid rgba(255,255,255,.35)' }}
          />
          <Typography
            variant="h2"
            fontWeight={800}
            sx={{ mb: 3, fontSize: { xs: '2rem', md: '3rem' }, lineHeight: 1.2 }}
          >
            {t('hero.title')}
          </Typography>
          <Typography
            variant="h6"
            sx={{ mb: 5, opacity: 0.9, fontWeight: 400, maxWidth: 640, mx: 'auto', lineHeight: 1.7 }}
          >
            {t('hero.subtitle')}
          </Typography>
          <Button
            variant="contained"
            size="large"
            onClick={openModal}
            sx={{
              bgcolor: 'white',
              color: 'primary.main',
              fontWeight: 700,
              px: 5,
              py: 1.5,
              borderRadius: 3,
              fontSize: '1.05rem',
              boxShadow: '0 8px 24px rgba(0,0,0,.2)',
              '&:hover': { bgcolor: 'grey.100' },
            }}
          >
            {t('hero.cta')}
          </Button>
          <Stack
            direction="row"
            justifyContent="center"
            flexWrap="wrap"
            gap={1.5}
            sx={{ mt: 5 }}
          >
            {trustSignals.map((s) => (
              <Chip
                key={s.key}
                label={t(s.key)}
                size="small"
                sx={{ bgcolor: 'rgba(255,255,255,.15)', color: 'white', fontWeight: 500 }}
              />
            ))}
          </Stack>
        </Container>
      </Box>

      {/* ── HOW IT WORKS ───────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.default' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={700} textAlign="center" gutterBottom>
            {t('howItWorks.title')}
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 8, maxWidth: 520, mx: 'auto' }}>
            {t('howItWorks.subtitle')}
          </Typography>
          <Grid container spacing={4}>
            {howItWorksSteps.map((step) => (
              <Grid item xs={12} md={4} key={step.num}>
                <Stack alignItems="center" textAlign="center" spacing={2}>
                  <Avatar
                    sx={{ width: 64, height: 64, bgcolor: 'primary.main', fontSize: '1.4rem', fontWeight: 800 }}
                  >
                    {step.num}
                  </Avatar>
                  <Typography variant="h6" fontWeight={700}>
                    {t(step.titleKey)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 280 }}>
                    {t(step.descKey)}
                  </Typography>
                </Stack>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      <Divider />

      {/* ── UNIFIED FEATURES (TABS) ─────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.paper' }}>
        <Container maxWidth="lg">
          <Tabs
            value={featuresTab}
            onChange={(_, v) => setFeaturesTab(v)}
            centered
            sx={{ mb: 2, '& .MuiTab-root': { fontWeight: 600, fontSize: '1rem' } }}
          >
            {tabData.map((tab, i) => (
              <Tab key={i} label={t(tab.labelKey)} />
            ))}
          </Tabs>
          <Divider sx={{ mb: 2 }} />
          <FeatureTabPanel
            features={tabData[featuresTab].features}
            titleKey={tabData[featuresTab].titleKey}
            subtitleKey={tabData[featuresTab].subtitleKey}
            onCta={openModal}
            ns="landing"
          />
        </Container>
      </Box>

      {/* ── TESTIMONIALS ───────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.default' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={700} textAlign="center" gutterBottom>
            {t('testimonials.title')}
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 8 }}>
            {t('testimonials.subtitle')}
          </Typography>
          <Grid container spacing={4}>
            {testimonials.map((i) => (
              <Grid item xs={12} md={4} key={i}>
                <Card
                  elevation={0}
                  sx={{
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 3,
                    height: '100%',
                    p: 3,
                  }}
                >
                  <FormatQuoteIcon sx={{ color: 'primary.light', fontSize: 40, mb: 1 }} />
                  <Typography variant="body1" color="text.secondary" sx={{ mb: 3, lineHeight: 1.8 }}>
                    "{t(`testimonials.${i}.quote`)}"
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Typography variant="subtitle2" fontWeight={700}>
                    {t(`testimonials.${i}.name`)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {t(`testimonials.${i}.role`)}
                  </Typography>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── SECURITY ───────────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.paper' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={700} textAlign="center" gutterBottom>
            {t('security.title')}
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 8 }}>
            {t('security.subtitle')}
          </Typography>
          <Grid container spacing={4}>
            {securityItems.map((item) => (
              <Grid item xs={12} sm={6} md={3} key={item.titleKey}>
                <Stack alignItems="center" textAlign="center" spacing={1.5}>
                  <Box sx={{ color: 'primary.main' }}>{item.icon}</Box>
                  <Typography variant="subtitle1" fontWeight={700}>
                    {t(item.titleKey)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t(item.descKey)}
                  </Typography>
                </Stack>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      {/* ── SINGLE CTA ─────────────────────────────────────────────────────── */}
      <Box
        sx={{
          py: { xs: 8, md: 12 },
          background: 'linear-gradient(135deg, #1565c0 0%, #0288d1 100%)',
          textAlign: 'center',
          color: 'white',
        }}
      >
        <Container maxWidth="sm">
          <Typography variant="h4" fontWeight={800} gutterBottom>
            {t('ctaSection.title')}
          </Typography>
          <Typography variant="body1" sx={{ mb: 5, opacity: 0.9, lineHeight: 1.8 }}>
            {t('ctaSection.subtitle')}
          </Typography>
          <Button
            variant="contained"
            size="large"
            onClick={openModal}
            sx={{
              bgcolor: 'white',
              color: 'primary.main',
              fontWeight: 700,
              px: 5,
              py: 1.5,
              borderRadius: 3,
              fontSize: '1.05rem',
              boxShadow: '0 8px 24px rgba(0,0,0,.2)',
              '&:hover': { bgcolor: 'grey.100' },
            }}
          >
            {t('ctaSection.cta')}
          </Button>
        </Container>
      </Box>

      {/* ── FOOTER ─────────────────────────────────────────────────────────── */}
      <Box sx={{ bgcolor: '#111827', color: 'grey.400', py: 6 }}>
        <Container maxWidth="lg">
          <Grid container spacing={4} alignItems="flex-start">
            <Grid item xs={12} md={4}>
              <Stack spacing={1.5}>
                <Stack direction="row" alignItems="center" gap={1}>
                  <SchoolIcon sx={{ color: '#90caf9' }} />
                  <Typography variant="h6" fontWeight={700} sx={{ color: 'white' }}>
                    Test-Pro
                  </Typography>
                </Stack>
                <Typography variant="body2">{t('footer.tagline')}</Typography>
              </Stack>
            </Grid>

            <Grid item xs={12} sm={6} md={4}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'white', mb: 1.5 }}>
                {t('footer.contact')}
              </Typography>
              <Typography variant="body2">{t('footer.email')}</Typography>
            </Grid>

            <Grid item xs={12} sm={6} md={4}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'white', mb: 1.5 }}>
                &nbsp;
              </Typography>
              <Stack spacing={1}>
                <Button
                  size="small"
                  sx={{ color: 'grey.400', justifyContent: 'flex-start', p: 0, textTransform: 'none', minWidth: 'auto' }}
                  onClick={() => navigate('/privacy')}
                >
                  {t('footer.links.privacy')}
                </Button>
                <Button
                  size="small"
                  sx={{ color: 'grey.400', justifyContent: 'flex-start', p: 0, textTransform: 'none', minWidth: 'auto' }}
                  onClick={() => navigate('/terms')}
                >
                  {t('footer.links.terms')}
                </Button>
                <Button
                  size="small"
                  sx={{ color: 'grey.400', justifyContent: 'flex-start', p: 0, textTransform: 'none', minWidth: 'auto' }}
                  onClick={() => window.open('mailto:support@testpro.uz')}
                >
                  {t('footer.links.help')}
                </Button>
              </Stack>
            </Grid>
          </Grid>

          <Divider sx={{ borderColor: 'rgba(255,255,255,.1)', my: 4 }} />
          <Typography variant="body2" textAlign="center">
            {t('footer.copyright', { year: new Date().getFullYear() })}
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}

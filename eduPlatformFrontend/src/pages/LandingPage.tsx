import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Container,
  Divider,
  Grid,
  Skeleton,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
  Avatar,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import SecurityIcon from '@mui/icons-material/Security';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import LanguageIcon from '@mui/icons-material/Language';
import AssignmentIcon from '@mui/icons-material/Assignment';
import GroupsIcon from '@mui/icons-material/Groups';
import BarChartIcon from '@mui/icons-material/BarChart';
import MonitorHeartIcon from '@mui/icons-material/MonitorHeart';
import PublicIcon from '@mui/icons-material/Public';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import PeopleIcon from '@mui/icons-material/People';
import FormatQuoteIcon from '@mui/icons-material/FormatQuote';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import Logo from '@/components/Logo';
import RoleSelectModal from '@/components/RoleSelectModal';
import LanguageSwitcher from '@/features/auth/components/LanguageSwitcher';
import { publicApi } from '@/api/publicApi';

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
// Constants
// ─────────────────────────────────────────────────────────────────────────────
const TEST_CATEGORY_KEYS = ['dtm', 'school', 'olympiad', 'certificate', 'attestation'] as const;

// ─────────────────────────────────────────────────────────────────────────────
// Main component
// ─────────────────────────────────────────────────────────────────────────────
export default function LandingPage() {
  const { t, i18n } = useTranslation('landing');
  const navigate = useNavigate();
  const [roleModalOpen, setRoleModalOpen] = useState(false);
  const [featuresTab, setFeaturesTab] = useState(0);
  const [promoCode, setPromoCode] = useState('');
  const [promoError, setPromoError] = useState(false);
  const [promoNavigating, setPromoNavigating] = useState(false);

  // Public API queries (no auth required)
  const { data: statsData } = useQuery({
    queryKey: ['public', 'stats'],
    queryFn: () => publicApi.getStats().then((r) => r.data.data),
    staleTime: 10 * 60 * 1000,
  });

  const {
    data: subjects,
    isLoading: subjectsLoading,
    isError: subjectsError,
  } = useQuery({
    queryKey: ['public', 'subjects', i18n.language],
    queryFn: () => publicApi.getSubjects(i18n.language).then((r) => r.data.data),
    staleTime: 10 * 60 * 1000,
  });

  const openModal = () => setRoleModalOpen(true);
  const closeModal = () => setRoleModalOpen(false);

  const scrollToPromo = () => {
    document.getElementById('promo-widget')?.scrollIntoView({ behavior: 'smooth' });
  };

  const handlePromoSubmit = () => {
    const code = promoCode.trim().toUpperCase();
    if (!code) {
      setPromoError(true);
      return;
    }
    setPromoError(false);
    setPromoNavigating(true);
    navigate(`/promo/${code}`);
  };

  // ── Feature cards ────────────────────────────────────────────────────────
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

  // Platform stats — use real API data with sensible fallbacks
  const platformStatItems = [
    {
      value: statsData?.totalUsers != null ? `${statsData.totalUsers.toLocaleString()}+` : '5 000+',
      labelKey: 'platformStats.users',
    },
    {
      value: statsData?.totalAttempts != null ? `${statsData.totalAttempts.toLocaleString()}+` : '50 000+',
      labelKey: 'platformStats.attempts',
    },
    {
      value: statsData?.totalSubjects != null ? `${statsData.totalSubjects}+` : '30+',
      labelKey: 'platformStats.subjects',
    },
    {
      value: t('platformStats.free'),
      labelKey: 'platformStats.freeLabel',
    },
  ];

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

          {/* Primary + secondary CTA buttons */}
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            justifyContent="center"
            alignItems="center"
            sx={{ mb: 5 }}
          >
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
            <Button
              variant="outlined"
              size="large"
              onClick={scrollToPromo}
              startIcon={<LocalOfferIcon />}
              sx={{
                color: 'white',
                borderColor: 'rgba(255,255,255,.6)',
                fontWeight: 600,
                px: 4,
                py: 1.5,
                borderRadius: 3,
                fontSize: '1rem',
                '&:hover': { borderColor: 'white', bgcolor: 'rgba(255,255,255,.1)' },
              }}
            >
              {t('hero.promoCta')}
            </Button>
          </Stack>

          {/* Trust signal chips */}
          <Stack direction="row" justifyContent="center" flexWrap="wrap" gap={1.5}>
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

      {/* ── PLATFORM STATS ─────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 6, md: 10 }, bgcolor: 'background.paper' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={700} textAlign="center" gutterBottom>
            {t('platformStats.title')}
          </Typography>
          <Grid container spacing={3} sx={{ mt: 3 }} justifyContent="center">
            {platformStatItems.map((item, i) => (
              <Grid item xs={6} md={3} key={i}>
                <Stack alignItems="center" textAlign="center" spacing={1}>
                  <Typography
                    variant="h3"
                    fontWeight={800}
                    sx={{ color: 'primary.main', fontSize: { xs: '2rem', md: '2.8rem' } }}
                  >
                    {item.value}
                  </Typography>
                  <Typography variant="body1" color="text.secondary" fontWeight={500}>
                    {t(item.labelKey)}
                  </Typography>
                </Stack>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      <Divider />

      {/* ── TEST CATEGORIES ────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.default' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={700} textAlign="center" gutterBottom>
            {t('testCategories.title')}
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 8, maxWidth: 540, mx: 'auto' }}>
            {t('testCategories.subtitle')}
          </Typography>
          <Grid container spacing={3}>
            {TEST_CATEGORY_KEYS.map((catKey) => (
              <Grid item xs={12} sm={6} md={4} key={catKey}>
                <Card
                  elevation={0}
                  sx={{
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 3,
                    height: '100%',
                    transition: 'all .2s',
                    '&:hover': {
                      boxShadow: 4,
                      borderColor: 'primary.main',
                      transform: 'translateY(-2px)',
                    },
                  }}
                >
                  <CardContent sx={{ p: 3 }}>
                    <Typography variant="h2" sx={{ mb: 1.5, fontSize: '2.5rem', lineHeight: 1 }}>
                      {t(`testCategories.${catKey}.emoji`)}
                    </Typography>
                    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1.5 }}>
                      <Typography variant="h6" fontWeight={700}>
                        {t(`testCategories.${catKey}.label`)}
                      </Typography>
                      <Chip
                        label={t(`testCategories.${catKey}.hint`)}
                        size="small"
                        color="primary"
                        variant="outlined"
                        sx={{ fontSize: '0.7rem' }}
                      />
                    </Stack>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5, lineHeight: 1.7 }}>
                      {t(`testCategories.${catKey}.desc`)}
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
                      <Chip
                        label={`${t('testCategories.forLabel')}: ${t(`testCategories.${catKey}.for`)}`}
                        size="small"
                        sx={{ bgcolor: 'action.hover', fontSize: '0.72rem' }}
                      />
                      <Chip
                        label={`${t('testCategories.levelLabel')}: ${t(`testCategories.${catKey}.level`)}`}
                        size="small"
                        color="success"
                        variant="outlined"
                        sx={{ fontSize: '0.72rem' }}
                      />
                    </Stack>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
          <Box sx={{ textAlign: 'center', mt: 5 }}>
            <Button variant="outlined" size="large" onClick={openModal} sx={{ borderRadius: 2, px: 5 }}>
              {t('testCategories.startBtn')}
            </Button>
          </Box>
        </Container>
      </Box>

      {/* ── HOW IT WORKS ───────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.paper' }}>
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

      {/* ── SUBJECTS GALLERY ───────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.default' }}>
        <Container maxWidth="lg">
          <Typography variant="h4" fontWeight={700} textAlign="center" gutterBottom>
            {t('subjects.title')}
          </Typography>
          <Typography variant="body1" color="text.secondary" textAlign="center" sx={{ mb: 6, maxWidth: 500, mx: 'auto' }}>
            {t('subjects.subtitle')}
          </Typography>

          {subjectsLoading ? (
            <Grid container spacing={3}>
              {Array.from({ length: 12 }).map((_, i) => (
                <Grid item xs={12} sm={6} md={4} key={i}>
                  <Skeleton variant="rounded" height={164} sx={{ borderRadius: 3 }} />
                </Grid>
              ))}
            </Grid>
          ) : subjectsError ? (
            <Box textAlign="center" py={6}>
              <Typography color="error" variant="body1">
                {t('subjects.error')}
              </Typography>
            </Box>
          ) : (subjects ?? []).length === 0 ? (
            <Box textAlign="center" py={6}>
              <Typography color="text.secondary" variant="body1">
                {t('subjects.noSubjects')}
              </Typography>
            </Box>
          ) : (
            <Grid container spacing={3}>
              {(subjects ?? []).map((subject, i) => (
                <Grid item xs={12} sm={6} md={4} key={i}>
                  <Card
                    elevation={0}
                    onClick={openModal}
                    sx={{
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 3,
                      height: '100%',
                      cursor: 'pointer',
                      transition: 'all .2s ease',
                      '&:hover': {
                        boxShadow: 6,
                        borderColor: 'primary.main',
                        transform: 'translateY(-4px)',
                      },
                    }}
                  >
                    <CardContent sx={{ p: 3 }}>
                      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 1.5 }}>
                        <Avatar
                          sx={{
                            bgcolor: subject.color ?? 'primary.main',
                            width: 44,
                            height: 44,
                            fontSize: '1.3rem',
                            flexShrink: 0,
                          }}
                        >
                          {subject.icon ?? subject.name.charAt(0).toUpperCase()}
                        </Avatar>
                        <Typography
                          variant="subtitle1"
                          fontWeight={700}
                          sx={{ lineHeight: 1.3 }}
                        >
                          {subject.name}
                        </Typography>
                      </Stack>
                      {subject.description && (
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          sx={{
                            mb: 2,
                            lineHeight: 1.7,
                            display: '-webkit-box',
                            WebkitLineClamp: 3,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {subject.description}
                        </Typography>
                      )}
                      <Chip
                        label={`${subject.testCount ?? 0} ${t('subjects.tests')}`}
                        size="small"
                        color="primary"
                        variant="outlined"
                        sx={{ fontWeight: 600 }}
                      />
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}

          <Box sx={{ textAlign: 'center', mt: 5 }}>
            <Button variant="contained" onClick={openModal} sx={{ borderRadius: 2, px: 4 }}>
              {t('subjects.viewAll')}
            </Button>
          </Box>
        </Container>
      </Box>

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

      {/* ── PROMO CODE WIDGET ──────────────────────────────────────────────── */}
      <Box
        id="promo-widget"
        sx={{
          py: { xs: 8, md: 12 },
          bgcolor: 'background.default',
          borderTop: '1px solid',
          borderColor: 'divider',
        }}
      >
        <Container maxWidth="sm">
          <Card
            elevation={0}
            sx={{
              border: '2px solid',
              borderColor: 'primary.main',
              borderRadius: 4,
            }}
          >
            <CardContent sx={{ p: { xs: 3, md: 5 }, textAlign: 'center' }}>
              <Avatar
                sx={{ width: 56, height: 56, bgcolor: 'primary.main', mx: 'auto', mb: 2 }}
              >
                <LocalOfferIcon sx={{ fontSize: 28 }} />
              </Avatar>
              <Typography variant="h5" fontWeight={700} gutterBottom>
                {t('promoWidget.title')}
              </Typography>
              <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
                {t('promoWidget.subtitle')}
              </Typography>
              <Stack spacing={2}>
                <TextField
                  fullWidth
                  value={promoCode}
                  onChange={(e) => {
                    setPromoCode(e.target.value.toUpperCase());
                    setPromoError(false);
                  }}
                  onKeyDown={(e) => e.key === 'Enter' && handlePromoSubmit()}
                  placeholder={t('promoWidget.placeholder')}
                  error={promoError}
                  helperText={promoError ? t('promoWidget.invalid') : t('promoWidget.hint')}
                  inputProps={{
                    maxLength: 12,
                    style: {
                      textAlign: 'center',
                      fontSize: '1.1rem',
                      letterSpacing: '0.15em',
                      fontWeight: 700,
                    },
                  }}
                  sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
                />
                <Button
                  variant="contained"
                  size="large"
                  onClick={handlePromoSubmit}
                  disabled={promoNavigating}
                  sx={{ borderRadius: 2, py: 1.5, fontWeight: 700, fontSize: '1rem' }}
                >
                  {promoNavigating ? t('promoWidget.navigating') : t('promoWidget.submit')}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Container>
      </Box>

      {/* ── TESTIMONIALS ───────────────────────────────────────────────────── */}
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.paper' }}>
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
      <Box sx={{ py: { xs: 8, md: 12 }, bgcolor: 'background.default' }}>
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
                    {t('footer.appName')}
                  </Typography>
                </Stack>
                <Typography variant="body2">{t('footer.tagline')}</Typography>
              </Stack>
            </Grid>

            <Grid item xs={12} sm={6} md={4}>
              <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'white', mb: 1.5 }}>
                {t('footer.contact')}
              </Typography>
              <Stack spacing={0.75}>
                <Button
                  size="small"
                  component="a"
                  href={`mailto:${t('footer.email')}`}
                  sx={{ color: 'grey.400', justifyContent: 'flex-start', p: 0, textTransform: 'none', minWidth: 'auto' }}
                >
                  {t('footer.email')}
                </Button>
                <Typography variant="subtitle2" fontWeight={700} sx={{ color: 'white', mt: 1.5 }}>
                  {t('footer.social')}
                </Typography>
                <Button
                  size="small"
                  component="a"
                  href="https://instagram.com/testpro_uz"
                  target="_blank"
                  rel="noopener noreferrer"
                  sx={{ color: 'grey.400', justifyContent: 'flex-start', p: 0, textTransform: 'none', minWidth: 'auto' }}
                >
                  Instagram: {t('footer.instagram')}
                </Button>
                <Button
                  size="small"
                  component="a"
                  href="https://t.me/testpro_uz"
                  target="_blank"
                  rel="noopener noreferrer"
                  sx={{ color: 'grey.400', justifyContent: 'flex-start', p: 0, textTransform: 'none', minWidth: 'auto' }}
                >
                  Telegram: {t('footer.telegram')}
                </Button>
              </Stack>
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
                  onClick={() => window.open(`mailto:${t('footer.email')}`)}
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

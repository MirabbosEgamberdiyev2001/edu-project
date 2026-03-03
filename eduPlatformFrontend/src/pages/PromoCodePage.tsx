import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box, Container, Paper, Typography, TextField, Button,
  CircularProgress, Alert, Divider, Chip, Stack,
} from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import QuizIcon from '@mui/icons-material/Quiz';
import PersonIcon from '@mui/icons-material/Person';
import { useTranslation } from 'react-i18next';
import { publicPromoApi, type PublicPromoInfoDto } from '@/api/publicPromoApi';
import { userApi } from '@/api/userApi';
import { storage } from '@/lib/storage';
import { useAuthStore } from '@/stores/authStore';
import { ACCESS_TOKEN_KEY } from '@/config';

export default function PromoCodePage() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('assignment');
  const setAuth = useAuthStore((s) => s.setAuth);

  const [info, setInfo] = useState<PublicPromoInfoDto | null>(null);
  const [verifying, setVerifying] = useState(true);
  const [verifyError, setVerifyError] = useState<string | null>(null);

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [enrolling, setEnrolling] = useState(false);
  const [enrollError, setEnrollError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) return;
    setVerifying(true);
    setVerifyError(null);
    publicPromoApi.verifyCode(code)
      .then((res) => {
        setInfo(res.data.data);
      })
      .catch((err) => {
        const msg = err?.response?.data?.message || t('promoPage.invalidCode');
        setVerifyError(msg);
      })
      .finally(() => setVerifying(false));
  }, [code, t]);

  const handleEnroll = async () => {
    if (!code || !firstName.trim() || !lastName.trim() || !phone.trim()) return;
    setEnrolling(true);
    setEnrollError(null);
    try {
      const res = await publicPromoApi.enroll(code, {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: phone.trim(),
      });
      const { accessToken, assignmentId } = res.data.data;

      // Store the access token
      localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);

      // Fetch user profile with the new token
      const userRes = await userApi.getCurrentUser();
      setAuth(userRes.data.data);

      // Navigate to available assignments or start attempt
      navigate(`/my-assignments`);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const msg = axiosErr?.response?.data?.message || t('promoPage.invalidCode');
      setEnrollError(msg);
    } finally {
      setEnrolling(false);
    }
  };

  if (verifying) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50', display: 'flex', alignItems: 'center' }}>
      <Container maxWidth="sm" sx={{ py: 4 }}>
        <Paper sx={{ p: 4 }}>
          <Box sx={{ textAlign: 'center', mb: 3 }}>
            <AssignmentIcon color="primary" sx={{ fontSize: 48, mb: 1 }} />
            <Typography variant="h5" fontWeight={700}>
              {t('promoPage.title')}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('promoPage.subtitle')}
            </Typography>
          </Box>

          {verifyError ? (
            <Alert severity="error" sx={{ mb: 2 }}>{verifyError}</Alert>
          ) : info ? (
            <>
              {/* Assignment Info Card */}
              <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'primary.50' }}>
                <Typography variant="subtitle2" color="primary" fontWeight={700} sx={{ mb: 1.5 }}>
                  {t('promoPage.assignmentInfo')}
                </Typography>
                <Typography variant="h6" fontWeight={600} sx={{ mb: 1 }}>
                  {info.assignmentTitle}
                </Typography>
                {info.testTitle && info.testTitle !== info.assignmentTitle && (
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                    {info.testTitle}
                  </Typography>
                )}
                <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
                  {info.questionCount && (
                    <Chip
                      icon={<QuizIcon />}
                      label={`${info.questionCount} ${t('questions', { ns: 'common', defaultValue: 'savol' })}`}
                      size="small"
                      variant="outlined"
                    />
                  )}
                  <Chip
                    icon={<AccessTimeIcon />}
                    label={`${info.durationMinutes} ${t('minutesShort')}`}
                    size="small"
                    variant="outlined"
                  />
                  {info.teacherFullName && (
                    <Chip
                      icon={<PersonIcon />}
                      label={info.teacherFullName}
                      size="small"
                      variant="outlined"
                    />
                  )}
                </Stack>
              </Paper>

              <Divider sx={{ mb: 3 }} />

              {/* Enrollment Form */}
              <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                {t('promoPage.enterDetails')}
              </Typography>

              <Stack spacing={2}>
                <TextField
                  label={t('promoPage.firstName')}
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  fullWidth
                  size="small"
                  required
                />
                <TextField
                  label={t('promoPage.lastName')}
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  fullWidth
                  size="small"
                  required
                />
                <TextField
                  label={t('promoPage.phone')}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  fullWidth
                  size="small"
                  required
                  placeholder="+998901234567"
                  type="tel"
                />

                {enrollError && (
                  <Alert severity="error">{enrollError}</Alert>
                )}

                <Button
                  variant="contained"
                  size="large"
                  fullWidth
                  onClick={handleEnroll}
                  disabled={enrolling || !firstName.trim() || !lastName.trim() || !phone.trim()}
                  startIcon={enrolling ? <CircularProgress size={20} color="inherit" /> : undefined}
                >
                  {enrolling ? '...' : t('promoPage.startTest')}
                </Button>
              </Stack>
            </>
          ) : null}
        </Paper>
      </Container>
    </Box>
  );
}

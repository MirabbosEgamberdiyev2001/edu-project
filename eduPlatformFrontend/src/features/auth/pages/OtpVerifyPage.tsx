import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Box, Button, Typography, CircularProgress, Alert, Fade } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import MarkEmailReadOutlinedIcon from '@mui/icons-material/MarkEmailReadOutlined';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import { useTranslation } from 'react-i18next';
import { useMutation } from '@tanstack/react-query';
import AuthLayout from '../components/AuthLayout';
import OtpInput from '../components/OtpInput';
import OtpCountdown from '../components/OtpCountdown';
import { useOtpVerify } from '../hooks/useOtpVerify';
import { authApi } from '@/api/authApi';
import { useToast } from '@/hooks/useToast';
import { OTP_LENGTH } from '@/config';
import type { RegisterRequest, ForgotPasswordRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

interface OtpState {
  identifier: string;
  purpose: 'REGISTER' | 'PASSWORD_RESET';
  registerData?: RegisterRequest;
  forgotPasswordData?: ForgotPasswordRequest;
}

export default function OtpVerifyPage() {
  const { t } = useTranslation('auth');
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const routerState = location.state as OtpState | null;
  const toast = useToast();
  const autoVerifyAttempted = useRef(false);

  // Merge: prefer router state, fallback to URL query params (from email link)
  const state = useMemo<OtpState | null>(() => {
    if (routerState?.identifier && routerState?.purpose) {
      return routerState;
    }
    const qIdentifier = searchParams.get('identifier');
    const qPurpose = searchParams.get('purpose');
    if (qIdentifier && (qPurpose === 'REGISTER' || qPurpose === 'PASSWORD_RESET')) {
      return { identifier: qIdentifier, purpose: qPurpose };
    }
    return null;
  }, [routerState, searchParams]);

  const canResend = Boolean(
    (state?.purpose === 'REGISTER' && routerState?.registerData) ||
      (state?.purpose === 'PASSWORD_RESET' && routerState?.forgotPasswordData),
  );

  // Pre-fill code from URL if present (email link with ?code=123456)
  const urlCode = searchParams.get('code') || '';
  const [code, setCode] = useState(urlCode);
  const [errorMsg, setErrorMsg] = useState('');
  const verifyOtp = useOtpVerify();

  // Auto-verify when opened from email link with code in URL
  useEffect(() => {
    if (
      !autoVerifyAttempted.current &&
      state?.identifier &&
      urlCode.length === OTP_LENGTH
    ) {
      autoVerifyAttempted.current = true;
      const isEmail = state.identifier.includes('@');
      verifyOtp.mutate(
        {
          code: urlCode,
          ...(isEmail ? { email: state.identifier } : { phone: state.identifier }),
        },
        {
          onError: (error) => {
            const axiosErr = error as AxiosError<ApiError>;
            setErrorMsg(axiosErr.response?.data?.message || t('otp.invalidCode'));
          },
        },
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const resendMutation = useMutation({
    mutationFn: async () => {
      if (routerState?.purpose === 'REGISTER' && routerState.registerData) {
        return authApi.register(routerState.registerData);
      }
      if (routerState?.purpose === 'PASSWORD_RESET' && routerState.forgotPasswordData) {
        return authApi.forgotPassword(routerState.forgotPasswordData);
      }
      throw new Error('Invalid state');
    },
    onSuccess: () => {
      setCode('');
      setErrorMsg('');
      toast.success(t('otp.resend'));
    },
    onError: () => {
      toast.error(t('common:error'));
    },
  });

  const doVerify = useCallback(
    (otpCode: string) => {
      if (!state || otpCode.length !== OTP_LENGTH || verifyOtp.isPending) return;

      setErrorMsg('');
      const isEmail = state.identifier.includes('@');
      verifyOtp.mutate(
        {
          code: otpCode,
          ...(isEmail ? { email: state.identifier } : { phone: state.identifier }),
        },
        {
          onError: (error) => {
            const axiosErr = error as AxiosError<ApiError>;
            setErrorMsg(axiosErr.response?.data?.message || t('otp.invalidCode'));
          },
        },
      );
    },
    [state, verifyOtp, t],
  );

  const handleSubmit = useCallback(() => {
    doVerify(code);
  }, [code, doVerify]);

  const handleComplete = useCallback(
    (completedCode: string) => {
      doVerify(completedCode);
    },
    [doVerify],
  );

  const handleResend = useCallback(() => {
    resendMutation.mutate();
  }, [resendMutation]);

  // No state and no query params - show message
  if (!state?.identifier || !state?.purpose) {
    return (
      <AuthLayout>
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Box
            sx={{
              width: 72,
              height: 72,
              borderRadius: '50%',
              bgcolor: 'error.lighter',
              background: (theme) =>
                `linear-gradient(135deg, ${theme.palette.error.main}15 0%, ${theme.palette.error.main}25 100%)`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              mx: 'auto',
              mb: 3,
            }}
          >
            <ErrorOutlineIcon sx={{ fontSize: 36, color: 'error.main' }} />
          </Box>
          <Typography variant="h6" fontWeight={600} gutterBottom>
            {t('otp.noSession')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3, lineHeight: 1.6 }}>
            {t('otp.noSessionDesc')}
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Button variant="contained" size="large" onClick={() => navigate('/auth/register')}>
              {t('register.title')}
            </Button>
            <Button variant="outlined" size="large" onClick={() => navigate('/auth/login')}>
              {t('login.title')}
            </Button>
          </Box>
        </Box>
      </AuthLayout>
    );
  }

  const maskedIdentifier = state.identifier.includes('@')
    ? state.identifier.replace(/(.{2})(.*)(@)/, '$1***$3')
    : state.identifier.replace(/(\+998\d{2})\d{5}(\d{2})/, '$1*****$2');

  return (
    <AuthLayout>
      <Box sx={{ textAlign: 'center' }}>
        {/* Icon */}
        <Box
          sx={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: (theme) =>
              `linear-gradient(135deg, ${theme.palette.primary.main}15 0%, ${theme.palette.primary.main}30 100%)`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            mx: 'auto',
            mb: 3,
          }}
        >
          <MarkEmailReadOutlinedIcon sx={{ fontSize: 36, color: 'primary.main' }} />
        </Box>

        <Typography variant="h5" fontWeight={700} gutterBottom>
          {t('otp.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1, lineHeight: 1.6 }}>
          {t('otp.subtitle', { identifier: maskedIdentifier })}
        </Typography>

        {/* Show identifier chip */}
        <Box
          sx={{
            display: 'inline-block',
            bgcolor: (theme) => `${theme.palette.primary.main}10`,
            color: 'primary.main',
            borderRadius: 2,
            px: 2,
            py: 0.5,
            mb: 4,
            fontSize: '0.875rem',
            fontWeight: 600,
          }}
        >
          {maskedIdentifier}
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
          <OtpInput
            value={code}
            onChange={setCode}
            onComplete={handleComplete}
            error={verifyOtp.isError}
            disabled={verifyOtp.isPending}
          />

          <Fade in={Boolean(verifyOtp.isError || errorMsg)}>
            <Alert severity="error" sx={{ width: '100%', borderRadius: 2 }}>
              {errorMsg || t('otp.invalidCode')}
            </Alert>
          </Fade>

          <Button
            variant="contained"
            fullWidth
            size="large"
            onClick={handleSubmit}
            disabled={code.length !== OTP_LENGTH || verifyOtp.isPending}
            sx={{
              py: 1.5,
              borderRadius: 2,
              textTransform: 'none',
              fontSize: '1rem',
              fontWeight: 600,
            }}
          >
            {verifyOtp.isPending ? (
              <CircularProgress size={22} color="inherit" />
            ) : (
              t('otp.submit')
            )}
          </Button>

          {canResend && (
            <OtpCountdown onResend={handleResend} isResending={resendMutation.isPending} />
          )}

          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => window.history.back()}
            sx={{ color: 'text.secondary', textTransform: 'none' }}
          >
            {t('otp.back')}
          </Button>
        </Box>
      </Box>
    </AuthLayout>
  );
}

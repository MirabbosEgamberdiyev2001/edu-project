import { useLocation, Navigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Typography, CircularProgress } from '@mui/material';
import LockResetIcon from '@mui/icons-material/LockReset';
import { useTranslation } from 'react-i18next';
import AuthLayout from '../components/AuthLayout';
import PasswordInput from '../components/PasswordInput';
import PasswordStrength from '../components/PasswordStrength';
import { resetPasswordSchema, type ResetPasswordFormData } from '../schemas/resetPasswordSchema';
import { useResetPassword } from '../hooks/useResetPassword';

export default function ResetPasswordPage() {
  const { t } = useTranslation('auth');
  const location = useLocation();
  const resetToken = (location.state as { resetToken?: string })?.resetToken;
  const resetPassword = useResetPassword();

  const {
    control,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  const newPassword = watch('newPassword');

  if (!resetToken) {
    return <Navigate to="/auth/login" replace />;
  }

  const onSubmit = (data: ResetPasswordFormData) => {
    resetPassword.mutate({ token: resetToken, newPassword: data.newPassword });
  };

  return (
    <AuthLayout>
      {/* Icon */}
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box sx={{
          width: 72, height: 72, borderRadius: '50%',
          background: (theme) =>
            `linear-gradient(135deg, ${theme.palette.success.main}20 0%, ${theme.palette.success.main}40 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          mx: 'auto', mb: 2,
        }}>
          <LockResetIcon sx={{ fontSize: 32, color: 'success.main' }} />
        </Box>
        <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5 }}>
          {t('resetPassword.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('resetPassword.subtitle')}
        </Typography>
      </Box>

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Controller
          name="newPassword"
          control={control}
          render={({ field }) => (
            <Box sx={{ mb: 2 }}>
              <PasswordInput
                {...field}
                label={t('resetPassword.newPassword')}
                error={!!errors.newPassword}
                helperText={errors.newPassword?.message ? t(errors.newPassword.message) : ''}
                autoComplete="new-password"
              />
              <PasswordStrength password={field.value || ''} />
            </Box>
          )}
        />

        <Controller
          name="confirmPassword"
          control={control}
          render={({ field }) => (
            <PasswordInput
              {...field}
              label={t('resetPassword.confirmPassword')}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message ? t(errors.confirmPassword.message) : ''}
              autoComplete="new-password"
              sx={{ mb: 3 }}
            />
          )}
        />

        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={resetPassword.isPending}
          sx={{ py: 1.5, borderRadius: 2, textTransform: 'none', fontSize: '1rem', fontWeight: 600 }}
        >
          {resetPassword.isPending ? <CircularProgress size={22} color="inherit" /> : t('resetPassword.submit')}
        </Button>
      </Box>
    </AuthLayout>
  );
}

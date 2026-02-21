import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Typography, Paper, CircularProgress } from '@mui/material';
import SecurityIcon from '@mui/icons-material/Security';
import { useTranslation } from 'react-i18next';
import PasswordInput from '../components/PasswordInput';
import PasswordStrength from '../components/PasswordStrength';
import { changePasswordSchema, type ChangePasswordFormData } from '../schemas/changePasswordSchema';
import { useChangePassword } from '../hooks/useChangePassword';

export default function ChangePasswordPage() {
  const { t } = useTranslation('auth');
  const changePassword = useChangePassword();

  const {
    control,
    handleSubmit,
    formState: { errors },
    watch,
    reset,
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });

  const newPassword = watch('newPassword');

  const onSubmit = (data: ChangePasswordFormData) => {
    changePassword.mutate(
      { currentPassword: data.currentPassword, newPassword: data.newPassword },
      { onSuccess: () => reset() },
    );
  };

  return (
    <Box sx={{ maxWidth: 520, mx: 'auto', mt: 4, px: 2 }}>
      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, sm: 4 },
          borderRadius: 3,
          border: '1px solid',
          borderColor: 'divider',
        }}
      >
        {/* Header */}
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Box sx={{
            width: 64, height: 64, borderRadius: '50%',
            background: 'linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            mx: 'auto', mb: 2,
          }}>
            <SecurityIcon sx={{ fontSize: 28, color: '#7c3aed' }} />
          </Box>
          <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5 }}>
            {t('changePassword.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('changePassword.subtitle')}
          </Typography>
        </Box>

        <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Controller
            name="currentPassword"
            control={control}
            render={({ field }) => (
              <PasswordInput
                {...field}
                label={t('changePassword.currentPassword')}
                error={!!errors.currentPassword}
                helperText={errors.currentPassword?.message ? t(errors.currentPassword.message) : ''}
                autoComplete="current-password"
                sx={{ mb: 2 }}
              />
            )}
          />

          <Controller
            name="newPassword"
            control={control}
            render={({ field }) => (
              <Box sx={{ mb: 2 }}>
                <PasswordInput
                  {...field}
                  label={t('changePassword.newPassword')}
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
                label={t('changePassword.confirmPassword')}
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
            disabled={changePassword.isPending}
            sx={{ py: 1.5 }}
          >
            {changePassword.isPending ? <CircularProgress size={22} color="inherit" /> : t('changePassword.submit')}
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}

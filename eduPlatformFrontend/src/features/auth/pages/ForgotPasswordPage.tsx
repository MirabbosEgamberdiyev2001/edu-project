import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Typography, Link as MuiLink, TextField, CircularProgress } from '@mui/material';
import { Link } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import MailOutlineIcon from '@mui/icons-material/MailOutline';
import { useTranslation } from 'react-i18next';
import AuthLayout from '../components/AuthLayout';
import AuthMethodTabs from '../components/AuthMethodTabs';
import PhoneInput from '../components/PhoneInput';
import { forgotPasswordSchema, type ForgotPasswordFormData } from '../schemas/forgotPasswordSchema';
import { useForgotPassword } from '../hooks/useForgotPassword';

export default function ForgotPasswordPage() {
  const { t } = useTranslation('auth');
  const [method, setMethod] = useState<'email' | 'phone'>('email');
  const forgotPassword = useForgotPassword();

  const {
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { method: 'email', email: '', phone: '' },
  });

  const handleMethodChange = (newMethod: 'email' | 'phone') => {
    setMethod(newMethod);
    reset({ method: newMethod, email: '', phone: '' });
  };

  const onSubmit = (data: ForgotPasswordFormData) => {
    const request = data.method === 'email'
      ? { email: data.email }
      : { phone: data.phone };
    forgotPassword.mutate(request);
  };

  return (
    <AuthLayout>
      {/* Icon */}
      <Box sx={{ textAlign: 'center', mb: 3 }}>
        <Box sx={{
          width: 72, height: 72, borderRadius: '50%',
          background: (theme) =>
            `linear-gradient(135deg, ${theme.palette.warning.light}40 0%, ${theme.palette.warning.light}70 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          mx: 'auto', mb: 2,
        }}>
          <MailOutlineIcon sx={{ fontSize: 32, color: 'warning.dark' }} />
        </Box>
        <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5 }}>
          {t('forgotPassword.title')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('forgotPassword.subtitle')}
        </Typography>
      </Box>

      <AuthMethodTabs value={method} onChange={handleMethodChange} />

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {method === 'email' ? (
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label={t('forgotPassword.email')}
                error={!!errors.email}
                helperText={errors.email?.message ? t(errors.email.message) : ''}
                autoComplete="email"
                sx={{ mb: 3 }}
              />
            )}
          />
        ) : (
          <Controller
            name="phone"
            control={control}
            render={({ field }) => (
              <PhoneInput
                value={field.value || ''}
                onChange={field.onChange}
                label={t('forgotPassword.phone')}
                error={!!errors.phone}
                helperText={errors.phone?.message ? t(errors.phone.message) : ''}
                sx={{ mb: 3 }}
              />
            )}
          />
        )}

        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={forgotPassword.isPending}
          sx={{ py: 1.5, borderRadius: 2, textTransform: 'none', fontSize: '1rem', fontWeight: 600 }}
        >
          {forgotPassword.isPending ? <CircularProgress size={22} color="inherit" /> : t('forgotPassword.submit')}
        </Button>

        <Box sx={{ textAlign: 'center', mt: 3 }}>
          <MuiLink
            component={Link}
            to="/auth/login"
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 0.5,
              fontWeight: 500,
              textDecoration: 'none',
              color: 'text.secondary',
              '&:hover': { color: 'primary.main' },
            }}
          >
            <ArrowBackIcon fontSize="small" />
            {t('forgotPassword.backToLogin')}
          </MuiLink>
        </Box>
      </Box>
    </AuthLayout>
  );
}

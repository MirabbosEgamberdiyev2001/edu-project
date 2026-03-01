import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Typography, Link as MuiLink, TextField, CircularProgress, Divider } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import AuthLayout from '../components/AuthLayout';
import AuthMethodTabs from '../components/AuthMethodTabs';
import GoogleLoginButton from '../components/GoogleLoginButton';
import PasswordInput from '../components/PasswordInput';
import PhoneInput from '../components/PhoneInput';
import { loginSchema, type LoginFormData } from '../schemas/loginSchema';
import { useLogin } from '../hooks/useLogin';

const isAdminPanel = window.location.hostname.startsWith('admin.');

export default function LoginPage() {
  const { t } = useTranslation('auth');
  const [method, setMethod] = useState<'email' | 'phone'>('email');
  const login = useLogin();

  const {
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { method: 'email', email: '', password: '' },
  });

  const handleMethodChange = (newMethod: 'email' | 'phone') => {
    setMethod(newMethod);
    reset({ method: newMethod, email: '', phone: '', password: '' });
  };

  const onSubmit = (data: LoginFormData) => {
    const request = data.method === 'email'
      ? { email: data.email, password: data.password }
      : { phone: data.phone, password: data.password };
    login.mutate(request);
  };

  return (
    <AuthLayout>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5 }}>
        {t('login.title')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {t('login.subtitle')}
      </Typography>

      <AuthMethodTabs value={method} onChange={handleMethodChange} />

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        {method === 'email' ? (
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label={t('login.email')}
                error={!!errors.email}
                helperText={errors.email?.message ? t(errors.email.message) : ''}
                autoComplete="email"
                sx={{ mb: 2.5 }}
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
                label={t('login.phone')}
                error={!!errors.phone}
                helperText={errors.phone?.message ? t(errors.phone.message) : ''}
                sx={{ mb: 2.5 }}
              />
            )}
          />
        )}

        <Controller
          name="password"
          control={control}
          render={({ field }) => (
            <PasswordInput
              {...field}
              label={t('login.password')}
              error={!!errors.password}
              helperText={errors.password?.message ? t(errors.password.message) : ''}
              autoComplete="current-password"
            />
          )}
        />

        <Box sx={{ textAlign: 'right', mt: 1, mb: 3 }}>
          <MuiLink
            component={Link}
            to="/auth/forgot-password"
            variant="body2"
            sx={{ fontWeight: 500, textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
          >
            {t('login.forgotPassword')}
          </MuiLink>
        </Box>

        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={login.isPending}
          sx={{ py: 1.5, borderRadius: 2, textTransform: 'none', fontSize: '1rem', fontWeight: 600 }}
        >
          {login.isPending ? <CircularProgress size={22} color="inherit" /> : t('login.submit')}
        </Button>

        {!isAdminPanel && (
          <>
            <Divider sx={{ my: 3 }}>
              <Typography variant="caption" color="text.secondary">
                {t('common:or')}
              </Typography>
            </Divider>
            <GoogleLoginButton />
            <Typography variant="body2" textAlign="center" sx={{ mt: 3 }}>
              {t('login.noAccount')}{' '}
              <MuiLink
                component={Link}
                to="/auth/register"
                sx={{ fontWeight: 600, textDecoration: 'none' }}
              >
                {t('login.register')}
              </MuiLink>
            </Typography>
          </>
        )}
      </Box>
    </AuthLayout>
  );
}

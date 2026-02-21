import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Typography, Link as MuiLink, TextField, CircularProgress, Divider } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import AuthLayout from '../components/AuthLayout';
import AuthMethodTabs from '../components/AuthMethodTabs';
import PasswordInput from '../components/PasswordInput';
import PasswordStrength from '../components/PasswordStrength';
import PhoneInput from '../components/PhoneInput';
import RoleSelect from '../components/RoleSelect';
import { registerSchema, type RegisterFormData } from '../schemas/registerSchema';
import { useRegister } from '../hooks/useRegister';
import { Role } from '@/types/user';

export default function RegisterPage() {
  const { t } = useTranslation('auth');
  const [method, setMethod] = useState<'email' | 'phone'>('email');
  const register = useRegister();

  const {
    control,
    handleSubmit,
    formState: { errors },
    watch,
    reset,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      method: 'email',
      email: '',
      firstName: '',
      lastName: '',
      password: '',
      confirmPassword: '',
      role: Role.STUDENT,
    },
  });

  const password = watch('password');

  const handleMethodChange = (newMethod: 'email' | 'phone') => {
    setMethod(newMethod);
    reset({
      method: newMethod,
      email: '', phone: '',
      firstName: '', lastName: '',
      password: '', confirmPassword: '',
      role: Role.STUDENT,
    });
  };

  const onSubmit = (data: RegisterFormData) => {
    const request = {
      firstName: data.firstName,
      lastName: data.lastName,
      password: data.password,
      role: data.role,
      ...(data.method === 'email' ? { email: data.email } : { phone: data.phone }),
    };
    register.mutate(request);
  };

  return (
    <AuthLayout>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 0.5 }}>
        {t('register.title')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {t('register.subtitle')}
      </Typography>

      <AuthMethodTabs value={method} onChange={handleMethodChange} />

      <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
          <Controller
            name="firstName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label={t('register.firstName')}
                error={!!errors.firstName}
                helperText={errors.firstName?.message ? t(errors.firstName.message) : ''}
              />
            )}
          />
          <Controller
            name="lastName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label={t('register.lastName')}
                error={!!errors.lastName}
                helperText={errors.lastName?.message ? t(errors.lastName.message) : ''}
              />
            )}
          />
        </Box>

        {method === 'email' ? (
          <Controller
            name="email"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label={t('register.email')}
                error={!!errors.email}
                helperText={errors.email?.message ? t(errors.email.message) : ''}
                autoComplete="email"
                sx={{ mb: 2 }}
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
                label={t('register.phone')}
                error={!!errors.phone}
                helperText={errors.phone?.message ? t(errors.phone.message) : ''}
                sx={{ mb: 2 }}
              />
            )}
          />
        )}

        <Controller
          name="password"
          control={control}
          render={({ field }) => (
            <Box sx={{ mb: 2 }}>
              <PasswordInput
                {...field}
                label={t('register.password')}
                error={!!errors.password}
                helperText={errors.password?.message ? t(errors.password.message) : ''}
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
              label={t('register.confirmPassword')}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword?.message ? t(errors.confirmPassword.message) : ''}
              autoComplete="new-password"
              sx={{ mb: 2 }}
            />
          )}
        />

        <Controller
          name="role"
          control={control}
          render={({ field }) => (
            <RoleSelect
              value={field.value}
              onChange={field.onChange}
              error={!!errors.role}
              helperText={errors.role?.message ? t(errors.role.message) : ''}
            />
          )}
        />

        <Button
          type="submit"
          variant="contained"
          fullWidth
          size="large"
          disabled={register.isPending}
          sx={{ mt: 3, py: 1.5, borderRadius: 2, textTransform: 'none', fontSize: '1rem', fontWeight: 600 }}
        >
          {register.isPending ? <CircularProgress size={22} color="inherit" /> : t('register.submit')}
        </Button>

        <Divider sx={{ my: 3 }}>
          <Typography variant="caption" color="text.secondary">
            {t('common:or')}
          </Typography>
        </Divider>

        <Typography variant="body2" textAlign="center">
          {t('register.hasAccount')}{' '}
          <MuiLink
            component={Link}
            to="/auth/login"
            sx={{ fontWeight: 600, textDecoration: 'none' }}
          >
            {t('register.login')}
          </MuiLink>
        </Typography>
      </Box>
    </AuthLayout>
  );
}
